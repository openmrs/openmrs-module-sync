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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SyncDrugOrderTest extends SyncBaseTest {

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
	public void shouldCreateDrugOrder() throws Exception {

		runSyncTest(new SyncTestHelper() {			

			PatientService patientService = Context.getPatientService();
			EncounterService encounterService = Context.getEncounterService();
			OrderService orderService = Context.getOrderService();
			ConceptService conceptService = Context.getConceptService();
			ProviderService providerService = Context.getProviderService();

			public void runOnChild() throws Exception {

				Patient patient = patientService.getPatient(2);
				assertThat(patient, notNullValue());

				Date encounterDate = new Date();
				
				Drug drug = conceptService.getDrug("Advil");
				assertThat(drug, notNullValue());

				Encounter encounter = new Encounter();
				encounter.setPatient(patient);
				encounter.setEncounterDatetime(encounterDate);
				encounter.setEncounterType(encounterService.getEncounterType("ADULTINITIAL"));

				DrugOrder drugOrder = new DrugOrder();
				drugOrder.setPatient(patient);
				drugOrder.setEncounter(encounter);
				drugOrder.setAction(Order.Action.NEW);
				drugOrder.setDrug(drug);
				drugOrder.setUrgency(Order.Urgency.ROUTINE);
				drugOrder.setDateActivated(encounterDate);
				drugOrder.setCareSetting(orderService.getCareSetting(2));
				drugOrder.setOrderType(orderService.getOrderType(1));
				drugOrder.setDosingType(FreeTextDosingInstructions.class);
				drugOrder.setDosingInstructions("Test instructions");
				drugOrder.setOrderer(providerService.getProvider(1));

				encounter.addOrder(drugOrder);

				encounterService.saveEncounter(encounter);

				List<Order> orders = orderService.getAllOrdersByPatient(patient);
				assertThat(orders.size(), is(1));
			}

			public void runOnParent() throws Exception {
				Patient patient = patientService.getPatient(2);
				List<Order> orders = orderService.getAllOrdersByPatient(patient);
				assertThat(orders.size(), is(1));
			}
		});
	}
}
