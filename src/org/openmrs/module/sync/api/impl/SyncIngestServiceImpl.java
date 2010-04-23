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
package org.openmrs.module.sync.api.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncItemState;
import org.openmrs.module.sync.SyncPreCommitAction;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncIngestService;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.api.db.SyncDAO;
import org.openmrs.module.sync.ingest.SyncImportItem;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.ingest.SyncIngestException;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.RemoteServerType;
import org.openmrs.module.sync.server.SyncServerRecord;
import org.openmrs.util.OpenmrsUtil;
import org.w3c.dom.NodeList;

public class SyncIngestServiceImpl implements SyncIngestService {

    private Log log = LogFactory.getLog(this.getClass());
    
    private SyncDAO dao;
    
    public void setSyncDAO(SyncDAO dao) {
    	this.dao = dao;
    }
    
    /**
     * 
     * @see org.openmrs.api.SyncIngestService#processSyncImportRecord(SyncImportRecord importRecord)
     * 
     * @param importRecord
     * @throws APIException
     */
    public void processSyncImportRecord(SyncImportRecord importRecord, RemoteServer server) throws APIException {
        if ( importRecord != null ) {
            if ( importRecord.getUuid() != null && importRecord.getState() != null ) {
                SyncRecord record = Context.getService(SyncService.class).getSyncRecordByOriginalUuid(importRecord.getUuid());
                if ( server.getServerType().equals(RemoteServerType.PARENT) ) {
                    // with parents, we set the actual state of the record
                    if ( importRecord.getState().equals(SyncRecordState.ALREADY_COMMITTED) ) record.setState(SyncRecordState.COMMITTED);
                    else if ( importRecord.getState().equals(SyncRecordState.NOT_SUPPOSED_TO_SYNC) ) record.setState(SyncRecordState.REJECTED);
                    else record.setState(importRecord.getState());
                } else {
                    // with non-parents we set state in the server-record
                    SyncServerRecord serverRecord = record.getServerRecord(server);
                    if ( importRecord.getState().equals(SyncRecordState.ALREADY_COMMITTED) ) serverRecord.setState(SyncRecordState.COMMITTED);
                    else if ( importRecord.getState().equals(SyncRecordState.NOT_SUPPOSED_TO_SYNC) ) serverRecord.setState(SyncRecordState.REJECTED);
                    else serverRecord.setState(importRecord.getState());
                }
                
                Context.getService(SyncService.class).updateSyncRecord(record);
            }
        }        
    }
    
