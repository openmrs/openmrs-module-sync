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
package org.openmrs.module.sync.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmrs.OpenmrsObject;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.sync.SyncClass;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncStatistic;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.SyncServerRecord;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database related methods for the Synchronization module.
 */
@Transactional
public interface SyncService {
	
	/**
	 * Create a new SyncRecord
	 * 
	 * @param SyncRecord The SyncRecord to create
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization Records"})
	public void createSyncRecord(SyncRecord record) throws APIException;
	
	/**
	 * Auto generated method comment
	 * 
	 * @param record
	 * @param originalUuid
	 */
	public void createSyncRecord(SyncRecord record, String originalUuid);
	
	/**
	 * Update a SyncRecord
	 * 
	 * @param SyncRecord The SyncRecord to update
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization Records"})
	public void updateSyncRecord(SyncRecord record) throws APIException;
	
	/**
	 * Delete a SyncRecord
	 * 
	 * @param SyncRecord The SyncRecord to delete
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization Records"})
	public void deleteSyncRecord(SyncRecord record) throws APIException;
	
	/**
	 * @param uuid of the SyncRecord to retrieve
	 * @return SyncRecord The SyncRecord or null if not found
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Records"})
	@Transactional(readOnly = true)
	public SyncRecord getSyncRecord(String uuid) throws APIException;
	
	@Transactional(readOnly = true)
	public SyncRecord getSyncRecordByOriginalUuid(String originalUuid) throws APIException;
	
	/**
	 * @return SyncRecord The latest SyncRecord or null if not found
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Records"})
	@Transactional(readOnly = true)
	public SyncRecord getLatestRecord() throws APIException;
	
	/**
	 * Create a new SyncImportRecord
	 * 
	 * @param SyncImportRecord The SyncImportRecord to create
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization Records"})
	public void createSyncImportRecord(SyncImportRecord record) throws APIException;
	
	/**
	 * Update a SyncImportRecord
	 * 
	 * @param SyncImportRecord The SyncImportRecord to update
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization Records"})
	public void updateSyncImportRecord(SyncImportRecord record) throws APIException;
	
	/**
	 * Delete a SyncImportRecord
	 * 
	 * @param SyncImportRecord The SyncImportRecord to delete
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization Records"})
	public void deleteSyncImportRecord(SyncImportRecord record) throws APIException;
	
	/**
	 * @param uuid of the SyncImportRecord to retrieve
	 * @return SyncRecord The SyncImportRecord or null if not found
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Records"})
	@Transactional(readOnly = true)
	public SyncImportRecord getSyncImportRecord(String uuid) throws APIException;
	
	/**
	 * Get all SyncImportRecords in a specific SyncRecordState
	 * 
	 * @param state SyncRecordState for the SyncImportRecords to be returned
	 * @return SyncRecord A list containing all SyncImportRecords with the given state
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Records"})
	@Transactional(readOnly = true)
	public List<SyncImportRecord> getSyncImportRecords(SyncRecordState state) throws APIException;
	
	/**
	 * Returns the first SyncRecord in either the PENDING SEND or the NEW state
	 * 
	 * @return SyncRecord The first SyncRecord matching the criteria, or null if none matches
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Records"})
	@Transactional(readOnly = true)
	public SyncRecord getFirstSyncRecordInQueue() throws APIException;
	
	/**
	 * Get all SyncRecords
	 * 
	 * @return SyncRecord A list containing all SyncRecords
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Records"})
	@Transactional(readOnly = true)
	public List<SyncRecord> getSyncRecords() throws APIException;
	
	/**
	 * Get all SyncRecords in a specific SyncRecordState
	 * 
	 * @param state SyncRecordState for the SyncRecords to be returned
	 * @return SyncRecord A list containing all SyncRecords with the given state
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Records"})
	@Transactional(readOnly = true)
	public List<SyncRecord> getSyncRecords(SyncRecordState state) throws APIException;
	
	/**
	 * Get all SyncRecords in a specific SyncRecordStates
	 * 
	 * @param states SyncRecordStates for the SyncRecords to be returned
	 * @return SyncRecord A list containing all SyncRecords with the given states
	 * @throws APIException
	 */
	@Authorized( { "View Synchronization Records" })
	@Transactional(readOnly = true)
	public List<SyncRecord> getSyncRecords(SyncRecordState[] states) throws APIException;
	
