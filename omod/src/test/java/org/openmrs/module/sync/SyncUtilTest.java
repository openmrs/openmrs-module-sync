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

import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.util.Assert;

/**
 * Tests various methods of the SyncUtil class.
 */
public class SyncUtilTest {

	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveInt(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "formId", new Integer(1).getClass());
		Assert.notNull(m);
	}
	
	public class Xform{
		
		int formId;

        public int getFormId() {
        	return formId;
        }

        public void setFormId(int formId) {
        	this.formId = formId;
        }
	}
}
