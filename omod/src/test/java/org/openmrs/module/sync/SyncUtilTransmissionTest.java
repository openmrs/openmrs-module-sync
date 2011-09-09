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

import junit.framework.Assert;
import org.junit.Ignore;

import org.junit.Test;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncTransmissionState;
import org.openmrs.module.sync.SyncUtilTransmission.ReceivingSize;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.ingest.SyncTransmissionResponse;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.annotation.NotTransactional;


/**
 * Tests methods on the SyncUtilTransmission class.
 */
public class SyncUtilTransmissionTest extends BaseModuleContextSensitiveTest implements Runnable {
	
	@Ignore
	@Test
	public void doFullSynchronize_shouldRunOneSyncTask() throws Exception {
		executeDataSet("org/openmrs/module/sync/include/SyncRemoteChildServer.xml");
		
		new Thread(this).start();
		
		SyncTransmissionResponse response = SyncUtilTransmission.doFullSynchronize(new ReceivingSize());
		Assert.assertEquals(SyncTransmissionState.ERROR_CANNOT_RUN_PARALLEL, response.getState());
	}
	
	public void run() {	
		SyncTransmissionResponse response = SyncUtilTransmission.doFullSynchronize(new ReceivingSize());
		Assert.assertEquals(SyncTransmissionState.OK_NOTHING_TO_DO, response.getState());
    }
}
