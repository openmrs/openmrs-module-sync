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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Controller;

/**
 * Tests for the {@link ConfigCurrentServerFormController}
 */
@Controller
public class ConfigCurrentServerFormControllerTest extends BaseModuleWebContextSensitiveTest {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	@Test
	public void shouldSaveCurrentServerSettings() throws Exception {
		SyncService syncService = Context.getService(SyncService.class);
		AdministrationService as = Context.getAdministrationService();
		
		// sanity check
		Assertions.assertNotSame("new server name", syncService.getServerName());
		Assertions.assertNull(syncService.getServerUuid());
		Assertions.assertNull(syncService.getAdminEmail());
		
		ConfigCurrentServerFormController controller = new ConfigCurrentServerFormController();
		
		controller.onSaveSettings("new server name", "some uuid", "the server email address", 97, 98, 99, 100, new MockHttpSession());

		Assertions.assertNotNull(syncService.getServerName());
		Assertions.assertNotNull(syncService.getServerUuid());
		Assertions.assertNotNull(syncService.getAdminEmail());
		Assertions.assertEquals("97", as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS));
		Assertions.assertEquals("98", as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_RECORDS_WEB));
		Assertions.assertEquals("99", as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_RECORDS_FILE));
		Assertions.assertEquals("100", as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT));
	}
	
}
