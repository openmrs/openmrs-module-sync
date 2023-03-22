package org.openmrs.module.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptAttribute;
import org.openmrs.ConceptAttributeType;
import org.openmrs.Location;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.customdatatype.datatype.FreeTextDatatype;
import org.openmrs.customdatatype.datatype.LocationDatatype;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public class SyncConceptAttributeTest extends SyncBaseTest{
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
    public void shouldSyncNewConceptAttributeType() throws Exception {
        runSyncTest(new SyncTestHelper() {

            public void runOnChild() throws Exception {
                ConceptService conceptService = Context.getConceptService();
                //there are 3 ConceptAttributeType in the test xml file
                Assertions.assertEquals(3, conceptService.getAllConceptAttributeTypes().size());
                ConceptAttributeType conceptAttributeType = new ConceptAttributeType();
                conceptAttributeType.setName("New ConceptAttributeType");
                conceptAttributeType.setDatatypeClassname(FreeTextDatatype.class.getName());
                conceptService.saveConceptAttributeType(conceptAttributeType);
                Assertions.assertNotNull(conceptAttributeType.getId());
                //A new ConceptAttributeType has been added
                Assertions.assertEquals(4, conceptService.getAllConceptAttributeTypes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                ConceptService conceptService = Context.getConceptService();
                //A new ConceptAttributeType has been added
                Assertions.assertEquals(4, conceptService.getAllConceptAttributeTypes().size());
                ConceptAttributeType newConceptAttributeType = conceptService.getConceptAttributeTypeByName("New ConceptAttributeType");
                // The new ConceptAttributeType has been synced up to the parent
                Assertions.assertNotNull(newConceptAttributeType);
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncConceptAttributeWithComplexDataType() throws Exception {
        runSyncTest(new SyncTestHelper() {

            public void runOnChild() throws Exception {
                ConceptService conceptService = Context.getConceptService();
                ConceptAttributeType conceptAttributeType = new ConceptAttributeType();
                conceptAttributeType.setName("Complex ConceptAttributeType");
                conceptAttributeType.setDatatypeClassname(LocationDatatype.class.getName());
                conceptService.saveConceptAttributeType(conceptAttributeType);
                Assertions.assertNotNull(conceptAttributeType.getId());
                Location healthCenterLocation = Context.getLocationService().getLocation("Someplace");
                Assertions.assertNotNull(healthCenterLocation);
                Concept concept = conceptService.getConcept(3);
                ConceptAttribute conceptAttribute = new ConceptAttribute();
                conceptAttribute.setAttributeType(conceptAttributeType);
                conceptAttribute.setValue(healthCenterLocation);
                conceptAttribute.setValueReferenceInternal(healthCenterLocation.getUuid());
                concept.addAttribute(conceptAttribute);
                conceptService.saveConcept(concept);
                //A new ConceptAttribute has been added
                List<ConceptAttribute> activeAttributes = concept.getActiveAttributes(conceptAttributeType);
                Assertions.assertEquals(1, activeAttributes.size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                ConceptService conceptService = Context.getConceptService();
                ConceptAttributeType conceptAttributeType = conceptService.getConceptAttributeTypeByName("Complex ConceptAttributeType");
                Assertions.assertNotNull(conceptAttributeType);
                Concept concept = conceptService.getConcept(3);
                List<ConceptAttribute> activeAttributes = concept.getActiveAttributes(conceptAttributeType);
                Assertions.assertNotNull(activeAttributes);
                Assertions.assertEquals(1, activeAttributes.size());
                Location location = (Location)activeAttributes.get(0).getValue();
                Assertions.assertEquals(location.getName(), "Someplace");
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncEditedConceptAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {

            public void runOnChild() throws Exception {
                ConceptService conceptService = Context.getConceptService();
                Concept concept = conceptService.getConcept(3);
                Assertions.assertNotNull(concept);
                Set<ConceptAttribute> conceptAttributes = concept.getAttributes();
                Assertions.assertEquals(2, conceptAttributes.size());
                for (ConceptAttribute conceptAttribute : conceptAttributes) {
                    if ("58E2424B-ADCA-4CF5-B234-0168EB42AD2A".equals(conceptAttribute.getAttributeType().getUuid())) {
                        // this is the "Audit free text" attribute
                        Assertions.assertEquals("text value" ,((String) conceptAttribute.getValue()));
                        conceptAttribute.setValue("new text value");
                    }
                }
                conceptService.saveConcept(concept);
                ConceptAttribute conceptAttribute = conceptService.getConceptAttributeByUuid("F0339AED-45F8-4E77-BEA5-E44A17576E80");
                Assertions.assertNotNull(conceptAttribute);
                Assertions.assertEquals("new text value" ,((String) conceptAttribute.getValue()));
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                ConceptService conceptService = Context.getConceptService();
                Concept concept = conceptService.getConcept(3);
                Assertions.assertNotNull(concept);
                Set<ConceptAttribute> conceptAttributes = concept.getAttributes();
                Assertions.assertEquals(2, conceptAttributes.size());
                for (ConceptAttribute conceptAttribute : conceptAttributes) {
                    if ("58E2424B-ADCA-4CF5-B234-0168EB42AD2A".equals(conceptAttribute.getAttributeType().getUuid())) {
                        // this is the "Audit free text" attribute
                        Assertions.assertEquals("new text value" ,((String) conceptAttribute.getValue()));
                    }
                }
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncDeletedConceptAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {

            public void runOnChild() throws Exception {
                ConceptService conceptService = Context.getConceptService();
                ConceptAttributeType auditFreeText = conceptService.getConceptAttributeTypeByName("Audit free text");
                Assertions.assertNotNull(auditFreeText);
                Assertions.assertEquals(auditFreeText.getName(), "Audit free text");
                Concept concept = conceptService.getConcept(7);
                Assertions.assertNotNull(concept);
                List<ConceptAttribute> activeAttributes = concept.getActiveAttributes(auditFreeText);
                Assertions.assertEquals(1, activeAttributes.size());
                ConceptAttribute attribute = activeAttributes.get(0);
                Assertions.assertEquals("following text value" ,((String) attribute.getValue()));
                concept.getAttributes().remove(attribute);
                Assertions.assertEquals(0, concept.getAttributes().size());
                conceptService.saveConcept(concept);
                Assertions.assertEquals(0, concept.getAttributes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                ConceptService conceptService = Context.getConceptService();
                Concept concept = conceptService.getConcept(7);
                Assertions.assertNotNull(concept);
                Set<ConceptAttribute> conceptAttributes = concept.getAttributes();
                Assertions.assertEquals(0, conceptAttributes.size());
            }
        });
    }
}
