package org.openmrs.module.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.openmrs.ProviderAttributeType;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.customdatatype.datatype.FreeTextDatatype;
import org.openmrs.customdatatype.datatype.LocationDatatype;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public class SyncProviderAttributeTest extends SyncBaseTest{
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
    public void shouldSyncNewProviderAttributeType() throws Exception {
        runSyncTest(new SyncTestHelper() {
            int ORIGINAL_COUNT = 0;
            public void runOnChild() throws Exception {
                ProviderService providerService = Context.getProviderService();
                ORIGINAL_COUNT = providerService.getAllProviderAttributeTypes().size();
                Assertions.assertTrue( ORIGINAL_COUNT > 0);
                ProviderAttributeType providerAttributeType = new ProviderAttributeType();
                providerAttributeType.setName("internal medicine");
                providerAttributeType.setMinOccurs(1);
                providerAttributeType.setMaxOccurs(5);
                providerAttributeType.setDatatypeClassname(FreeTextDatatype.class.getName());
                providerService.saveProviderAttributeType(providerAttributeType);
                Assertions.assertEquals(ORIGINAL_COUNT + 1 , providerService.getAllProviderAttributeTypes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                ProviderService providerService = Context.getProviderService();
                Assertions.assertEquals(ORIGINAL_COUNT + 1 , providerService.getAllProviderAttributeTypes().size());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncProviderAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {
            int ORIGINAL_COUNT = 0;
            public void runOnChild() throws Exception {
                ProviderService providerService = Context.getProviderService();
                ProviderAttributeType providerAttributeType = providerService.getProviderAttributeTypeByUuid("9516cc50-6f9f-11e0-8414-001e378eb67d");
                Assertions.assertNotNull(providerAttributeType);
                Provider provider = providerService.getProvider(1);
                Assertions.assertNotNull(provider);
                ORIGINAL_COUNT = provider.getAttributes().size();
                ProviderAttribute providerAttribute = new ProviderAttribute();
                providerAttribute.setAttributeType(providerAttributeType);
                providerAttribute.setProvider(provider);
                providerAttribute.setValue("some value");
                provider.setAttribute(providerAttribute);
                providerService.saveProvider(provider);
                Assertions.assertEquals(ORIGINAL_COUNT + 1, provider.getAttributes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                ProviderService providerService = Context.getProviderService();
                Provider provider = providerService.getProvider(1);
                Assertions.assertNotNull(provider);
                Collection<ProviderAttribute> providerAttributes = provider.getAttributes();
                Assertions.assertEquals(ORIGINAL_COUNT + 1, providerAttributes.size() );
                Assertions.assertEquals("some value", providerAttributes.iterator().next().getValueReference());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncDeletedProviderAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {
            int ORIGINAL_COUNT = 0;
            public void runOnChild() throws Exception {
                ProviderService providerService = Context.getProviderService();
                ProviderAttributeType providerAttributeType = providerService.getProviderAttributeTypeByUuid("9516cc50-6f9f-11e0-8414-001e378eb67d");
                Assertions.assertNotNull(providerAttributeType);
                Provider provider = providerService.getProvider(1);
                Assertions.assertNotNull(provider);
                ORIGINAL_COUNT = provider.getAttributes().size();
                Assertions.assertTrue(provider.getAttributes().size() == 1 );
                ProviderAttribute providerAttribute = provider.getActiveAttributes(providerAttributeType).get(0);
                Assertions.assertNotNull(providerAttribute);
                provider.getAttributes().remove(providerAttribute);
                providerService.saveProvider(provider);
                Assertions.assertEquals(ORIGINAL_COUNT - 1, provider.getAttributes().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                ProviderService providerService = Context.getProviderService();
                Provider provider = providerService.getProvider(1);
                Assertions.assertNotNull(provider);
                Assertions.assertEquals(ORIGINAL_COUNT - 1, provider.getAttributes().size());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncEditedProviderAttribute() throws Exception {
        runSyncTest(new SyncTestHelper() {

            public void runOnChild() throws Exception {
                ProviderService providerService = Context.getProviderService();
                ProviderAttributeType providerAttributeType = providerService.getProviderAttributeTypeByUuid("9516cc50-6f9f-11e0-8414-001e378eb67d");
                Assertions.assertNotNull(providerAttributeType);
                Provider provider = providerService.getProvider(1);
                Assertions.assertNotNull(provider);
                Assertions.assertTrue(provider.getAttributes().size() == 1 );
                ProviderAttribute providerAttribute = provider.getActiveAttributes(providerAttributeType).get(0);
                Assertions.assertNotNull(providerAttribute);
                providerAttribute.setValue("updated value");
                providerService.saveProvider(provider);
                Assertions.assertEquals("updated value", provider.getAttributes().iterator().next().getValue());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                ProviderService providerService = Context.getProviderService();
                Provider provider = providerService.getProvider(1);
                Assertions.assertNotNull(provider);
                Assertions.assertEquals("updated value", provider.getAttributes().iterator().next().getValue());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldProviderAttributeWithComplexDatatype() throws Exception {
        runSyncTest(new SyncTestHelper() {
            String NEW_PROVIDER_ATTRIBUTE_TYPE_UUID="";
            public void runOnChild() throws Exception {
                ProviderService providerService = Context.getProviderService();
                ProviderAttributeType providerAttributeType = new ProviderAttributeType();
                providerAttributeType.setName("special location");
                providerAttributeType.setMinOccurs(1);
                providerAttributeType.setMaxOccurs(5);
                providerAttributeType.setDatatypeClassname(LocationDatatype.class.getName());
                providerService.saveProviderAttributeType(providerAttributeType);
                Assertions.assertNotNull(providerAttributeType.getId());
                NEW_PROVIDER_ATTRIBUTE_TYPE_UUID = providerAttributeType.getUuid();
                Location healthCenterLocation = Context.getLocationService().getLocation("Someplace");
                Assertions.assertNotNull(healthCenterLocation);

                Provider provider = providerService.getProvider(1);
                Assertions.assertNotNull(provider);
                ProviderAttribute providerAttribute = new ProviderAttribute();
                providerAttribute.setAttributeType(providerAttributeType);
                providerAttribute.setProvider(provider);
                providerAttribute.setValue(healthCenterLocation);
                provider.setAttribute(providerAttribute);
                providerService.saveProvider(provider);
                Assertions.assertEquals(healthCenterLocation, provider.getActiveAttributes(providerAttributeType).get(0).getValue());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() throws Exception {
                ProviderService providerService = Context.getProviderService();
                ProviderAttributeType providerAttributeType = providerService.getProviderAttributeTypeByUuid(NEW_PROVIDER_ATTRIBUTE_TYPE_UUID);
                Assertions.assertNotNull(providerAttributeType);
                Provider provider = providerService.getProvider(1);
                Assertions.assertNotNull(provider);
                Location healthCenterLocation = Context.getLocationService().getLocation("Someplace");
                Assertions.assertNotNull(healthCenterLocation);
                Assertions.assertEquals(healthCenterLocation, provider.getActiveAttributes(providerAttributeType).get(0).getValue());
            }
        });
    }
}
