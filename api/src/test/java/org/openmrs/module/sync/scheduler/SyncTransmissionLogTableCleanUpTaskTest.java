package org.openmrs.module.sync.scheduler;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Willa aka Baba Imu on 6/4/18.
 */
public class SyncTransmissionLogTableCleanUpTaskTest extends BaseModuleWebContextSensitiveTest {
    @Test
    public void shouldDeleteTransmissionLogsUpToAGivenDate() throws Exception {

        executeDataSet("org/openmrs/module/sync/include/transmissionLogs.xml");

        SyncService syncService = Context.getService(SyncService.class);

        int allLogs = syncService.getCountOfAllTransmissionLogs();
        Calendar cal = Calendar.getInstance();
        int[] may42018 = {2018, 4, 4};
        cal.set(may42018[0], may42018[1], may42018[2]);

        int daysBack = SyncUtil.daysBetween(cal.getTime(), new Date());

        SyncTransmissionLogTableCleanUpTask task = new SyncTransmissionLogTableCleanUpTask();

        TaskDefinition td = new TaskDefinition();
        td.setProperty(SyncTransmissionLogTableCleanUpTask.PROPERTY_DAYS_BACK, String.valueOf(daysBack));
        task.initialize(td);

        task.execute();

        // because task.execute closes the session again
        Context.clearSession();
        Context.openSession();
        int allLogsAfter = syncService.getCountOfAllTransmissionLogs();

        // Only six records passes the age criteria set
        Assert.assertEquals(11, allLogs);
        Assert.assertEquals(5, allLogsAfter);
    }
}
