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

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.ConceptDatatype;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.TestUtil;
import org.openmrs.test.Verifies;

/**
 * Tests methods in the SyncService
 */
public class SyncServiceTest extends BaseModuleContextSensitiveTest {
	
	/**
	 * @see {@link SyncService#getOpenmrsObjectByUuid(Class,String)}
	 */
	@Test
	@Verifies(value = "should get any openmrs object by its uuid", method = "getOpenmrsObjectByUuid(Class,String)")
	public void getOpenmrsObjectByUuid_shouldGetAnyOpenmrsObjectByItsUuid() throws Exception {
		ConceptDatatype dt = Context.getService(SyncService.class).getOpenmrsObjectByUuid(ConceptDatatype.class,
		    "c5f90600-cdf2-4085-bb61-8952bbbe8cab");
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
    	
    	TestUtil.printOutTableContents(getConnection(), "sync_record", "sync_server_record", "sync_server");
    	
    	SyncService syncService = Context.getService(SyncService.class);
    	
    	// sanity check
    	List<SyncRecord> records = syncService.getSyncRecords();
		Assert.assertEquals(64, records.size());
    	
    	syncService.deleteSyncRecords(null, new Date());
    	
    	TestUtil.printOutTableContents(getConnection(), "sync_record", "sync_server_record", "sync_server");
		
    	Context.clearSession(); // because we have the other records sitting in memory
    	records = syncService.getSyncRecords();
		
		Assert.assertEquals(59, records.size());
    }
	
}
