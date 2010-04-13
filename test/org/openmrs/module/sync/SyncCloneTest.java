/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.sync;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.test.annotation.NotTransactional;

/**
 * 
 */
public class SyncCloneTest extends SyncBaseTest {

	/**
	 * @see org.openmrs.synchronization.engine.SyncBaseTest#getInitialDataset()
	 */
	@Override
	public String getInitialDataset() {
		return "org/openmrs/module/sync/include/SyncCreateTest.xml";
	}

	@Test
	@NotTransactional
	public void testJDBCClone() throws Exception {
		runSyncTest(new SyncTestHelper() {
			long checksumOne, checksumTwo;
			File fileOne;
			File fileTwo;
			SyncService syncService = Context.getService(SyncService.class);

			public void runOnParent() {
				try {
					File dir = SyncUtil.getSyncApplicationDir();
					fileOne = new File(dir, SyncConstants.CLONE_IMPORT_FILE_NAME
					        + SyncConstants.SYNC_FILENAME_MASK.format(new Date())
					        + "_one.sql");
					syncService.exportChildDB(null, new FileOutputStream(fileOne));
					checksumOne = checksum(fileOne);
				} catch (Exception e) {
					log.error("Sync clone JDBC test export error", e);
				}
			}

			public void runOnChild() {
				try {
					syncService.importParentDB(new FileInputStream(fileOne));
					File dir = SyncUtil.getSyncApplicationDir();
					fileTwo = new File(dir, SyncConstants.CLONE_IMPORT_FILE_NAME
					        + SyncConstants.SYNC_FILENAME_MASK.format(new Date())
					        + "_two.sql");
					syncService.exportChildDB(null, new FileOutputStream(fileTwo));
					checksumTwo = checksum(fileTwo);
				} catch (Exception e) {
					log.error("Sync clone JDBC test import error", e);
				}
				assertTrue("Failed to validate the checksum for the two sync clonedumps",
				           checksumOne == checksumTwo);
			}
		});
	}

	@Test
	@NotTransactional
	public void testMySqlDump() throws Exception {
		
		runSyncTest(new SyncTestHelper() {
			long checksumOne, checksumTwo;
			File fileOne;
			File fileTwo;
			SyncService syncService = Context.getService(SyncService.class);

			public void runOnParent() {
				try {
					fileOne = syncService.generateDataFile();
					checksumOne = checksum(fileOne);
				} catch (Exception e) {
					log.error("Sync clone MYSQL test export error", e);
				}
			}

			public void runOnChild() {
				try {
					syncService.execGeneratedFile(fileOne);
					fileTwo =syncService.generateDataFile();
					checksumTwo = checksum(fileTwo);
				} catch (Exception e) {
					log.error("Sync clone MYSQL test import error", e);
				}
				assertTrue("Failed to validate the checksum for the two sync clone dumps",
				           checksumOne == checksumTwo);
			}
		});
	}

	/*
	 * Calculate the checksum on a file located at <b>fileName</b>
	 * 
	 * @return checksum on that file
	 * 
	 * @param fileName the location of the file
	 * 
	 * @throws Exception
	 */
	public long checksum(File file) throws Exception {
		long checksum;
		byte[] buff = new byte[1024];
		CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file),
		                                                new Adler32());
		while (cis.read(buff) >= 0)
			;
		checksum = cis.getChecksum().getValue();
		return checksum;
	}
}
