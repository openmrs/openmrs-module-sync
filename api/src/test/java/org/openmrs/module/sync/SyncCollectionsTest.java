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

import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.*;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.test.annotation.NotTransactional;

import java.util.List;

import static org.junit.Assert.*;

/**
 *
 */
public class SyncCollectionsTest extends SyncBaseTest {

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
	@NotTransactional
	@Ignore("This test fails currently.  Ticket ")
	public void shouldSyncMapsOfNonOpenmrsObjects() throws Exception {
		runSyncTest(new SyncTestHelper() {

			UserService us = Context.getUserService();

			public void runOnChild() {
				User u = us.getUser(1);
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD, "true");
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCALE, "en");
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_LOGIN_ATTEMPTS, "0");
				us.saveUser(u, null);

				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD, "false");
				u.setUserProperty(OpenmrsConstants.USER_PROPERTY_LOGIN_ATTEMPTS, "1");
				us.saveUser(u, null);
			}

			@Override
			public void changedBeingApplied(List<SyncRecord> syncRecords, Record record) throws Exception {
				super.changedBeingApplied(syncRecords, record);
			}

			public void runOnParent() {
				User u = us.getUser(1);
				assertFalse(Boolean.valueOf(u.getUserProperties().get(OpenmrsConstants.USER_PROPERTY_CHANGE_PASSWORD)));
				assertTrue("en".equals(u.getUserProperties().get(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCALE)));
				assertTrue("1".equals(u.getUserProperties().get(OpenmrsConstants.USER_PROPERTY_LOGIN_ATTEMPTS)));
			}
		});
	}
}