package org.openmrs.sync.rest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
public class CreateChildServerRestController {
    protected Log log = LogFactory.getLog(getClass());

    @GetMapping(value = "/rest/v1/sync/downloadDb", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] getDbBackup() throws IOException {
        try {
            File outputFile = Context.getService(SyncService.class).generateDataFile();
            InputStream in = new FileInputStream(outputFile);
            return IOUtils.toByteArray(in);
        } catch (Exception e) {
            log.warn("Error while generating backup file", e);
            return e.toString().getBytes();
        }
    }
}
