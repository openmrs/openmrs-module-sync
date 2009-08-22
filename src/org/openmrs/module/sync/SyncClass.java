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


/**
  *
  */
public class SyncClass {
    private Integer syncClassId;
    private String name;
    private SyncClassType type;
    private Boolean defaultTo;
    private Boolean defaultFrom;
    
    public Boolean getDefaultFrom() {
        return defaultFrom;
    }
    public void setDefaultFrom(Boolean defaultFrom) {
        this.defaultFrom = defaultFrom;
    }
    public Boolean getDefaultTo() {
        return defaultTo;
    }
    public void setDefaultTo(Boolean defaultTo) {
        this.defaultTo = defaultTo;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getSyncClassId() {
        return syncClassId;
    }
    public void setSyncClassId(Integer syncClassId) {
        this.syncClassId = syncClassId;
    }
    public SyncClassType getType() {
        return type;
    }
    public void setType(SyncClassType type) {
        this.type = type;
    }
    
    
    
}
