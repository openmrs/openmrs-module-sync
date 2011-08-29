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

import org.openmrs.OpenmrsObject;
import org.openmrs.module.sync.server.RemoteServer;

/**
 * This class is similar to the {@link SyncClass} in that it is used to mark {@link OpenmrsObject}
 * classes that should not be sync'ed. <br/>
 * The difference is that this class is per-sync-server.  
 */
public class SyncServerClass {
	
	private Integer serverClassId;
	
	private RemoteServer syncServer;
	
	private SyncClass syncClass;
	
	private Boolean sendTo = Boolean.TRUE;
	
	private Boolean receiveFrom = Boolean.TRUE;
	
	public SyncServerClass() {
	}
	
	/**
	 * @param server
	 * @param syncClass2
	 */
	public SyncServerClass(RemoteServer server, SyncClass syncClass) {
		this.syncServer = server;
		this.syncClass = syncClass;
		this.sendTo = syncClass.getDefaultSendTo();
		this.receiveFrom = syncClass.getDefaultReceiveFrom();
	}
	
	public Boolean getReceiveFrom() {
		return receiveFrom;
	}
	
	public void setReceiveFrom(Boolean receiveFrom) {
		this.receiveFrom = receiveFrom;
	}
	
	public Boolean getSendTo() {
		return sendTo;
	}
	
	public void setSendTo(Boolean sendTo) {
		this.sendTo = sendTo;
	}
	
	public Integer getServerClassId() {
		return serverClassId;
	}
	
	public void setServerClassId(Integer serverClassId) {
		this.serverClassId = serverClassId;
	}
	
	public SyncClass getSyncClass() {
		return syncClass;
	}
	
	public void setSyncClass(SyncClass syncClass) {
		this.syncClass = syncClass;
	}
	
	public RemoteServer getSyncServer() {
		return syncServer;
	}
	
	public void setSyncServer(RemoteServer syncServer) {
		this.syncServer = syncServer;
	}
	
}
