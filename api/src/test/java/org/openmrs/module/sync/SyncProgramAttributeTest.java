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
import org.openmrs.Location;
import org.openmrs.PatientProgram;
import org.openmrs.PatientProgramAttribute;
import org.openmrs.ProgramAttributeType;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.List;
import java.util.Set;

/**
 * Testing syncing of PatientProgramAttributes
 */
public class SyncProgramAttributeTest extends SyncBaseTest {

    @Autowired
    @Qualifier("programWorkflowService")
    ProgramWorkflowService programService;

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

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldSyncProgramAttributeOfTypeLocation() throws Exception {
		runSyncTest(new SyncTestHelper() {

			public void runOnChild() throws Exception {

				ProgramAttributeType programAttributeType = programService.getProgramAttributeType(2); // Health Center Location
				Assertions.assertNotNull(programAttributeType);
				PatientProgram pp = programService.getPatientProgram(1);
				Assertions.assertNotNull(pp);
				Location healthCenterLocation = Context.getLocationService().getLocation("Someplace");
				Assertions.assertNotNull(healthCenterLocation);
				PatientProgramAttribute attr = new PatientProgramAttribute();
				attr.setAttributeType(programAttributeType);
				attr.setValue(healthCenterLocation);
				attr.setPatientProgram(pp);
				pp.addAttribute(attr);
				programService.savePatientProgram(pp);
				List<PatientProgramAttribute> activeAttributes = pp.getActiveAttributes(programAttributeType);
				Assertions.assertEquals(1, activeAttributes.size());
			}

			@Override
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				super.changedBeingApplied(syncRecords, record);
			}

			public void runOnParent() throws Exception {
				PatientProgram pp = programService.getPatientProgram(1);
				Set<PatientProgramAttribute> attributes = pp.getAttributes();
				Assertions.assertEquals(1, attributes.size());
				Location location = (Location) attributes.iterator().next().getValue();
				Assertions.assertNotNull(location);
				Assertions.assertEquals(location.getName(), "Someplace");
			}
		});
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldSyncProgramAttributeWithUpdatedValue() throws Exception {
		runSyncTest(new SyncTestHelper() {

			public void runOnChild() throws Exception {
				ProgramAttributeType programAttributeType = programService.getProgramAttributeTypeByUuid("42ec23bf-b9f9-11ed-9873-0242ac120002");
				Assertions.assertNotNull(programAttributeType);
				Assertions.assertEquals(programAttributeType.getName(), "Transfer Status");
				PatientProgram pp = programService.getPatientProgram(2);
				List<PatientProgramAttribute> activeAttributes = pp.getActiveAttributes(programAttributeType);
				Assertions.assertEquals(1, activeAttributes.size());
				PatientProgramAttribute attr = activeAttributes.get(0);
				Assertions.assertEquals(attr.getValueReference(), "transfer-in");
				attr.setValue("transfer-out");
				programService.savePatientProgram(pp);
			}

			@Override
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				super.changedBeingApplied(syncRecords, record);
			}

			public void runOnParent() throws Exception {
				PatientProgram pp = programService.getPatientProgram(2);
				Set<PatientProgramAttribute> attributes = pp.getAttributes();
				Assertions.assertEquals(1, attributes.size());
				PatientProgramAttribute attribute = attributes.iterator().next();
				Assertions.assertEquals(1, attribute.getAttributeType().getId());
				Assertions.assertEquals(2, attribute.getPatientProgram().getId());
				Assertions.assertEquals("transfer-out", attribute.getValueReference());
				Assertions.assertEquals("transfer-out", attribute.getValue());
			}
		});
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldSyncProgramAttributeWhichWasDeleted() throws Exception {
		runSyncTest(new SyncTestHelper() {

			public void runOnChild() throws Exception {
				ProgramAttributeType programAttributeType = programService.getProgramAttributeTypeByUuid("42ec23bf-b9f9-11ed-9873-0242ac120002");
				Assertions.assertNotNull(programAttributeType);
				Assertions.assertEquals(programAttributeType.getName(), "Transfer Status");
				PatientProgram pp = programService.getPatientProgram(2);
				List<PatientProgramAttribute> activeAttributes = pp.getActiveAttributes(programAttributeType);
				Assertions.assertEquals(1, activeAttributes.size());
				PatientProgramAttribute attr = activeAttributes.get(0);
				Assertions.assertEquals(attr.getValue(), "transfer-in");
				pp.getAttributes().remove(attr);
				Assertions.assertEquals(0, pp.getAttributes().size());
				programService.savePatientProgram(pp);
				Assertions.assertEquals(0, pp.getAttributes().size()); // the attribute has been removed
			}

			@Override
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				super.changedBeingApplied(syncRecords, record);
			}

			public void runOnParent() throws Exception {
				PatientProgram pp = programService.getPatientProgram(2);
				Set<PatientProgramAttribute> attributes = pp.getAttributes();
				Assertions.assertEquals(0, attributes.size());
			}
		});
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldSyncProgramAttribute() throws Exception {
		runSyncTest(new SyncTestHelper() {

			public void runOnChild() throws Exception {
				ProgramAttributeType pat = programService.getProgramAttributeType(1); // Transfer Status
                PatientProgram pp = programService.getPatientProgram(1);
				PatientProgramAttribute attr = new PatientProgramAttribute();
				attr.setValue("PENDING");
                attr.setAttributeType(pat);
                attr.setPatientProgram(pp);
				pp.addAttribute(attr);
                programService.savePatientProgram(pp);
			}

			@Override
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				super.changedBeingApplied(syncRecords, record);
			}

			public void runOnParent() throws Exception {
                PatientProgram pp = programService.getPatientProgram(1);
				Set<PatientProgramAttribute> attributes = pp.getAttributes();
				Assertions.assertEquals(1, attributes.size());
				PatientProgramAttribute attribute = attributes.iterator().next();
				Assertions.assertEquals(1, attribute.getAttributeType().getId());
				Assertions.assertEquals(1, attribute.getPatientProgram().getId());
				Assertions.assertEquals("PENDING", attribute.getValueReference());
				Assertions.assertEquals("PENDING", attribute.getValue());
			}
		});
	}
	
}
