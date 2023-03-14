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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.SerializedObject;
import org.openmrs.api.db.SerializedObjectDAO;
import org.openmrs.api.db.hibernate.HibernateSerializedObjectDAO;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.serialization.OpenmrsSerializer;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class tests the {@link SerializedObjectDAO} linked to from the Context. Currently that file
 * is the {@link HibernateSerializedObjectDAO}.
 */
public class SyncSerializedObjectTest extends SyncBaseTest {
	
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
	public void shouldSyncSerializedObject() throws Exception {
		runSyncTest(new SyncTestHelper() {

			String uuid = null;
			
			public void runOnChild() throws Exception {

				UserService userService = Context.getUserService();
				
				//just random data
				SerializedObject serializedObject = new SerializedObject();
				serializedObject.setUuid(uuid);
				serializedObject.setName("blah");
				serializedObject.setDescription("This is to test saving a report");
				serializedObject.setCreator(userService.getUser(1));
				serializedObject.setDateCreated(new Date());
				serializedObject.setUuid(userService.getUser(2).getUuid());
				serializedObject.setType(User.class.getName());
				serializedObject.setSubtype(User.class.getName());
				serializedObject.setSerializationClass(OpenmrsSerializer.class);
				serializedObject.setSerializedData("gook");
				
				SyncService ss = Context.getService(SyncService.class);
				ss.saveOrUpdate(serializedObject);

				uuid = serializedObject.getUuid();
			}
			
			public void runOnParent() throws Exception {
				SyncService ss = Context.getService(SyncService.class);
				SerializedObject o = ss.getOpenmrsObjectByUuid(SerializedObject.class, uuid);

				assertNotNull(o);
				assertEquals("blah",o.getName());
				assertEquals(OpenmrsSerializer.class,o.getSerializationClass());
			}
		});
	}	

}