    /**
     * Applies  synchronization record against the local data store in single transaction.  
     * <p/> Remarks: Exceptions are always thrown if something goes wrong while processing the record in order to abort sync items as 
     * one transaction. To report back SyncImportRecord accurately in case of exception, notice that SyncIngestException contains
     * SyncImportRecord. In case of exception, callers should inspect this value as it will contain more information about the status of sync
     * item as it failed.
     * 
     * @param record SyncRecord to be processed
     * @param server Server where the record came from
     * @return
     */
    public SyncImportRecord processSyncRecord(SyncRecord record, RemoteServer server) throws SyncIngestException {
        
    	ArrayList<SyncItem> deletedItems = new ArrayList<SyncItem>();
    	SyncImportRecord importRecord = new SyncImportRecord();
        importRecord.setState(SyncRecordState.FAILED);  // by default, until we know otherwise
        importRecord.setRetryCount(record.getRetryCount());
        importRecord.setTimestamp(record.getTimestamp());
        List<SyncPreCommitAction> preCommitRecordActions = new ArrayList<SyncPreCommitAction> ();  
        
        SyncService syncService = Context.getService(SyncService.class);
		try {
            // first, let's see if this server even accepts this kind of syncRecord
            if ( OpenmrsUtil.containsAny(record.getContainedClassSet(), server.getClassesNotReceived())) {
                importRecord.setState(SyncRecordState.NOT_SUPPOSED_TO_SYNC);
                log.warn("\nNOT INGESTING RECORD with " + record.getContainedClasses() + " BECAUSE SERVER IS NOT READY TO ACCEPT ALL CONTAINED OBJECTS\n");
            } else {
                //log.warn("\nINGESTING ALL CLASSES: " + recordClasses + " BECAUSE SERVER IS READY TO ACCEPT ALL");
                // second, let's see if this SyncRecord has already been imported
                // use the original record id to locate import_record copy
                log.debug("AT THIS POINT, ORIGINALUUID FOR RECORD IS " + record.getOriginalUuid());
                importRecord = syncService.getSyncImportRecord(record.getOriginalUuid());
                boolean isUpdateNeeded = false;
                
                if ( importRecord == null ) {
                	log.info("ImportRecord does not exist, so creating new one");
                    isUpdateNeeded = true;
                    importRecord = new SyncImportRecord(record);
                    importRecord.setState(SyncRecordState.FAILED);
                    importRecord.setUuid(record.getOriginalUuid());
                    syncService.createSyncImportRecord(importRecord);
                } else {
                	if(log.isWarnEnabled()) {
                		log.warn("ImportRecord already exists and has retry count: " + importRecord.getRetryCount() + ", state: " + importRecord.getState());
                	}
                    SyncRecordState state = importRecord.getState();
                    if ( state.equals(SyncRecordState.COMMITTED)  || state.equals(SyncRecordState.ALREADY_COMMITTED) ) {
                        // apparently, the remote/child server exporting to this server doesn't realize it's
                        // committed, so let's remind by sending back this import record with already_committed
                        importRecord.setState(SyncRecordState.ALREADY_COMMITTED);
                    } else if (state.equals(SyncRecordState.FAILED)) {
                		//mark as failed and retry next time
                    	importRecord.setState(SyncRecordState.FAILED);
                		importRecord.setRetryCount(importRecord.getRetryCount() + 1);
                		isUpdateNeeded = true;
                    }else {
                        isUpdateNeeded = true;
                    }
                }
                
                if ( isUpdateNeeded ) {
                    log.debug("Looks like update is needed");
                	
                    boolean isError = false;
                            
                    //as we start setting properties, suspend session flushing 
                    syncService.setFlushModeManual();

                    // for each sync item, process it and insert/update the database; 
                    //put deletes into deletedItems collection -- these will get processed last
                    for ( SyncItem item : record.getItems() ) {
                    	if (item.getState() == SyncItemState.DELETED) {
                    		deletedItems.add(item);
                    	} else {
	                        SyncImportItem importedItem = this.processSyncItem(item, record.getOriginalUuid() + "|" + server.getUuid(),preCommitRecordActions);
	                        importedItem.setKey(item.getKey());
	                        importRecord.addItem(importedItem);
	                        if ( !importedItem.getState().equals(SyncItemState.SYNCHRONIZED)) isError = true;
                    	}
                    }
                    
                    syncService.flushSession();
                    syncService.setFlushModeAutomatic();
                    
                    /* now run through deletes: deletes must be processed after inserts/updates
                     * because of hibernate flushing semantics inside transactions:
                     * if deleted entity is part of a collection on another object within the same session
                     * and this object gets flushed, error is thrown stating that deleted entities must first be removed
                     * from collection; this happens immediately when stmts are executed (and not at the Tx boundary) because
                     * default hibernate FlushMode is AUTO. To further avoid this issue, explicitly suspend flushing for the 
                     * duration of deletes.
                     */
                	syncService.setFlushModeManual(); 
                    for ( SyncItem item : deletedItems ) {
                        SyncImportItem importedItem = this.processSyncItem(item, record.getOriginalUuid() + "|" + server.getUuid(),preCommitRecordActions);
                        importedItem.setKey(item.getKey());
                        importRecord.addItem(importedItem);
                        if ( !importedItem.getState().equals(SyncItemState.SYNCHRONIZED)) isError = true;
                    }
                    syncService.flushSession();
                    syncService.setFlushModeAutomatic();
                    
                    /* 
                     * finally execute the pending actions that resulted from processing all sync items 
                     */
                    syncService.setFlushModeManual();
                    SyncUtil.applyPreCommitRecordActions(preCommitRecordActions);
                    syncService.flushSession();
                    syncService.setFlushModeAutomatic();
                    
                    if ( !isError ) {
                        importRecord.setState(SyncRecordState.COMMITTED);
                        
                        // now that we know there's no error, we have to prevent this change from being sent back to the originating server
                        /*
                         * This actually can't be done here, since hibernate may not yet commit the record - 
                         * instead we have to get hacky: in processxxx() methods we set originalUuid and then commit changes.
                         * Once that is done, the interceptor pulls the original uuid out and calls SyncServiceImpl.createSyncRecord()
                         * where sync records *are* not written for the originting server
                         * 
                        SyncRecord newRecord = Context.getService(SyncService.class).getSyncRecord(record.getOriginalUuid());
                        if ( newRecord != null ) {
                            if ( server.getServerType().equals(RemoteServerType.PARENT)) {
                                newRecord.setState(SyncRecordState.COMMITTED);
                            } else {
                                SyncServerRecord serverRecord = newRecord.getServerRecord(server);
                                if ( serverRecord != null ) {
                                    serverRecord.setState(SyncRecordState.COMMITTED);
                                    
                                } else {
                                    log.warn("No server record was created for server " + server.getNickname() + " and record " + record.getOriginalUuid());
                                }
                            }
                            Context.getService(SyncService.class).updateSyncRecord(newRecord);

                        } else {
                            log.warn("Can't find newly created record on system by originalUuid" + record.getOriginalUuid());
                        }
                        */
                    } else {
                    	//One of SyncItem commits failed, throw to rollback and set failure information.
                    	log.warn("Error while processing SyncRecord with original uuid " + record.getOriginalUuid() + " (" + record.getContainedClasses() + ")");
                        importRecord.setState(SyncRecordState.FAILED);
                        throw new SyncIngestException(SyncConstants.ERROR_ITEM_NOT_COMMITTED,null,null,importRecord);
                    }
                    
                    //syncService.updateSyncImportRecord(importRecord);
                }
            }
        } catch (SyncIngestException e) {
	        log.error("Unable to ingest a sync request", e);
        	//fill in sync import record and rethrow to abort tx
	        importRecord.setState(SyncRecordState.FAILED);
	        importRecord.setErrorMessage(e.getMessage());
        	e.setSyncImportRecord(importRecord);
        	throw (e);
        }
        catch (Exception e ) {
        	log.error("Unexpected exception occurred when processing sync records", e);
            //fill in sync import record and rethrow to abort tx
            importRecord.setState(SyncRecordState.FAILED);
            importRecord.setErrorMessage(e.getMessage());
            throw new SyncIngestException(e,SyncConstants.ERROR_RECORD_UNEXPECTED,null,null,importRecord);
        } finally {
        	syncService.updateSyncImportRecord(importRecord);
        	
        	//reset the flush mode back to automatic, no matter what
        	syncService.setFlushModeAutomatic();
        }

        return importRecord;
    }
    
