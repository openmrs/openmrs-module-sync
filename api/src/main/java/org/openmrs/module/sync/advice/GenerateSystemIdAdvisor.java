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
package org.openmrs.module.sync.advice;

import java.lang.reflect.Method;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.patient.impl.LuhnIdentifierValidator;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.util.StringUtils;

/**
 * This class wraps around every method in the {@link UserService} class. Only the generateSystemId
 * class is singled out and the String returned from it has the the current sync system's
 * user-defined name in it.
 */
public class GenerateSystemIdAdvisor extends StaticMethodMatcherPointcutAdvisor implements Advisor {
	
	private static final long serialVersionUID = 38539204394323L;
	
	private Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * Match only to the "generateSystemId method
	 * 
	 * @see org.springframework.aop.MethodMatcher#matches(java.lang.reflect.Method, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public boolean matches(Method method, Class targetClass) {
		return method.getName().equals("generateSystemId");
	}
	
	@Override
	public Advice getAdvice() {
		return new GenerateSystemIdAdvice();
	}
	
	/**
	 * This class has the actual logic to insert the name into the system id
	 */
	private class GenerateSystemIdAdvice implements MethodInterceptor {
		
		/**
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		public Object invoke(MethodInvocation invocation) throws Throwable {
			
			// the system id to return
			String systemId = null;
			
			UserService userService = Context.getUserService();
			SyncService syncService = Context.getService(SyncService.class);
			
			String serverName = syncService.getServerName();
			
			if (!StringUtils.hasLength(serverName)) {
				// if serverName is empty or null, use the standard method implementation
				systemId = (String) invocation.proceed();
			} else {
				// Hardcoding Luhn algorithm since all existing openmrs user ids have
				// had check digits generated this way.
				LuhnIdentifierValidator liv = new LuhnIdentifierValidator();
				User user = new User(); // temporary user to do the duplicate checking with
				
				Integer offset = 0;
				do {
					// mimic what dao.generateSystemId does
					Integer numberOfUsers = userService.getAllUsers().size() + 1;
					
					// generate and increment the system id if necessary
					Integer generatedId = numberOfUsers + offset++;
					
					systemId = serverName + "_" + generatedId.toString();
					
					try {
						systemId = liv.getValidIdentifier(systemId);
					}
					catch (Exception e) {
						log.error("error getting check digit", e);
						return systemId;
					}
					
					// loop until we find a system id that no one has
					user.setSystemId(systemId);
				} while (userService.hasDuplicateUsername(user));
			}
			
			return systemId;
		}
	}
	
}
