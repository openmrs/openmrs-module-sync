package org.openmrs.module.sync;

import org.openmrs.OpenmrsObject;

/**
 * Transient class used to hold an Openmrs Object and the sync item state associated with that
 * object. Used in the SyncIngestService.processSyncItem to keep track of all objects processed so
 * that any class-specified or state-specified pre-commit actions can be applied.
 */
public class SyncProcessedObject {
	
	OpenmrsObject object;
	
	SyncItemState state;
	
	public SyncProcessedObject() {
	}
	
	public SyncProcessedObject(OpenmrsObject object, SyncItemState state) {
		this.object = object;
		this.state = state;
	}
	
	public OpenmrsObject getObject() {
		return object;
	}
	
	public void setObject(OpenmrsObject object) {
		this.object = object;
	}
	
	public SyncItemState getState() {
		return state;
	}
	
	public void setState(SyncItemState state) {
		this.state = state;
	}
	
}
