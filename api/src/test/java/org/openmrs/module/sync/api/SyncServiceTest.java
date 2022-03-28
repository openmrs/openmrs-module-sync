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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.ConceptDatatype;
import org.openmrs.GlobalProperty;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAttributeType;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncTransmissionStatus;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.TransmissionLog;
import org.openmrs.module.sync.api.impl.SyncServiceImpl;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.SyncServerRecord;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Tests methods in the SyncService
 */
public class SyncServiceTest extends BaseModuleContextSensitiveTest {

	private SyncService syncService;
	private static final String SERVER_DATA_SET_FILE = "org/openmrs/module/sync/include/SyncServerClasses.xml";
	private static final String TRANSMISSION_DATA_SET_FILE = "org/openmrs/module/sync/include/transmissionLogs.xml";
	/**
	 * @see {@link SyncService#getOpenmrsObjectByUuid(Class,String)}
	 */
	@BeforeEach
	public void setup() throws Exception {
		syncService = Context.getService(SyncService.class);
		executeDataSet(SERVER_DATA_SET_FILE);
		executeDataSet(TRANSMISSION_DATA_SET_FILE);
	}

	@Test
	@Verifies(value = "should get any openmrs object by its uuid", method = "getOpenmrsObjectByUuid(Class,String)")
	public void getOpenmrsObjectByUuid_shouldGetAnyOpenmrsObjectByItsUuid() throws Exception {
		ConceptDatatype dt = Context.getService(SyncService.class).getOpenmrsObjectByUuid(ConceptDatatype.class,
		    "8d4a4ab4-c2cc-11de-8d13-0010c6dffd0f");
		Assert.assertNotNull(dt);
		Assert.assertEquals(Integer.valueOf(3), dt.getConceptDatatypeId());
	}

	/**
     * @see {@link SyncService#deleteSyncRecords(null,Date)}
     * 
     */
    @Test
    @Verifies(value = "should delete all sync records if server is root node", method = "deleteSyncRecords(null,Date)")
    public void deleteSyncRecords_shouldDeleteAllSyncRecordsIfServerIsRootNode() throws Exception {
    	executeDataSet("org/openmrs/module/sync/include/SyncRecords.xml");
		
    	SyncService syncService = Context.getService(SyncService.class);
    	syncService.deleteSyncRecords(null, new Date());
    	List<SyncRecord> records = syncService.getSyncRecords();
		
		Assert.assertEquals(0, records.size());
    }

	/**
     * @see {@link SyncService#deleteSyncRecords(null,Date)}
     * 
     */
    @Test
    @Verifies(value = "should only delete committed sync records if child node", method = "deleteSyncRecords(null,Date)")
    public void deleteSyncRecords_shouldOnlyDeleteCommittedSyncRecordsIfChildNode() throws Exception {
    	executeDataSet("org/openmrs/module/sync/include/SyncRecords.xml");
    	executeDataSet("org/openmrs/module/sync/include/SyncRecordsAddingParent.xml");
    	
    	SyncService syncService = Context.getService(SyncService.class);
    	
    	// sanity check
    	List<SyncRecord> records = syncService.getSyncRecords();
		Assert.assertEquals(64, records.size());
    	
    	syncService.deleteSyncRecords(null, new Date());
		
    	Context.clearSession(); // because we have the other records sitting in memory
    	records = syncService.getSyncRecords();
		
		Assert.assertEquals(59, records.size());
    }
    
    @Test
    @Verifies(value = "should exclude only types setup for all sync servers", method = "shouldSynchronize(Object)")
    public void shouldSynchronize_shouldOnlySyncValidTypes() throws Exception {
    	SyncService syncService = Context.getService(SyncService.class);
    	SyncServiceImpl.refreshServerClassesCollection();
    	
		Assert.assertFalse(syncService.shouldSynchronize(new GlobalProperty("test","test")));
    	
		Assert.assertTrue(syncService.shouldSynchronize(new PatientIdentifierType())); //marked 'yes' in all

		Assert.assertTrue(syncService.shouldSynchronize(new PersonAttributeType())); //marked as 'yes' in one server
		
		Assert.assertTrue(syncService.shouldSynchronize(new Person())); //not in sync_server_classes
		
		Assert.assertTrue(syncService.shouldSynchronize(new User())); //not in one sync_server_class
		
    }

