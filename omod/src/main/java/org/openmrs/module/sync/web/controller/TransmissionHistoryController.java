package org.openmrs.module.sync.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncTransmissionStatus;
import org.openmrs.module.sync.TransmissionLog;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.RemoteServer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Created by Willa aka Baba Imu on 5/31/18.
 */
@Controller
public class TransmissionHistoryController {
    private Log log = LogFactory.getLog(this.getClass());

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "module/sync/transmissionHistory.form", method = RequestMethod.GET)
    public void showThePage(ModelMap modelMap,
                            @RequestParam(required = false) Integer size,
                            @RequestParam(required = false) Integer startIndex,
                            @RequestParam(required = false) Integer serverId,
                            @RequestParam(required = false) String status) throws Exception {

        // default the list size to 20 items
        if (size == null) {
            AdministrationService as = Context.getAdministrationService();
            String max = as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS,
                    SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT_DEFAULT);
            size = Integer.valueOf(max);
        }

        log.debug("Vewing history page with size: " + size);

        if(startIndex == null) {
            startIndex = 0;
        }

        SyncTransmissionStatus transmissionStatus = null;
        if(status != null) {
            try {
                transmissionStatus = SyncTransmissionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException iae) {
                // Log and ignore.
                log.debug("Invalid enum type passed " + iae.getMessage());
            }
        }

        SyncService syncService = Context.getService(SyncService.class);
        RemoteServer server = null;
        if(serverId != null) {
            server = syncService.getRemoteServer(serverId);
        }
        // Get total number of history logs
        Integer logsCount = syncService.getCountOfAllTransmissionLogs();

        // Get the logs
        List<TransmissionLog> transmissionLogs = syncService.getAllTransmissionLogs();
        modelMap.put("size", size);
        modelMap.put("logsCount", logsCount);
        modelMap.put("transmissionLogs", transmissionLogs);
        modelMap.put("syncDateDisplayFormat", TimestampNormalizer.DATETIME_DISPLAY_FORMAT);
    }
}