    /**
     * Note: preCommitRecordActions collection is provided as a way for the OpenmrsObject instances to 'schedule' action that is necessary
     * for processing of the object yet it cannot be applied until the end of the processing of the parent sync record. For example, rebuild XSN
     * cannot happen until all form fields held in the sync items are applied first; thus the call to rebuild XSN need to happen after all
     * sync items were processed and before committing the sync record.
     * 
     * HashMap contained in the collection is to capture the action, and the necessary object to resolve that action. The action
     * is understood and applied by {@link org.openmrs.synchronization.SyncUtil#applyPreCommitRecordActions(ArrayList)}
     * 
     * @see org.openmrs.api.SyncIngestService#processSyncItem(org.openmrs.module.sync.SyncItem, java.lang.String, java.util.ArrayList)
     * @see org.openmrs.synchronization.SyncUtil#applyPreCommitRecordActions(ArrayList)
     * 
     */
    public SyncImportItem processSyncItem(SyncItem item, String originalUuid,List<SyncPreCommitAction> preCommitRecordActions)  throws APIException {
    	String itemContent = null;
        SyncImportItem ret = null; 

        try {
        	ret = new SyncImportItem();
            //ret.setContent(itemContent); - no need to copy content back: the server that send it knows it already
            ret.setState(SyncItemState.UNKNOWN);

            Object o = null;
			itemContent = item.getContent();
			
            if (log.isDebugEnabled()) {
                log.debug("STARTING TO PROCESS: " + itemContent);
                log.debug("SyncItem state is: " + item.getState());
            }
            
            o = SyncUtil.getRootObject(itemContent);
            if (o instanceof org.hibernate.collection.PersistentCollection) {
            	log.debug("Processing a persistent collection");
            	dao.processCollection(o.getClass(),itemContent,originalUuid);
            } else {
            	processOpenmrsObject((OpenmrsObject)o,item,originalUuid,preCommitRecordActions);
            }
            ret.setState(SyncItemState.SYNCHRONIZED);                
        } catch (SyncIngestException e) {
        	//MUST RETHROW to abort transaction
        	e.setSyncItemContent(itemContent);
        	throw (e);
        }
        catch (Exception e) {
        	//MUST RETHROW to abort transaction
            throw new SyncIngestException(e,SyncConstants.ERROR_ITEM_UNEXPECTED, null, itemContent,null);
        }       
        
        return ret;        
    }

