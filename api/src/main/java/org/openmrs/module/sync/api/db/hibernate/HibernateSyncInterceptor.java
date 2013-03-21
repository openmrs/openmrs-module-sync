/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.sync.api.db.hibernate;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.collection.AbstractPersistentCollection;
import org.hibernate.collection.PersistentList;
import org.hibernate.collection.PersistentMap;
import org.hibernate.collection.PersistentSet;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Projections;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.Type;
import org.openmrs.Cohort;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncException;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncItemKey;
import org.openmrs.module.sync.SyncItemState;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncSubclassStub;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Normalizer;
import org.openmrs.module.sync.serialization.Package;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.notification.Alert;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.RoleConstants;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.util.StringUtils;

/**
 * Implements 'change interception' for data synchronization feature using Hibernate interceptor
 * mechanism. Intercepted changes are recorded into the synchronization journal table in DB.
 * <p>
 * For detailed technical discussion see feature technical documentation on openmrs.org.
 * 
 * @see org.hibernate.EmptyInterceptor
 */
public class HibernateSyncInterceptor extends EmptyInterceptor implements ApplicationContextAware {
	
	/**
	 * Helper container class to store type/value tuple for a given object property. Utilized during
	 * serialization of intercepted entity changes.
	 * 
	 * @see HibernateSyncInterceptor#packageObject(OpenmrsObject, Object[], String[], Type[],
	 *      SyncItemState)
	 */
	protected class PropertyClassValue {
		
		String clazz, value;
		
		public String getClazz() {
			return clazz;
		}
		
		public String getValue() {
			return value;
		}
		
		public PropertyClassValue(String clazz, String value) {
			this.clazz = clazz;
			this.value = value;
		}
	}
	
	/**
	 * From Spring docs: There might be a single instance of Interceptor for a SessionFactory, or a
	 * new instance might be specified for each Session. Whichever approach is used, the interceptor
	 * must be serializable if the Session is to be serializable. This means that
	 * SessionFactory-scoped interceptors should implement readResolve().
	 */
	private static final long serialVersionUID = -4905755656754047400L;
	
	protected final Log log = LogFactory.getLog(HibernateSyncInterceptor.class);
	
	protected SyncService syncService = null;
	
	/*
	 * App context. This is needed to retrieve an instance of current Spring
	 * SessionFactory. There should be a better way to do this but we
	 * collectively couldn't find one.
	 */
	private ApplicationContext context;
	
	static final String sp = "_";
	
	private static ThreadLocal<SyncRecord> syncRecordHolder = new ThreadLocal<SyncRecord>();
	
	private ThreadLocal<Boolean> deactivated = new ThreadLocal<Boolean>();
	
	private ThreadLocal<HashSet<Object>> pendingFlushHolder = new ThreadLocal<HashSet<Object>>();
	
	private ThreadLocal<HashSet<OpenmrsObject>> postInsertModifications = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	public HibernateSyncInterceptor() {
		log.info("Initializing the synchronization interceptor");
	}
	
	/**
	 * Deactivates synchronization. Will be reset on transaction completion or manually.
	 */
	public void deactivateTransactionSerialization() {
		deactivated.set(true);
	}
	
	/**
	 * Re-activates synchronization.
	 */
	public void activateTransactionSerialization() {
		deactivated.remove();
	}
	
	/**
	 * Intercepts the start of a transaction. A new SyncRecord is created for this transaction/
	 * thread to keep track of changes done during the transaction. Kept ThreadLocal.
	 * 
	 * @see org.hibernate.EmptyInterceptor#afterTransactionBegin(org.hibernate.Transaction)
	 */
	@Override
	public void afterTransactionBegin(Transaction tx) {
		if (log.isTraceEnabled())
			log.trace("afterTransactionBegin: " + tx + " deactivated: " + deactivated.get());
		
		if (syncRecordHolder.get() != null) {
			log.warn("Replacing existing SyncRecord in SyncRecord holder");
		}
		
		syncRecordHolder.set(new SyncRecord());
	}
	
	/**
	 * Convenience method to get the {@link SyncService} from the {@link Context}.
	 * 
	 * @return the syncService (may be cached)
	 */
	private SyncService getSyncService() {
		if (syncService == null)
			syncService = Context.getService(SyncService.class);
		
		return syncService;
	}
	
	/**
	 * Intercepts after the transaction is completed, also called on rollback. Clean up any
	 * remaining ThreadLocal objects/reset.
	 * 
	 * @see org.hibernate.EmptyInterceptor#afterTransactionCompletion(org.hibernate.Transaction)
	 */
	@Override
	public void beforeTransactionCompletion(Transaction tx) {
		if (log.isDebugEnabled())
			log.debug("beforeTransactionCompletion: " + tx + " deactivated: " + deactivated.get());
		
		try {
			// If synchronization is NOT deactivated
			if (deactivated.get() == null) {
				SyncRecord record = syncRecordHolder.get();
				syncRecordHolder.remove();
				
				// Does this transaction contain any serialized changes?
				if (record.hasItems()) {
					
					if (log.isDebugEnabled())
						log.debug(record.getItems().size() + " SyncItems in SyncRecord, saving!");
					
					//update the record with any post-insert updates
					if (this.postInsertModifications.get() != null && this.postInsertModifications.get().isEmpty() == false) {
						processPostInsertModifications(record);
					}
					
					// Grab user if we have one, and use the UUID of the user as
					// creator of this SyncRecord
					User user = Context.getAuthenticatedUser();
					if (user != null) {
						record.setCreator(user.getUuid());
					}
					
					// Grab database version
					record.setDatabaseVersion(OpenmrsConstants.OPENMRS_VERSION_SHORT);
					
					// Complete the record
					record.setUuid(SyncUtil.generateUuid());
					if (record.getOriginalUuid() == null) {
						if (log.isInfoEnabled())
							log.info("OriginalUuid is null, so assigning a new UUID: " + record.getUuid());
						record.setOriginalUuid(record.getUuid());
					} else {
						if (log.isInfoEnabled())
							log.info("OriginalUuid is: " + record.getOriginalUuid());
					}
					record.setState(SyncRecordState.NEW);
					record.setTimestamp(new Date());
					record.setRetryCount(0);
					// Save SyncRecord
					getSyncService().createSyncRecord(record, record.getOriginalUuid());
				} else {
					// note: this will happen all the time with read-only
					// transactions
					if (log.isTraceEnabled())
						log.trace("No SyncItems in SyncRecord, save discarded (note: maybe a read-only transaction)!");
				}
			}
		}
		catch (Exception ex) {
			log.error("Journal error\n", ex);
			tx.rollback();//discard any changes
			
			//Create an alert for the user and super users in a new session and transaction
			SessionFactory sf = ((SessionFactory) this.context.getBean("sessionFactory"));
			Session session = SessionFactoryUtils.getNewSession(sf);
			deactivated.set(true);
			
			try {
				Set<User> uniqueUsersToAlert = new HashSet<User>();
				User currentUser = Context.getAuthenticatedUser();
				UserService us = Context.getUserService();
				uniqueUsersToAlert.addAll(us.getUsersByRole(us.getRole(RoleConstants.SUPERUSER)));
				//Dont't send alert to current user if this is being run by daemon user
				if (currentUser != null && !"A4F30A1B-5EB9-11DF-A648-37A07F9C90FB".equalsIgnoreCase(currentUser.getUuid())) {
					uniqueUsersToAlert.add(currentUser);
				}
				
				Transaction newTx = session.beginTransaction();
				String msg = Context.getMessageSourceService().getMessage("sync.record.notSaved");
				session.save(new Alert(msg, uniqueUsersToAlert));
				newTx.commit();
				session.flush();
			}
			catch (Exception e) {
				log.warn("Failed to create an alert after a failed attempt to create a sync record", e);
			}
			finally {
				session.close();
			}
			
			throw (new SyncException("Error in interceptor, see log messages and callstack.", ex));
		}
		finally {
			this.postInsertModifications.remove();
			deactivated.remove();
		}
	}
	
