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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.springframework.test.annotation.NotTransactional;

/**
 *
 */
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
    @NotTransactional
	public void shouldCreateDrugOrder() throws Exception {
		runSyncTest(new SyncTestHelper() {			

			OrderService orderService = Context.getOrderService();

			public void runOnChild() {

				Patient patient = Context.getPatientService().getPatient(new Integer(2));
				assertNotNull(patient);
				
				Drug drug = Context.getConceptService().getDrugByNameOrId("Advil");
				assertNotNull(drug);
								
				Concept concept = drug.getConcept();
				assertNotNull(concept);

				DrugOrder drugOrder = new DrugOrder();
				drugOrder.setDrug(drug);
				drugOrder.setConcept(concept);
				drugOrder.setPatient(patient);
				drugOrder.setDose(new Double(1.0));
				drugOrder.setFrequency("4 times per day");
				drugOrder.setInstructions("");				
				drugOrder.setStartDate(new Date());	
				drugOrder.setDateCreated(new Date());
				drugOrder.setVoided(new Boolean(false));

				// This is really annoying, but currently 1.10 is backwards incompatible in the OrderService
				// and OrderType, Order, and DrugOrder classes, so this is to try to get it to work.  MS 2/April/2013
				try {
					Method getOrderType = OrderService.class.getDeclaredMethod("getOrderType", Integer.class);
					Object orderType = getOrderType.invoke(orderService, 1);
					assertNotNull(orderType);

					for (Method m : DrugOrder.class.getMethods()) {
						if (m.getName().equals("setOrderType")) {
							m.invoke(drugOrder, orderType);
						}
						if (m.getName().equals("setUnits")) {
							m.invoke(drugOrder, "tabs");
						}
					}
				}
				catch (Exception e) {
					Assert.assertTrue(TestUtil.isOpenmrsVersionAtLeast("1.10"));
				}

				orderService.saveOrder(drugOrder);
				
				List<DrugOrder> orders = Context.getOrderService().getDrugOrdersByPatient(patient);
				assertTrue(orders.size() == 2);
			}
			public void runOnParent() {

				Patient patient = Context.getPatientService().getPatient(new Integer(2));
				List<DrugOrder> orders = Context.getOrderService().getDrugOrdersByPatient(patient);
				Assert.assertEquals(2, orders.size());
			}
		});
	}	

	@Test
    @NotTransactional	
	public void shouldUpdateDrugOrder() throws Exception {
		runSyncTest(new SyncTestHelper() {			
			public void runOnChild() {
				
				DrugOrder order = Context.getOrderService().getOrder(1, DrugOrder.class);

				assert(order.getDose().doubleValue() != 10.0);
				//log.info("Instructions: " + order.getInstructions());
				order.setInstructions("test");
				order.setDose(10.0);
				Context.getOrderService().saveOrder(order);
				Assert.assertEquals(10.0, order.getDose().doubleValue());
				
			}
			public void runOnParent() {

				DrugOrder order = Context.getOrderService().getOrder(1, DrugOrder.class);
				Assert.assertEquals(10.0, order.getDose().doubleValue());
			}
		});
	}
	
	
	/*
	
	public void shouldDeleteDrugOrder() throws Exception { 
		
		runSyncTest(new SyncTestHelper() {			
			AdministrationService adminService = Context.getAdministrationService();
			public void runOnChild() {
		
			
			}
			public void runOnParent() {
			
			}
		});		
	}
	*/
	
	/*
	public void shouldVoidDrugOrder() throws Exception { 
		
		runSyncTest(new SyncTestHelper() {			
			AdministrationService adminService = Context.getAdministrationService();
			EncounterService encounterService = Context.getEncounterService();
			public void runOnChild() {
				Order o = Context.getOrderService().getOrder(orderId);
				Context.getOrderService().voidOrder(o, voidReason);
			}
			public void runOnParent() {
				
			}
		});		
	}	
	*/
	
	/*
	public void shouldDiscontinueDrugOrder() throws Exception { 
		
		runSyncTest(new SyncTestHelper() {			
			AdministrationService adminService = Context.getAdministrationService();
			EncounterService encounterService = Context.getEncounterService();
			public void runOnChild() {				

				Order order = Context.getOrderService().getOrder(orderId);
				Concept discontinueReason = Context.getConceptService().getConceptByIdOrName(discontinueReasonId);
				Context.getOrderService().discontinueOrder(order, discontinueReason, new Date());	

			}
			public void runOnParent() {
				
			}
		});		
	}	
	*/
	
	/*
	public void shouldVoidDrugOrder() throws Exception { 
		
		runSyncTest(new SyncTestHelper() {			
			AdministrationService adminService = Context.getAdministrationService();
			EncounterService encounterService = Context.getEncounterService();
			public void runOnChild() {				
				Patient p = Context.getPatientService().getPatient(patientId);
				Context.getOrderService().voidDrugSet(p, drugSetId, voidReason, OrderService.SHOW_CURRENT);
			}
			public void runOnParent() {
				
			}
		});		
	}		
	*/
	
	/*
	public void shouldVoidDrugOrder() throws Exception { 
		
		runSyncTest(new SyncTestHelper() {			
			AdministrationService adminService = Context.getAdministrationService();
			EncounterService encounterService = Context.getEncounterService();
			public void runOnChild() {				
				Patient p = Context.getPatientService().getPatient(patientId);
				Context.getOrderService().voidDrugSet(p, drugSetId, voidReason, OrderService.SHOW_COMPLETE);	
			}
			public void runOnParent() {
				
			}
		});		
	}			
	*/
	

}
