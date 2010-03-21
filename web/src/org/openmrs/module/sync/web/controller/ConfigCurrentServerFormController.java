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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncClass;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for the page that lets the user configure the settings on the current server
 */
@Controller
public class ConfigCurrentServerFormController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Save the settings
	 * 
	 * @param serverName
	 * @param serverUuid
	 * @param serverAdminEmail
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/module/sync/configCurrentServer", method = RequestMethod.POST, params = "action=save")
	protected String onSaveSettings(@RequestParam("serverName") String serverName,
	                                @RequestParam("serverUuid") String serverUuid,
	                                @RequestParam(value = "serverAdminEmail", required = false) String serverAdminEmail,
	                                @RequestParam(value = "maxPageRecords", required = false) Integer maxPageRecords,
	                                @RequestParam(value = "maxRetryCount", required = false) Integer maxRetryCount,
	                                HttpSession httpSession) throws Exception {
		
		log.debug("in onSave for current server");
		
		if (!Context.isAuthenticated())
			throw new APIAuthenticationException("Not authenticated!");
		
		SyncService syncService = Context.getService(SyncService.class);
		syncService.saveServerName(serverName);
		syncService.saveServerUuid(serverUuid);
		syncService.saveAdminEmail(serverAdminEmail);
		
		// save global property settings
		AdministrationService as = Context.getAdministrationService();
		if (maxRetryCount != null) {
			GlobalProperty gp = as.getGlobalPropertyObject(SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT);
			if (!maxRetryCount.toString().equals(gp.getPropertyValue())) {
				gp.setPropertyValue(maxRetryCount.toString());
				as.saveGlobalProperty(gp);
			}
		}
		
		if (maxPageRecords != null) {
			GlobalProperty gp = as.getGlobalPropertyObject(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS);
			if (!maxPageRecords.toString().equals(gp.getPropertyValue())) {
				gp.setPropertyValue(maxPageRecords.toString());
				as.saveGlobalProperty(gp);
			}
		}
		
		httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "sync.config.current.settingsSaved");
		
		return "redirect:/module/sync/configCurrentServer.form";
	}
	
	/**
	 * Saving the default classes for the current server
	 * 
	 * @param defaultSendTo
	 * @param defaultReceiveFrom
	 * @return the name of the jsp page to show
	 * @throws Exception
	 */
	@RequestMapping(value = "/module/sync/configCurrentServer", method = RequestMethod.POST, params = "action=saveClasses")
	protected String onSaveClasses(
	                               HttpSession httpSession,
	                               @RequestParam(value = "defaultNotSendTo", required = false) List<String> defaultNotSendTo,
	                               @RequestParam(value = "defaultNotReceiveFrom", required = false) List<String> defaultNotReceiveFrom)
	                                                                                                                             throws Exception {
		
		log.debug("in onSaveClasses for current server");
		
		if (defaultNotSendTo == null)
			defaultNotSendTo = Collections.emptyList();
		
		if (defaultNotReceiveFrom == null)
			defaultNotReceiveFrom = Collections.emptyList();
		
		log.debug("sendto: " + defaultNotSendTo.size());
		log.debug("receiveFrom: " + defaultNotReceiveFrom.size());
		
		SyncService syncService = Context.getService(SyncService.class);
		
		List<SyncClass> currentSyncClasses = syncService.getSyncClasses();

		Set<SyncClass> syncClassesToSave = new HashSet<SyncClass>();
		
		// mark all currentSyncClasses that are not in the lists
		for (SyncClass syncClass : currentSyncClasses) {
			if (!defaultNotSendTo.contains(syncClass.getName()) && !syncClass.getDefaultSendTo()) {
				syncClass.setDefaultSendTo(true);
				syncClassesToSave.add(syncClass);
			}
			
			if (!defaultNotReceiveFrom.contains(syncClass.getName()) && !syncClass.getDefaultReceiveFrom()) {
				syncClass.setDefaultReceiveFrom(true);
				syncClassesToSave.add(syncClass);
			}
		}
		
		// unmark all currentSyncClasses that are in the sendTo list
		for (String className : defaultNotSendTo) {
			boolean foundClass = false;
			for (SyncClass currentClass : currentSyncClasses) {
				if (currentClass.getName().equals(className)) {
					foundClass = true;
					currentClass.setDefaultSendTo(false);
					syncClassesToSave.add(currentClass);
				}
			}
			
			// we need to add a new item to the list
			if (!foundClass) {
				SyncClass newSyncClass = new SyncClass();
				newSyncClass.setName(className);
				newSyncClass.setDefaultSendTo(false);
				// we must add this to the list of current classes so that the receiveFrom list picks it up instead of creating a new one
				currentSyncClasses.add(newSyncClass);
				syncClassesToSave.add(newSyncClass);
			}
		}
		
		// unmark all currentSyncClasses that are in the sendTo list
		for (String className : defaultNotReceiveFrom) {
			boolean foundClass = false;
			for (SyncClass currentClass : currentSyncClasses) {
				if (currentClass.getName().equals(className)) {
					foundClass = true;
					currentClass.setDefaultReceiveFrom(false);
					syncClassesToSave.add(currentClass);
				}
			}
			
			// we need to add a new item to the list
			if (!foundClass) {
				SyncClass newSyncClass = new SyncClass();
				newSyncClass.setName(className);
				newSyncClass.setDefaultReceiveFrom(false);
				syncClassesToSave.add(newSyncClass);
			}
		}
		
		// save all classes that we've added or modified
		for (SyncClass syncClass : syncClassesToSave) {
			syncService.saveSyncClass(syncClass);
		}
		
		httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "sync.config.current.classesSaved");
		
		return "redirect:/module/sync/configCurrentServer.form";
		
	}
	
	/**
	 * Set up variables for just viewing the page
	 * 
	 * @param modelMap
	 * @return the name of the jsp page to load
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/module/sync/configCurrentServer", method = RequestMethod.GET)
	protected String showPage(ModelMap modelMap) throws Exception {
		
		if (Context.isAuthenticated()) {
			SyncService syncService = Context.getService(SyncService.class);
			AdministrationService as = Context.getAdministrationService();
			
			// this is how the old controller got the server uuid
			//SyncSource source = new SyncSourceJournal();
			//obj.put("localServerUuid", source.getSyncSourceUuid());
			
			modelMap.put("localServerUuid", syncService.getServerUuid());
			modelMap.put("localServerName", syncService.getServerName());
			modelMap.put("localServerAdminEmail", syncService.getAdminEmail());
			modelMap.put("maxPageRecords", as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS, SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT_DEFAULT));
			modelMap.put("maxRetryCount", as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT, SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT_DEFAULT));

			// advanced section
			
			// default classes
			List<SyncClass> defaultSyncClasses = syncService.getSyncClasses();
			modelMap.put("syncClasses", defaultSyncClasses);

			// openmrs objects
			List<Class<OpenmrsObject>> openmrsObjects = syncService.getAllOpenmrsObjects();
			modelMap.put("openmrsObjectClasses", openmrsObjects);
		}
		
		return "/module/sync/configCurrentServerForm";
	}
	
}
