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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.stereotype.Controller;

/**
 * Tests for the {@link ConfigServerFormController}
 */
@Controller
public class ConfigServerFormControllerTest  extends BaseModuleContextSensitiveTest {
        
        /** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
    

        /**
         * Test of saveOrUpdateServerClasses method, of class ConfigServerFormController.
         */
        @Test
        public void saveOrUpdateServerClasses_shouldTrimClassnames() {
            RemoteServer testServer = new RemoteServer();
            HashSet<String> serverClasses = new HashSet<String>();
            testServer.setServerClasses((Set)serverClasses);
            List <String> classNamesNotToSend = new ArrayList();
            List <String> classNamesNotReceiveFrom = new ArrayList();
            classNamesNotToSend.add("  org.openmrs.GlobalProperty ");
            classNamesNotReceiveFrom.add("  org.openmrs.GlobalProperty ");
            
            ConfigServerFormController instance = new ConfigServerFormController();
            instance.saveOrUpdateServerClasses(testServer, classNamesNotToSend, classNamesNotReceiveFrom);
            Set<String> classesNotSent = testServer.getClassesNotSent();
            Set<String> classesNotReceived = testServer.getClassesNotReceived();
            
            Assert.assertEquals("org.openmrs.GlobalProperty", classesNotSent.iterator().next());
            Assert.assertEquals("org.openmrs.GlobalProperty", classesNotReceived.iterator().next());
        }
}