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

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testing syncing of the {@link Obs} object
 */
public class SyncObsTest extends SyncBaseTest {
	
	@Override
	public String getInitialDataset() {
        try {
            return "org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldSyncVoidedObs() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			String uuid = null;
			
			public void runOnChild() throws Exception {
				ObsService os = Context.getObsService();
				
				Obs obs = os.getObs(3);
				obs.setValueText("Some value");
				Obs newlySavedObs = os.saveObs(obs, "testing the voiding process");

				// make sure the "new obsId:" in the voidReason gets changed to a uuid
				SyncService ss = Context.getService(SyncService.class);
				List<SyncRecord> records = ss.getSyncRecords();
				SyncRecord record = records.get(records.size() - 1);
				SyncItem item = record.getItems().toArray(new SyncItem[] {})[1];
				Assert.assertTrue(item.getContent().contains("testing the voiding process"));
				
				uuid = newlySavedObs.getUuid(); // we'll check the new obs on the other side for this uuid 
			}
			
			public void runOnParent() throws Exception {
				ObsService os = Context.getObsService();
				SyncService ss = Context.getService(SyncService.class);
				
				// test to make sure the voidReason references the right new obs

				Obs newObs = ss.getOpenmrsObjectByUuid(Obs.class, uuid); // this is the new obs that was created by the update
				Obs voidedObs = os.getObs(3); // this is the old obs that was edited and hence voided
				
				// voidReason should be ".... (new obsId: 5)"
				Assert.assertTrue(voidedObs.getVoidReason().equals("testing the voiding process"));
			}
		});
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldSyncObsGroup() throws Exception {
		runSyncTest(new SyncTestHelper() {

			String uuid = null;

			public void runOnChild() throws Exception {
				EncounterService es = Context.getEncounterService();

				Encounter e = es.getEncounter(1);
				Obs group = new Obs();
				group.setEncounter(e);
				group.setPerson(e.getPatient());
				group.setObsDatetime(e.getEncounterDatetime());
				group.setConcept(Context.getConceptService().getConcept(15));

				Obs civilStatus = new Obs();
				civilStatus.setEncounter(e);
				civilStatus.setPerson(e.getPatient());
				civilStatus.setObsDatetime(e.getEncounterDatetime());
				civilStatus.setConcept(Context.getConceptService().getConcept(12));  // Civil Status
				civilStatus.setValueCoded(Context.getConceptService().getConcept(13));  // Married
				group.addGroupMember(civilStatus);

				Obs weight = new Obs();
				weight.setEncounter(e);
				weight.setPerson(e.getPatient());
				weight.setObsDatetime(e.getEncounterDatetime());
				weight.setConcept(Context.getConceptService().getConcept(10));  // Weight
				weight.setValueNumeric(50.0);
				group.addGroupMember(weight);

				e.addObs(group);

				e = es.saveEncounter(e);

				// make sure the "new obsId:" in the voidReason gets changed to a uuid
				SyncService ss = Context.getService(SyncService.class);
				List<SyncRecord> records = ss.getSyncRecords();

				uuid = e.getUuid(); // we'll check the new obs on the other side for this uuid
			}

			public void runOnParent() throws Exception {
				EncounterService es = Context.getEncounterService();
				SyncService ss = Context.getService(SyncService.class);

				Encounter encounter = es.getEncounterByUuid(uuid);
				assertEquals(encounter.getObs().size(), 3);
			}
		});
	}
	
}
