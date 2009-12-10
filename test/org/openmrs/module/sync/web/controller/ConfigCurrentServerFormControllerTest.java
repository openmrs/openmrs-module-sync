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
package org.openmrs.module.sync.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Controller;

/**
 * Tests for the {@link ConfigCurrentServerFormController}
 */
@Controller
public class ConfigCurrentServerFormControllerTest extends BaseModuleContextSensitiveTest {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	@Test
	public void shouldSaveCurrentServerSettings() throws Exception {
		SyncService syncService = Context.getService(SyncService.class);
		
		// sanity check
		Assert.assertNotSame("new server name", syncService.getServerName());
		Assert.assertNull(syncService.getServerUuid());
		Assert.assertNull(syncService.getAdminEmail());
		
		ConfigCurrentServerFormController controller = new ConfigCurrentServerFormController();
		
		controller.onSaveSettings("new server name", "some uuid", "the server email address", new MockHttpSession());
		
		Assert.assertNotNull(syncService.getServerName());
		Assert.assertNotNull(syncService.getServerUuid());
		Assert.assertNotNull(syncService.getAdminEmail());
	}
	
}