	/**
	 * Packages up deletes and sets the item state to DELETED.
	 * 
	 * @see #packageObject(OpenmrsObject, Object[], String[], Type[], Serializable, SyncItemState)
	 */
	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		
		if (log.isInfoEnabled()) {
			log.info("onDelete: " + entity.getClass().getName());
		}
		
		// first see if entity should be written to the journal at all
		if (!this.shouldSynchronize(entity)) {
			if (log.isDebugEnabled())
				log.debug("Determined entity not to be journaled, exiting onDelete.");
			return;
		}
		
		// create new flush holder if needed
		if (pendingFlushHolder.get() == null) {
			pendingFlushHolder.set(new HashSet<Object>());
		}
		
		// add to flush holder: i.e. indicate there is something to be processed
		if (!pendingFlushHolder.get().contains(entity)) {
			pendingFlushHolder.get().add(entity);
		}
		
		// now package
		packageObject((OpenmrsObject) entity, state, propertyNames, types, id, SyncItemState.DELETED);
		
		return;
		
	}
	
	/**
	 * Called before an object is saved. Triggers in our case for new objects (inserts) Packages up
	 * the changes and sets item state to NEW.
	 * 
	 * @return false if data is unmodified by this interceptor, true if modified. Adding UUIDs to
	 *         new objects that lack them.
	 * @see org.hibernate.EmptyInterceptor#onSave(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (log.isDebugEnabled())
			log.debug("onSave: " + state.toString());
		
		// first see if entity should be written to the journal at all
		if (!this.shouldSynchronize(entity)) {
			if (log.isDebugEnabled()) {
				log.debug("Determined entity not to be journaled, exiting onSave.");
			}
		} else {
			
			// create new flush holder if needed
			if (pendingFlushHolder.get() == null)
				pendingFlushHolder.set(new HashSet<Object>());
			
			if (!pendingFlushHolder.get().contains(entity)) {
				pendingFlushHolder.get().add(entity);
			}
			
			//set-up new set for managing objects in need of post-insert updates
			if (postInsertModifications.get() == null)
				postInsertModifications.set(new HashSet<OpenmrsObject>());
			
			packageObject((OpenmrsObject) entity, state, propertyNames, types, id, SyncItemState.NEW);
		}
		
		// we didn't modify the object, so return false
		return false;
	}
	
	/**
	 * Called before an object is updated in the database. Packages up the changes and sets sync
	 * state to NEW for any objects we care about synchronizing.
	 * 
	 * @return false if data is unmodified by this interceptor, true if modified. Adding UUIDs to
	 *         new objects that lack them.
	 * @see org.hibernate.EmptyInterceptor#onFlushDirty(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
	                            String[] propertyNames, Type[] types) {
		if (log.isDebugEnabled())
			log.debug("onFlushDirty: " + entity.getClass().getName());
		
		// first see if entity should be written to the journal at all
		if (!this.shouldSynchronize(entity)) {
			if (log.isDebugEnabled())
				log.debug("Determined entity not to be journaled, exiting onFlushDirty.");
		} else {
			/*
			 * NOTE: Accomodate Hibernate auto-flush semantics (as best as we
			 * understand them): In case of sync ingest: When processing
			 * SyncRecord with >1 sync item via ProcessSyncRecord() on parent,
			 * calls to get object/update object by uuid may cause auto-flush of
			 * pending updates; this would result in redundant sync items within
			 * a sync record. Use threadLocal HashSet to only keep one instance
			 * of dirty object for single hibernate flush. Note that this is
			 * (i.e. incurring autoflush()) is not normally observed in rest of
			 * openmrs service layer since most of the data change calls are
			 * encapsulated in single transactions.
			 */

			// create new holder if needed
			if (pendingFlushHolder.get() == null)
				pendingFlushHolder.set(new HashSet<Object>());
			
			if (!pendingFlushHolder.get().contains(entity)) {
				pendingFlushHolder.get().add(entity);
			}
			
			//NPE can only happen if flush is called outside of transaction.  SYNC-194.
			if (syncRecordHolder.get() == null)
				log.warn("Unable to save record a flush of " + entity.getClass().getName() + " with id: " + id + " because it occurs outside of the normal transaction boundaries");
			else
				packageObject((OpenmrsObject) entity, currentState, propertyNames, types, id, SyncItemState.UPDATED);
			
		}
		
		// we didn't modify anything, so return false
		return false;
	}
	
	@Override
	public void postFlush(Iterator entities) {
		
		if (log.isDebugEnabled())
			log.debug("postFlush called.");
		
		// clear the holder
		pendingFlushHolder.remove();
	}
	
	/**
	 * Intercept prepared stmts for logging purposes only.
	 * <p>
	 * NOTE: At this point, we are ignoring any prepared statements. This method gets called on any
	 * prepared stmt; meaning selects also which makes handling this reliably difficult.
	 * Fundamentally, short of replaying sql as is on parent, it is difficult to imagine safe and
	 * complete implementation.
	 * <p>
	 * Preferred approach is to weed out all dynamic SQL from openMRS DB layer and if absolutely
	 * necessary, create a hook for DB layer code to Explicitly specify what SQL should be passed to
	 * the parent during synchronization.
	 * 
	 * @see org.hibernate.EmptyInterceptor#onPrepareStatement(java.lang.String)
	 */
	@Override
	public String onPrepareStatement(String sql) {
		if (log.isInfoEnabled())
			log.debug("onPrepareStatement. sql: " + sql);
		
		return sql;
	}
	
	/**
	 * Handles collection remove event. As can be seen in org.hibernate.engine.Collections,
	 * hibernate only calls remove when it is about to recreate a collection.
	 * 
	 * @see org.hibernate.engine.Collections.prepareCollectionForUpdate
	 * @see org.openmrs.api.impl.SyncIngestServiceImpl
	 */
	@Override
	public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
		if (log.isDebugEnabled()) {
			log.debug("no-op: COLLECTION remove with key: " + key);
		}
		
		//no-op
		return;
	}
	
	/**
	 * Handles collection recreate. Recreate is triggered by hibernate when collection object is
	 * replaced by new/different instance.
	 * <p>
	 * remarks: See hibernate AbstractFlushingEventListener and org.hibernate.engine.Collections
	 * implementation to understand how collection updates are hooked up in hibernate, specifically
	 * see Collections.prepareCollectionForUpdate().
	 * 
	 * @see org.hibernate.engine.Collections
	 * @see org.hibernate.event.def.AbstractFlushingEventListener
	 */
	@Override
	public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
		if (log.isDebugEnabled()) {
			log.debug("COLLECTION recreate with key: " + key);
		}
		
		if (!(collection instanceof AbstractPersistentCollection)) {
			log.info("Unsupported collection type; collection must derive from AbstractPersistentCollection,"
			        + " collection type was:" + collection.getClass().getName());
			return;
		}
		;
		
		this.processHibernateCollection((AbstractPersistentCollection) collection, key, "recreate");
		
	}
	
	/**
	 * Handles updates of a collection (i.e. added/removed entries).
	 * <p>
	 * remarks: See hibernate AbstractFlushingEventListener implementation to understand how
	 * collection updates are hooked up in hibernate.
	 * 
	 * @see org.hibernate.engine.Collections
	 * @see org.hibernate.event.def.AbstractFlushingEventListener
	 */
	@Override
	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
		if (log.isDebugEnabled()) {
			log.debug("COLLECTION update with key: " + key);
		}
		
		if (!(collection instanceof AbstractPersistentCollection)) {
			log.info("Unsupported collection type; collection must derive from AbstractPersistentCollection,"
			        + " collection type was:" + collection.getClass().getName());
			return;
		}
		;
		
		this.processHibernateCollection((AbstractPersistentCollection) collection, key, "update");
	}
	
	/**
	 * Serializes and packages an intercepted change in object state.
	 * <p>
	 * IMPORTANT serialization notes:
	 * <p>
	 * Transient Properties. Transients are not serialized/journalled. Marking an object property as
	 * transient is the supported way of designating it as something not to be recorded into the
	 * journal.
	 * <p/>
	 * Hibernate Identity property. A property designated in Hibernate as identity (i.e. primary
	 * key) *is* not serialized. This is because sync does not enforce global uniqueness of database
	 * primary keys. Instead, custom uuid property is used. This allows us to continue to use native
	 * types for 'traditional' entity relationships.
	 * 
	 * @param entity The object changed.
	 * @param currentState Array containing data for each field in the object as they will be saved.
	 * @param propertyNames Array containing name for each field in the object, corresponding to
	 *            currentState.
	 * @param types Array containing Type of the field in the object, corresponding to currentState.
	 * @param state SyncItemState, e.g. NEW, UPDATED, DELETED
	 * @param id Value of the identifier for this entity
	 */
	protected void packageObject(OpenmrsObject entity, Object[] currentState, String[] propertyNames, Type[] types,
	                             Serializable id, SyncItemState state) throws SyncException {
		
		String objectUuid = null;
		String originalRecordUuid = null;
		Set<String> transientProps = null;
		String infoMsg = null;
		SessionFactory factory = null;
		
		ClassMetadata data = null;
		String idPropertyName = null;
		org.hibernate.tuple.IdentifierProperty idPropertyObj = null;
		
		// The container of values to be serialized:
		// Holds tuples of <property-name> -> {<property-type-name>,
		// <property-value as string>}
		HashMap<String, PropertyClassValue> values = new HashMap<String, PropertyClassValue>();
		
		try {

			// boolean isUuidAssigned = assignUUID(entity, currentState,
			// propertyNames, state);
			objectUuid = entity.getUuid();
			
			// pull-out sync-network wide change id for the sync *record* (not
			// the entity itself),
			// if one was already assigned (i.e. this change is coming from some
			// other server)
			if (this.syncRecordHolder.get() != null) {
				originalRecordUuid = this.syncRecordHolder.get().getOriginalUuid();
			}
			
			if (log.isInfoEnabled()) {
				// build up a starting msg for all logging:
				StringBuilder sb = new StringBuilder();
				sb.append("In PackageObject, entity type:");
				sb.append(entity.getClass().getName());
				sb.append(", entity uuid:");
				sb.append(objectUuid);
				sb.append(", originalUuid uuid:");
				sb.append(originalRecordUuid);
				log.info(sb.toString());
			}
			
			// Transient properties are not serialized.
			transientProps = new HashSet<String>();
			for (Field f : entity.getClass().getDeclaredFields()) {
				if (Modifier.isTransient(f.getModifiers())) {
					transientProps.add(f.getName());
					if (log.isInfoEnabled())
						log.info("The field " + f.getName() + " is transient - so we won't serialize it");
				}
			}
			
			/*
			 * Retrieve metadata for this type; we need to determine what is the
			 * PK field for this type. We need to know this since PK values are
			 * *not* journalled; values of primary keys are assigned where
			 * physical DB records are created. This is so to avoid issues with
			 * id collisions.
			 * 
			 * In case of <generator class="assigned" />, the Identifier
			 * property is already assigned value and needs to be journalled.
			 * Also, the prop will *not* be part of currentState,thus we need to
			 * pull it out with reflection/metadata.
			 */
			factory = (SessionFactory) this.context.getBean("sessionFactory");
			data = factory.getClassMetadata(entity.getClass());
			if (data.hasIdentifierProperty()) {
				idPropertyName = data.getIdentifierPropertyName();
				idPropertyObj = ((org.hibernate.persister.entity.AbstractEntityPersister) data).getEntityMetamodel()
				        .getIdentifierProperty();
				
				//DT: the only NativeIfNotAssignedIdentityGenerator pojo in openmrs is Concept, and now that we have MetadataSharing, we DON'T want new Concepts to get sent with their conceptIds.
				//				//Sync-160
				//				if (id == null && 
				//					state == SyncItemState.NEW &&
				//					idPropertyObj.getIdentifierGenerator() instanceof org.openmrs.api.db.hibernate.NativeIfNotAssignedIdentityGenerator) {
				//					//Save the reference to this obj for later
				//					postInsertModifications.get().add(entity);
				//				}
				
				if (id != null && idPropertyObj.getIdentifierGenerator() != null
				        && (idPropertyObj.getIdentifierGenerator() instanceof org.hibernate.id.Assigned
				        //	|| idPropertyObj.getIdentifierGenerator() instanceof org.openmrs.api.db.hibernate.NativeIfNotAssignedIdentityGenerator
				        )) {
					// serialize value as string
					values.put(idPropertyName, new PropertyClassValue(id.getClass().getName(), id.toString()));
				}
			} else if (data.getIdentifierType() instanceof EmbeddedComponentType) {
				// if we have a component identifier type (like AlertRecipient),
				// make
				// sure we include those properties
				EmbeddedComponentType type = (EmbeddedComponentType) data.getIdentifierType();
				for (int i = 0; i < type.getPropertyNames().length; i++) {
					String propertyName = type.getPropertyNames()[i];
					Object propertyValue = type.getPropertyValue(entity, i, org.hibernate.EntityMode.POJO);
					addProperty(values, entity, type.getSubtypes()[i], propertyName, propertyValue, infoMsg);
				}
			}
			
			/*
			 * Loop through all the properties/values and put in a hash for
			 * duplicate removal
			 */
			for (int i = 0; i < types.length; i++) {
				String typeName = types[i].getName();
				if (log.isDebugEnabled())
					log.debug("Processing, type: " + typeName + " Field: " + propertyNames[i]);
				
				if (propertyNames[i].equals(idPropertyName) && log.isInfoEnabled())
					log.info(infoMsg + ", Id for this class: " + idPropertyName + " , value:" + currentState[i]);
				
				if (currentState[i] != null) {
					// is this the primary key or transient? if so, we don't
					// want to serialize
					if (propertyNames[i].equals(idPropertyName)
					        || ("personId".equals(idPropertyName) && "patientId".equals(propertyNames[i]))
					        //|| ("personId".equals(idPropertyName) && "userId".equals(propertyNames[i]))
					        || transientProps.contains(propertyNames[i])) {
						// if (log.isInfoEnabled())
						log.info("Skipping property (" + propertyNames[i]
						        + ") because it's either the primary key or it's transient.");
						
					} else {
						
						addProperty(values, entity, types[i], propertyNames[i], currentState[i], infoMsg);
					}
				} else {
					// current state null -- skip
					if (log.isDebugEnabled())
						log.debug("Field Type: " + typeName + " Field Name: " + propertyNames[i] + " is null, skipped");
				}
			}
			
			/*
			 * Now serialize the data identified and put in the value-map
			 */
			// Setup the serialization data structures to hold the state
			Package pkg = new Package();
			String className = entity.getClass().getName();
			Record xml = pkg.createRecordForWrite(className);
			Item entityItem = xml.getRootItem();
			
			// loop through the map of the properties that need to be serialized
			for (Map.Entry<String, PropertyClassValue> me : values.entrySet()) {
				String property = me.getKey();
				
				// if we are processing onDelete event all we need is uuid
				if ((state == SyncItemState.DELETED) && (!"uuid".equals(property))) {
					continue;
				}
				
				try {
					PropertyClassValue pcv = me.getValue();
					appendRecord(xml, entity, entityItem, property, pcv.getClazz(), pcv.getValue());
				}
				catch (Exception e) {
					String msg = "Could not append attribute. Error while processing property: " + property + " - "
					        + e.getMessage();
					throw (new SyncException(msg, e));
				}
			}
			
			values.clear(); // Be nice to GC
			
			if (objectUuid == null)
				throw new SyncException("uuid is null for: " + className + " with id: " + id);
			
			/*
			 * Create SyncItem and store change in SyncRecord kept in
			 * ThreadLocal.
			 */
			SyncItem syncItem = new SyncItem();
			syncItem.setKey(new SyncItemKey<String>(objectUuid, String.class));
			syncItem.setState(state);
			syncItem.setContent(xml.toStringAsDocumentFragement());
			syncItem.setContainedType(entity.getClass());
			
			if (log.isDebugEnabled())
				log.debug("Adding SyncItem to SyncRecord");
			
			syncRecordHolder.get().addItem(syncItem);
			syncRecordHolder.get().addContainedClass(entity.getClass().getName());
			
			// set the originating uuid for the record: do this once per Tx;
			// else we may end up with empty string
			if (syncRecordHolder.get().getOriginalUuid() == null || "".equals(syncRecordHolder.get().getOriginalUuid())) {
				syncRecordHolder.get().setOriginalUuid(originalRecordUuid);
			}
		}
		catch (SyncException ex) {
			log.error("Journal error\n", ex);
			throw (ex);
		}
		catch (Exception e) {
			log.error("Journal error\n", e);
			throw (new SyncException("Error in interceptor, see log messages and callstack.", e));
		}
		
		return;
	}
	
	/**
	 * Convenience method to add a property to the given list of values to turn into xml
	 * 
	 * @param values
	 * @param entity
	 * @param propertyType
	 * @param propertyName
	 * @param propertyValue
	 * @param infoMsg
	 * @throws Exception
	 */
	private void addProperty(HashMap<String, PropertyClassValue> values, OpenmrsObject entity, Type propertyType,
	                         String propertyName, Object propertyValue, String infoMsg) throws Exception {
		Normalizer n;
		String propertyTypeName = propertyType.getName();
		if ((n = SyncUtil.getNormalizer(propertyTypeName)) != null) {
			// Handle safe types like
			// boolean/String/integer/timestamp via Normalizers
			values.put(propertyName, new PropertyClassValue(propertyTypeName, n.toString(propertyValue)));
		} else if ((n = SyncUtil.getNormalizer(propertyValue.getClass())) != null) {
			values.put(propertyName, new PropertyClassValue(propertyValue.getClass().getName(), n.toString(propertyValue)));
		} else if (propertyType.isCollectionType() && (n = isCollectionOfSafeTypes(entity, propertyName)) != null) {
			// if the property is a list/set/collection AND the members of that
			// collection are a "safe type",
			// then we put the values into the xml
			values.put(propertyName, new PropertyClassValue(propertyTypeName, n.toString(propertyValue)));
		}

		/*
		 * Not a safe type, check if the object implements the OpenmrsObject
		 * interface
		 */
		else if (propertyValue instanceof OpenmrsObject) {
			OpenmrsObject childObject = (OpenmrsObject) propertyValue;
			// child objects are not always loaded if not
			// needed, so let's surround this with try/catch,
			// package only if need to
			String childUuid = null;
			try {
				childUuid = childObject.getUuid();
			}
			catch (LazyInitializationException e) {
				if (log.isWarnEnabled())
					log.warn("Attempted to package/serialize child object, but child object was not yet initialized (and thus was null)");
				if (propertyType.getReturnedClass().equals(User.class)) {
					// Wait - do we still need to do this, now
					// that we have sync bidirectional?
					// If User objects are sync'ing, then why
					// can't these just be uuids?
					// IS THIS RELIABLE??!?
					log.warn("SUBSTITUTED AUTHENTICATED USER FOR ACTUAL USER");
					childUuid = Context.getAuthenticatedUser().getUuid();
					
					// adding this in to test if this ever gets hit and/or
					// should be removed now in the bi-sync world
					throw new APIException("SHOULD NOT BE HERE");
				} else {
					// TODO: abort here also?
					log.error("COULD NOT SUBSTITUTE AUTHENTICATED USER FOR ACTUAL USER");
				}
			}
			catch (Exception e) {
				log.error(infoMsg + ", Could not find child object - object is null, therefore uuid is null");
				throw (e);
			}
			
			/*
			 * child object is OpenmrsObject but its uuid is null, final
			 * attempt: load via PK if PK value available common scenario: this
			 * can happen when people are saving object graphs that are (at
			 * least partially) manually constructed (i.e. setting concept on
			 * obs just by filling in conceptid without first fetching the full
			 * concept state from DB for perf. reasons
			 */
			if (childUuid == null) {
				childUuid = fetchUuid(childObject);
				if (log.isDebugEnabled()) {
					log.debug(infoMsg + "Field was null, attempted to fetch uuid with the following results");
					log.debug("Field type:" + childObject.getClass().getName() + ",uuid:" + childUuid);
				}
			}
			
			if (childUuid != null) {
				values.put(propertyName, new PropertyClassValue(propertyTypeName, childUuid));
			} else {
				String msg = infoMsg + ", Field value should be synchronized, but uuid is null.  Field Type: "
				        + propertyType + " Field Name: " + propertyName;
				log.error(msg);
				throw (new SyncException(msg));
			}
		} else {
			// state != null but it is not safetype or
			// implements OpenmrsObject: do not package and log
			// as info
			if (log.isInfoEnabled())
				log.info(infoMsg + ", Field Type: " + propertyType + " Field Name: " + propertyName
				        + " is not safe or OpenmrsObject, skipped!");
		}
		
	}
	
	/**
	 * Checks the collection to see if it is a collection of supported types. If so, then it returns
	 * appropriate normalizer. Note, this handles maps too.
	 * 
	 * @param object
	 * @param propertyName
	 * @return a Normalizer for the given type or null if not a safe type
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	private Normalizer isCollectionOfSafeTypes(OpenmrsObject object, String propertyName) throws SecurityException,
	                                                                                     NoSuchFieldException {
		try {
			
			java.lang.reflect.ParameterizedType collectionType = ((java.lang.reflect.ParameterizedType) object.getClass()
			        .getDeclaredField(propertyName).getGenericType());
			if (Map.class.isAssignableFrom((Class) collectionType.getRawType())) {
				//this is a map; Map<K,V>: verify that K and V are of types we know how to process
				java.lang.reflect.Type keyType = collectionType.getActualTypeArguments()[0];
				java.lang.reflect.Type valueType = collectionType.getActualTypeArguments()[1];
				Normalizer keyNormalizer = SyncUtil.getNormalizer((Class) keyType);
				Normalizer valueNormalizer = SyncUtil.getNormalizer((Class) valueType);
				if (keyNormalizer != null && valueNormalizer != null) {
					return SyncUtil.getNormalizer((Class) collectionType.getRawType());
				} else {
					return null;
				}
			} else {
				//this is some other collection, so just get a normalizer for its 
				return SyncUtil.getNormalizer((Class) (collectionType.getActualTypeArguments()[0]));
			}
			
		}
		catch (Throwable t) {
			// might get here if the property is on a superclass to the object
			
			log.trace("Unable to get collection field: " + propertyName + " from object " + object.getClass()
			        + " for some reason", t);
		}
		
		// on errors just return null
		return null;
	}
	
	/**
	 * Adds a property value to the existing serialization record as a string.
	 * <p>
	 * If data is null it will be skipped, no empty serialization items are written. In case of xml
	 * serialization, the data will be serialized as: &lt;property
	 * type='classname'&gt;data&lt;/property&gt;
	 * 
	 * @param xml record node to append to
	 * @param entity the object holding the given property
	 * @param parent the pointer to the root parent node
	 * @param property new item name (in case of xml serialization this will be child element name)
	 * @param classname type of the property, will be recorded as attribute named 'type' on the
	 *            child item
	 * @param data String content, in case of xml serialized as text node (i.e. not CDATA)
	 * @throws Exception
	 */
	protected void appendRecord(Record xml, OpenmrsObject entity, Item parent, String property, String classname, String data)
	                                                                                                                          throws Exception {
		// if (data != null && data.length() > 0) {
		// this will break if we don't allow data.length==0 - some string values
		// are required NOT NULL, but can be blank
		if (data != null) {
			Item item = xml.createItem(parent, property);
			item.setAttribute("type", classname);
			data = transformItemForSyncRecord(item, entity, property, data);
			xml.createText(item, data);
		}
	}
	
	/**
	 * Called while saving a SyncRecord to allow for manipulating what is stored. The impl of this
	 * method transforms the {@link PersonAttribute#getValue()} and {@link Obs#getVoidReason()}
	 * methods to not reference primary keys. (Instead the uuid is referenced and then dereferenced
	 * before being saved). If no transformation is to take place, the data is returned as given.
	 * 
	 * @param item the serialized sync item associated with this record
	 * @param entity the OpenmrsObject containing the property
	 * @param property the property name
	 * @param data the current value for the
	 * @return the transformed (or unchanged) data to save in the SyncRecord
	 */
	public String transformItemForSyncRecord(Item item, OpenmrsObject entity, String property, String data) {
		// data will not be null here, so NPE checks are not needed
		
		if (entity instanceof PersonAttribute && "value".equals(property)) {
			PersonAttribute attr = (PersonAttribute) entity;
			// use PersonAttributeType.format to get the uuid
			if (attr.getAttributeType() == null)
				throw new SyncException("Unable to find person attr type on attr with uuid: " + entity.getUuid());
			String className = attr.getAttributeType().getFormat();
			try {
				Class c = Context.loadClass(className);
				item.setAttribute("type", className);
				
				// An empty string represents an empty value. Return it as the UUID does not exist.
				if ((data.trim()).isEmpty())
					return data;
				
				// only convert to uuid if this is an OpenMrs object
				// otherwise, we are just storing a simple String or Integer
				// value
				if (OpenmrsObject.class.isAssignableFrom(c)) {
					String valueObjectUuid = fetchUuid(c, Integer.valueOf(data));
					return valueObjectUuid;
				}
			}
			catch (Throwable t) {
				log.warn("Unable to get class of type: " + className + " for sync'ing attribute.value column", t);
			}
		} else if (entity instanceof PersonAttributeType && "foreignKey".equals(property)) {
			if (StringUtils.hasLength(data)) {
				PersonAttributeType attrType = (PersonAttributeType) entity;
				String className = attrType.getFormat();
				try {
					Class c = Context.loadClass(className);
					String foreignKeyObjectUuid = fetchUuid(c, Integer.valueOf(data));
					
					// set the class name on this to be the uuid-ized type
					// instead of java.lang.Integer.
					// the SyncUtil.valForField method will handle changing this
					// back to an integer
					item.setAttribute("type", className);
					return foreignKeyObjectUuid;
				}
				catch (Throwable t) {
					log.warn("Unable to get class of type: " + className + " for sync'ing foreignKey column", t);
				}
			}
		} else if (entity instanceof Obs && "voidReason".equals(property)) {
			if (data.contains("(new obsId: ")) {
				// rip out the obs id and replace it with a uuid
				String voidReason = String.copyValueOf(data.toCharArray()); // copy
				// the
				// string
				// so
				// that
				// we're
				// operating
				// on
				// a
				// new
				// object
				int start = voidReason.lastIndexOf(" ") + 1;
				int end = voidReason.length() - 1;
				String obsId = voidReason.substring(start, end);
				try {
					String newObsUuid = fetchUuid(Obs.class, Integer.valueOf(obsId));
					return data.substring(0, data.lastIndexOf(" ")) + " " + newObsUuid + ")";
				}
				catch (Exception e) {
					log.trace("unable to get uuid from obs pk: " + obsId, e);
				}
			}
		} else if (entity instanceof Cohort && "memberIds".equals(property)) {
			// convert integer patient ids to uuids
			try {
				item.setAttribute("type", "java.util.Set<org.openmrs.Patient>");
				StringBuilder sb = new StringBuilder();
				
				data = data.replaceFirst("\\[", "").replaceFirst("\\]", "");
				
				sb.append("[");
				String[] fieldVals = data.split(",");
				for (int x = 0; x < fieldVals.length; x++) {
					if (x >= 1)
						sb.append(", ");
					
					String eachFieldVal = fieldVals[x].trim(); // take out whitespace
					String uuid = fetchUuid(Patient.class, Integer.valueOf(eachFieldVal));
					sb.append(uuid);
					
				}
				
				sb.append("]");
				
				return sb.toString();
				
			}
			catch (Throwable t) {
				log.warn("Unable to get Patient for sync'ing cohort.memberIds property", t);
			}
			
		}
		
		return data;
	}
	
	/**
	 * Determines if entity is to be 'synchronized'. There are two ways this can happen:
	 * <p>
	 * 1. Entity implements OpenmrsObject interface.
	 * <p>
	 * 2. Interceptor supports manual override to suspend synchronization by setting the deactivated
	 * bit (see {@link #deactivateTransactionSerialization()}). This option is provided only for
	 * rare occasions when previous methods are not sufficient (i.e suspending interception in case
	 * of inline sql).
	 * 
	 * @param entity Object to examine.
	 * @return true if entity should be synchronized, else false.
	 * @see org.openmrs.synchronization.OpenmrsObject
	 * @see org.openmrs.synchronization.OpenmrsObjectInstance
	 */
	protected boolean shouldSynchronize(Object entity) {
		
		Boolean ret = true;
		
		// check if this object is to be sync-ed: compare against the configured classes
		// for time being, suspend any flushing -- we are in the middle of hibernate stack
		SessionFactory factory = (SessionFactory) this.context.getBean("sessionFactory");
		org.hibernate.FlushMode flushMode = factory.getCurrentSession().getFlushMode();
		factory.getCurrentSession().setFlushMode(org.hibernate.FlushMode.MANUAL);
		
		try {
			ret = this.getSyncService().shouldSynchronize(entity);
		}
		catch (Exception ex) {
			log.warn("Journal error\n", ex);
			//log error info as warning but continue on
		}
		finally {
			if (factory != null) {
				factory.getCurrentSession().setFlushMode(flushMode);
			}
		}
		
		// finally, if 'deactivated' bit was set manually for the whole sync, return accordingly
		if (deactivated.get() != null)
			ret = false;
		
		return ret;
	}
	
	/**
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}
	
	/**
	 * Retrieves uuid of OpenmrsObject instance from the storage based on identity value (i.e. PK).
	 * <p>
	 * Remarks: It is important for the implementation to avoid loading obj into session while
	 * trying to determine its uuid. As a result, the implementation uses the combination of
	 * reflection to determine the object's identifier value and Hibernate criteria in order to
	 * build select statement for getting the uuid. The reason to avoid fetching the obj is because
	 * doing it causes an error in hibernate when processing disconnected proxies. Specifically,
	 * during obs edit, several properties are are disconnected as the form controller uses Criteria
	 * object to construct select queury session.clear() and then session.merge(). Finally,
	 * implementation suspends any state flushing to avoid any weird auto-flush events being
	 * triggered while select is being executed.
	 * 
	 * @param obj Instance of OpenmrsObject for which to retrieve uuid for.
	 * @return uuid from storage if obj identity value is set, else null.
	 * @see ForeignKeys
	 */
	protected String fetchUuid(OpenmrsObject obj) {
		String uuid = null;
		Object idPropertyValue = null;
		Method m = null;
		
		// what are you doing to me?!
		if (obj == null)
			return null;
		
		try {
			
			SessionFactory factory = (SessionFactory) this.context.getBean("sessionFactory");
			Class objTrueType = null;
			if (obj instanceof HibernateProxy) {
				objTrueType = org.hibernate.proxy.HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
			} else {
				objTrueType = obj.getClass();
			}
			
			try {
				idPropertyValue = obj.getId();
			}
			catch (Throwable t) {
				log.debug("Unable to get internal identifier for obj: " + obj, t);
				
				// ClassMetadata is only available for entities configured in
				// hibernate
				ClassMetadata data = factory.getClassMetadata(objTrueType);
				if (data != null) {
					String idPropertyName = data.getIdentifierPropertyName();
					if (idPropertyName != null) {
						
						m = SyncUtil.getGetterMethod(objTrueType, idPropertyName);
						if (m != null) {
							idPropertyValue = m.invoke(obj, (Object[]) null);
						}
					}
				}
			}
			
			uuid = fetchUuid(objTrueType, idPropertyValue);
			
		}
		catch (Exception ex) {
			// something went wrong - no matter just return null
			uuid = null;
			log.warn("Error in fetchUuid: returning null", ex);
		}
		
		return uuid;
	}
	
	/**
	 * See {@link #fetchUuid(OpenmrsObject)}
	 * 
	 * @param objTrueType
	 * @param idPropertyValue
	 * @return
	 */
	protected String fetchUuid(Class objTrueType, Object idPropertyValue) {
		String uuid = null;
		
		// for time being, suspend any flushing
		SessionFactory factory = (SessionFactory) this.context.getBean("sessionFactory");
		org.hibernate.FlushMode flushMode = factory.getCurrentSession().getFlushMode();
		factory.getCurrentSession().setFlushMode(org.hibernate.FlushMode.MANUAL);
		
		try {
			// try to fetch the instance and get its uuid
			if (idPropertyValue != null) {
				// build sql to fetch uuid - avoid loading obj into session
				org.hibernate.Criteria criteria = factory.getCurrentSession().createCriteria(objTrueType);
				criteria.add(Expression.idEq(idPropertyValue));
				criteria.setProjection(Projections.property("uuid"));
				uuid = (String) criteria.uniqueResult();
				
				if (uuid == null)
					log.warn("Unable to find obj of type: " + objTrueType + " with primary key: " + idPropertyValue);
				
				return uuid;
			}
		}
		finally {
			if (factory != null) {
				factory.getCurrentSession().setFlushMode(flushMode);
			}
		}
		
		return null;
	}
	
	/**
	 * Processes changes to hibernate collections. At the moment, only persistent sets are
	 * supported.
	 * <p>
	 * Remarks: Note that simple lists and maps of primitive types are supported also by default via
	 * normalizers and do not require explicit handling as shown here for sets of any reference
	 * types.
	 * <p>
	 * 
	 * @param collection Instance of Hibernate AbstractPersistentCollection to process.
	 * @param key key of owner for the collection.
	 * @param action hibernate 'action' being performed: update, recreate. note, deletes are handled
	 *            via re-create
	 */
	protected void processHibernateCollection(AbstractPersistentCollection collection, Serializable key, String action) {
		
		if (!(collection instanceof PersistentSet || collection instanceof PersistentMap || collection instanceof PersistentList)) {
			log.info("Unsupported collection type, collection type was:" + collection.getClass().getName());
			return;
		}

		
		OpenmrsObject owner = null;
		String originalRecordUuid = null;
		SessionFactory factory = null;
		LinkedHashMap<String, OpenmrsObject> entriesHolder = null;
		
		// we only process recreate and update
		if (!"update".equals(action) && !"recreate".equals(action)) {
			log.error("Unexpected 'action' supplied, valid values: recreate, update. value provided: " + action);
			throw new CallbackException("Unexpected 'action' supplied while processing a persistent set.");
		}
		
		// retrieve owner and original uuid if there is one
		if (collection.getOwner() instanceof OpenmrsObject) {
			owner = (OpenmrsObject) collection.getOwner();
			
			if (!this.shouldSynchronize(owner)) {
				if (log.isDebugEnabled())
					log.debug("Determined entity not to be journaled, exiting onDelete.");
				return;
			}
			
			if (syncRecordHolder.get() != null) {
				originalRecordUuid = syncRecordHolder.get().getOriginalUuid();
			}
			
		} else {
			log.info("Cannot process collection where owner is not OpenmrsObject.");
			return;
		}
		
		factory = (SessionFactory) this.context.getBean("sessionFactory");
		
		/*
		 * determine if this set needs to be processed. Process if: 1. it is
		 * recreate or 2. is dirty && current state does not equal stored
		 * snapshot
		 */
		boolean process = false;
		if ("recreate".equals(action)) {
			process = true;
		} else {
			if (collection.isDirty()) {
				org.hibernate.persister.collection.CollectionPersister persister = ((org.hibernate.engine.SessionFactoryImplementor) factory)
				        .getCollectionPersister(collection.getRole());
				Object ss = null;
				try { // code around hibernate bug:
					  // http://opensource.atlassian.com/projects/hibernate/browse/HHH-2937
					ss = collection.getSnapshot(persister);
				}
				catch (NullPointerException ex) {}
				if (ss == null) {
					log.debug("snapshot is null");
					if (collection.empty())
						process = false;
					else
						process = true;
				} else if (!collection.equalsSnapshot(persister)) {
					process = true;
				}
				;
			}
			
			if (!process) {
				log.info("set processing, no update needed: not dirty or current state and snapshots are same");
			}
		}
		if (!process)
			return;
		
		// pull out the property name on owner that corresponds to the collection
		ClassMetadata data = factory.getClassMetadata(owner.getClass());
		String[] propNames = data.getPropertyNames();
		// this is the name of the property on owner object that contains the set
		String ownerPropertyName = null;
		
		for (String propName : propNames) {
			Object propertyVal = data.getPropertyValue(owner, propName, org.hibernate.EntityMode.POJO);
			// note: test both with equals() and == because
			// PersistentSet.equals()
			// actually does not handle equality of two persistent sets well
			if (collection == propertyVal || collection.equals(propertyVal)) {
				ownerPropertyName = propName;
				break;
			}
		}
		if (ownerPropertyName == null) {
			log.error("Could not find the property on owner object that corresponds to the collection being processed.");
			log.error("owner info: \ntype: " + owner.getClass().getName() + ", \nuuid: " + owner.getUuid()
			        + ",\n property name for collection: " + ownerPropertyName);
			throw new CallbackException(
			        "Could not find the property on owner object that corresponds to the collection being processed.");
		}
		
		//now we know this needs to be processed. Proceed accordingly:
		if (collection instanceof PersistentSet || collection instanceof PersistentList) {
			processPersistentCollection(collection, key, action, originalRecordUuid, owner, ownerPropertyName);
		}
		
		return;
	}
	
	/**
	 * Processes changes to persistent collection that contains instances of OpenmrsObject objects.
	 * <p>
	 * Remarks:
	 * <p>
	 * Xml 'schema' for the sync item content for the persisted collection follows. Note that for persisted
	 * collections syncItemKey is a composite of owner object uuid and the property name that contains the
	 * collection. <br/>
	 * &lt;persistent-collection&gt; element: wrapper element <br/>
	 * &lt;owner uuid='' propertyName='' type='' action='recreate|update' &gt; element: this
	 * captures the information about the object that holds reference to the collection being
	 * processed <br/>
	 * -uuid: owner object uuid <br/>
	 * -properyName: names of the property on owner object that holds this collection <br/>
	 * -type: owner class name <br/>
	 * -action: recreate, update -- these are collection events defined by hibernate interceptor <br/>
	 * &lt;entry action='update|delete' uuid='' type='' &gt; element: this captures info about
	 * individual collection entries: <br/>
	 * -action: what is being done to this item of the collection: delete (item was removed from the
	 * collection) or update (item was added to the collection) <br/>
	 * -uuid: entry's uuid <br/>
	 * -type: class name
	 * 
	 * @param collection Instance of Hibernate AbstractPersistentCollection to process.
	 * @param key key of owner for the collection.
	 * @param action action being performed on the collection: update, recreate
	 */
	private void processPersistentCollection(AbstractPersistentCollection collection, Serializable key, String action, String originalRecordUuid,
	                                  OpenmrsObject owner, String ownerPropertyName) {
		
		SessionFactory factory = null;
		LinkedHashMap<String, OpenmrsObject> entriesHolder = null;
		
		factory = (SessionFactory) this.context.getBean("sessionFactory");
		
		// Setup the serialization data structures to hold the state
		Package pkg = new Package();
		entriesHolder = new LinkedHashMap<String, OpenmrsObject>();
		try {
			
			// find out what entries need to be serialized
			for (Object entry : (Iterable)collection) {
				if (entry instanceof OpenmrsObject) {
					OpenmrsObject obj = (OpenmrsObject) entry;
					
					// attempt to retrieve entry uuid
					String entryUuid = obj.getUuid();
					if (entryUuid == null) {
						entryUuid = fetchUuid(obj);
						if (log.isDebugEnabled()) {
							log.debug("Entry uuid was null, attempted to fetch uuid with the following results");
							log.debug("Entry type:" + obj.getClass().getName() + ",uuid:" + entryUuid);
						}
					}
					// well, this is messed up: have an instance of
					// OpenmrsObject but has no uuid
					if (entryUuid == null) {
						log.error("Cannot handle collection entries where uuid is null.");
						throw new CallbackException("Cannot handle collection entries where uuid is null.");
					}
					
					// add it to the holder to avoid possible duplicates: key =
					// uuid + action
					entriesHolder.put(entryUuid + "|update", obj);
				} else if (SyncUtil.getNormalizer(entry.getClass()) == null) {
					log.warn("Cannot handle collections where entries are not OpenmrsObject here. Type was " + entry.getClass()
					        + " in property " + ownerPropertyName + " in class " + owner.getClass());
					// skip out early because we don't want to write any xml for it
					// it was handled by the normal property writer hopefully
					return;
				} else {
					// don't do anything else (recreating/packaging) with these collections
					return;
				}
			}
			
			// add on deletes
			if (!"recreate".equals(action) && collection.getRole() != null) {
				org.hibernate.persister.collection.CollectionPersister persister = ((org.hibernate.engine.SessionFactoryImplementor) factory)
				        .getCollectionPersister(collection.getRole());
				Iterator it = collection.getDeletes(persister, false);
				if (it != null) {
					while (it.hasNext()) {
						Object entryDelete = it.next();
						if (entryDelete instanceof OpenmrsObject) {
							OpenmrsObject objDelete = (OpenmrsObject) entryDelete;
							// attempt to retrieve entry uuid
							String entryDeleteUuid = objDelete.getUuid();
							if (entryDeleteUuid == null) {
								entryDeleteUuid = fetchUuid(objDelete);
								if (log.isDebugEnabled()) {
									log.debug("Entry uuid was null, attempted to fetch uuid with the following results");
									log.debug("Entry type:" + entryDeleteUuid.getClass().getName() + ",uuid:"
									        + entryDeleteUuid);
								}
							}
							// well, this is messed up: have an instance of
							// OpenmrsObject but has no uuid
							if (entryDeleteUuid == null) {
								log.error("Cannot handle collection delete entries where uuid is null.");
								throw new CallbackException("Cannot handle collection delete entries where uuid is null.");
							}
							
							// add it to the holder to avoid possible
							// duplicates: key = uuid + action
                            // also, only add if there is no update action for the same object: see SYNC-280
                            if (!entriesHolder.containsKey(entryDeleteUuid + "|update")) {
							    entriesHolder.put(entryDeleteUuid + "|delete", objDelete);
                            }
							
						} else {
							// TODO: more debug info
							log.warn("Cannot handle collections where entries are not OpenmrsObject!");
							// skip out early because we don't want to write any
							// xml for it. it
							// was handled by the normal property writer
							// hopefully
							return;
						}
					}
				}
			}
			
			/*
			 * Create SyncItem and store change in SyncRecord kept in
			 * ThreadLocal. note: when making SyncItemKey, make it a composite
			 * string of uuid + prop. name to avoid collisions with updates to
			 * parent object or updates to more than one collection on same
			 * owner
			 */

			// Setup the serialization data structures to hold the state
			Record xml = pkg.createRecordForWrite(collection.getClass().getName());
			Item entityItem = xml.getRootItem();
			
			// serialize owner info: we will need type, prop name where collection
			// goes, and owner uuid
			Item item = xml.createItem(entityItem, "owner");
			item.setAttribute("type", this.getType(owner));
			item.setAttribute("properyName", ownerPropertyName);
			item.setAttribute("action", action);
			item.setAttribute("uuid", owner.getUuid());
			
			// build out the xml for the item content
			Boolean hasNoAutomaticPrimaryKey = null;
			String type = null;
			for (String entryKey : entriesHolder.keySet()) {
				OpenmrsObject entryObject = entriesHolder.get(entryKey);
				if (type == null) {
					type = this.getType(entryObject);
					hasNoAutomaticPrimaryKey = SyncUtil.hasNoAutomaticPrimaryKey(type);
				}
				
				Item temp = xml.createItem(entityItem, "entry");
				temp.setAttribute("type", type);
				temp.setAttribute("action", entryKey.substring(entryKey.indexOf('|') + 1));
				temp.setAttribute("uuid", entryObject.getUuid());
				if (hasNoAutomaticPrimaryKey) {
					temp.setAttribute("primaryKey", syncService.getPrimaryKey(entryObject));
				}
			}
			
			SyncItem syncItem = new SyncItem();
			syncItem.setKey(new SyncItemKey<String>(owner.getUuid() + "|" + ownerPropertyName, String.class));
			syncItem.setState(SyncItemState.UPDATED);
			syncItem.setContainedType(collection.getClass());
			syncItem.setContent(xml.toStringAsDocumentFragement());
			
			syncRecordHolder.get().addOrRemoveAndAddItem(syncItem);
			syncRecordHolder.get().addContainedClass(owner.getClass().getName());
			
			// do the original uuid dance, same as in packageObject
			if (syncRecordHolder.get().getOriginalUuid() == null || "".equals(syncRecordHolder.get().getOriginalUuid())) {
				syncRecordHolder.get().setOriginalUuid(originalRecordUuid);
			}
		}
		catch (Exception ex) {
			log.error("Error processing Persistent collection, see callstack and inner expection", ex);
			throw new CallbackException("Error processing Persistent collection, see callstack and inner expection.", ex);
		}
	}
	
	/**
	 * Returns string representation of type for given object. The main idea is to strip off the
	 * hibernate proxy info, if it happens to be present.
	 * 
	 * @param obj object
	 * @return
	 */
	private String getType(Object obj) {
		
		// be defensive about it
		if (obj == null) {
			throw new CallbackException("Error trying to determine type for object; object is null.");
		}
		
		Object concreteObj = obj;
		if (obj instanceof org.hibernate.proxy.HibernateProxy) {
			concreteObj = ((HibernateProxy) obj).getHibernateLazyInitializer().getImplementation();
		}
		
		return concreteObj.getClass().getName();
	}
	
	/**
	 * Sets the originating uuid for the sync record. This is done once per Tx; else we may end up
	 * with an empty string. NOTE: This code is needed because we need for entity to know if it is
	 * genuine local change or it is coming from the ingest code. This is what original_uuid field
	 * in sync_journal is used for. The way sync record uuids from ingest code are passed to the
	 * interceptor is by calling this method: the *1st* thing ingest code will do when processing
	 * changes is to issue this call to let interceptor know we are about to process ingest changes.
	 * This is done so that the intercetor can pull out the 'original' record uuid associated with
	 * the change to the entity that is being processed. Since ingest and interceptor do not have
	 * direct reference to each other, there is no simple way to pass this info directly. Note that
	 * this technique *relies* on the fact that syncRecordHolder is ThreadLocal; in other words, the
	 * uuid is passed and stored on the 'stack' by using thread local storage to ensure that
	 * multiple calling worker threads that maybe ingesting records concurrently are storring their
	 * respective record state locally. More: read javadoc on SyncRecord to see what
	 * SyncRecord.OriginalUuid is and how it is used.
	 * 
	 * @param originalRecordUuid String representing value of orig record uuid
	 */
	public static void setOriginalRecordUuid(String originalRecordUuid) {
		
		if (syncRecordHolder.get() != null) {
			if (syncRecordHolder.get().getOriginalUuid() == null || "".equals(syncRecordHolder.get().getOriginalUuid())) {
				//orig record uuid not filled in yet, do so now
				syncRecordHolder.get().setOriginalUuid(originalRecordUuid);
			} else {
				//no-op the orig record info is already set
				//TODO: perhaps we need to do the comparison here and if the orig uuid is being
				//overriden by something else then thrown an exception?
				
			}
		}
		return;
	}
	
	/**
	 * Clears out the orginal record uuid from the pending record by setting it to null. This can
	 * happen if the current Tx is being aborted for some reason. Technically, if the Tx is being
	 * aborted then the pending record will never be saved, that is call to
	 * beforeTransactionCompletion() where we attempt to save the record never happens. This method
	 * is however provided for completeness and defensive coding in
	 * SyncIngestServiceImpl.processSyncItem() that does cleanup as the exception resulting in
	 * abort(s) are behing raised.
	 * 
	 * @see org.openmrs.module.sync.api.SyncIngestServiceImpl#processSyncItem(org.openmrs.module.sync.SyncItem,
	 *      java.lang.String, java.util.Map)
	 */
	public static void clearOriginalRecordUuid() {
		
		if (syncRecordHolder.get() != null) {
			//clear out the value from the pending record
			syncRecordHolder.get().setOriginalUuid(null);
		}
		return;
	}
	
	/**
	 * Adds syncItem to pending sync record for the patient stub necessary to handle new patient
	 * from the existing user scenario. See {@link SyncSubclassStub} class comments for detailed description
	 * of how this works.
	 * 
	 * @see SyncSubclassStub
	 */
	public static void addSyncItemForSubclassStub(SyncSubclassStub stub) {
		
		try {
			
			// Setup the serialization data structures to hold the state
			Package pkg = new Package();
			String className = stub.getClass().getName();
			Record xml = pkg.createRecordForWrite(className);
			Item parentItem = xml.getRootItem();
			Item item = null;
			
			//uuid
			item = xml.createItem(parentItem, "uuid");
			item.setAttribute("type", stub.getUuid().getClass().getName());
			xml.createText(item, stub.getUuid());
			
			//requiredColumnNames
			item = xml.createItem(parentItem, "requiredColumnNames");
			item.setAttribute("type", "java.util.List<java.lang.String>");
			String value = "[";
			for (int x=0; x < stub.getRequiredColumnNames().size(); x++) {
				if (x != 0)
					value += ",";
				//value += "\"" + stub.getRequiredColumnNames().get(x) + "\"";
				value += stub.getRequiredColumnNames().get(x);
			}
			value += "]";
			
			xml.createText(item, value);
			
			//requiredColumnValues
			item = xml.createItem(parentItem, "requiredColumnValues");
			item.setAttribute("type", "java.util.List<java.lang.String>");
			value = "[";
			for (int x=0; x < stub.getRequiredColumnValues().size(); x++) {
				String columnvalue = stub.getRequiredColumnValues().get(x);
				if (x != 0)
					value += ",";
				value += columnvalue;
			}
			value += "]";
			xml.createText(item, value);
			
			//requiredColumnClasses
			item = xml.createItem(parentItem, "requiredColumnClasses");
			item.setAttribute("type", "java.util.List<java.lang.String>");
			value = "[";
			for (int x=0; x < stub.getRequiredColumnClasses().size(); x++) {
				String columnvalue = stub.getRequiredColumnClasses().get(x);
				if (x != 0)
					value += ",";
				//value += "\"" + columnvalue + "\"";
				value += columnvalue;
			}
			value += "]";
			xml.createText(item, value);
			
			//parentTable
			item = xml.createItem(parentItem, "parentTable");
			item.setAttribute("type", stub.getParentTable().getClass().getName());
			xml.createText(item, stub.getParentTable());
			//parentTableId
			item = xml.createItem(parentItem, "parentTableId");
			item.setAttribute("type", stub.getParentTableId().getClass().getName());
			xml.createText(item, stub.getParentTableId());
			//subclassTable
			item = xml.createItem(parentItem, "subclassTable");
			item.setAttribute("type", stub.getSubclassTable().getClass().getName());
			xml.createText(item, stub.getSubclassTable());
			//subclassTableId
			item = xml.createItem(parentItem, "subclassTableId");
			item.setAttribute("type", stub.getSubclassTableId().getClass().getName());
			xml.createText(item, stub.getSubclassTableId());
			
			SyncItem syncItem = new SyncItem();
			syncItem.setKey(new SyncItemKey<String>(stub.getUuid(), String.class));
			syncItem.setState(SyncItemState.NEW);
			syncItem.setContent(xml.toStringAsDocumentFragement());
			syncItem.setContainedType(stub.getClass());
			
			syncRecordHolder.get().addItem(syncItem);
			syncRecordHolder.get().addContainedClass(stub.getClass().getName());
			
		}
		catch (SyncException syncEx) {
			//just rethrow it
			throw (syncEx);
		}
		catch (Exception e) {
			throw (new SyncException("Unknow error while creating patient stub for patient uuid: " + stub.getUuid(), e));
		}
		
		return;
	}
	
	/**
	 * Deals with the problem of auto-generated values such as auto-increment primary keys that are
	 * assigned by the underlying DB layer. This is necessary since at the time onSave() event is
	 * fired the auto-increment primary key values are not yet known. Therefore if those values are
	 * to be saved into the record, we need to 'come back' to it once they have been fetched and add
	 * the values to the record.
	 * 
	 * @param record record
	 */
	private void processPostInsertModifications(SyncRecord record) {
		
		HashSet<OpenmrsObject> tmp = this.postInsertModifications.get();
		
		if (record == null || record.hasItems() == false || tmp == null) {
			return;
		}
		
		Collection<SyncItem> items = record.getItems();
		if (items == null || items.isEmpty() == true) {
			return;
		}
		
		SessionFactory factory = (SessionFactory) this.context.getBean("sessionFactory");
		ClassMetadata data = null;
		Object idPropertyValue = null;
		String idPropertyName = null;
		org.hibernate.tuple.IdentifierProperty idPropertyObj = null;
		
		try {
			for (OpenmrsObject obj : tmp) {
				data = factory.getClassMetadata(obj.getClass());
				if (!data.hasIdentifierProperty()) {
					break;
				}
				idPropertyValue = data.getIdentifier(obj, org.hibernate.EntityMode.POJO);
				idPropertyName = data.getIdentifierPropertyName();
				idPropertyObj = ((org.hibernate.persister.entity.AbstractEntityPersister) data).getEntityMetamodel()
				        .getIdentifierProperty();
				//now find it in the record and update it
				for (SyncItem item : items) {
					if (item.getContainedType() == obj.getClass()) {
						if (item.getKey().getKeyValue().equals(obj.getUuid())) {
							//found the right SyncItem
							try {
								String newIdStringValue = SyncUtil.getNormalizer(idPropertyValue.getClass()).toString(
								    idPropertyValue);
								String itemContent = item.getContent();
								Record xml = Record.create(itemContent);
								Item idItem = xml.getItem(idPropertyName);
								if (idItem == null) {
									//id wasn't serialized initially; i.e. was null add it in now
									this.appendRecord(xml, obj, xml.getRootItem(), idPropertyName, idPropertyValue
									        .getClass().getName(), newIdStringValue);
								} else {
									//id is there, update the value
									//TODO:
								}
								;
								
								//now finally replace the SyncItem content
								item.setContent(xml.toStringAsDocumentFragement());
							}
							finally {
								break;
							}
						}
					}
					
				}
			}
		}
		catch (Exception ex) {
			//TODO: log info and continue: something went wrong, 
			//one way or the other we weren't able to apply the mods; so just continue
		}
	}
}
