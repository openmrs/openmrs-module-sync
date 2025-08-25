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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SyncService;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.RemoteServer;

/**
 * SyncSource to sync OpenMRS tables based on last_changed_local.
 */
public class SyncSourceJournal implements SyncSource {
	
	private final Log log = LogFactory.getLog(getClass());
	
	// constructor(s)
	public SyncSourceJournal() {
	}
	
	// properties
	
	// Public Methods
	
	/**
	 * Std. method for retrieving last sync local; uses global prop, note in case of journal sync,
	 * we could just infer this from the status of the journal: last sync local is the date of the
	 * last entry with status of pending, or new
	 */
	public SyncPoint<Date> getLastSyncLocal() {
		Date val = null;
		String sVal = Context.getService(SyncService.class).getGlobalProperty(SyncConstants.LAST_SYNC_LOCAL);
		try {
			val = (sVal == null || "".equals(sVal)) ? null : new SimpleDateFormat(TimestampNormalizer.DATETIME_MASK)
			        .parse(sVal);
		}
		catch (ParseException e) {
			log.error("Error DateFormat parsing " + sVal, e);
			throw new SyncException("Error DateFormat parsing " + sVal, e);
		}
		return new SyncPoint<Date>(val);
	}
	
	public void setLastSyncLocal(SyncPoint p) {
		String sVal = null;
		
		sVal = (p.getValue() == null || "".equals(sVal)) ? null : new SimpleDateFormat(TimestampNormalizer.DATETIME_MASK)
		        .format(p.getValue());
		// use getSynchronizationService to avoid logging this changes to the journal
		Context.getService(SyncService.class).setGlobalProperty(SyncConstants.LAST_SYNC_LOCAL, sVal);
		
		return;
	}
	
	/*
	 * Last sync remote: timestamp of the last data *received* from parent
	 */
	public SyncPoint<Date> getLastSyncRemote() {
		Date val = null;
		
		String sVal = Context.getService(SyncService.class).getGlobalProperty(SyncConstants.LAST_SYNC_REMOTE);
		try {
			
			val = (sVal == null || "".equals(sVal)) ? null : new SimpleDateFormat(TimestampNormalizer.DATETIME_MASK)
			        .parse(sVal);
		}
		catch (ParseException e) {
			log.error("error DateFormat parsing " + sVal, e);
		}
		return new SyncPoint<Date>(val);
	}
	
	public void setLastSyncRemote(SyncPoint p) {
		String sVal = null;
		
		sVal = (p.getValue() == null || "".equals(sVal)) ? null : new SimpleDateFormat(TimestampNormalizer.DATETIME_MASK)
		        .format(p.getValue());
		// use getSynchronizationService to avoid logging this changes to the journal
		Context.getService(SyncService.class).setGlobalProperty(SyncConstants.LAST_SYNC_REMOTE, sVal);
		
		return;
	}
	
	// gets the 'next' SyncPoint: in case of timestamp implementation, just get current date/time
	public SyncPoint<Date> moveSyncPoint() {
		
		return new SyncPoint<Date>(new Date());
	}
	
	// no op: journal has delete records; get 'changed' returns deleted also
	public List<SyncRecord> getDeleted(SyncPoint from, SyncPoint to) throws SyncException {
		List<SyncRecord> deleted = new ArrayList<SyncRecord>();
		
		return deleted;
	}
	
	// state-based version
	// no op: journal has delete records; get 'changed' returns deleted also
	public List<SyncRecord> getDeleted() throws SyncException {
		List<SyncRecord> deleted = new ArrayList<SyncRecord>();
		
		return deleted;
	}
	
	// retrieve journal records > 'from' && <= 'to' && record status = 'new' or
	// 'failed'
	public List<SyncRecord> getChanged(SyncPoint from, SyncPoint to) throws SyncException {
		List<SyncRecord> changed = new ArrayList<SyncRecord>();
		
		try {
			
			Date fromDate = (Date) from.getValue();
			Date toDate = (Date) to.getValue();
			
			//handle nulls
			if (fromDate == null)
				fromDate = new Date(0L);
			if (toDate == null)
				toDate = new Date(0L);
			
			SyncService syncService = Context.getService(SyncService.class);
			changed = syncService.getSyncRecordsBetween(fromDate, toDate);
			
		}
		catch (Exception e) {
			// TODO
			log.error("error in getChanged ", e);
		}
		
		return changed;
	}
	
	public List<SyncRecord> getChanged(Integer maxSyncRecords) throws SyncException {
		List<SyncRecord> changed = new ArrayList<SyncRecord>();
		
		try {
			SyncService syncService = Context.getService(SyncService.class);
			changed = syncService.getSyncRecords(SyncConstants.SYNC_TO_PARENT_STATES, maxSyncRecords, null);
			
		}
		catch (Exception e) {
			// TODO
			log.error("error in getChanged ", e);
		}
		
		return changed;
	}
	
	// state-based version that takes into consideration what should/shouldn't be sent to a given server
	public List<SyncRecord> getChanged(RemoteServer server, Integer maxResults) throws SyncException {
		List<SyncRecord> changed = new ArrayList<SyncRecord>();
		
		try {
			SyncService syncService = Context.getService(SyncService.class);
			changed = syncService.getSyncRecordsAndUpdateAsNotSupposedToSync(SyncConstants.SYNC_TO_PARENT_STATES, server, maxResults, null);
			
		}
		catch (Exception e) {
			// TODO
			log.error("error in getChanged ", e);
		}
		
		return changed;
	}
	
	/*
	 * no-op for journal sync -- all changes (deletes, inserts, updates are received in transactional order
	 * via applyChanged
	 */
	public void applyDeleted(List<SyncRecord> records) throws SyncException {
		
		return;
	}
	
	public void applyChanged(List<SyncRecord> records) throws SyncException {
		
		//TODO - process the changeset
		
		return;
	}
	
	public String getSyncSourceUuid() {
		return Context.getService(SyncService.class).getGlobalProperty(SyncConstants.PROPERTY_SERVER_UUID);
	}
	
	public void setSyncSourceUuid(String uuid) {
		Context.getService(SyncService.class).setGlobalProperty(SyncConstants.PROPERTY_SERVER_UUID, uuid);
		
		return;
	}
	
}
