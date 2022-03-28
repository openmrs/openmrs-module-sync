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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.RelationshipType;
import org.openmrs.api.context.Context;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testing of delete methods and whether that action is synchronized
 */
public class SyncOnDeleteTest extends SyncBaseTest {

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
	public void shouldDeletePatientIdentfierType() throws Exception {
		runSyncTest(new SyncTestHelper() {
			public void runOnChild(){				
				// make sure the patient identifier type is there
				PatientIdentifierType pit = Context.getPatientService().getPatientIdentifierType(1);
				assertNotNull("The patient identifier type could not be found in child server!", pit);
				
				// do the deleting
				Context.getPatientService().purgePatientIdentifierType(pit);
				
				pit = Context.getPatientService().getPatientIdentifierType(1);
				assertNull("The patient identifier type should have been deleted!", pit);
			}
			public void runOnParent() {
				// make sure it was deleted by sync
				PatientIdentifierType pit = Context.getPatientService().getPatientIdentifierType(1);
				assertNull("The patient identifier type should have been deleted!", pit);
			}
		});
	}	
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldDeleteRelationshipType() throws Exception {
		runSyncTest(new SyncTestHelper() {
			public void runOnChild(){
				// make sure the patient identifier type is there
				RelationshipType rt = Context.getPersonService().getRelationshipType(1);
				assertNotNull("The relationship type could not be found in child server!", rt);
				
				// do the deleting
				Context.getPersonService().purgeRelationshipType(rt);
				
				rt = Context.getPersonService().getRelationshipType(1);
				assertNull("The relationship type should have been deleted!", rt);
			}
			public void runOnParent() {
				// make sure it was deleted by sync
				RelationshipType rt = Context.getPersonService().getRelationshipType(1);
				assertNull("The relationship type should have been deleted!", rt);
			}
		});
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldDeletePersonAttributeType() throws Exception {
		runSyncTest(new SyncTestHelper() {
			public void runOnChild(){
				// make sure the patient identifier type is there
				PersonAttributeType pat = Context.getPersonService().getPersonAttributeType(1);
				assertNotNull("The PersonAttributeType could not be found in child server!", pat);
				
				// do the deleting
				Context.getPersonService().purgePersonAttributeType(pat);
				
				pat = Context.getPersonService().getPersonAttributeType(1);
				assertNull("The PersonAttributeType should have been deleted!", pat);
			}
			public void runOnParent() {
				// make sure it was deleted by sync
				PersonAttributeType pat = Context.getPersonService().getPersonAttributeType(1);
				assertNull("The PersonAttributeType should have been deleted!", pat);
			}
		});
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldDeletePatientName() throws Exception {
		runSyncTest(new SyncTestHelper() {
			PatientIdentifierType pit;
			public void runOnChild() {
				pit = Context.getPatientService().getPatientIdentifierType(2);
				Location loc = Context.getLocationService().getLocation("Someplace");
				Patient p = Context.getPatientService().getPatient(2);
				p.removeName(p.getPersonName());
				p.addName(new PersonName("Peter", null, "Parker"));
				p.addIdentifier(new PatientIdentifier("super123", pit, loc));
				Context.getPatientService().savePatient(p);
			}
			public void runOnParent() {
				Patient p = Context.getPatientService().getPatient(2);
				assertEquals("Name should be Peter Parker", "Peter Parker", p.getPersonName().toString());
				boolean found = false;
				for (PatientIdentifier id : p.getIdentifiers())
					if (id.getIdentifier().equals("super123") && id.getIdentifierType().equals(pit))
						found = true;
				assertTrue("Couldn't find new ID", found);
			}
		});
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldDeletePatient() throws Exception {
		runSyncTest(new SyncTestHelper() {
			Integer patientId = 4;
			
			public void runOnChild() {
				Patient p = Context.getPatientService().getPatient(patientId);
				Context.getPatientService().purgePatient(p);				
			}
			public void runOnParent() {
				Patient p = Context.getPatientService().getPatient(patientId);
				assertNull("Patient should have been deleted!", p);
			}
		});
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldDeleteConcept() throws Exception {
			runSyncTest(new SyncTestHelper() {
				// note that we need to test a concept that can be deleted (i.e., doesn't have any associated answer concepts, obs, etc)  but that has an associated concept word so we can test the special case of
				// needing to explicitly delete concept words
				Integer conceptId = 14;  
				
				public void runOnChild() {
					Concept conceptToDelete = Context.getConceptService().getConcept(conceptId);
					
					// delete the concept
					Context.getConceptService().purgeConcept(conceptToDelete);
				}
				
				public void runOnParent() {
					Context.clearSession();
					
					// confirm that it has been deleted on parent
					Concept deletedConcept = Context.getConceptService().getConcept(conceptId);
					Assert.assertNull(deletedConcept); 
				}
			});
	}
}
