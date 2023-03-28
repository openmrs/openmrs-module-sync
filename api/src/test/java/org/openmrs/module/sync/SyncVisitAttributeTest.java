package org.openmrs.module.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Location;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.customdatatype.datatype.FreeTextDatatype;
import org.openmrs.customdatatype.datatype.LocationDatatype;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class SyncVisitAttributeTest extends SyncBaseTest{
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
    public void shouldSyncNewVisitAttributeType() throws Exception {
        runSyncTest(new SyncTestHelper() {
            int ORIGINAL_COUNT = 0;
            public void runOnChild() throws Exception {
                VisitService visitService = Context.getVisitService();
                ORIGINAL_COUNT = visitService.getAllVisitAttributeTypes().size();
                Assertions.assertTrue( ORIGINAL_COUNT > 0);
                VisitAttributeType visitAttributeType = new VisitAttributeType();
                visitAttributeType.setName("special visit");
                visitAttributeType.setMinOccurs(1);
                visitAttributeType.setMaxOccurs(5);
                visitAttributeType.setDatatypeClassname(FreeTextDatatype.class.getName());
                visitService.saveVisitAttributeType(visitAttributeType);
                Assertions.assertEquals(ORIGINAL_COUNT + 1 , visitService.getAllVisitAttributeTypes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                VisitService visitService = Context.getVisitService();
                Assertions.assertEquals(ORIGINAL_COUNT + 1 , visitService.getAllVisitAttributeTypes().size());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncVisitAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {
            String NEW_ATTRIBUTE_TYPE_UUID="";
            public void runOnChild() throws Exception {
                VisitService visitService = Context.getVisitService();
                int ORIGINAL_COUNT = visitService.getAllVisitAttributeTypes().size();
                Assertions.assertTrue( ORIGINAL_COUNT > 0);
                VisitAttributeType visitAttributeType = new VisitAttributeType();
                visitAttributeType.setName("special visit");
                visitAttributeType.setMinOccurs(1);
                visitAttributeType.setMaxOccurs(5);
                visitAttributeType.setDatatypeClassname(FreeTextDatatype.class.getName());
                visitService.saveVisitAttributeType(visitAttributeType);
                Assertions.assertEquals(ORIGINAL_COUNT + 1 , visitService.getAllVisitAttributeTypes().size());
                NEW_ATTRIBUTE_TYPE_UUID = visitAttributeType.getUuid();
                Visit visit = visitService.getVisit(1);
                Assertions.assertNotNull(visit);
                VisitAttribute visitAttribute = new VisitAttribute();
                visitAttribute.setVisit(visit);
                visitAttribute.setAttributeType(visitAttributeType);
                visitAttribute.setValue("clinical trial");
                visit.addAttribute(visitAttribute);
                visitService.saveVisit(visit);
                Assertions.assertEquals("clinical trial", visit.getActiveAttributes(visitAttributeType).get(0).getValue());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                VisitService visitService = Context.getVisitService();
                VisitAttributeType visitAttributeType = visitService.getVisitAttributeTypeByUuid(NEW_ATTRIBUTE_TYPE_UUID);
                Assertions.assertNotNull(visitAttributeType);
                Visit visit = visitService.getVisit(1);
                Assertions.assertNotNull(visit);
                Assertions.assertEquals("clinical trial", visit.getActiveAttributes(visitAttributeType).get(0).getValue());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncDeletedVisitAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {
            int ORIGINAL_COUNT = 0;
            public void runOnChild() throws Exception {
                VisitService visitService = Context.getVisitService();
                VisitAttributeType visitAttributeType = visitService.getVisitAttributeTypeByUuid("3561BB75-F8E3-4914-8CBF-46EF6E99823B");
                Assertions.assertNotNull(visitAttributeType);
                Visit visit = visitService.getVisit(1);
                Assertions.assertNotNull(visit);
                ORIGINAL_COUNT = visit.getAttributes().size();
                Assertions.assertTrue( ORIGINAL_COUNT > 0);
                VisitAttribute visitAttribute = visit.getActiveAttributes(visitAttributeType).get(0);
                Assertions.assertNotNull(visitAttribute);
                visit.getAttributes().remove(visitAttribute);
                visitService.saveVisit(visit);
                Assertions.assertEquals(ORIGINAL_COUNT - 1 , visit.getAttributes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                VisitService visitService = Context.getVisitService();
                VisitAttributeType visitAttributeType = visitService.getVisitAttributeTypeByUuid("3561BB75-F8E3-4914-8CBF-46EF6E99823B");
                Assertions.assertNotNull(visitAttributeType);
                Visit visit = visitService.getVisit(1);
                Assertions.assertNotNull(visit);
                Assertions.assertEquals(0, visit.getActiveAttributes(visitAttributeType).size());
                Assertions.assertEquals(ORIGINAL_COUNT - 1 , visit.getAttributes().size());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncVisitAttributeWithComplexDatatype() throws Exception {
        runSyncTest(new SyncTestHelper() {
            String NEW_ATTRIBUTE_TYPE_UUID="";
            public void runOnChild() throws Exception {
                VisitService visitService = Context.getVisitService();
                int ORIGINAL_COUNT = visitService.getAllVisitAttributeTypes().size();
                Assertions.assertTrue( ORIGINAL_COUNT > 0);
                VisitAttributeType visitAttributeType = new VisitAttributeType();
                visitAttributeType.setName("special location");
                visitAttributeType.setMinOccurs(1);
                visitAttributeType.setMaxOccurs(5);
                visitAttributeType.setDatatypeClassname(LocationDatatype.class.getName());
                visitService.saveVisitAttributeType(visitAttributeType);
                Assertions.assertEquals(ORIGINAL_COUNT + 1 , visitService.getAllVisitAttributeTypes().size());
                NEW_ATTRIBUTE_TYPE_UUID = visitAttributeType.getUuid();
                Location healthCenterLocation = Context.getLocationService().getLocation("Someplace");
                Assertions.assertNotNull(healthCenterLocation);
                Visit visit = visitService.getVisit(1);
                Assertions.assertNotNull(visit);
                VisitAttribute visitAttribute = new VisitAttribute();
                visitAttribute.setVisit(visit);
                visitAttribute.setAttributeType(visitAttributeType);
                visitAttribute.setValue(healthCenterLocation);
                visit.addAttribute(visitAttribute);
                visitService.saveVisit(visit);
                Assertions.assertEquals(healthCenterLocation, visit.getActiveAttributes(visitAttributeType).get(0).getValue());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                VisitService visitService = Context.getVisitService();
                VisitAttributeType visitAttributeType = visitService.getVisitAttributeTypeByUuid(NEW_ATTRIBUTE_TYPE_UUID);
                Assertions.assertNotNull(visitAttributeType);
                Location healthCenterLocation = Context.getLocationService().getLocation("Someplace");
                Assertions.assertNotNull(healthCenterLocation);
                Visit visit = visitService.getVisit(1);
                Assertions.assertNotNull(visit);
                Assertions.assertEquals(healthCenterLocation, visit.getActiveAttributes(visitAttributeType).get(0).getValue());
            }
        });
    }
}
