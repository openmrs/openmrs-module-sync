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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tests sync of patient identifier
 * See:  SYNC-369
 */
public class SyncPatientIdentifierTest extends SyncBaseTest {

	@Autowired
	PatientService patientService;

	@Override
    public String getInitialDataset() {
        try {
            return "org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	protected List<SyncRecord> getSyncRecords() throws Exception {
		return Context.getService(SyncService.class).getSyncRecords();
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldSyncPatientIdentifiers() throws Exception {
		runSyncTest(new IdentifierSyncTestHelper(null, false));
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldSyncPatientIdentifiersIfPatientProgramIsNullAndFieldIsNotFound() throws Exception {
		runSyncTest(new IdentifierSyncTestHelper("[null]", false));
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldNotSyncPatientIdentifiersIfPatientProgramIsNotNullAndFieldIsNotFound() throws Exception {
		runSyncTest(new IdentifierSyncTestHelper("0bb5846a-168d-102b-8d9c-e44ed545d86c", true));
	}

	class IdentifierSyncTestHelper extends SyncTestHelper {

		public IdentifierSyncTestHelper(String patientProgramValue, boolean expectedException) {
			super();
			this.patientProgramValue = patientProgramValue;
			this.expectedException = expectedException;
		}

		final String patientUuid = "196bef28-168c-102b-8d9c-e44ed545d86c";
		final String patientIdentifierTypeUuid = "3010e86a-144e-102b-8d9c-e44ed545d86c";
		final String locationUuid = "2d865af4-144e-102b-8d9c-e44ed545d86c";
		final String patientIdentifierUuid = "54d3ca4a-d1ee-421c-a74e-5336c7519888";
		final String patientIdentifierValue = "ABC123";
		String patientProgramValue = null;
		boolean expectedException = false;

		public void runOnChild() {
			PatientIdentifier pi = new PatientIdentifier();
			pi.setUuid(patientIdentifierUuid);
			pi.setPatient(patientService.getPatientByUuid(patientUuid));
			pi.setIdentifierType(patientService.getPatientIdentifierTypeByUuid(patientIdentifierTypeUuid));
			pi.setIdentifier(patientIdentifierValue);
			pi.setLocation(Context.getLocationService().getLocationByUuid(locationUuid));
			patientService.savePatientIdentifier(pi);
		}

		@Override
		public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
			if (patientProgramValue != null) {
				SyncRecord syncRecord = syncRecords.get(0);
				Assertions.assertEquals(1, syncRecord.getItems().size());
				SyncItem item = syncRecord.getItems().iterator().next();
				String itemContent = item.getContent();
				String searchString = "<org.openmrs.PatientIdentifier>";
				String textToAppend = "<patientProgram type=\"org.openmrs.PatientProgram\">" + patientProgramValue + "</patientProgram>";
				itemContent = itemContent.replaceAll(searchString, searchString + textToAppend);
				item.setContent(itemContent);
			}
		}

		public void runOnParent() {
			PatientIdentifier pi = patientService.getPatientIdentifierByUuid(patientIdentifierUuid);
			if (expectedException) {
				Assertions.assertNull(pi);
			}
			else {
				Assertions.assertNotNull(pi);
				Assertions.assertEquals(pi.getPatient().getUuid(), patientUuid);
				Assertions.assertEquals(pi.getIdentifierType().getUuid(), patientIdentifierTypeUuid);
				Assertions.assertEquals(pi.getIdentifier(), patientIdentifierValue);
				Assertions.assertEquals(pi.getLocation().getUuid(), locationUuid);
            }
		}

	}
}
