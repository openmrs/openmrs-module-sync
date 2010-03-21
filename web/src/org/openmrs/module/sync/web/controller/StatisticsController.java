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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncItem;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.Item;
import org.openmrs.module.sync.serialization.Record;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 */
public class StatisticsController extends SimpleFormController {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());
    private SyncStatisticsCommand command=null;

    public class SyncStatisticsCommand {
		private String datePattern;
		private Date fromDate;
		private Date toDate;
		private List<SyncRecord> records;
		
		public SyncStatisticsCommand() { }
		public Date getFromDate() {
			return fromDate;
		}
		public void setFromDate(Date fromDate) {
			this.fromDate = fromDate;
		}
		public Date getToDate() {
			return toDate;
		}
		public void setToDate(Date toDate) {
			this.toDate = toDate;
		}
		public String getDatePattern() {
			return datePattern;
		}
		public void setDatePattern(String datePattern) {
			this.datePattern = datePattern;
		}
		public void setRecords(List<SyncRecord> records) {
			this.records = records;
		}
		public List<SyncRecord> getRecords() {
			return this.records;
		}
		
	}
 // Move this to message.properties or OpenmrsConstants
	public static String DEFAULT_DATE_PATTERN = "MM/dd/yyyy HH:mm:ss";
	public static DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat(DEFAULT_DATE_PATTERN);
	
    /**
     * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
     *      org.springframework.web.bind.ServletRequestDataBinder)
     */
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
		super.initBinder(request, binder);
		binder.registerCustomEditor(java.util.Date.class, new CustomDateEditor(DEFAULT_DATE_FORMAT, true));
	}

    /**
     * 
     * This is called prior to displaying a form for the first time. It tells
     * Spring the form/command object to load into the request
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    protected Object formBackingObject(HttpServletRequest request)
            throws ServletException {
        if (Context.isAuthenticated()) {
        	SyncService syncService = Context.getService(SyncService.class);
        	if(command==null){
        		command=new SyncStatisticsCommand();
        		command.setDatePattern(DEFAULT_DATE_PATTERN);
            	command.setFromDate(null);
            	command.setToDate(new Date());
                command.setRecords(syncService.getSyncRecords());
        	}
        	else if(command.getFromDate()==null){
        		command=new SyncStatisticsCommand();
        		command.setDatePattern(DEFAULT_DATE_PATTERN);
            	command.setFromDate(null);
            	command.setToDate(new Date());
                command.setRecords(syncService.getSyncRecords());
        	}
        		
        }

		if (command == null)
			command = new SyncStatisticsCommand();
    	
        return command;
    }
    
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object obj, BindException errors) throws Exception {
		
		ModelAndView result = new ModelAndView(new RedirectView(getSuccessView()));
		
		command=(SyncStatisticsCommand)obj;
		Date startDate=command.getFromDate();
		Date endDate=command.getToDate();
		
		SyncService syncService = Context.getService(SyncService.class);
		List<SyncRecord>rec=null;
		if(startDate!=null && endDate!=null)
			rec=syncService.getSyncRecordsBetween(startDate, endDate);
		else if(startDate!=null)
			rec=syncService.getSyncRecordsSince(startDate);
		else{
			rec=syncService.getSyncRecords();
			command.setToDate(new Date());
		}
		
    	command.setDatePattern(DEFAULT_DATE_PATTERN);
        command.setRecords(rec);
		
		return result;
	}

	@Override
    protected Map<String, Object> referenceData(HttpServletRequest request, Object obj, Errors errors) throws Exception {
		Map<String,Object> ret = new HashMap<String,Object>();
		
		Map<String,String> recordTypes = new HashMap<String,String>();
		Map<Object,String> itemTypes = new HashMap<Object,String>();
		Map<Object,String> itemGuids = new HashMap<Object,String>();
		Map<String,String> recordText = new HashMap<String,String>();
        Map<String,String> recordChangeType = new HashMap<String,String>();
        // Sync statistics 
        int totalRecords=0;
        int synchronizedRecords=0;
        int newRecords=0;
        int pendingRecords=0;
        int sentRecords=0;
        int sendFailedRecords=0;
        int ingestFailedRecords=0;
        int retriedRecords=0;
        int failedStoppedRecords=0;
        int notSyncRecords=0;
        int rejectedRecords=0;
        int unknownstateRecords=0;
        
        SyncStatisticsCommand command=(SyncStatisticsCommand)obj;
        List<SyncRecord> recordList =command.getRecords();
        
        totalRecords=recordList.size();
        // warning: right now we are assuming there is only 1 item per record
        for ( SyncRecord record : recordList ) {
            SyncRecordState state=record.getState();
        	if(state==SyncRecordState.ALREADY_COMMITTED||state==SyncRecordState.COMMITTED)
        		synchronizedRecords++;
        	else if(state==SyncRecordState.NEW)
        		newRecords++;
        	else if(state==SyncRecordState.PENDING_SEND)
        		pendingRecords++;
        	else if(state==SyncRecordState.SENT)
        		sentRecords++;
        	else if(state==SyncRecordState.SEND_FAILED)
        		sendFailedRecords++;
        	else if(state==SyncRecordState.FAILED)
        		ingestFailedRecords++;
        	else if(state==SyncRecordState.SENT_AGAIN)
        		retriedRecords++;
        	else if(state==SyncRecordState.FAILED_AND_STOPPED)
        		failedStoppedRecords++;
        	else if(state==SyncRecordState.NOT_SUPPOSED_TO_SYNC)
        		notSyncRecords++;
        	else if(state==SyncRecordState.REJECTED)
        		rejectedRecords++;
        	else 
        		unknownstateRecords++;
        	
            String mainClassName = null;
            String mainGuid = null;
            String mainState = null;
            
			for ( SyncItem item : record.getItems() ) {
				String syncItem = item.getContent();
                mainState = item.getState().toString();
				Record xml = Record.create(syncItem);
				Item root = xml.getRootItem();
				String className = root.getNode().getNodeName().substring("org.openmrs.".length());
				itemTypes.put(item.getKey().getKeyValue(), className);
				if ( mainClassName == null ) mainClassName = className;
                
				//String itemInfoKey = itemInfoKeys.get(className);
				
				// now we have to go through the item child nodes to find the real GUID that we want
				NodeList nodes = root.getNode().getChildNodes();
				for ( int i = 0; i < nodes.getLength(); i++ ) {
					Node n = nodes.item(i);
					String propName = n.getNodeName();
					if ( propName.equalsIgnoreCase("guid") ) {
                        String guid = n.getTextContent();
						itemGuids.put(item.getKey().getKeyValue(), guid);
                        if ( mainGuid == null ) mainGuid = guid;
                    }
				}
			}

			// persistent sets should show something other than their mainClassName (persistedSet)
			if ( mainClassName.indexOf("Persistent") >= 0 ) mainClassName = record.getContainedClasses();
			
            recordTypes.put(record.getUuid(), mainClassName);
            recordChangeType.put(record.getUuid(), mainState);

            // refactored - CA 21 Jan 2008
            String displayName = "";
            try {
                displayName = SyncUtil.displayName(mainClassName, mainGuid);
            } catch ( Exception e ) {
            	// some methods like Concept.getName() throw Exception s all the time...
            	displayName = "";
            }
            if ( displayName != null ) if ( displayName.length() > 0 ) recordText.put(record.getUuid(), displayName);
        }
        
        // reference statistics
        ret.put("totalRecords",new Integer(totalRecords) );
        ret.put("synchronizedRecords", new Integer(synchronizedRecords));
        ret.put("newRecords",  new Integer(newRecords));
        ret.put("pendingRecords",  new Integer(pendingRecords));
        ret.put("sentRecords",  new Integer(sentRecords));
        ret.put("sendFailedRecords",  new Integer(sendFailedRecords));
        ret.put("ingestFailedRecords",  new Integer(ingestFailedRecords));
        ret.put("retriedRecords",  new Integer(retriedRecords));
        ret.put("failedStoppedRecords", new Integer(failedStoppedRecords));
        ret.put("notSyncRecords", new Integer(notSyncRecords));
        ret.put("rejectedRecords",  new Integer(rejectedRecords));
        ret.put("unknownstateRecords", new Integer(unknownstateRecords));
        
        ret.put("recordTypes", recordTypes);
        ret.put("itemTypes", itemTypes);
        ret.put("itemGuids", itemGuids);
        ret.put("recordText", recordText);
        ret.put("recordChangeType", recordChangeType);
        ret.put("parent", Context.getService(SyncService.class).getParentServer());
        ret.put("servers", Context.getService(SyncService.class).getRemoteServers());
        ret.put("syncDateDisplayFormat", TimestampNormalizer.DATETIME_DISPLAY_FORMAT);
        
        
	    return ret;
    }

}