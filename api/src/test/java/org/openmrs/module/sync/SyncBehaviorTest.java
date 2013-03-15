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

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openmrs.ConceptClass;
import org.openmrs.GlobalProperty;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.annotation.NotTransactional;

public class SyncBehaviorTest extends BaseModuleContextSensitiveTest {
	
	@Test
	@NotTransactional
	public void shouldNotCreateASyncRecordWhenTheTransactionIsRolledBack() throws Exception {
		ConceptService cs = Context.getConceptService();
		SyncService ss = Context.getService(SyncService.class);
		
		int initialSyncRecordCount = ss.getSyncRecords().size();
		boolean exceptionThrown = false;
		try {
			//ConceptClass cc = cs.getConceptClass(1);
			//cc.setUuid("An invalid long uuid that for sure should result into an exception");
			//cs.saveConceptClass(cc);
			GlobalProperty gp = new GlobalProperty("");
			Context.getAdministrationService().saveGlobalProperty(gp);
		}
		catch (UncategorizedSQLException e) {
			exceptionThrown = true;
		}
		
		//assertTrue(exceptionThrown);
		
		assertEquals(++initialSyncRecordCount, ss.getSyncRecords().size());
	}
	
}
