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
import org.openmrs.module.sync.SyncUtil;
import org.springframework.util.Assert;

/**
 * Tests various methods of the SyncUtil class.
 */
public class SyncUtilTest {

	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveInt(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "intField", new Integer(1).getClass());
		Assert.notNull(m);
	}
	
	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveLong(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "longField", new Long(1).getClass());
		Assert.notNull(m);
	}
	
	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveDouble(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "doubleField", new Double(1).getClass());
		Assert.notNull(m);
	}
	
	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveFloat(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "floatField", new Float(1).getClass());
		Assert.notNull(m);
	}
	
	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveBoolean(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "booleanField", new Boolean(true).getClass());
		Assert.notNull(m);
	}
	
	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveShort(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "shortField", new Short((short)1).getClass());
		Assert.notNull(m);
	}
	
	@Test
	public void getSetterMethod_shouldReturnMethodForPrimitiveByte(){
		Method m = SyncUtil.getSetterMethod(new Xform().getClass(), "byteField", new Byte((byte)1).getClass());
		Assert.notNull(m);
	}
	
	public class Xform {
		
		int intField;
		long longField;
		double doubleField;
		float floatField;
		boolean booleanField;
		byte byteField;
		short shortField;

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
	}
}