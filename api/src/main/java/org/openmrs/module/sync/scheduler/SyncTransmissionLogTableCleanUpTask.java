package org.openmrs.module.sync.scheduler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.util.Map;

public class SyncTransmissionLogTableCleanUpTask extends AbstractTask {
    private Log log = LogFactory.getLog(this.getClass());
    private Integer DEFAULT_DAYS_BACK_TO_START_DELETE = 7;

    protected static final String PROPERTY_DAYS_BACK = "delete_transmission_logs_older_than_x_days";

    @Override
    public void execute() {
        Context.openSession();
        log.debug("Starting transmission log clean up task");
        try {
            // get the possibly user-defined settings
            Map<String, String> props = null;
            if (taskDefinition != null)
                props = taskDefinition.getProperties();
            Integer daysBack = CleanupSyncTablesTask.getIntegerProperty(PROPERTY_DAYS_BACK, props, DEFAULT_DAYS_BACK_TO_START_DELETE);

            // do the actual deleting
            SyncService syncService = Context.getService(SyncService.class);
            Integer quantityDeleted = syncService.deleteOldTransmissionLogRecords(daysBack);

            log.info("There were " + quantityDeleted + " transmission logs records cleaned out");
        }
        catch (Throwable t) {
            log.error("Error while doing sync table cleanup", t);
            throw new APIException(t);
        }
        finally {
            Context.closeSession();
        }
    }
}
