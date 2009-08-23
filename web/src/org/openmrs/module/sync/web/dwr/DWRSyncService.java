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
 *
 */
public class DWRSyncService {

	protected final Log log = LogFactory.getLog(getClass());

	public SyncConnectionTestItem testConnection(String address, String username, String password) {
		SyncConnectionTestItem item = new SyncConnectionTestItem();
		item.setConnectionState(ServerConnectionState.NO_ADDRESS.toString());
		
		if ( address != null && address.length() > 0 ) {
			ConnectionResponse connResponse = ServerConnection.test(address, username, password);
	
	   		// constructor for SyncTransmissionResponse is null-safe
	    	SyncTransmissionResponse str = new SyncTransmissionResponse(connResponse);

	    	// constructor for SyncConnectionTestItem is null-safe
	    	item = new SyncConnectionTestItem(str);
		}
		
		return item;
	}

	public SyncTransmissionResponseItem syncToParent() {
		SyncTransmissionResponseItem transmissionResponse = new SyncTransmissionResponseItem(); 
    	transmissionResponse.setErrorMessage(SyncConstants.ERROR_SEND_FAILED.toString());
    	transmissionResponse.setFileName(SyncConstants.FILENAME_SEND_FAILED);
    	transmissionResponse.setUuid(SyncConstants.UUID_UNKNOWN);
    	transmissionResponse.setTransmissionState(SyncTransmissionState.FAILED.toString());

    	SyncTransmissionResponse response = SyncUtilTransmission.doFullSynchronize();
    	
    	if ( response != null ) {
    		transmissionResponse = new SyncTransmissionResponseItem(response);
    	}
    	
    	return transmissionResponse;
	}

}
