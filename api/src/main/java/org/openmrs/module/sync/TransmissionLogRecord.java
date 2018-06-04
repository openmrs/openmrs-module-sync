package org.openmrs.module.sync;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "sync_transmission_log_record")
public class TransmissionLogRecord implements Serializable {
    @Id
    @GeneratedValue
    @Column(name = "transmission_log_record_id")
    private Integer transmissionLogRecordId;

    @ManyToOne
    @JoinColumn(name = "transmission_log_id")
    private TransmissionLog transmissionLog;

    @ManyToOne
    @JoinColumn(name = "record_id")
    private SyncRecord record;

    public Integer getTransmissionLogRecordId() {
        return transmissionLogRecordId;
    }

    public void setTransmissionLogRecordId(Integer transmissionLogRecordId) {
        this.transmissionLogRecordId = transmissionLogRecordId;
    }

    public TransmissionLog getTransmissionLog() {
        return transmissionLog;
    }

    public void setTransmissionLog(TransmissionLog transmissionLog) {
        this.transmissionLog = transmissionLog;
    }

    public SyncRecord getRecord() {
        return record;
    }

    public void setRecord(SyncRecord record) {
        this.record = record;
    }
}