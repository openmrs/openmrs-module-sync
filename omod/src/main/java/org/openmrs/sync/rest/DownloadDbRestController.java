package org.openmrs.sync.rest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;


@RestController
public class DownloadDbRestController {
    protected Log log = LogFactory.getLog(getClass());

    private static String MANAGE_SYNC_PRIVILEGE = "Manage Synchronization";

    @GetMapping(value = "/rest/v1/sync/downloadDb")
    public ResponseEntity<byte[]> getDbBackup() throws IOException {
        if (Context.hasPrivilege(MANAGE_SYNC_PRIVILEGE)) {
            try {
                File outputFile = Context.getService(SyncService.class).generateDataFile();
                InputStream in = new FileInputStream(outputFile);
                MediaType mediaType = MediaType.parseMediaType("application/octet-stream");
                return ResponseEntity.ok().contentType(mediaType).body(IOUtils.toByteArray(in));
            } catch (Exception e) {
                log.warn("Error while generating backup file", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage().getBytes());
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
