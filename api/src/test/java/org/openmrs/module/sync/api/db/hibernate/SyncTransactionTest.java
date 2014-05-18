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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.EncounterType;
import org.openmrs.VisitType;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.TestUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.test.ExampleTransactionalService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.NotTransactional;

import java.util.List;

/**
 * Tests methods in HibernateSyncInterceptor
 */
public class SyncTransactionTest extends BaseModuleContextSensitiveTest {

	@Autowired
	ExampleTransactionalService testService;

	@Before
	@Override
	public void baseSetupWithStandardDataAndAuthentication() throws Exception {
		Context.openSession();
		initializeInMemoryDatabase();
		executeDataSet(EXAMPLE_XML_DATASET_PACKAGE_PATH);
		executeDataSet("org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest"));
		authenticate();
	}

	@After
	@Override
	public void deleteAllData() throws Exception {
		super.deleteAllData();
		Context.closeSession();
	}

	protected List<SyncRecord> getSyncRecords() throws Exception {
		return Context.getService(SyncService.class).getSyncRecords();
	}

	@Test
	@NotTransactional
	public void shouldTestAllInSameTx() throws Exception {
		{
			EncounterType encounterType = new EncounterType();
			encounterType.setName("Test Encounter Type");
			encounterType.setDescription("Encounter Type Description");
			testService.saveObjectInTransaction(encounterType);
			Context.flushSession();
		}
		{
			VisitType visitType = new VisitType();
			visitType.setName("Test Visit Type");
			visitType.setDescription("Visit Type Description");
			testService.saveObjectInTransaction(visitType);
			Context.flushSession();
		}
		List<SyncRecord> records = getSyncRecords();
		Assert.assertEquals(2, records.size());
	}

	@Test
	@NotTransactional
	public void shouldTestFirstInNewTx() throws Exception {
		{
			EncounterType encounterType = new EncounterType();
			encounterType.setName("Test Encounter Type");
			encounterType.setDescription("Encounter Type Description");
			testService.saveObjectInNewTransaction(encounterType);
			Context.flushSession();
		}
		{
			VisitType visitType = new VisitType();
			visitType.setName("Test Visit Type");
			visitType.setDescription("Visit Type Description");
			testService.saveObjectInTransaction(visitType);
			Context.flushSession();
		}
		List<SyncRecord> records = getSyncRecords();
		Assert.assertEquals(2, records.size());
	}

	@Test
	@NotTransactional
	public void shouldTestSecondInNewTx() throws Exception {
		{
			EncounterType encounterType = new EncounterType();
			encounterType.setName("Test Encounter Type");
			encounterType.setDescription("Encounter Type Description");
			testService.saveObjectInTransaction(encounterType);
			Context.flushSession();
		}
		{
			VisitType visitType = new VisitType();
			visitType.setName("Test Visit Type");
			visitType.setDescription("Visit Type Description");
			testService.saveObjectInNewTransaction(visitType);
			Context.flushSession();
		}
		List<SyncRecord> records = getSyncRecords();
		Assert.assertEquals(2, records.size());
	}

	@Test
	@NotTransactional
	public void shouldTestBothInNewTx() throws Exception {
		{
			EncounterType encounterType = new EncounterType();
			encounterType.setName("Test Encounter Type");
			encounterType.setDescription("Encounter Type Description");
			testService.saveObjectInNewTransaction(encounterType);
			Context.flushSession();
		}
		{
			VisitType visitType = new VisitType();
			visitType.setName("Test Visit Type");
			visitType.setDescription("Visit Type Description");
			testService.saveObjectInNewTransaction(visitType);
			Context.flushSession();
		}
		List<SyncRecord> records = getSyncRecords();
		Assert.assertEquals(2, records.size());
	}

	@Test
	@NotTransactional
	public void shouldTestFirstInNoTransaction() throws Exception {
		{
			EncounterType encounterType = new EncounterType();
			encounterType.setName("Test Encounter Type");
			encounterType.setDescription("Encounter Type Description");
			testService.saveObjectNoTransaction(encounterType);
			Context.flushSession();
		}
		{
			VisitType visitType = new VisitType();
			visitType.setName("Test Visit Type");
			visitType.setDescription("Visit Type Description");
			testService.saveObjectInTransaction(visitType);
			Context.flushSession();
		}
		List<SyncRecord> records = getSyncRecords();
		Assert.assertEquals(2, records.size());
	}
}
