package org.openmrs.sync.rest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;

@RestController
public class DownloadDbRestController {
    protected Log log = LogFactory.getLog(getClass());

    private static String MANAGE_SYNC_PRIVILEGE = "Manage Synchronization";

    @GetMapping(value = "/rest/v1/sync/downloadDb")
    public ResponseEntity<InputStreamResource> getDbBackup() throws IOException {
        if (Context.hasPrivilege(MANAGE_SYNC_PRIVILEGE)) {
            try {
                File outputFile = Context.getService(SyncService.class).generateDataFile();
                InputStream in = new FileInputStream(outputFile);
                MediaType mediaType = MediaType.parseMediaType("application/octet-stream");
                return ResponseEntity.ok().contentType(mediaType).body(new InputStreamResource(in));
            } catch (Exception e) {
                log.warn("Error while generating backup file", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new InputStreamResource(new ByteArrayInputStream(e.getMessage().getBytes())));
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
