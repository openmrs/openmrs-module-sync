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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.*;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.test.annotation.NotTransactional;

import java.util.Date;
import java.util.UUID;

/**
 * Tests methods in the SyncIngestService
 */
public class SyncIngestServiceTest extends BaseModuleContextSensitiveTest {

	@Before
	public void loadData() throws Exception {
		executeDataSet("org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest"));
		executeDataSet("org/openmrs/module/sync/include/SyncRemoteChildServer.xml");
	}

	/**
	 * @see {@link SyncService#getOpenmrsObjectByUuid(Class,String)}
	 */
	@Test
	@Verifies(value = "should create sync import record if successful", method = "processSyncRecord(SyncRecord,RemoteServer)")
	@NotTransactional
	public void processSyncRecord_shouldCreateSyncImportRecordIfSuccessful() throws Exception {

		RemoteServer parent = Context.getService(SyncService.class).getParentServer();
		Assert.assertNotNull(parent);

		SyncRecord record = createValidSyncRecord();
		Context.getService(SyncIngestService.class).processSyncRecord(record, parent);

		SyncImportRecord importRecord = Context.getService(SyncService.class).getSyncImportRecord(record.getOriginalUuid());
		Assert.assertNotNull(importRecord);
		Assert.assertEquals(SyncRecordState.COMMITTED, importRecord.getState());
		//org.openmrs.test.TestUtil.printOutTableContents(getConnection(), "sync_import");
	}

	/**
	 * @see {@link SyncService#getOpenmrsObjectByUuid(Class,String)}
	 */
	@Ignore("This test is written to demonstrate the issue reported in SYNC-310, and should be activated as that ticket is addressed")
	@Test
	@Verifies(value = "should create sync inmport records if error occurs", method = "processSyncRecord(SyncRecord,RemoteServer)")
	@NotTransactional
	public void processSyncRecord_shouldCreateSyncImportRecordIfErrorOccurs() throws Exception {

		RemoteServer parent = Context.getService(SyncService.class).getParentServer();
		Assert.assertNotNull(parent);

		SyncRecord record = createValidSyncRecord();

		// Setting containedType to null will lead to an exception.  This is what will currently
		// happen if you try to import an object from one server that another server doesn't know about
		// eg. if you have a module that saves it's own OpenmrsObjects installed on the parent but not the child
		record.getItems().iterator().next().setContainedType(null);

		boolean exceptionThrown = false;
		try {
			Context.getService(SyncIngestService.class).processSyncRecord(record, parent);
		}
		catch (Exception e) {
			exceptionThrown =  true;
		}
		Assert.assertTrue(exceptionThrown);

		SyncImportRecord importRecord = Context.getService(SyncService.class).getSyncImportRecord(record.getOriginalUuid());
		Assert.assertNotNull(importRecord);
		Assert.assertEquals(SyncRecordState.FAILED, importRecord.getState());
		//org.openmrs.test.TestUtil.printOutTableContents(getConnection(), "sync_import");
	}

	protected SyncRecord createValidSyncRecord() {
		SyncRecord record = new SyncRecord();
		record.setUuid(UUID.randomUUID().toString());
		record.setOriginalUuid(UUID.randomUUID().toString());
		record.setState(SyncRecordState.NEW);
		record.setContainedClasses("org.openmrs.EncounterType");
		record.setDatabaseVersion(OpenmrsConstants.OPENMRS_VERSION_SHORT);
		record.setRetryCount(0);
		record.setTimestamp(new Date());

		SyncItem item = new SyncItem();
		item.setContainedType(EncounterType.class);
		item.setKey(new SyncItemKey<String>(UUID.randomUUID().toString(), String.class));
		item.setContent("<org.openmrs.EncounterType><description type=\"string\">Test Encounter Type</description><name type=\"string\">Test Encounter Type</name><retired type=\"boolean\">false</retired><dateCreated type=\"timestamp\">2013-03-22T18:29:26.249-0400</dateCreated><uuid type=\"string\">e5b4b20b-da7f-4e07-9201-5be196c13585</uuid><creator type=\"org.openmrs.User\">873786be-17b8-4284-8a1e-66c479dd119f</creator></org.openmrs.EncounterType>");
		item.setState(SyncItemState.NEW);
		record.addItem(item);

		return record;
	}
}