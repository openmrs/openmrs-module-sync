package org.openmrs.module.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.LocationAttributeType;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class SyncLocationAttributeTest extends SyncBaseTest{
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
    public void shouldSyncNewLocationAttributeType() throws Exception {
        runSyncTest(new SyncTestHelper() {
            int ORIGINAL_COUNT = 0;
            public void runOnChild() throws Exception {
                LocationService locationService = Context.getLocationService();
                List<LocationAttributeType> allLocationAttributeTypes = locationService.getAllLocationAttributeTypes();
                Assertions.assertNotNull(allLocationAttributeTypes);
                ORIGINAL_COUNT = allLocationAttributeTypes.size();
                Assertions.assertTrue( ORIGINAL_COUNT > 1);
                LocationAttributeType phoneAttrType = new LocationAttributeType();
                phoneAttrType.setName("Facility Phone");
                phoneAttrType.setMinOccurs(0);
                phoneAttrType.setMaxOccurs(1);
                phoneAttrType.setDatatypeClassname("org.openmrs.customdatatype.datatype.FreeTextDatatype");
                locationService.saveLocationAttributeType(phoneAttrType);
                Assertions.assertTrue(locationService.getAllLocationAttributeTypes().size() == ORIGINAL_COUNT + 1);
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                LocationService locationService = Context.getLocationService();
                //A new ConceptAttributeType has been added
                Assertions.assertEquals(ORIGINAL_COUNT + 1, locationService.getAllLocationAttributeTypes().size());
                LocationAttributeType newLocationAttributeType = locationService.getLocationAttributeTypeByName("Facility Phone") ;
                // The new LocationAttributeType has been synced up to the parent
                Assertions.assertNotNull(newLocationAttributeType);
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncEditedLocationAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {
            int ORIGINAL_COUNT = 0;
            public void runOnChild() throws Exception {
                LocationService locationService = Context.getLocationService();
                LocationAttributeType attributeType = locationService.getLocationAttributeTypeByName("Text info");
                Assertions.assertNotNull(attributeType);
                Location location = locationService.getLocation("Other place");
                Assertions.assertNotNull(location);
                List<LocationAttribute> activeAttributes = location.getActiveAttributes(attributeType);
                Assertions.assertNotNull(activeAttributes);
                LocationAttribute locationAttribute = activeAttributes.get(0);
                Assertions.assertEquals("Safe location", locationAttribute.getValue());
                locationAttribute.setValue("New updated location");
                locationService.saveLocation(location);
                Assertions.assertEquals("New updated location", location.getActiveAttributes(attributeType).get(0).getValue());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                LocationService locationService = Context.getLocationService();
                LocationAttributeType attributeType = locationService.getLocationAttributeTypeByName("Text info");
                Assertions.assertNotNull(attributeType);
                Location location = locationService.getLocation("Other place");
                Assertions.assertNotNull(location);
                List<LocationAttribute> activeAttributes = location.getActiveAttributes(attributeType);
                Assertions.assertNotNull(activeAttributes);
                LocationAttribute locationAttribute = activeAttributes.get(0);
                Assertions.assertEquals("New updated location", locationAttribute.getValue());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Disabled // This test is disabled for OpenMRS 2.7+ due to conflict between NOT_SUPPORTED operation and removing an attribute
    public void shouldSyncDeletedLocationAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {
            int ORIGINAL_COUNT = 0;
            public void runOnChild() throws Exception {
                LocationService locationService = Context.getLocationService();
                LocationAttributeType attributeType = locationService.getLocationAttributeTypeByName("Text info");
                Assertions.assertNotNull(attributeType);
                Location location = locationService.getLocation("Other place");
                Assertions.assertNotNull(location);
                List<LocationAttribute> activeAttributes = location.getActiveAttributes(attributeType);
                Assertions.assertNotNull(activeAttributes);
                LocationAttribute locationAttribute = activeAttributes.get(0);
                location.getAttributes().remove(locationAttribute);
                locationService.saveLocation(location);
                Assertions.assertEquals(0,  location.getActiveAttributes(attributeType).size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                LocationService locationService = Context.getLocationService();
                LocationAttributeType attributeType = locationService.getLocationAttributeTypeByName("Text info");
                Assertions.assertNotNull(attributeType);
                Location location = locationService.getLocation("Other place");
                Assertions.assertNotNull(location);
                Assertions.assertEquals(0,  location.getActiveAttributes(attributeType).size());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncLocationAttributeWithComplexDatatype() throws Exception {
        runSyncTest(new SyncTestHelper() {
            int ORIGINAL_COUNT = 0;
            public void runOnChild() throws Exception {
                LocationService locationService = Context.getLocationService();
                List<LocationAttributeType> allLocationAttributeTypes = locationService.getAllLocationAttributeTypes();
                Assertions.assertNotNull(allLocationAttributeTypes);
                ORIGINAL_COUNT = allLocationAttributeTypes.size();
                Assertions.assertTrue( ORIGINAL_COUNT > 1);
                LocationAttributeType clinicType = new LocationAttributeType();
                clinicType.setName("Speciality Clinic");
                clinicType.setMinOccurs(0);
                clinicType.setMaxOccurs(1);
                clinicType.setDatatypeClassname("org.openmrs.customdatatype.datatype.ConceptDatatype");
                locationService.saveLocationAttributeType(clinicType);
                Assertions.assertTrue(locationService.getAllLocationAttributeTypes().size() == ORIGINAL_COUNT + 1);
                Location location = locationService.getLocation("Other place");
                Assertions.assertNotNull(location);
                Concept concept = Context.getConceptService().getConcept(3);
                Assertions.assertNotNull(concept);
                LocationAttribute locationAttribute = new LocationAttribute();
                locationAttribute.setAttributeType(clinicType);
                locationAttribute.setValue(concept);
                location.addAttribute(locationAttribute);
                locationService.saveLocation(location);
                Assertions.assertEquals(1, location.getActiveAttributes(clinicType).size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                LocationService locationService = Context.getLocationService();
                //A new ConceptAttributeType has been added
                Assertions.assertEquals(ORIGINAL_COUNT + 1, locationService.getAllLocationAttributeTypes().size());
                LocationAttributeType newLocationAttributeType = locationService.getLocationAttributeTypeByName("Speciality Clinic") ;
                // The new LocationAttributeType has been synced up to the parent
                Assertions.assertNotNull(newLocationAttributeType);
                Location location = locationService.getLocation("Other place");
                Assertions.assertNotNull(location);
                Concept concept = Context.getConceptService().getConcept(3);
                Assertions.assertNotNull(concept);
                Assertions.assertEquals(1, location.getActiveAttributes(newLocationAttributeType).size());
                LocationAttribute locationAttribute = location.getActiveAttributes(newLocationAttributeType).get(0);
                Assertions.assertNotNull(locationAttribute);
                Assertions.assertEquals(concept, locationAttribute.getValue());
            }
        });
    }
}
