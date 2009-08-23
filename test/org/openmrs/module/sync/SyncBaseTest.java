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
package org.openmrs.module.sync;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncIngestService;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.FilePackage;
import org.openmrs.module.sync.serialization.IItem;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 *  Sets up common routines and initialization for all sync tests. Note for all sync tests:
 *  MUST MARK AS NotTransctional so that Tx that is created in runOnChild() method is
 *  committed upon exit of that method. 
 *  
 *  Note: org.springframework.transaction.annotation.Propagation.REQUIRES_NEW doesn't help
 *  since on most EDBMS systems it doesn't do what spec says
 *
 */
public abstract class SyncBaseTest extends BaseModuleContextSensitiveTest {

	protected final Log log = LogFactory.getLog(getClass());
	public DateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
	
	public abstract String getInitialDataset();
	
	@Override
	public void baseSetupWithStandardDataAndAuthentication() throws Exception {
		// Do nothing
	}
	
	protected void setupSyncTestChild() throws Exception {
		initializeInMemoryDatabase();
		authenticate();
		executeDataSet(getInitialDataset());
	}
	
	@Transactional
	@Rollback(false)
	protected void runOnChild(SyncTestHelper testMethods) throws Exception {
		log.info("\n************************************* Running On Child *************************************");
		testMethods.runOnChild();		
	}

	@Transactional
	@Rollback(false)
	protected void runOnParent(SyncTestHelper testMethods) throws Exception {
		authenticate();
		
        //now run parent
		log.info("\n************************************* Running on Parent *************************************");		
		testMethods.runOnParent();
	}

	//this is final step so do rollback and let the test finish
	@Transactional
	protected void runOnChild2(SyncTestHelper testMethods) throws Exception {
		authenticate();
		
        //now run parent
		log.info("\n************************************* Running on Child2 *************************************");		
		testMethods.runOnParent();		
	}
	
	/**
	 * Sets up initial data set before set of instructions simulating child changes is executed.
	 * 
	 * @see #runOnChild(SyncTestHelper)
	 * @see #runSyncTest(SyncTestHelper)
	 * 
	 * @throws Exception
	 */
	@Transactional
	@Rollback(false)
	protected void beforeRunOnChild() throws Exception {
		Context.openSession();
		deleteAllData();
		executeDataSet("org/openmrs/module/sync/include/SyncCreateTest.xml");
		authenticate();
	}
	
	@Transactional
	@Rollback(false)
	protected void applySyncChanges() throws Exception {
		Context.openSession();
		
		//get sync records created
		List<SyncRecord> syncRecords = Context.getService(SyncService.class).getSyncRecords();
		if (syncRecords == null || syncRecords.size() == 0) {
			assertFalse("No changes found (i.e. sync records size is 0)", true);
		}
		
		//now reload db from scratch
		log.info("\n************************************* Reload Database *************************************");
		deleteAllData();
		executeDataSet("org/openmrs/module/sync/include/SyncCreateTest.xml");
		executeDataSet("org/openmrs/module/sync/include/SyncRemoteChildServer.xml");
		
		authenticate();
		
		log.info("\n************************************* Sync Record(s) to Process *************************************");
        FilePackage pkg = new FilePackage();
        Record record = pkg.createRecordForWrite("SyncTest");
        Item top = record.getRootItem();
		for (SyncRecord syncRecord : syncRecords) {			
            ((IItem) syncRecord).save(record, top);
		}
        try {
            log.info("Sync record:\n" + record.toString());
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("Serialization failed with an exception: " + e.getMessage());
        }		

        log.info("\n************************************* Processing Sync Record(s) *************************************");
		RemoteServer origin = Context.getService(SyncService.class).getRemoteServer(1);
		for (SyncRecord syncRecord : syncRecords) {			
			Context.getService(SyncIngestService.class).processSyncRecord(syncRecord, origin);
		}
		
		return;
	}

	/**
	 * Executes the sync test workflow:
	 * <br/>1. prepopulate DB
	 * <br/>2. Execute set of instructions simulating sync child
	 * <br/>3. Fetch sync records, re-initialize DB for parent and then apply the sync records
	 * <br/>4. Execute set of instructions  simulating sync parent; typically just asserts to ensure child changes
	 * came across.
	 * 
	 *<br/>Note: The non-transactional vs. transactional behavior of helper methods: each step must be in its own Tx since sync flushes
	 * its sync record at Tx boundary. Consequently it is required for the overall test to run as non-transactional
	 * and each individual step to be transactional; as stated in class comments, true nested transactions are RDMS fantasy,
	 * it mostly doesn't exist.
	 * 
	 * @param testMethods helper object holding methods for child and parent execution
	 * @throws Exception
	 */
	@NotTransactional
	public void runSyncTest(SyncTestHelper testMethods) throws Exception {
		
		this.beforeRunOnChild();
		
		this.runOnChild(testMethods);
		
		this.applySyncChanges();
		
		this.runOnParent(testMethods);
		
		//now that parent is committed; replay the parent's log against child #2
		//after that is done, the data should be the same again
		this.applySyncChanges();
		
		//now finish by checking the changes record on parent against the target state
		this.runOnChild2(testMethods);
	}	
}

	
