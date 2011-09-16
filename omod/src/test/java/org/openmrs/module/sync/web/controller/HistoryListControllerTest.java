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
package org.openmrs.module.sync.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.server.SyncServerRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;

/**
 * Tests for the {@link HistoryListController}
 */
@Controller
public class HistoryListControllerTest extends BaseModuleContextSensitiveTest {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	@Test
	public void shouldResetSelectedRecordsAsIgnore() throws Exception {
		
		executeDataSet("org/openmrs/module/sync/include/SyncHistoryListRecords.xml");
		authenticate();
		
		SyncService syncService = Context.getService(SyncService.class);
		
		String uuids = "e1165fef-f3ae-411a-8de8-cb07419f90a4 c7c38315-285d-471a-94cd-1fdc71a5459b 2d49b210-f25d-4d55-a6b0-7588680b0fb7";
		String[] uuidArray = uuids.split(" ");
     	for (String uuid : uuidArray) {
     		SyncRecord record = syncService.getSyncRecord(uuid);
     		Assert.assertEquals(SyncRecordState.NEW, record.getState());
					
			for (SyncServerRecord serverRecord : record.getServerRecords()) {
				Assert.assertEquals(SyncRecordState.NEW, serverRecord.getState());
			}
     	}
		
		HistoryListController controller = new HistoryListController();
		controller.historyResetRemoveRecords(new ModelMap(), new MockHttpServletRequest(), uuids, "remove", 1, 1);
		
		for (String uuid : uuidArray) {
     		SyncRecord record = syncService.getSyncRecord(uuid);
     		Assert.assertEquals(SyncRecordState.NOT_SUPPOSED_TO_SYNC, record.getState());
					
			for (SyncServerRecord serverRecord : record.getServerRecords()) {
				Assert.assertEquals(SyncRecordState.NOT_SUPPOSED_TO_SYNC, serverRecord.getState());
			}
     	}
	}
	
	@Test
	public void shouldResetSelectedRecordsAsNew() throws Exception {

		executeDataSet("org/openmrs/module/sync/include/SyncHistoryListRecords.xml");
		authenticate();
		
		SyncService syncService = Context.getService(SyncService.class);
		
		String uuids = "e2609952-21da-432d-9760-53794bfb777a b7748880-235e-4a70-922d-a290966c04a1 ebbf5215-e2d6-4b6d-ae49-79f0f22ee3b4";
		String[] uuidArray = uuids.split(" ");
     	for (String uuid : uuidArray) {
     		SyncRecord record = syncService.getSyncRecord(uuid);
     		Assert.assertEquals(SyncRecordState.NOT_SUPPOSED_TO_SYNC, record.getState());
					
			for (SyncServerRecord serverRecord : record.getServerRecords()) {
				Assert.assertEquals(SyncRecordState.NOT_SUPPOSED_TO_SYNC, serverRecord.getState());
			}
     	}
		
		HistoryListController controller = new HistoryListController();
		controller.historyResetRemoveRecords(new ModelMap(), new MockHttpServletRequest(), uuids, "reset", 1, 1);
		
		for (String uuid : uuidArray) {
     		SyncRecord record = syncService.getSyncRecord(uuid);
     		Assert.assertEquals(SyncRecordState.NEW, record.getState());
					
			for (SyncServerRecord serverRecord : record.getServerRecords()) {
				Assert.assertEquals(SyncRecordState.NEW, serverRecord.getState());
			}
     	}
	}
}
