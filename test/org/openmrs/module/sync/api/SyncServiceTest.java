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
package org.openmrs.module.sync.api;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.ConceptDatatype;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

/**
 * Tests methods in the SyncService
 */
public class SyncServiceTest extends BaseModuleContextSensitiveTest {
	
	/**
	 * @see {@link SynchronizationService#getOpenmrsObjectByUuid(Class<QT;>,String)}
	 */
	@Test
	@Verifies(value = "should get any openmrs object by its uuid", method = "getOpenmrsObjectByUuid(Class<QT;>,String)")
	public void getOpenmrsObjectByUuid_shouldGetAnyOpenmrsObjectByItsUuid() throws Exception {
		ConceptDatatype dt = Context.getService(SynchronizationService.class).getOpenmrsObjectByUuid(ConceptDatatype.class,
		    "c5f90600-cdf2-4085-bb61-8952bbbe8cab");
		Assert.assertNotNull(dt);
		Assert.assertEquals(Integer.valueOf(3), dt.getConceptDatatypeId());
	}
}
