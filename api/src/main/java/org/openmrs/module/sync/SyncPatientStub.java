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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Patient;

/**
 * Utility class that exists in support of odd case of saving patient who is already
 * user in the system. The reason special handling is necessary is that patient save
 * in case of the patient already being user is currently handled outside of normal
 * hibernate POJO. This is because Patient inherits from Person and so does User,
 * therefore hibernate save on new Patient when User already exists throws exception.
 * This is coded around during save of patient in HibernatePatientDAO.savePatient. 
 * Specifically,  private HibernatePatientDAO.insertPatientStubIfNeeded() uses jdbc to 
 * insert a row into patient table.
 * 
 * <p/>Naturally, this bypasses sync all together resulting in 'missing' 'NEW' syncItem for
 * patient object when this scenario is run on the server with sync enabled.
 * 
 * <p/>The solution. Sync deals with the core issue of openmrs api bypassing hibernate in
 * this use case by 'listening' on savePatient() method AOP and then performing a 
 * compensating step of manufacturing necessary 'NEW' syncItem. This is implemented as follows.
 * 
 * <p/>During the save of the patient:
 * <br/>1. SavePatientAdvice AOP intercepts all savePatient() invocations and calls to 
 * SyncService.handleInsertPatientStubIfNeeded() to perform compensating actions, if needed.
 * 
 * <br/>2. SyncService.handleInsertPatientStubIfNeeded() figures if the pending save is
 * indeed case of new patient from existing user and if it is, then calls via static method
 * on HibernateSyncInterceptor to add the new sync item for just this purpose: the stub for 
 * patient table. 
 * 
 * <br/>3. In order to create new sync item, the interceptor needs to serialize relevant state
 * into sync record. This is done by using this utility holder class, SyncPatientStub. The only
 * purpose of this class is to be serialization vehicle for needed information: uuid, creator,
 * dateCreated.
 * 
 * <p/>The ingest of this newly created sync item is processed as follows: 
 * <br/>1. Ingest logic in SyncIngestServiceImpl.processOpenmrsObject() works as for any 
 * other openmrs object; it delegates the handling of 'NEW' on instance of openmrs object to
 * syncUtil.updateOpenmrsObject.
 * 
 * <br/>2. syncUtil.updateOpenmrsObject determines that instance of SyncPatientStub is 
 * being processed and hence we can't just save it via hibernate session.saveorupdate.
 * Instead, special methods on SyncIngestService & SyncDAO exists to handle this: 
 * dao.processSyncPatientStub. 
 * 
 * <br/>3. As the SyncIngestService.processSyncPatientStub() delegates to dao.dao.processSyncPatientStub(),
 * the dao then performs necessary insert on patient table. This is done by first by using the stub's uuid
 * to look-up the existing person record person_id which is then used as patient_id for the new row
 * in the patient table.
 * 
 * <br/>4. Finally after the row was created, the DAO also invokes the interceptor.addSyncItemForPatientStub()
 * so that the appropriate syncItem is created during ingest too. Note this must be there sync the ingesting
 * change can be propagated later to other servers.
 * 
 * 
 * @see org.openmrs.module.sync.advice.SavePatientAdvice
 * @see org.openmrs.module.sync.api.SyncService#handleInsertPatientStubIfNeeded(Patient)
 * @see org.openmrs.module.sync.api.db.hibernate.HibernateSyncInterceptor#addSyncItemForPatientStub(SyncPatientStub)
 * @see org.openmrs.module.sync.api.impl.SyncIngestServiceImpl#processSyncPatientStub(SyncPatientStub)
 * @see org.openmrs.module.sync.SyncUtil#updateOpenmrsObject(org.openmrs.OpenmrsObject, String, String)
 * @see org.openmrs.module.sync.api.db.hibernate.HibernateSyncDAO#processSyncPatientStub(SyncPatientStub)
 * 
 */
public class SyncPatientStub extends BaseOpenmrsData implements java.io.Serializable {
	
	public static final long serialVersionUID = 93123L;
	
	private transient static final Log log = LogFactory.getLog(SyncPatientStub.class);
	
	// Fields
	private Integer patientId;	
			
	// Constructors
	/** default constructor */
	public SyncPatientStub() {
	}

	public SyncPatientStub(Patient p) {
		this.setPatientId(p.getPatientId());
		this.setUuid(p.getUuid());
		this.setCreator(p.getCreator());
		this.setDateCreated(p.getDateCreated());
		this.setVoided(false);
	}
	
	public Integer getPatientId() {
	    return patientId;
    }

	public void setPatientId(Integer id) {
	    this.patientId = id;
    }
	
	public Integer getId() {
	    return getPatientId();
    }

	public void setId(Integer id) {
	    setPatientId(id);
    }	
}
