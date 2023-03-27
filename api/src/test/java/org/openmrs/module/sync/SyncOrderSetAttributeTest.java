package org.openmrs.module.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.OrderSet;
import org.openmrs.OrderSetAttribute;
import org.openmrs.OrderSetAttributeType;
import org.openmrs.OrderSetMember;
import org.openmrs.api.OrderSetService;
import org.openmrs.api.context.Context;
import org.openmrs.customdatatype.datatype.FreeTextDatatype;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SyncOrderSetAttributeTest extends SyncBaseTest{
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
    public void shouldSyncNewOrderSetAttributeType() throws Exception {
        runSyncTest(new SyncTestHelper() {
            int ORIGINAL_COUNT = 0;
            public void runOnChild() throws Exception {
                OrderSetService orderSetService = Context.getOrderSetService();
                ORIGINAL_COUNT = orderSetService.getAllOrderSetAttributeTypes().size();
                OrderSetAttributeType orderSetAttributeType = new OrderSetAttributeType();
                orderSetAttributeType.setName("Surgery");
                orderSetAttributeType.setDatatypeClassname(FreeTextDatatype.class.getName());
                orderSetService.saveOrderSetAttributeType(orderSetAttributeType);
                Assertions.assertNotNull(orderSetAttributeType.getId());
                Assertions.assertEquals(ORIGINAL_COUNT + 1, orderSetService.getAllOrderSetAttributeTypes().size() );
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                OrderSetService orderSetService = Context.getOrderSetService();
                Assertions.assertEquals(ORIGINAL_COUNT + 1, orderSetService.getAllOrderSetAttributeTypes().size() );
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncOrderSetAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {
            String NEW_ORDER_SET_ATTRIBUTE_UUID = null;
            public void runOnChild() throws Exception {

                OrderSetService orderSetService = Context.getOrderSetService();
                OrderSetAttributeType attributeType = orderSetService.getOrderSetAttributeTypeByName("Special order");
                Assertions.assertNotNull(attributeType);

                OrderSet orderSet = new OrderSet();
                orderSet.setOperator(OrderSet.Operator.ALL);
                orderSet.setName("NewOrderSet");
                orderSet.setDescription("New Order Set");
                OrderSetMember orderSetMember = new OrderSetMember();
                orderSetMember.setOrderType(Context.getOrderService().getOrderType(1));
                orderSetMember.setConcept(Context.getConceptService().getConcept(3));
                orderSetMember.setCreator(attributeType.getCreator());
                orderSetMember.setDateCreated(new Date());
                orderSetMember.setRetired(false);
                List<OrderSetMember> orderSetMembers = new ArrayList<>(Arrays.asList(orderSetMember));
                orderSet.setOrderSetMembers(orderSetMembers);
                OrderSetAttribute orderSetAttribute = new OrderSetAttribute();
                orderSetAttribute.setAttributeType(attributeType);
                orderSetAttribute.setOrderSet(orderSet);
                orderSetAttribute.setValueReferenceInternal("order set 1");

                orderSet.addAttribute(orderSetAttribute);
                orderSet = orderSetService.saveOrderSet(orderSet);

                Assertions.assertNotNull(orderSet.getId());
                NEW_ORDER_SET_ATTRIBUTE_UUID = orderSet.getActiveAttributes(attributeType).get(0).getUuid();
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                OrderSetService orderSetService = Context.getOrderSetService();
                OrderSetAttribute orderSetAttribute = orderSetService.getOrderSetAttributeByUuid(NEW_ORDER_SET_ATTRIBUTE_UUID);
                Assertions.assertNotNull(orderSetAttribute);
                Assertions.assertEquals("order set 1", orderSetAttribute.getValueReference());
            }
        });
    }
}
