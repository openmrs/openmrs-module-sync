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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.obs.ComplexData;
import org.openmrs.obs.handler.ImageHandler;
import org.openmrs.util.OpenmrsClassLoader;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testing syncing of the {@link Obs} object
 */
public class SyncObsTest extends SyncBaseTest {
	
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
	public void shouldSyncVoidedObs() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			String uuid = null;
			
			public void runOnChild() throws Exception {
				ObsService os = Context.getObsService();
				
				Obs obs = os.getObs(3);
				obs.setValueText("Some value");
				Obs newlySavedObs = os.saveObs(obs, "testing the voiding process");

				// make sure the "new obsId:" in the voidReason gets changed to a uuid
				SyncService ss = Context.getService(SyncService.class);
				List<SyncRecord> records = ss.getSyncRecords();
				SyncRecord record = records.get(records.size() - 1);
				SyncItem item = record.getItems().toArray(new SyncItem[] {})[1];
				assertThat(item.getContent(), containsString("testing the voiding process"));
				
				uuid = newlySavedObs.getUuid(); // we'll check the new obs on the other side for this uuid 
			}
			
			public void runOnParent() throws Exception {
				ObsService os = Context.getObsService();
				SyncService ss = Context.getService(SyncService.class);
				
				// test to make sure the voidReason references the right new obs

				Obs newObs = ss.getOpenmrsObjectByUuid(Obs.class, uuid); // this is the new obs that was created by the update
				Obs voidedObs = os.getObs(3); // this is the old obs that was edited and hence voided
				
				// voidReason should be ".... (new obsId: 5)"
				assertThat(voidedObs.getVoidReason(), is("testing the voiding process"));
			}
		});
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldSyncComplexObs() throws Exception {

		final PersonService ps = Context.getPersonService();
		final ObsService os = Context.getObsService();
		final ConceptService cs = Context.getConceptService();
		final LocationService ls = Context.getLocationService();
		final AdministrationService as = Context.getAdministrationService();

		final Date obsDate = new Date();

		runSyncTest(new SyncTestHelper() {

			Obs childObs;
			byte[] childComplexData;

			public void runOnChild() throws Exception {
				String imgFilePath = "ComplexObsTestImage.png";
				BufferedImage img;
				InputStream in = null;
				try {
					in = OpenmrsClassLoader.getInstance().getResourceAsStream(imgFilePath);
					img = ImageIO.read(in);
				}
				finally {
					IOUtils.closeQuietly(in);
				}

				Obs complexObs = new Obs();
				complexObs.setPerson(ps.getPerson(2));
				complexObs.setObsDatetime(obsDate);
				complexObs.setLocation(ls.getLocation(1));
				complexObs.setConcept(cs.getConcept(8473)); // Concept of type Complex with ImageHandler
				complexObs.setComplexData(new ComplexData("ComplexTest.png", img));

				childObs = os.saveObs(complexObs, "Test sync of complex text obs");

				File complexObsFile = null;
				try {
					complexObsFile = ImageHandler.getComplexDataFile(childObs);
					assertThat(complexObsFile, notNullValue());
					assertThat(complexObsFile.exists(), is(true));
					childComplexData = FileUtils.readFileToByteArray(complexObsFile);
					assertThat(childComplexData.length > 0, is(true));
				}
				finally {
					FileUtils.deleteQuietly(complexObsFile);
				}
			}

			public void runOnParent() throws Exception {
				Obs parentObs = os.getObsByUuid(childObs.getUuid());
				assertThat(parentObs, notNullValue());
				assertThat(parentObs.getValueComplex(), is(childObs.getValueComplex()));
				File complexObsFile = ImageHandler.getComplexDataFile(parentObs);
				assertThat(complexObsFile, notNullValue());
				assertThat(complexObsFile.exists(), is(true));
				byte[] parentComplexData = FileUtils.readFileToByteArray(complexObsFile);
				assertThat(parentComplexData.length > 0, is(true));
				assertThat(parentComplexData, is(childComplexData));
				FileUtils.deleteQuietly(complexObsFile);
			}
		});
	}

}