    /**
     * Processes serialized SyncItem state by attempting to hydrate the object SyncItem represents and then using OpenMRS service layer to
     * update the hydrated instance of OpenmrsObject object.
     * <p/>Remarks: This implementation relies on internal knowledge of how SyncItems are serialized: it iterates over direct child nodes of the root xml
     * node in incoming assuming they are serialized public properties of the object that is being hydrated. Consequently, for each child node, 
     * property setter is determined and then called. After setting all properties, OpenMRS service layer API is used to actually save 
     * the object into persistent store. The details of how property setters are determined and how appropriate service layer methods
     * are determined are contained in SyncUtil class.
     * <p/>
     * SyncItem with status of DELETED is handled differently from insert/update: In case of a delete, all that is needed (and sent) 
     * is the object type and its UUID. Consequently, the process for handling deletes consists of first fetching 
     * existing object by uuid and then deleting it by a call to sync service API. Note, if object is not found in DB by its uuid, we
     * skip the delete and record warning message. 
     * <p/>
     * preCommitRecordActions collection is provided as a way for the OpenmrsObject instances to 'schedule' action that is necessary
     * for processing of the object yet it cannot be applied until the end of the processing of the parent sync record. For example, rebuild XSN
     * cannot happen until all form fields held in the sync items are applied first; thus the call to rebuild XSN need to happen after all
     * sync items were processed and before committing the sync record.
     *  
     * @param o empty instance of class that this SyncItem represents 
     * @param incoming Serialized state of SyncItem.
     * @param originalUuid Unique id of the object that is stored in SyncItem recorded when this object was first created. NOTE:
     * this value is retained and forwarded unchanged throughout the network of sychronizing servers in order to avoid re-applying
     * same changes over and over.
     * @param preCommitRecordActions collection of actions that will be applied a the end of the processing sync record
     * 
     * @see SyncUtil#setProperty(Object, String, Object)
     * @see SyncUtil#getOpenmrsObj(String, String)
     * @see SyncUtil#updateOpenmrsObject(OpenmrsObject, String, String, List)
     */
    private void processOpenmrsObject(OpenmrsObject o, SyncItem item, String originalUuid, List<SyncPreCommitAction> preCommitRecordActions) throws Exception {

    	String itemContent = null;
        String className = null;
        boolean alreadyExists = false;
        boolean isDelete = false;
        ArrayList<Field> allFields = null;
        NodeList nodes = null;

        isDelete = (item.getState() == SyncItemState.DELETED) ? true : false; 
        itemContent = item.getContent();
    	className = o.getClass().getName();
        allFields = SyncUtil.getAllFields(o);  // get fields, both in class and superclass - we'll need to know what type each field is
        nodes = SyncUtil.getChildNodes(itemContent);  // get all child nodes (xml) of the root object

	    if ( o == null || className == null || allFields == null || nodes == null ) {
	    	log.warn("Item is missing a className or all fields or nodes");
	    	throw new SyncIngestException(SyncConstants.ERROR_ITEM_NOCLASS, className, itemContent,null);
	    }

	    String uuid = SyncUtil.getAttribute(nodes, "uuid", allFields);
        OpenmrsObject objOld = SyncUtil.getOpenmrsObj(className, uuid);
        if ( objOld != null ) {
            o = objOld;
            alreadyExists = true;
        }
	       
        if (log.isDebugEnabled()) {
	        log.debug("isUpdate: " + alreadyExists);
	        log.debug("isDelete: " + isDelete);
        }
                
        /*
		 * Pass the original uuid to interceptor: this will prevent the change
		 * from being sent back to originating server the technique used here is
		 * to simply fire an update to 'fake' global property which will be then
		 * made on the same transaction that the real commit will come on.
		 * Interceptor code is watching for this update. For more info see
		 * HibernateSyncInterceptor.setOriginalUuid()
		 */
		Context.getService(SyncService.class).setGlobalProperty(
				SyncConstants.PROPERTY_ORIGINAL_UUID, originalUuid);
        
    	//execute delete if instance was found and operation is delete
        if (alreadyExists && isDelete) {
        	SyncUtil.deleteOpenmrsObject(o);
        }else if (!alreadyExists && isDelete) { 
        	log.warn("Object to be deleted was not found in the database. skipping delete operation:");
        	log.warn("-object type: " + o.getClass().toString());
        	log.warn("-object uuid: " + uuid);
        } else {
            //if we are doing insert/update:
            //1. set serialized props state
        	//2. force it down the hibernate's throat with help of openmrs api
	        for ( int i = 0; i < nodes.getLength(); i++ ) {
	            try {
	            	log.debug("trying to set property: " + nodes.item(i).getNodeName() + " in className " + className);
	                SyncUtil.setProperty(o, nodes.item(i), allFields);
	            } catch ( Exception e ) {
	            	log.error("Error when trying to set " + nodes.item(i).getNodeName() + ", which is a " + className, e);
	                throw new SyncIngestException(e, SyncConstants.ERROR_ITEM_UNSET_PROPERTY, nodes.item(i).getNodeName() + "," + className, itemContent,null);
	            }
	        }
        	        
	        // now try to commit this fully inflated object
	        try {
	        	log.debug("About to update or create a " + className + " object, uuid: '" + uuid + "'");
	            SyncUtil.updateOpenmrsObject(o, className, uuid, preCommitRecordActions);
	            Context.getService(SyncService.class).flushSession();
	        } catch ( Exception e ) {
	        	// don't include stacktrace here because the parent classes log it sufficiently
	        	log.error("Unexpected exception occurred while saving openmrsobject: " + className + ", uuid '" + uuid + "'");
	            throw new SyncIngestException(e, SyncConstants.ERROR_ITEM_NOT_COMMITTED, className, itemContent, null);
	        }
        }
        	                
        return;
    }
}
