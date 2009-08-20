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

import org.openmrs.api.APIException;
import org.openmrs.module.sync.SyncPreCommitAction;
import org.openmrs.module.sync.engine.SyncItem;
import org.openmrs.module.sync.engine.SyncRecord;
import org.openmrs.module.sync.ingest.SyncImportItem;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.ingest.SyncIngestException;
import org.openmrs.module.sync.server.RemoteServer;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface SynchronizationIngestService {

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
     * @param preCommitRecordActions
     * @return
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Records"})
    public SyncImportItem processSyncItem(SyncItem item, String originalUuid,List<SyncPreCommitAction> preCommitRecordActions)  throws APIException;
}
