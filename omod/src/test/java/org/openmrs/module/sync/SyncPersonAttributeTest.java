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
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.springframework.test.annotation.NotTransactional;

/**
 * Testing syncing of PersonAttributes via the Person object
 */
public class SyncPersonAttributeTest extends SyncBaseTest {
	
	@Override
	public String getInitialDataset() {
		return "org/openmrs/module/sync/include/SyncCreateTest.xml";
	}
	
	@Test
	@NotTransactional
	public void shouldSavePersonAttributeTypeAndPersistForeignKeyPK() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			public void runOnChild() throws Exception {
				PersonService personService = Context.getPersonService();
				PatientService patientService = Context.getPatientService();
				
				PersonAttributeType type = personService.getPersonAttributeType(6); // health district
				
				Patient patient = patientService.getPatient(4);
				PersonAttribute attr = new PersonAttribute(type, "Some Location");
				patient.addAttribute(attr);
				//TestUtil.printOutTableContents(getConnection(), "person_attribute");
				patientService.savePatient(patient);
				
			}
			
			public void runOnParent() throws Exception {
				PersonService ps = Context.getPersonService();
				
				Person person = ps.getPerson(4);
				PersonAttribute attr = person.getAttribute(6);
				Assert.assertEquals("Some Location", attr.getValue());
			}
		});
	}
	
}
