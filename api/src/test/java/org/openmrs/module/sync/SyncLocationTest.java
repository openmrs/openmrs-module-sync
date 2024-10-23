package org.openmrs.module.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Location;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.db.hibernate.HibernateSyncInterceptor;
import org.openmrs.module.sync.serialization.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.openmrs.module.sync.SyncConstants.PROPERTY_IGNORE_LOCATION_TAGS;

public class SyncLocationTest extends SyncBaseTest {

    @Autowired
    private LocationService locationService;

    @Override
    public String getInitialDataset() {
        try {
            return "org/openmrs/module/sync/include/" + new TestUtil().getTestDatasetFilename("syncCreateTest");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void setupIgnoreLocationTags() {
        HibernateSyncInterceptor.getInstance().setIgnoreLocationTags(null);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncNewLocation() throws Exception {

        String locationUuid = UUID.randomUUID().toString();
        String locationName = "New Location";

        runSyncTest(new SyncTestHelper() {
            int ORIGINAL_COUNT = 0;
            public void runOnChild() {
                List<Location> allLocations = locationService.getAllLocations();
                Assertions.assertNotNull(allLocations);
                ORIGINAL_COUNT = allLocations.size();
                Assertions.assertTrue( ORIGINAL_COUNT > 1);
                Location newLocation = new Location();
                newLocation.setUuid(locationUuid);
                newLocation.setName(locationName);
                locationService.saveLocation(newLocation);
                Assertions.assertEquals(ORIGINAL_COUNT + 1, locationService.getAllLocations().size());
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() {
                Assertions.assertEquals(ORIGINAL_COUNT + 1, locationService.getAllLocations().size());
                Location newLocation = locationService.getLocationByUuid(locationUuid) ;
                Assertions.assertNotNull(newLocation);
                Assertions.assertEquals(locationName, newLocation.getName());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncEditedLocation() throws Exception {
        runSyncTest(new SyncTestHelper() {

            final String locationUuid = "2d865f38-144e-102b-8d9c-e44ed545d86c";
            final String locationName = "Someplace";
            final String newLocationName = "Someplace New";

            public void runOnChild() {
                Location location = locationService.getLocationByUuid(locationUuid);
                Assertions.assertNotNull(location);
                Assertions.assertEquals(locationName, location.getName());
                location.setName(newLocationName);
                locationService.saveLocation(location);
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() {
                Location location = locationService.getLocationByUuid(locationUuid);
                Assertions.assertNotNull(location);
                Assertions.assertEquals(newLocationName, location.getName());
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldSyncLocationTags() throws Exception {
        runSyncTest(new SyncTestHelper() {

            final String locationUuid = "2d865f38-144e-102b-8d9c-e44ed545d86c";
            final String visitLocationTagName = "Visit Location";
            final String loginLocationTagName = "Login Location";

            public void runOnChild() {
                Location location = locationService.getLocationByUuid(locationUuid);
                Assertions.assertNotNull(location);
                Assertions.assertFalse(location.hasTag(visitLocationTagName));
                Assertions.assertTrue(location.hasTag(loginLocationTagName));
                location.addTag(locationService.getLocationTagByName(visitLocationTagName));
                locationService.saveLocation(location);
                Assertions.assertTrue(location.hasTag(visitLocationTagName));
                Assertions.assertTrue(location.hasTag(loginLocationTagName));
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() {
                Location location = locationService.getLocationByUuid(locationUuid);
                Assertions.assertNotNull(location);
                Assertions.assertTrue(location.hasTag(visitLocationTagName));
                Assertions.assertTrue(location.hasTag(loginLocationTagName));
            }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldNotSyncLocationTagsIfConfiguredToIgnore() throws Exception {
        runSyncTest(new SyncTestHelper() {

            final String locationUuid = "2d865f38-144e-102b-8d9c-e44ed545d86c";
            final String visitLocationTagName = "Visit Location";
            final String loginLocationTagName = "Login Location";

            public void runOnChild() {
                Context.getAdministrationService().setGlobalProperty(PROPERTY_IGNORE_LOCATION_TAGS, "true");
                Location location = locationService.getLocationByUuid(locationUuid);
                Assertions.assertNotNull(location);
                Assertions.assertFalse(location.hasTag(visitLocationTagName));
                Assertions.assertTrue(location.hasTag(loginLocationTagName));
                location.addTag(locationService.getLocationTagByName(visitLocationTagName));
                locationService.saveLocation(location);
                Assertions.assertTrue(location.hasTag(visitLocationTagName));
                Assertions.assertTrue(location.hasTag(loginLocationTagName));
            }

            @Override
            public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
                super.changedBeingApplied(syncRecords, record);
            }

            public void runOnParent() {
                Location location = locationService.getLocationByUuid(locationUuid);
                Assertions.assertNotNull(location);
                Assertions.assertFalse(location.hasTag(visitLocationTagName));
                Assertions.assertTrue(location.hasTag(loginLocationTagName));
            }
        });
    }
}
