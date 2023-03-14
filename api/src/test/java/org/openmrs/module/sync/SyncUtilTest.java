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
import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Tests various methods of the SyncUtil class.
 */
public class SyncUtilTest {

	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveInt(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "intField");
		assertNotNull(m);
	}
	
	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveLong(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "longField");
		assertNotNull(m);
	}
	
	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveDouble(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "doubleField");
		assertNotNull(m);
	}
	
	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveFloat(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "floatField");
		assertNotNull(m);
	}
	
	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveBoolean(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "booleanField");
		assertNotNull(m);
	}
	
	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveShort(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "shortField");
		assertNotNull(m);
	}
	
	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveByte(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "byteField");
		assertNotNull(m);
	}

	@Test
	public void setProperty_shouldSetFieldDirectlyWithNoSetter() throws Exception {
		Object someObject = new Object();
		Xform obj = new Xform();
		assertNull(obj.getFieldWithNoSetter());
		SyncUtil.setProperty(obj, "fieldWithNoSetter", someObject);
		assertSame(someObject, obj.getFieldWithNoSetter());

	}
	
	public class Xform {
		
		int intField;
		long longField;
		double doubleField;
		float floatField;
		boolean booleanField;
		byte byteField;
		short shortField;
		private Object fieldWithNoSetter = null;

        public int getIntField() {
        	return intField;
        }

        public void setIntField(int intField) {
        	this.intField = intField;
        }
		
        public long getLongField() {
        	return longField;
        }
		
        public void setLongField(long longField) {
        	this.longField = longField;
        }
		
        public double getDoubleField() {
        	return doubleField;
        }
		
        public void setDoubleField(double doubleField) {
        	this.doubleField = doubleField;
        }

        public float getFloatField() {
        	return floatField;
        }
		
        public void setFloatField(float floatField) {
        	this.floatField = floatField;
        }
		
        public boolean getBooleanField() {
        	return booleanField;
        }
		
        public void setBooleanField(boolean booleanField) {
        	this.booleanField = booleanField;
        }
		
        public byte getByteField() {
        	return byteField;
        }
	
        public void setByteField(byte byteField) {
        	this.byteField = byteField;
        }
	
        public short getShortField() {
        	return shortField;
        }
		
        public void setShortField(short shortField) {
        	this.shortField = shortField;
        }

		public Object getFieldWithNoSetter() {
			return fieldWithNoSetter;
		}
	}

	@Test
	public void daysBetween_shouldGiveTheRightNumberOfDaysBetweenDates() {
		Date now = new Date();
		Calendar later = Calendar.getInstance();
		later.setTime(now);
		later.add(Calendar.DAY_OF_YEAR, 5);

		Assert.assertEquals(5, SyncUtil.daysBetween(now, later.getTime()));
		Assert.assertEquals(0, SyncUtil.daysBetween(now, now));
	}
}