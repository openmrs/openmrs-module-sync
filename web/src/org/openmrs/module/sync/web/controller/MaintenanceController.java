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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 */
public class MaintenanceController extends
        SimpleFormController {

	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());

	public Integer maxPageRecords = Integer.parseInt(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS_DEFAULT);
	public Integer currentPage = 1;
	public Integer maxPages = 1;
	public Integer totalRecords = 0;

	/**
	 * 
	 * This is called prior to displaying a form for the first time. It tells
	 * Spring the form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	protected Object formBackingObject(HttpServletRequest request)
	        throws ServletException {
		// default empty Object
		List<SyncRecord> recordList = new ArrayList<SyncRecord>();
		List<SyncRecord> returnList = new ArrayList<SyncRecord>();
		List<SyncRecord> matchesList = new ArrayList<SyncRecord>();
		String keyword = ServletRequestUtils.getStringParameter(request,
		                                                        "keyword",
		                                                        "");
		String page = ServletRequestUtils.getStringParameter(request,
		                                                     "page",
		                                                     "");

		// only fill the Object if the user has authenticated properly
		if (Context.isAuthenticated()) {
			SyncService syncService = Context.getService(SyncService.class);

			// if ("".equals(keyword) || keyword == null)
			// return new ArrayList<SyncRecord>();

			recordList.addAll(syncService.getSyncRecords());

			// sync records Search implementation
			// warning: right now we are assuming there is only 1 item per
			// record
			for (SyncRecord record : recordList) {

				String mainClassName = null;
				String mainUuid = null;

				for (SyncItem item : record.getItems()) {
					try {
						String syncItem = item.getContent();
						Record xml;

						xml = Record.create(syncItem);

						Item root = xml.getRootItem();
						String className = root.getNode()
						                       .getNodeName()
						                       .substring("org.openmrs.".length());
						if (mainClassName == null)
							mainClassName = className;

						// String itemInfoKey = itemInfoKeys.get(className);

						// now we have to go through the item child nodes to
						// find
						// the
						// real UUID that we want
						NodeList nodes = root.getNode().getChildNodes();
						for (int i = 0; i < nodes.getLength(); i++) {
							Node n = nodes.item(i);
							String propName = n.getNodeName();
							if (propName.equalsIgnoreCase("uuid")) {
								String uuid = n.getTextContent();
								// itemUuids.put(item.getKey().getKeyValue(),
								// uuid);
								if (mainUuid == null)
									mainUuid = uuid;
							}
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						log.error("Error generated", e);
					}

				}

				// persistent sets should show something other than their
				// mainClassName (persistedSet)
				if (mainClassName.indexOf("Persistent") >= 0)
					mainClassName = record.getContainedClasses();
				String displayName = "";
				try {
					displayName = SyncUtil.displayName(mainClassName, mainUuid);
				} catch (Exception e) {
					// some methods like Concept.getName() throw Exception s all
					// the
					// time...
					displayName = "";
				}
				if (displayName != null)
					if (displayName.length() > 0)
						if (displayName.toLowerCase()
						               .contains(keyword.toLowerCase())
						        && (keyword.replace(" ", "").length() >= 3)) {
							matchesList.add(record);
							log.warn("Matches found :::::::: "
							        + matchesList.size());
						}
			}

			String maxPageRecordsString = Context.getAdministrationService()
			                                     .getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS, SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS_DEFAULT);

			maxPageRecords = Integer.parseInt(maxPageRecordsString);
			
			if (maxPageRecords < 1) {
				maxPageRecords = Integer.parseInt(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS_DEFAULT);
			}

			// Adding paging
			totalRecords = matchesList.size();
			if (matchesList.size() % maxPageRecords == 0)
				maxPages = (int) (totalRecords / maxPageRecords);
			else
				maxPages = (int) (totalRecords / maxPageRecords) + 1;

			if (page != "")
				currentPage = Integer.parseInt(page);
			if (currentPage > maxPages)
				currentPage = 1;

			returnList.clear();
			int start = (currentPage - 1) * maxPageRecords;
			for (int i = 0; start + i < totalRecords && i < maxPageRecords; i++) {
				returnList.add(matchesList.get(start + i));
			}

		}

		return returnList;
	}

	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request, Object obj,
	        Errors errors) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();

		List<GlobalProperty> globalPropList = new ArrayList<GlobalProperty>();
		List<GlobalProperty> syncPropList = new ArrayList<GlobalProperty>();
		Map<String, String> recordTypes = new HashMap<String, String>();
		Map<Object, String> itemTypes = new HashMap<Object, String>();
		Map<Object, String> itemUuids = new HashMap<Object, String>();
		Map<String, String> recordText = new HashMap<String, String>();
		Map<String, String> recordChangeType = new HashMap<String, String>();
		// Map<String,String> itemInfoKeys = new HashMap<String,String>();
		List<SyncRecord> recordList = (ArrayList<SyncRecord>) obj;
		String keyword = ServletRequestUtils.getStringParameter(request,
		                                                        "keyword",
		                                                        "");

		// itemInfoKeys.put("Patient", "gender,birthdate");
		// itemInfoKeys.put("PersonName", "name");
		// itemInfoKeys.put("User", "username");

		// warning: right now we are assuming there is only 1 item per record
		for (SyncRecord record : recordList) {

			String mainClassName = null;
			String mainUuid = null;
			String mainState = null;

			for (SyncItem item : record.getItems()) {
				String syncItem = item.getContent();
				mainState = item.getState().toString();
				Record xml = Record.create(syncItem);
				Item root = xml.getRootItem();
				String className = root.getNode()
				                       .getNodeName()
				                       .substring("org.openmrs.".length());
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
			} catch (Exception e) {
				// some methods like Concept.getName() throw Exception s all the
				// time...
				displayName = "";
			}
			if (displayName != null)
				if (displayName.length() > 0)
					recordText.put(record.getUuid(), displayName);
		}
		
		globalPropList=Context.getAdministrationService().getAllGlobalProperties();
		for(GlobalProperty prop: globalPropList){
			if(prop.getProperty().equals(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS))
				syncPropList.add(prop);
			else if(prop.getProperty().equals(SyncConstants.PROPERTY_NAME_MAX_RECORDS))
				syncPropList.add(prop);
			else if(prop.getProperty().equals(SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT))
				syncPropList.add(prop);
		}
		
		ret.put("keyword", keyword);
		ret.put("syncProps", syncPropList);
		ret.put("totalRecords", totalRecords);
		ret.put("currentPage", currentPage);
		ret.put("maxPages", maxPages);
		ret.put("recordTypes", recordTypes);
		ret.put("itemTypes", itemTypes);
		ret.put("itemUuids", itemUuids);
		// ret.put("itemInfo", itemInfo);
		ret.put("recordText", recordText);
		ret.put("recordChangeType", recordChangeType);
		ret.put("parent", Context.getService(SyncService.class).getParentServer());
		ret.put("servers", Context.getService(SyncService.class)
		                          .getRemoteServers());
		ret.put("syncDateDisplayFormat",
		        TimestampNormalizer.DATETIME_DISPLAY_FORMAT);

		return ret;
	}

}