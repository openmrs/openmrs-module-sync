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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Controller behind the history page showing all sync'd items.
 */
@Controller
public class HistoryListController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/module/sync/history", method = RequestMethod.GET)
	public void showThePage(ModelMap modelMap, @RequestParam(value = "firstRecordId", required = false) Integer firstRecordId,
	                        @RequestParam(value = "size", required = false) Integer size) throws Exception {
		
		// default the list size to 20 items
		if (size == null) {
			AdministrationService as = Context.getAdministrationService();
			String max = as.getGlobalProperty(SyncConstants.PROPERTY_NAME_MAX_PAGE_RECORDS, SyncConstants.PROPERTY_NAME_MAX_RETRY_COUNT_DEFAULT);
			size = Integer.valueOf(max);
		}
		
		log.error("Vewing history page with size: " + size);
		
		List<SyncRecord> recordList = null;
		
		// only fill the Object if the user has authenticated properly
		if (Context.isAuthenticated()) {
			SyncService ss = Context.getService(SyncService.class);
			recordList = ss.getSyncRecords(firstRecordId, size);
		}
		
		if (recordList == null)
			recordList = Collections.emptyList();
		
		Map<String, String> recordTypes = new HashMap<String, String>();
		Map<Object, String> itemTypes = new HashMap<Object, String>();
		Map<Object, String> itemUuids = new HashMap<Object, String>();
		Map<String, String> recordText = new HashMap<String, String>();
		Map<String, String> recordChangeType = new HashMap<String, String>();
		
		for (SyncRecord record : recordList) {
			
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
				
				// now we have to go through the item child nodes to find the real UUID that we want
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
			
			// persistent sets should show something other than their mainClassName (persistedSet)
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
				// some methods like Concept.getName() throw Exception s all the time...
				displayName = "";
			}
			if (displayName != null)
				if (displayName.length() > 0)
					recordText.put(record.getUuid(), displayName);
		}
		
		modelMap.put("syncRecords", recordList);
		
		modelMap.put("recordTypes", recordTypes);
		modelMap.put("itemTypes", itemTypes);
		modelMap.put("itemUuids", itemUuids);
		modelMap.put("recordText", recordText);
		modelMap.put("recordChangeType", recordChangeType);
		
		modelMap.put("parent", Context.getService(SyncService.class).getParentServer());
		modelMap.put("servers", Context.getService(SyncService.class).getRemoteServers());
		modelMap.put("syncDateDisplayFormat", TimestampNormalizer.DATETIME_DISPLAY_FORMAT);
		
		modelMap.put("firstRecordId", firstRecordId);
		modelMap.put("size", size);
	}
	
}
