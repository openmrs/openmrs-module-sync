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
package org.openmrs.module.sync.server;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.openmrs.module.sync.SyncServerClass;
import org.openmrs.module.sync.SyncTransmissionState;

/**
 *
 */
public class RemoteServer {
	
	private Integer serverId;
	private String nickname;
	private String address;
	private RemoteServerType serverType;
	private String username;
	private String password;
    private Date lastSync;
    private SyncTransmissionState lastSyncState;
    private Set<SyncServerClass> serverClasses;
    private Set<SyncServerRecord> serverRecords;
    private String uuid;
    private Boolean disabled = false;
    private String childUsername = null;
	
    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Set<SyncServerClass> getServerClasses() {
        return serverClasses;
    }

    public void setServerClasses(Set<SyncServerClass> serverClasses) {
        this.serverClasses = serverClasses;
    }

    public Date getLastSync() {
        return lastSync;
    }

    public void setLastSync(Date lastSync) {
        this.lastSync = lastSync;
    }

    public SyncTransmissionState getLastSyncState() {
        return lastSyncState;
    }

    public void setLastSyncState(SyncTransmissionState value) {
        this.lastSyncState = value;
    }

    public String getAddress() {
    	return address;
    }

	public void setAddress(String address) {
    	this.address = address;
    }

	public Boolean getIsSSL() {
    	return this.address.startsWith("https");
    }

	public String getNickname() {
    	return nickname;
    }

	public void setNickname(String nickname) {
    	this.nickname = nickname;
    }

	public String getPassword() {
    	return password;
    }

	public void setPassword(String password) {
    	this.password = password;
    }

	public Integer getServerId() {
    	return serverId;
    }

	public void setServerId(Integer serverId) {
    	this.serverId = serverId;
    }
	
	public RemoteServerType getServerType() {
    	return serverType;
    }
	
	public void setServerType(RemoteServerType serverType) {
    	this.serverType = serverType;
    }
	
	public String getUsername() {
    	return username;
    }
	
	public void setUsername(String username) {
    	this.username = username;
    }
	
	/**
	 * Uses the {@link #getServerClasses()} method to figure out which classes
     * should not be sync'd to this server.
	 * 
	 * @return
	 */
	public Set<String> getClassesNotSent() {
	    Set<String> ret = new HashSet<String>();
        
        if ( this.serverClasses != null ) {
            for ( SyncServerClass serverClass : this.serverClasses ) {
                if ( serverClass.getSendTo() == false ) ret.add(serverClass.getSyncClass().getName());
            }
        }
        
        return  ret;
    }
    
    /**
     * Uses the {@link #getServerClasses()} method to figure out which classes
     * should not be sync'd from this server.
     * 
     * @return
     */
    public Set<String> getClassesNotReceived() {
        Set<String> ret = new HashSet<String>();
        
        if ( this.serverClasses != null ) {
            for ( SyncServerClass serverClass : this.serverClasses ) {
                if ( serverClass.getReceiveFrom() == false ) ret.add(serverClass.getSyncClass().getName());
            }
        }

        return  ret;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((address == null) ? 0 : address.hashCode());
        result = PRIME * result + ((uuid == null) ? 0 : uuid.hashCode());
        result = PRIME * result + ((lastSync == null) ? 0 : lastSync.hashCode());
        result = PRIME * result + ((nickname == null) ? 0 : nickname.hashCode());
        result = PRIME * result + ((password == null) ? 0 : password.hashCode());
        result = PRIME * result + ((serverClasses == null) ? 0 : serverClasses.hashCode());
        result = PRIME * result + ((serverId == null) ? 0 : serverId.hashCode());
        result = PRIME * result + ((serverType == null) ? 0 : serverType.hashCode());
        result = PRIME * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final RemoteServer other = (RemoteServer) obj;
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        if (lastSync == null) {
            if (other.lastSync != null)
                return false;
        } else if (!lastSync.equals(other.lastSync))
            return false;
        if (nickname == null) {
            if (other.nickname != null)
                return false;
        } else if (!nickname.equals(other.nickname))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (serverClasses == null) {
            if (other.serverClasses != null)
                return false;
        } else if (!serverClasses.equals(other.serverClasses))
            return false;
        if (serverId == null) {
            if (other.serverId != null)
                return false;
        } else if (!serverId.equals(other.serverId))
            return false;
        if (serverType == null) {
            if (other.serverType != null)
                return false;
        } else if (!serverType.equals(other.serverType))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }

    public String getChildUsername() {
        return childUsername;
    }

    public void setChildUsername(String childUsername) {
        this.childUsername = childUsername;
    }

	public Set<SyncServerRecord> getServerRecords() {
    	return serverRecords;
    }

	public void setServerRecords(Set<SyncServerRecord> serverRecords) {
    	this.serverRecords = serverRecords;
    }

}
