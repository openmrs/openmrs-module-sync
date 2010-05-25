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

import java.util.List;
import java.util.Map;

import org.openmrs.OpenmrsObject;
import org.openmrs.annotation.Logging;
import org.openmrs.api.APIException;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.ingest.SyncImportItem;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.ingest.SyncIngestException;
import org.openmrs.module.sync.server.RemoteServer;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface SyncIngestService {

    /**
     * Processes SyncRecord and create corresponding sync import record.
     * @param SyncRecord The SyncRecord to create
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Records"})
    public SyncImportRecord processSyncRecord(SyncRecord record, RemoteServer server) throws SyncIngestException;
    
    /**
     * Processes SyncImportRecord.
     * @param SyncRecord The SyncRecord to update
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Records"})
    public void processSyncImportRecord(SyncImportRecord importRecord, RemoteServer server) throws APIException;
    
    /**
     * Processes incoming SyncItem against the local server instance.
     * 
     * @param item instance of syncItem to be processed.
     * @param originalUuid
     * @param processedObjects a map of classname to the list of objects that have been processed.  This item's object is added to this list
     * @return
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Records"})
    @Logging(ignoreAllArgumentValues=true)
    public SyncImportItem processSyncItem(SyncItem item, String originalUuid, Map<String, List<OpenmrsObject>> processedObjects) throws APIException;

	/**
	 * Does any post-record import processing right before flushing to the
	 * database. The things that need to be done relates to logic that is
	 * usually in the service save* methods. This method is called after all
	 * sync items in a sync record have been processed but before anything is
	 * flushed to the db<br/>
	 * <br/>
	 * For example, ConceptWords must be updated after a new ConceptName is
	 * received. <br/>
	 * <br/>
	 * The Formentry module AOPs around this method to rebuild the XSN any time
	 * a new XSN comes through.
	 * 
	 * @param processedObjects
	 *            a map from classname to the list of objects of that class that
	 *            were updated
	 * @throws APIException
	 */
    @Logging(ignoreAllArgumentValues=true)
	public void applyPreCommitRecordActions(Map<String, List<OpenmrsObject>> processedObjects) throws APIException;
}
