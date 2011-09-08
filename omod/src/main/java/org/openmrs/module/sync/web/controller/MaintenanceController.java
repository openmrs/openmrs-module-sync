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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.web.WebConstants;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 */
public class MaintenanceController extends SimpleFormController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	public Integer maxPageRecords = Integer.parseInt(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS_DEFAULT);
	
	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#processFormSubmission(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView processFormSubmission(HttpServletRequest request, HttpServletResponse response, Object command,
	                                             BindException errors) throws Exception {
		TaskDefinition task = (TaskDefinition) command;
		Map<String, String> properties = new HashMap<String, String>();
		String[] names = ServletRequestUtils.getStringParameters(request, "propertyName");
		String[] values = ServletRequestUtils.getStringParameters(request, "propertyValue");
		if (names != null) {
			for (int x = 0; x < names.length; x++) {
				if (names[x].length() > 0)
					properties.put(names[x], values[x]);
			}
		}
		task.setProperties(properties);
		
		return super.processFormSubmission(request, response, command, errors);
	}
	
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring the
	 * form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		TaskDefinition taskModel = null;
		Collection<TaskDefinition> tasks = Context.getSchedulerService().getRegisteredTasks();
		if (tasks != null) {
			for (TaskDefinition task : tasks) {
				if (task.getTaskClass().equals(SyncConstants.CLEAN_UP_OLD_RECORDS_TASK_CLASS_NAME)) {
					taskModel = task;
				}
			}
		}
		
		return taskModel;
	}
	
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request, Object obj, Errors errors) throws Exception {
		
		Map<String, Object> ret = new HashMap<String, Object>();
		
		List<SyncRecord> returnList = new ArrayList<SyncRecord>();
		List<SyncRecord> matchesList = new ArrayList<SyncRecord>();
		String keyword = ServletRequestUtils.getStringParameter(request, "keyword", "");
		Integer page = ServletRequestUtils.getIntParameter(request, "page", 1);
		
		Integer maxPages = 1;
		Integer totalRecords = 0;
		
		// only fill the Object if the user has authenticated properly
		if (Context.isAuthenticated()) {
			SyncService syncService = Context.getService(SyncService.class);
			
			// if ("".equals(keyword) || keyword == null)
			// return new ArrayList<SyncRecord>();
			
			if (StringUtils.hasText(keyword))
				matchesList = syncService.getSyncRecords(keyword);
			
			String maxPageRecordsString = Context.getAdministrationService().getGlobalProperty(
			    SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS, SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS_DEFAULT);
			
			try {
				maxPageRecords = Integer.parseInt(maxPageRecordsString);
			}
			catch (NumberFormatException e) {
				log.warn("Unable to format gp: " + SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS + " into an integer", e);
			}
			
			if (maxPageRecords < 1) {
				maxPageRecords = Integer.parseInt(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS_DEFAULT);
			}
			
			// Adding paging
			totalRecords = matchesList.size();
			if (matchesList.size() % maxPageRecords == 0)
				maxPages = (int) (totalRecords / maxPageRecords);
			else
				maxPages = (int) (totalRecords / maxPageRecords) + 1;
			
			if (page > maxPages)
				page = 1;
			
			returnList.clear();
			int start = (page - 1) * maxPageRecords;
			for (int i = 0; start + i < totalRecords && i < maxPageRecords; i++) {
				returnList.add(matchesList.get(start + i));
			}
			
		}
		
		List<GlobalProperty> globalPropList = new ArrayList<GlobalProperty>();
		List<GlobalProperty> syncPropList = new ArrayList<GlobalProperty>();
		Map<String, String> recordTypes = new HashMap<String, String>();
		Map<Object, String> itemTypes = new HashMap<Object, String>();
		Map<Object, String> itemUuids = new HashMap<Object, String>();
		Map<String, String> recordText = new HashMap<String, String>();
		Map<String, String> recordChangeType = new HashMap<String, String>();
		
		// warning: right now we are assuming there is only 1 item per record
		for (SyncRecord record : returnList) {
			
			String mainClassName = null;
			String mainUuid = null;
			String mainState = null;
			
			for (SyncItem item : record.getItems()) {
				String syncItem = item.getContent();
				mainState = item.getState().toString();
				Record xml = Record.create(syncItem);
				Item root = xml.getRootItem();
				String className = root.getNode().getNodeName().substring("org.openmrs.".length());
				itemTypes.put(item.getKey().getKeyValue(), className);
				if (mainClassName == null)
					mainClassName = className;
				
				// String itemInfoKey = itemInfoKeys.get(className);
				
				// now we have to go through the item child nodes to find the
				// real UUID that we want
				NodeList nodes = root.getNode().getChildNodes();
				for (int i = 0; i < nodes.getLength(); i++) {
					Node n = nodes.item(i);
					String propName = n.getNodeName();
					if (propName.equalsIgnoreCase("uuid")) {
						String uuid = n.getTextContent();
						itemUuids.put(item.getKey().getKeyValue(), uuid);
						if (mainUuid == null)
							mainUuid = uuid;
					}
				}
			}
			
			// persistent sets should show something other than their
			// mainClassName (persistedSet)
			if (mainClassName.indexOf("Persistent") >= 0)
				mainClassName = record.getContainedClasses();
			
			recordTypes.put(record.getUuid(), mainClassName);
			recordChangeType.put(record.getUuid(), mainState);
			
			// refactored - CA 21 Jan 2008
			String displayName = "";
			try {
				displayName = SyncUtil.displayName(mainClassName, mainUuid);
			}
			catch (Exception e) {
				// some methods like Concept.getName() throw Exception s all the
				// time...
				displayName = "";
			}
			if (displayName != null)
				if (displayName.length() > 0)
					recordText.put(record.getUuid(), displayName);
		}
		
		globalPropList = Context.getAdministrationService().getAllGlobalProperties();
		for (GlobalProperty prop : globalPropList) {
			if (prop.getProperty().equals(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS))
				syncPropList.add(prop);
			else if (prop.getProperty().equals(SyncConstants.PROPERTY_NAME_MAX_RECORDS))
				syncPropList.add(prop);
			else if (prop.getProperty().equals(SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT))
				syncPropList.add(prop);
		}
		
		ret.put("keyword", keyword);
		ret.put("syncProps", syncPropList);
		ret.put("totalRecords", totalRecords);
		ret.put("currentPage", page);
		ret.put("maxPages", maxPages);
		ret.put("recordTypes", recordTypes);
		ret.put("itemTypes", itemTypes);
		ret.put("itemUuids", itemUuids);
		// ret.put("itemInfo", itemInfo);
		ret.put("recordText", recordText);
		ret.put("recordChangeType", recordChangeType);
		ret.put("parent", Context.getService(SyncService.class).getParentServer());
		ret.put("servers", Context.getService(SyncService.class).getRemoteServers());
		ret.put("syncDateDisplayFormat", TimestampNormalizer.DATETIME_DISPLAY_FORMAT);
		ret.put("synchronizationMaintenanceList", returnList);
		
		return ret;
	}
	
	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
	                                BindException errors) throws Exception {
		
		try {
			TaskDefinition task = (TaskDefinition) command;
			//reschedule the task if it is started, is not running and
			if (task.getStarted() && (task.getTaskInstance() == null || !task.getTaskInstance().isExecuting()))
				Context.getSchedulerService().rescheduleTask(task);
			else
				Context.getSchedulerService().saveTask(task);
			
			request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, "sync.maintenance.manage.changesSaved");
		}
		catch (APIException e) {
			errors.reject("sync.maintenance.manage.failedToSaveTaskProperties");
			return showForm(request, errors, getFormView());
		}
		
		return new ModelAndView(new RedirectView(getSuccessView()));
	}
	
}
