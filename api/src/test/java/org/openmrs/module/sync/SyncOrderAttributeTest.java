package org.openmrs.module.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.TestOrder;
import org.openmrs.OrderAttribute;
import org.openmrs.OrderAttributeType;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.customdatatype.datatype.ConceptDatatype;
import org.openmrs.customdatatype.datatype.FreeTextDatatype;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public class SyncOrderAttributeTest extends SyncBaseTest {
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
                ORIGINAL_COUNT = orderService.getAllOrderAttributeTypes().size();
                Order order = orderService.getOrder(1);
                Assertions.assertNotNull(order);
                OrderAttributeType orderAttributeType = new OrderAttributeType();
                orderAttributeType.setName("External Referral");
                orderAttributeType.setMinOccurs(1);
                orderAttributeType.setMaxOccurs(5);
                orderAttributeType.setDatatypeClassname(FreeTextDatatype.class.getName());
                orderService.saveOrderAttributeType(orderAttributeType);
                Assertions.assertEquals(ORIGINAL_COUNT + 1 , orderService.getAllOrderAttributeTypes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                OrderService orderService = Context.getOrderService();
                Assertions.assertEquals(ORIGINAL_COUNT + 1 , orderService.getAllOrderAttributeTypes().size());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncNewOrderAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {
            String NEW_ORDER_ATTRIBUTE_UUID = null;

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
                testOrder = orderService.saveOrder(testOrder, null);
                Assertions.assertEquals("new referral value", testOrder.getActiveAttributes(orderAttributeType).get(0).getValueReference());
                NEW_ORDER_ATTRIBUTE_UUID = testOrder.getActiveAttributes(orderAttributeType).get(0).getUuid();
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                OrderService orderService = Context.getOrderService();
                OrderAttribute orderAttribute = orderService.getOrderAttributeByUuid(NEW_ORDER_ATTRIBUTE_UUID);
                Assertions.assertNotNull(orderAttribute);
                Assertions.assertEquals("new referral value", orderAttribute.getValueReference());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldOrderAttributeWithComplexDatatype() throws Exception {
        runSyncTest(new SyncTestHelper() {
            String NEW_ORDER_ATTRIBUTE_UUID = null;

            public void runOnChild() throws Exception {
                OrderService orderService = Context.getOrderService();
                OrderAttributeType orderAttributeType = new OrderAttributeType();
                orderAttributeType.setName("Special study reference");
                orderAttributeType.setMinOccurs(1);
                orderAttributeType.setMaxOccurs(5);
                orderAttributeType.setDatatypeClassname(ConceptDatatype.class.getName());
                orderAttributeType = orderService.saveOrderAttributeType(orderAttributeType);
                Assertions.assertNotNull(orderAttributeType.getId());

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
                Concept concept = Context.getConceptService().getConcept(3);
                Assertions.assertNotNull(concept);
                //orderAttribute.setValue(concept);
                orderAttribute.setValueReferenceInternal(concept.getUuid());
                testOrder.addAttribute(orderAttribute);
                testOrder = orderService.saveOrder(testOrder, null);
                Assertions.assertEquals(concept, testOrder.getActiveAttributes(orderAttributeType).get(0).getValue());
                NEW_ORDER_ATTRIBUTE_UUID = testOrder.getActiveAttributes(orderAttributeType).get(0).getUuid();
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                OrderService orderService = Context.getOrderService();
                OrderAttribute orderAttribute = orderService.getOrderAttributeByUuid(NEW_ORDER_ATTRIBUTE_UUID);
                Assertions.assertNotNull(orderAttribute);
                Concept concept = Context.getConceptService().getConcept(3);
                Assertions.assertNotNull(concept);
                Assertions.assertEquals(concept, orderAttribute.getValue());
            }
        });
    }
}