	/**
	 * @see {@link SyncService#getSyncRecord(Integer)}
	 * 
	 */
	@Test
	@Verifies(value = "should get a record by its primary key", method = "getSyncRecord(Integer)")
	public void getSyncRecord_shouldGetARecordByItsPrimaryKey()
			throws Exception {
		executeDataSet("org/openmrs/module/sync/include/SyncRecords.xml");
		
		SyncService syncService = Context.getService(SyncService.class);
    	
		SyncRecord syncRecord = syncService.getSyncRecord(4);
		
		Assert.assertEquals("c7c38315-285d-471a-94cd-1fdc71a5459b", syncRecord.getUuid());
	}

	/**
	 * @see {@link SyncService#getSyncRecords(String)}
	 * 
	 */
	@Test
	@Verifies(value = "should find a record given a string in its payload", method = "getSyncRecords(String)")
	public void getSyncRecords_shouldFindARecordGivenAStringInItsPayload()
			throws Exception {
		executeDataSet("org/openmrs/module/sync/include/SyncRecords.xml");
		
		SyncService syncService = Context.getService(SyncService.class);
    	
		List<SyncRecord> syncRecords = syncService.getSyncRecords("ConceptDatatype");
		
		Assert.assertEquals(7, syncRecords.size());
	}
	
	/**
	 * @see {@link SyncService#getSyncServerRecord(Integer)}
	 */
	@Test
	@Verifies(value = "should get a syncServerRecord by its primary key", method = "getSyncServerRecord(Integer)")
	public void getSyncServerRecord_shouldGetASyncServerRecordByItsPrimaryKey() throws Exception {
		executeDataSet("org/openmrs/module/sync/include/SyncRecords.xml");

		SyncServerRecord ssr = syncService.getSyncServerRecord(57);
		
		Assert.assertEquals(1, ssr.getSyncRecord().getRecordId().intValue());
		Assert.assertEquals(1, ssr.getSyncServer().getServerId().intValue());
		Assert.assertEquals(SyncRecordState.NOT_SUPPOSED_TO_SYNC, ssr.getState());
		
		Assert.assertNull(syncService.getSyncServerRecord(445544));
	}

	@Test
	public void saveTransmissionLog_shouldSaveTransmissionLogInstance() {
		TransmissionLog log = new TransmissionLog();
		log.setSyncRecords(syncService.getSyncRecords());
		log.setStatus(SyncTransmissionStatus.FAILURE);

		Assert.assertNull(log.getTransmissionLogId());
		syncService.saveTransmissionLog(log);
		Assert.assertNotNull(log.getTransmissionLogId());
	}

	@Test
	public void getCountOfAllTransmissionLogs_shouldReturnTheCorrectNumber() {
		Assert.assertEquals(11, syncService.getCountOfAllTransmissionLogs());
	}

	@Test
	public void getCountOfAllTransmissionLogs_shouldReturnTheCorrectCountOfTransmissionsLogsForServer() {
		RemoteServer server = syncService.getRemoteServer(2);
		int count = syncService.getCountOfAllTransmissionLogsForServer(server);
		Assert.assertEquals(3, count);
	}

	@Test
	public void deleteOldTransmissionLogs_shouldDeleteUpToaGivenDate() {
		Calendar cal = Calendar.getInstance();
		int may = 4;
		cal.set(2018, may, 3, 10, 30, 50 );
		int all = syncService.getCountOfAllTransmissionLogs();
		int removed = syncService.deleteOldTransmissionLogRecords(cal.getTime());

		int allAfter = syncService.getCountOfAllTransmissionLogs();
		Assert.assertEquals(6, removed);
		Assert.assertEquals(all, removed + allAfter);
	}

	@Test
	public void deleteOldTransmissionLogs_shouldDeleteGivenNumberofDaysOld() {
		Calendar cal = Calendar.getInstance();
		int[] may42018 = {2018, 4, 4};
		cal.set(may42018[0], may42018[1], may42018[2]);

		Date now = new Date();
		int daysPassed = SyncUtil.daysBetween(cal.getTime(), now);
		int all = syncService.getCountOfAllTransmissionLogs();
		int removed = syncService.deleteOldTransmissionLogRecords(daysPassed);

		int allAfter = syncService.getCountOfAllTransmissionLogs();
		Assert.assertEquals(6, removed);
		Assert.assertEquals(all, removed + allAfter);
	}
}