	/**
	 * Get all SyncRecords in a specific SyncRecordStates, that the server allows sending for
	 * (per-server basis). Filters out records with classes that are not sync-able (see
	 * RemoteServr.getClassesSent() for more info on how this works). Updates status of filtered out
	 * classes to 'not_supposed_to_sync'.
	 * 
	 * @param states SyncRecordStates for the SyncRecords to be returned
	 * @param server Server these records will be sent to, so we can filter on Class
	 * @return SyncRecord A list containing all SyncRecords with the given states
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Records"})
	public List<SyncRecord> getSyncRecords(SyncRecordState[] states, RemoteServer server) throws APIException;
	
	/**
	 * Get all SyncRecords in a specific SyncRecordStates
	 * 
	 * @param states SyncRecordStates for the SyncRecords to be returned
	 * @return SyncRecord A list containing all SyncRecords with the given states
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Records"})
	@Transactional(readOnly = true)
	public List<SyncRecord> getSyncRecords(SyncRecordState[] states, boolean inverse) throws APIException;
	
	/**
	 * Get all SyncRecords after a given timestamp
	 * 
	 * @param from Timestamp specifying lower bound, not included.
	 * @return SyncRecord A list containing all SyncRecords with a timestamp after the given
	 *         timestamp
	 * @throws APIException
	 */
	//Authorized({"View Synchronization Records"})
	@Transactional(readOnly = true)
	public List<SyncRecord> getSyncRecordsSince(Date from) throws APIException;
	
	/**
	 * Get all SyncRecords between two timestamps, including the to-timestamp.
	 * 
	 * @param from Timestamp specifying lower bound, not included.
	 * @param to Timestamp specifying upper bound, included.
	 * @return SyncRecord A list containing all SyncRecords with a timestamp between the from
	 *         timestamp and up to and including the to timestamp
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Records"})
	@Transactional(readOnly = true)
	public List<SyncRecord> getSyncRecordsBetween(Date from, Date to) throws APIException;
	
	/**
	 * Get the most recent sync records
	 * 
	 * @param firstRecordId the first SyncRecord#getRecordId() to return
	 * @param numberToReturn the max number of records to return
	 * @return SyncRecord A list containing all SyncRecords ordered from most recent to oldest
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Records"})
	@Transactional(readOnly = true)
	public List<SyncRecord> getSyncRecords(Integer firstRecordId, Integer numberToReturn) throws APIException;
	
	/**
	 * Deletes all {@link SyncRecord}s that have the given states (optional) and are before the
	 * given date. <br/>
	 * <br/>
	 * If <code>states</code> is null, then automatic detection of the current server setup is done
	 * and the appropriate states are chosen to be deleted:
	 * <dl>
	 * <dt>if server has a parent (meaning this server is a leaf node)</dt>
	 * <dd>sync_server_record will be empty, thus what we need to do is to delete all sync_record
	 * rows that are COMMITTED or NOT_SUPPOSED_TO_SYNC</dd>
	 * <dt>if server is root node (no parent, only children)</dt>
	 * <dd>sync_record has only rows with 'NEW' (or NOT_SUPPOSED_TO_SYNC) status delete
	 * sync_server_record rows first that are safe to delete then delete rows irrespective of status
	 * in sync_record that have *no* rows in sync_server_record</dd>
	 * </dl>
	 * <br/>
	 * All {@link SyncServerRecord}s are deleted that are before the given date and have are either
	 * {@link SyncRecordState#COMMITTED} or {@link SyncRecordState#NOT_SUPPOSED_TO_SYNC}.
	 * 
	 * @param states the states on {@link SyncServerRecord} to delete (or null if automatic selection
	 *            should be done)
	 * @param to the date to delete before
	 * @return the number of delete records
	 * @throws DAOException
	 * @should delete all sync records if server is root node
	 * @should only delete committed sync records if child node
	 */
	public Integer deleteSyncRecords(SyncRecordState[] states, Date to) throws APIException;
	
