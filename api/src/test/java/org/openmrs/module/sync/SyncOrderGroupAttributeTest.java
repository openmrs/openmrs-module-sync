package org.openmrs.module.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.OrderGroup;
import org.openmrs.OrderGroupAttribute;
import org.openmrs.OrderAttribute;
import org.openmrs.OrderAttributeType;
import org.openmrs.OrderGroupAttributeType;
import org.openmrs.TestOrder;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.customdatatype.datatype.FreeTextDatatype;
import org.openmrs.customdatatype.datatype.ConceptDatatype;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


public class SyncOrderGroupAttributeTest extends SyncBaseTest{
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
    public void shouldSyncNewOrderAttributeType() throws Exception {
        runSyncTest(new SyncTestHelper() {
            int ORIGINAL_COUNT = 0;
            public void runOnChild() throws Exception {
                OrderService orderService = Context.getOrderService();
                ORIGINAL_COUNT = orderService.getAllOrderGroupAttributeTypes().size();
                OrderGroupAttributeType orderGroupAttributeType = new OrderGroupAttributeType();
                orderGroupAttributeType.setName("Surgery");
                orderGroupAttributeType.setDatatypeClassname(FreeTextDatatype.class.getName());
                orderService.saveOrderGroupAttributeType(orderGroupAttributeType);
                Assertions.assertNotNull(orderGroupAttributeType.getId());
                Assertions.assertEquals(ORIGINAL_COUNT + 1, orderService.getAllOrderGroupAttributeTypes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                OrderService orderService = Context.getOrderService();
                Assertions.assertEquals(ORIGINAL_COUNT + 1 , orderService.getAllOrderGroupAttributeTypes().size());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncNewOrderGroupAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {
            String NEW_ORDER_GROUP_ATTRIBUTE_UUID = null;

            public void runOnChild() throws Exception {
                OrderService orderService = Context.getOrderService();
                OrderAttributeType orderAttributeType = orderService.getOrderAttributeTypeByName("Referral");
                Assertions.assertNotNull(orderAttributeType);
                Order testOrder = new TestOrder();
                testOrder.setPatient(Context.getPatientService().getPatient(6));
                testOrder.setConcept(Context.getConceptService().getConcept(3));
                testOrder.setOrderer(Context.getProviderService().getProvider(1));
                testOrder.setCareSetting(orderService.getCareSetting(1));
                testOrder.setOrderType(orderService.getOrderType(17));
                testOrder.setEncounter(Context.getEncounterService().getEncounter(2));
                testOrder.setDateActivated(new Date());
                final String NAMESPACE = "namespace";
                final String FORMFIELD_PATH = "formFieldPath";
                testOrder.setFormField(NAMESPACE, FORMFIELD_PATH);
                OrderAttribute orderAttribute = new OrderAttribute();
                orderAttribute.setAttributeType(orderAttributeType);
                orderAttribute.setOrder(testOrder);
                orderAttribute.setValueReferenceInternal("new referral value");
                testOrder.addAttribute(orderAttribute);

                OrderGroupAttributeType virology = orderService.getOrderGroupAttributeTypeByName("Virology");
                Assertions.assertNotNull(virology);

                OrderGroup orderGroup = new OrderGroup();
                orderGroup.addOrder(testOrder);
                orderGroup.setPatient(testOrder.getPatient());
                orderGroup.setEncounter(testOrder.getEncounter());

                OrderGroupAttribute orderGroupAttribute = new OrderGroupAttribute();
                orderGroupAttribute.setAttributeType(virology);
                orderGroupAttribute.setOrderGroup(orderGroup);
                orderGroupAttribute.setValueReferenceInternal("Test 1");

                orderGroup.addAttribute(orderGroupAttribute);
                orderGroup = orderService.saveOrderGroup(orderGroup);
                Assertions.assertNotNull(orderGroup.getOrderGroupId());
                NEW_ORDER_GROUP_ATTRIBUTE_UUID = orderGroup.getActiveAttributes(virology).get(0).getUuid();
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                OrderService orderService = Context.getOrderService();
                OrderGroupAttribute orderGroupAttribute = orderService.getOrderGroupAttributeByUuid(NEW_ORDER_GROUP_ATTRIBUTE_UUID);
                Assertions.assertNotNull(orderGroupAttribute);
                Assertions.assertEquals("Test 1", orderGroupAttribute.getValueReference());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncOrderGroupAttributeWithComplexDatatype() throws Exception {
        runSyncTest(new SyncTestHelper() {
            String NEW_ORDER_GROUP_ATTRIBUTE_UUID = null;

            public void runOnChild() throws Exception {
                OrderService orderService = Context.getOrderService();
                OrderAttributeType orderAttributeType = orderService.getOrderAttributeTypeByName("Referral");
                Assertions.assertNotNull(orderAttributeType);
                Order testOrder = new TestOrder();
                testOrder.setPatient(Context.getPatientService().getPatient(6));
                testOrder.setConcept(Context.getConceptService().getConcept(3));
                testOrder.setOrderer(Context.getProviderService().getProvider(1));
                testOrder.setCareSetting(orderService.getCareSetting(1));
                testOrder.setOrderType(orderService.getOrderType(17));
                testOrder.setEncounter(Context.getEncounterService().getEncounter(2));
                testOrder.setDateActivated(new Date());
                final String NAMESPACE = "namespace";
                final String FORMFIELD_PATH = "formFieldPath";
                testOrder.setFormField(NAMESPACE, FORMFIELD_PATH);
                OrderAttribute orderAttribute = new OrderAttribute();
                orderAttribute.setAttributeType(orderAttributeType);
                orderAttribute.setOrder(testOrder);
                orderAttribute.setValueReferenceInternal("new referral value");
                testOrder.addAttribute(orderAttribute);

                OrderGroupAttributeType clinicalTrial = new OrderGroupAttributeType();
                clinicalTrial.setName("clinical trial");
                clinicalTrial.setMinOccurs(1);
                clinicalTrial.setMaxOccurs(5);
                clinicalTrial.setDatatypeClassname(ConceptDatatype.class.getName());
                clinicalTrial = orderService.saveOrderGroupAttributeType(clinicalTrial);
                Assertions.assertNotNull(clinicalTrial.getId());

                OrderGroup orderGroup = new OrderGroup();
                orderGroup.addOrder(testOrder);
                orderGroup.setPatient(testOrder.getPatient());
                orderGroup.setEncounter(testOrder.getEncounter());

                OrderGroupAttribute orderGroupAttribute = new OrderGroupAttribute();
                orderGroupAttribute.setAttributeType(clinicalTrial);
                orderGroupAttribute.setOrderGroup(orderGroup);
                Concept concept = Context.getConceptService().getConcept(3);
                Assertions.assertNotNull(concept);
                orderGroupAttribute.setValueReferenceInternal(concept.getUuid());

                orderGroup.addAttribute(orderGroupAttribute);
                orderGroup = orderService.saveOrderGroup(orderGroup);
                Assertions.assertNotNull(orderGroup.getOrderGroupId());
                NEW_ORDER_GROUP_ATTRIBUTE_UUID = orderGroup.getActiveAttributes(clinicalTrial).get(0).getUuid();
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                OrderService orderService = Context.getOrderService();
                OrderGroupAttribute orderGroupAttribute = orderService.getOrderGroupAttributeByUuid(NEW_ORDER_GROUP_ATTRIBUTE_UUID);
                Assertions.assertNotNull(orderGroupAttribute);
                Concept concept = Context.getConceptService().getConcept(3);
                Assertions.assertNotNull(concept);
                Assertions.assertEquals(concept, orderGroupAttribute.getValue());
            }
        });
    }
}
