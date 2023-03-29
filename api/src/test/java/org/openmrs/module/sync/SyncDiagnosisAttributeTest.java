package org.openmrs.module.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Diagnosis;
import org.openmrs.DiagnosisAttribute;
import org.openmrs.DiagnosisAttributeType;
import org.openmrs.Location;
import org.openmrs.api.DiagnosisService;
import org.openmrs.api.context.Context;
import org.openmrs.customdatatype.datatype.FreeTextDatatype;
import org.openmrs.customdatatype.datatype.LocationDatatype;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SyncDiagnosisAttributeTest extends SyncBaseTest {
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
    public void shouldSyncDeletedDiagnosisAttributeType() throws Exception {
        runSyncTest(new SyncTestHelper() {
            public int ORIGINAL_COUNT = 0;
            public void runOnChild() throws Exception {
                DiagnosisService diagnosisService = Context.getDiagnosisService();
                ORIGINAL_COUNT = diagnosisService.getAllDiagnosisAttributeTypes().size();
                Assertions.assertTrue(ORIGINAL_COUNT > 0);
                DiagnosisAttributeType diagnosisAttributeType = diagnosisService.getDiagnosisAttributeTypeById(2);
                Assertions.assertNotNull(diagnosisAttributeType);
                diagnosisService.purgeDiagnosisAttributeType(diagnosisAttributeType);
                Assertions.assertEquals( ORIGINAL_COUNT - 1, diagnosisService.getAllDiagnosisAttributeTypes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                DiagnosisService diagnosisService = Context.getDiagnosisService();
                DiagnosisAttributeType diagnosisAttributeType = diagnosisService.getDiagnosisAttributeTypeById(2);
                // DiagnosisAttributeType with diagnosis_attribute_type_id="2" has been deleted
                Assertions.assertNull(diagnosisAttributeType);
                Assertions.assertEquals( ORIGINAL_COUNT - 1, diagnosisService.getAllDiagnosisAttributeTypes().size());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncNewDiagnosisAttributeType() throws Exception {
        runSyncTest(new SyncTestHelper() {
            public int ORIGINAL_COUNT = 0;
            String NEW_DIAGNOSIS_ATTRIBUTE_TYPE_UUID = "E4C10AE3-F2B7-40A3-AF4A-623A0615CAEE";

            public void runOnChild() throws Exception {
                DiagnosisService diagnosisService = Context.getDiagnosisService();
                ORIGINAL_COUNT = diagnosisService.getAllDiagnosisAttributeTypes().size();
                Assertions.assertTrue(ORIGINAL_COUNT > 0);
                Diagnosis diagnosis = diagnosisService.getDiagnosis(1);
                Assertions.assertNotNull(diagnosis);
                DiagnosisAttributeType diagnosisAttributeType = new DiagnosisAttributeType();
                diagnosisAttributeType.setName("Clinical Decision Support System");
                diagnosisAttributeType.setMinOccurs(1);
                diagnosisAttributeType.setMaxOccurs(5);
                diagnosisAttributeType.setDatatypeClassname(FreeTextDatatype.class.getName());
                diagnosisAttributeType.setCreator(diagnosis.getCreator());
                diagnosisAttributeType.setDateCreated(diagnosis.getDateCreated());
                diagnosisAttributeType.setRetired(false);
                diagnosisAttributeType.setUuid(NEW_DIAGNOSIS_ATTRIBUTE_TYPE_UUID);
                diagnosisService.saveDiagnosisAttributeType(diagnosisAttributeType);
                Assertions.assertNotNull(diagnosisAttributeType);
                Assertions.assertEquals( ORIGINAL_COUNT + 1, diagnosisService.getAllDiagnosisAttributeTypes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                DiagnosisService diagnosisService = Context.getDiagnosisService();
                DiagnosisAttributeType diagnosisAttributeType = diagnosisService.getDiagnosisAttributeTypeByUuid(NEW_DIAGNOSIS_ATTRIBUTE_TYPE_UUID);
                Assertions.assertNotNull(diagnosisAttributeType);
                Assertions.assertEquals( ORIGINAL_COUNT + 1, diagnosisService.getAllDiagnosisAttributeTypes().size());
            }
        });
    }
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncNewDiagnosisAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {
            public int ORIGINAL_COUNT = 0;
            String DIAGNOSIS_ATTRIBUTE_TYPE_UUID = "9f41142f-a605-4247-9fa2-30ec8adcf7fb";

            public void runOnChild() throws Exception {
                DiagnosisService diagnosisService = Context.getDiagnosisService();
                Diagnosis diagnosis = diagnosisService.getDiagnosis(1);
                Assertions.assertNotNull(diagnosis);
                ORIGINAL_COUNT = diagnosis.getActiveAttributes().size();
                DiagnosisAttributeType diagnosisAttributeType = diagnosisService.getDiagnosisAttributeTypeByUuid(DIAGNOSIS_ATTRIBUTE_TYPE_UUID);
                Assertions.assertNotNull(diagnosisAttributeType);
                DiagnosisAttribute diagnosisAttribute = new DiagnosisAttribute();
                diagnosisAttribute.setAttributeType(diagnosisAttributeType);
                diagnosisAttribute.setCreator(diagnosis.getCreator());
                diagnosisAttribute.setVoided(false);
                diagnosisAttribute.setValueReferenceInternal("Diagnosis Attribute Reference");
                diagnosis.addAttribute(diagnosisAttribute);
                diagnosisService.save(diagnosis);
                assertNotNull(diagnosisAttribute.getId(), "Successfully Saved DiagnosisAttribute");
                Assertions.assertEquals( ORIGINAL_COUNT + 1, diagnosis.getActiveAttributes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                DiagnosisService diagnosisService = Context.getDiagnosisService();
                DiagnosisAttributeType diagnosisAttributeType = diagnosisService.getDiagnosisAttributeTypeByUuid(DIAGNOSIS_ATTRIBUTE_TYPE_UUID);
                Assertions.assertNotNull(diagnosisAttributeType);
                Diagnosis diagnosis = diagnosisService.getDiagnosis(1);
                Assertions.assertNotNull(diagnosis);
                Assertions.assertEquals( ORIGINAL_COUNT + 1, diagnosis.getActiveAttributes().size());
                List<DiagnosisAttribute> activeAttributes = diagnosis.getActiveAttributes(diagnosisAttributeType);
                Assertions.assertNotNull(activeAttributes);
                Assertions.assertEquals("Diagnosis Attribute Reference", activeAttributes.get(0).getValueReference());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncEditedDiagnosisAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {
            String DIAGNOSIS_ATTRIBUTE_TYPE_UUID = "949daf5b-a83e-4b65-b914-502a553243d3";

            public void runOnChild() throws Exception {
                DiagnosisService diagnosisService = Context.getDiagnosisService();
                Diagnosis diagnosis = diagnosisService.getDiagnosis(1);
                Assertions.assertNotNull(diagnosis);
                DiagnosisAttributeType diagnosisAttributeType = diagnosisService.getDiagnosisAttributeTypeByUuid(DIAGNOSIS_ATTRIBUTE_TYPE_UUID);
                Assertions.assertNotNull(diagnosisAttributeType);
                List<DiagnosisAttribute> activeAttributes = diagnosis.getActiveAttributes(diagnosisAttributeType);
                Assertions.assertNotNull(activeAttributes);
                Assertions.assertEquals(1, activeAttributes.size());
                Assertions.assertEquals("Testing Reference", activeAttributes.get(0).getValueReference());
                activeAttributes.get(0).setValueReferenceInternal("New value");
                diagnosisService.save(diagnosis);
                Assertions.assertEquals( "New value", diagnosis.getActiveAttributes(diagnosisAttributeType).get(0).getValueReference());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                DiagnosisService diagnosisService = Context.getDiagnosisService();
                DiagnosisAttributeType diagnosisAttributeType = diagnosisService.getDiagnosisAttributeTypeByUuid(DIAGNOSIS_ATTRIBUTE_TYPE_UUID);
                Assertions.assertNotNull(diagnosisAttributeType);
                Diagnosis diagnosis = diagnosisService.getDiagnosis(1);
                Assertions.assertNotNull(diagnosis);
                Assertions.assertEquals( "New value", diagnosis.getActiveAttributes(diagnosisAttributeType).get(0).getValueReference());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncDiagnosisAttributeWithComplexDatatype() throws Exception {
        runSyncTest(new SyncTestHelper() {
            public int ORIGINAL_COUNT = 0;
            String NEW_DIAGNOSIS_ATTRIBUTE_TYPE_UUID = "C542E8C6-2BFF-4258-A68B-ACB7EC5FB5A0";

            public void runOnChild() throws Exception {
                DiagnosisService diagnosisService = Context.getDiagnosisService();
                Diagnosis diagnosis = diagnosisService.getDiagnosis(1);
                Assertions.assertNotNull(diagnosis);
                ORIGINAL_COUNT = diagnosis.getActiveAttributes().size();
                Assertions.assertTrue(ORIGINAL_COUNT > 0);
                DiagnosisAttributeType diagnosisAttributeType = new DiagnosisAttributeType();
                diagnosisAttributeType.setName("Location Support System");
                diagnosisAttributeType.setMinOccurs(1);
                diagnosisAttributeType.setMaxOccurs(5);
                diagnosisAttributeType.setDatatypeClassname(LocationDatatype.class.getName());
                diagnosisAttributeType.setCreator(diagnosis.getCreator());
                diagnosisAttributeType.setDateCreated(diagnosis.getDateCreated());
                diagnosisAttributeType.setRetired(false);
                diagnosisAttributeType.setUuid(NEW_DIAGNOSIS_ATTRIBUTE_TYPE_UUID);
                diagnosisService.saveDiagnosisAttributeType(diagnosisAttributeType);
                Assertions.assertNotNull(diagnosisAttributeType);

                DiagnosisAttribute diagnosisAttribute = new DiagnosisAttribute();
                diagnosisAttribute.setAttributeType(diagnosisAttributeType);
                diagnosisAttribute.setCreator(diagnosis.getCreator());
                diagnosisAttribute.setVoided(false);
                Location healthCenterLocation = Context.getLocationService().getLocation("Someplace");
                Assertions.assertNotNull(healthCenterLocation);
                //diagnosisAttribute.setValue(healthCenterLocation);
                diagnosisAttribute.setValueReferenceInternal(healthCenterLocation.getUuid());
                diagnosis.addAttribute(diagnosisAttribute);
                diagnosisService.save(diagnosis);
                Assertions.assertEquals( ORIGINAL_COUNT + 1, diagnosis.getActiveAttributes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                DiagnosisService diagnosisService = Context.getDiagnosisService();
                DiagnosisAttributeType diagnosisAttributeType = diagnosisService.getDiagnosisAttributeTypeByUuid(NEW_DIAGNOSIS_ATTRIBUTE_TYPE_UUID);
                Assertions.assertNotNull(diagnosisAttributeType);
                Diagnosis diagnosis = diagnosisService.getDiagnosis(1);
                Assertions.assertNotNull(diagnosis);
                List<DiagnosisAttribute> activeAttributes = diagnosis.getActiveAttributes(diagnosisAttributeType);
                Assertions.assertNotNull(activeAttributes);
                Location location = (Location) activeAttributes.get(0).getValue();
                Assertions.assertEquals(location.getName(), "Someplace");
            }
        });
    }
}