	/**
	 * Retrieve value of given global property using synchronization data access mechanisms.
	 * 
	 * @param propertyName
	 * @return
	 */
	//@Authorized({"View Synchronization Records"})
	@Transactional(readOnly = true)
	public String getGlobalProperty(String propertyName) throws APIException;
	
	/**
	 * Set global property related to synchronization; notably bypasses any changeset recording
	 * mechanisms.
	 * 
	 * @param propertyName String specifying property name which value is to be set.
	 * @param propertyValue String specifying property value to be set.
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization Records"})
	public void setGlobalProperty(String propertyName, String propertyValue) throws APIException;
	
	/**
	 * Update or create a server (child or parent)
	 * 
	 * @param server The RemoteServer to persist in the database
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization Servers"})
	public void saveRemoteServer(RemoteServer server) throws APIException;
	
	/**
	 * Delete a RemoteServer
	 * 
	 * @param server The RemoteServer to delete
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization Servers"})
	public void deleteRemoteServer(RemoteServer server) throws APIException;
	
	/**
	 * @param serverId of the RemoteServer to retrieve
	 * @return RemoteServer The RemoteServer or null if not found
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Servers"})
	@Transactional(readOnly = true)
	public RemoteServer getRemoteServer(Integer serverId) throws APIException;
	
	/**
	 * @param uuid of the RemoteServer to retrieve
	 * @return RemoteServer The RemoteServer or null if not found
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Servers"})
	@Transactional(readOnly = true)
	public RemoteServer getRemoteServer(String uuid) throws APIException;
	
	/**
	 * @param username child_username of the RemoteServer to retrieve
	 * @return RemoteServer The RemoteServer or null if not found
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Servers"})
	@Transactional(readOnly = true)
	public RemoteServer getRemoteServerByUsername(String username) throws APIException;
	
	/**
	 * @return List of all {@link RemoteServer}s defined -- both parent and child.
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Servers"})
	@Transactional(readOnly = true)
	public List<RemoteServer> getRemoteServers() throws APIException;
	
	/**
	 * @return RemoteServer The RemoteServer defined as the parent to this current server or 
	 * null if this server is the root of all other servers
	 * @throws APIException
	 */
	//@Authorized({"View Synchronization Servers"})
	@Transactional(readOnly = true)
	public RemoteServer getParentServer() throws APIException;
	
	/**
	 * Retrieves globally unique id of the server.
	 * 
	 * @return uuid of the server. String representation of java.util.UUID.
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	public String getServerUuid() throws APIException;
	
	/**
	 * Sets globally unique id of the server. WARNING: Use only during initial server setup.
	 * WARNING: DO NOT CALL this method unless you fully understand the implication of this action.
	 * Specifically, changing already assigned UUID for a server will cause it to loose its link to
	 * history of changes that may be designated for this server.
	 * 
	 * @param uuid unique UUID of the server. String representation of java.util.UUID.
	 * @throws APIException
	 */
	public void saveServerUuid(String uuid) throws APIException;
	
	/**
	 * Retrieve user friendly nickname for the server that is (by convention) unique for the given
	 * sync network of servers.
	 * 
	 * @return name of the server.
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	public String getServerName() throws APIException;
	
	/**
	 * Sets friendly server name. WARNING: Use only during initial server setup. WARNING: DO NOT
	 * CALL this method unless you fully understand the implication of this action. Similarly to
	 * {@link #setServerUuid(String)} some data loss may occur if called while server is functioning
	 * as part of the sync network.
	 * 
	 * @param name new server name
	 * @throws APIException
	 */
	public void saveServerName(String name) throws APIException;
	
	/**
	 * Get the stored administrative email address or null if none 
	 * 
	 * @return admin email address or null
	 * @throws APIException
	 */
	public String getAdminEmail() throws APIException;
	
    /**
     * Save the admin email address for this server
     * 
     * @param email the admin's email address
     * @throws APIException
     */
    public void saveAdminEmail(String email) throws APIException;
    
