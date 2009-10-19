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

import java.util.List;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.handler.ORUR01Handler;
import org.openmrs.test.TestUtil;
import org.springframework.test.annotation.NotTransactional;

import ca.uhn.hl7v2.app.MessageTypeRouter;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.GenericParser;


/**
 * Tests the ORUR01 parser 
 */
public class SyncORUR01HandlerTest extends SyncBaseTest {
	
	// hl7 parser to be used throughout
	
	
	@Override
	public String getInitialDataset() {
		return "org/openmrs/module/sync/include/SyncCreateTest.xml";
	}
	
	@Test
	@NotTransactional
	public void shouldFindUuidsOnConcepts() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			Patient patient = new Patient(4);
			Concept question = new Concept(10);
			
			public void runOnChild() throws Exception {
				// sanity check to make sure the right number of observations are there
				List<Obs> obsForPatient = Context.getObsService().getObservationsByPersonAndConcept(patient, question);
				assertEquals(0, obsForPatient.size());
				
				
				GenericParser parser = new GenericParser();
				MessageTypeRouter router = new MessageTypeRouter();
				router.registerApplication("ORU", "R01", new ORUR01Handler());
				
				String hl7string = "MSH|^~\\&|FORMENTRY|AMRS.ELD|HL7LISTENER|AMRS.ELD|20080902151831||ORU^R01|yow3LEP6bycnLfoPyI31|P|2.5|1||||||||3^AMRS.ELD.FORMID\rPID|||4^^^^||Indakasi^Testarius^Ambote||\rPV1||O|1||||1^Super User (1-8)|||||||||||||||||||||||||||||||||||||20080831|||||||V\rORC|RE||||||||20080902150000|1^Super User\rOBR|1|||1238^MEDICAL RECORD OBSERVATIONS^99DCT\rOBX|1|NM|10^WEIGHT^99DCT||88.87|||||||||20090930";
				Message hl7message = parser.parse(hl7string);
				router.processMessage(hl7message);
			}
			
			public void runOnParent() throws Exception {
				List<Obs> obsForPatient = Context.getObsService().getObservationsByPersonAndConcept(patient, question);
				assertEquals(1, obsForPatient.size()); // there should be one more obs now for this patient
				assertEquals(88.87, obsForPatient.get(0).getValueNumeric(), 0);
				TestUtil.printOutTableContents(getConnection(), "obs");
			}
		});
	}
	
}
