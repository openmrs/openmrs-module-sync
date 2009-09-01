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
package org.openmrs.module.sync.web.dwr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncTransmissionState;
import org.openmrs.module.sync.SyncUtilTransmission;
import org.openmrs.module.sync.ingest.SyncTransmissionResponse;
import org.openmrs.module.sync.server.ConnectionResponse;
import org.openmrs.module.sync.server.ServerConnection;
import org.openmrs.module.sync.server.ServerConnectionState;

/**
 * DWR methods used by the sync module
 */
public class DWRSyncService {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Pings the given server with the given username/password to make sure the settings are correct
	 * 
	 * @param address url to ping
	 * @param username username with which to log in
	 * @param password password with which to log in
	 * @return SyncConnectionTestItem that contains success or failure
	 */
	public SyncConnectionTestItem testConnection(String address, String username, String password) {
		SyncConnectionTestItem item = new SyncConnectionTestItem();
		item.setConnectionState(ServerConnectionState.NO_ADDRESS.toString());
		
		if (address != null && address.length() > 0) {
			ConnectionResponse connResponse = ServerConnection.test(address, username, password);
			
			// constructor for SyncTransmissionResponse is null-safe
			SyncTransmissionResponse str = new SyncTransmissionResponse(connResponse);
			
			// constructor for SyncConnectionTestItem is null-safe
			item = new SyncConnectionTestItem(str);
		}
		
		return item;
	}
	
	/**
	 * Used by the status.list page to send data to the parent and show the results
	 * 
	 * @return results of the transmission
	 */
	public SyncTransmissionResponseItem syncToParent() {
		SyncTransmissionResponse response = SyncUtilTransmission.doFullSynchronize();
		
		if (response != null) {
			return new SyncTransmissionResponseItem(response);
		} else {
			SyncTransmissionResponseItem transmissionResponse = new SyncTransmissionResponseItem();
			transmissionResponse.setErrorMessage(SyncConstants.ERROR_SEND_FAILED.toString());
			transmissionResponse.setFileName(SyncConstants.FILENAME_SEND_FAILED);
			transmissionResponse.setUuid(SyncConstants.UUID_UNKNOWN);
			transmissionResponse.setTransmissionState(SyncTransmissionState.FAILED.toString());
			return transmissionResponse;
		}
		
	}
	
}