	/**
	 * Update or create a SyncClass
	 * 
	 * @param SyncClass The SyncClass to update
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization"})
	public void saveSyncClass(SyncClass syncClass) throws APIException;
	
	/**
	 * Delete a SyncClass
	 * 
	 * @param SyncClass The SyncClass to delete
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization"})
	public void deleteSyncClass(SyncClass syncClass) throws APIException;
	
	/**
	 * @param syncClassId of the SyncClass to retrieve
	 * @return SyncClass The SyncClass or null if not found
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization"})
	@Transactional(readOnly = true)
	public SyncClass getSyncClass(Integer syncClassId) throws APIException;
	
	/**
	 * @return List<SyncClass> The latest default {@link SyncClass}es
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization"})
	@Transactional(readOnly = true)
	public List<SyncClass> getSyncClasses() throws APIException;
	
	/**
	 * @param String of the String class name to retrieve
	 * @return SyncClass The SyncClass or null if not found
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization"})
	@Transactional(readOnly = true)
	public SyncClass getSyncClassByName(String className) throws APIException;
	
	/**
	 * Deletes instance of OpenmrsObject from data storage.
	 * 
	 * @param o instance to delete
	 * @throws APIException
	 */
	//@Authorized({"Manage Synchronization"})
	@Transactional
	public void deleteOpenmrsObject(OpenmrsObject o) throws APIException;
	
	/**
	 * Exposes ability to change persistence flush semantics.
	 * 
	 * @throws APIException
	 * @see org.openmrs.module.sync.api.db.SyncDAO#setFlushModeManual()
	 */
	public void setFlushModeManual() throws APIException;
	
	/**
	 * Exposes ability to change persistence flush semantics.
	 * 
	 * @throws APIException
	 * @see org.openmrs.module.sync.api.db.SyncDAO#setFlushModeAutomatic()
	 */
	public void setFlushModeAutomatic() throws APIException;
	
	public void flushSession() throws APIException;
	
	/**
	 * Processes save/update to instance of OpenmrsObject by persisting it into local persistance
	 * store.
	 * 
	 * @param object instance of OpenmrsObject to be processed.
	 * @return
	 * @throws APIException
	 */
	public void saveOrUpdate(OpenmrsObject object) throws APIException;
	
	/**
	 * @param fromDate start date
	 * @param toDate end date
	 * @return
	 * @throws DAOException
	 */
	public Map<RemoteServer, Set<SyncStatistic>> getSyncStatistics(Date fromDate, Date toDate) throws DAOException;
	
	public boolean checkUuidsForClass(Class clazz) throws APIException;
	
	/**
	 * Gets any type of OpenmrsObject given a class and a UUID
	 * 
	 * @param <T> works for any OpenmrsObject subclass
	 * @param clazz
	 * @param uuid
	 * @return
	 * @should get any openmrs object by its uuid
	 */
	public <T extends OpenmrsObject> T getOpenmrsObjectByUuid(Class<T> clazz, String uuid);
	
	/**
	 * Get all possible classes that extend OpenmrsObject in the system
	 * 
	 * @return a list of {@link OpenmrsObject}
	 */
	public List<Class<OpenmrsObject>> getAllOpenmrsObjects();
	
	/**
	 * Dumps the entire database, much like what you'd get from the mysqldump
	 * command, and adds a few lines to set the child's GUID, and delete sync
	 * history
	 * 
	 * @param guidForChild if not null, use this as the guid for the child
	 *        server, otherwise autogenerate one
	 * @param out write the sql here
	 * @throws APIException
	 */
	// @Authorized({"Backup Entire Database"})
	@Transactional(readOnly = true)
	public void exportChildDB(String guidForChild, OutputStream os)
	        throws APIException;

	/**
	 * imports a synchronization database backup from the parent
	 * 
	 * @throws DAOException
	 */
	public void importParentDB(InputStream in) throws APIException;

	/**
	 * Dumps the entire database with the mysqldump command
	 */
	public String generateDataFile() throws APIException;

	public void execGeneratedFile(String generatedDataFileName)
	        throws APIException;
}
