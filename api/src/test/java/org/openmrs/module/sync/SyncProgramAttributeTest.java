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
import org.junit.Test;
import org.openmrs.PatientProgram;
import org.openmrs.PatientProgramAttribute;
import org.openmrs.ProgramAttributeType;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

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
                assertEquals(1, attributes.size());
                PatientProgramAttribute attribute = attributes.iterator().next();
                assertEquals("1", attribute.getAttributeType().getId().toString());
                assertEquals("1", attribute.getPatientProgram().getId().toString());
                assertEquals("PENDING", attribute.getValueReference());
                assertEquals("PENDING", attribute.getValue());
            }
        });
    }

}
