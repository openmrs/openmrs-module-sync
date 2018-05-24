package org.openmrs.module.sync;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openmrs.User;
import org.openmrs.module.sync.server.RemoteServer;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "sync_transmission_log")
public class TransmissionLog implements Serializable {
    @Id @GeneratedValue
    @Column(name = "transmission_log_id")
    private Integer transmissionLogId;

    @Column(name = "run_at")
    private Date runAt;

    @ManyToOne
    @JoinColumn(name = "run_by", referencedColumnName = "user_id")
    private User runBy;

    @Enumerated(EnumType.STRING)
    private SyncTransmissionStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "detailed_error")
    private String detailedError;

    @OneToMany(mappedBy = "transmissionLog", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<TransmissionLogRecord> transmissionLogRecords = new ArrayList<TransmissionLogRecord>();

    @OneToOne
    @JoinColumn(name = "server_id", referencedColumnName = "server_id")
    private RemoteServer remoteServer;

    public TransmissionLog() {}

    public TransmissionLog(Exception e, RemoteServer remoteServer) {
        this.errorMessage = e.getMessage();
        this.detailedError = ExceptionUtils.getStackTrace(e);
        this.status = SyncTransmissionStatus.FAILURE;
        this.remoteServer = remoteServer;
    }

    public Integer getTransmissionLogId() {
        return transmissionLogId;
    }

    public void setTransmissionLogId(Integer transmissionLogId) {
        this.transmissionLogId = transmissionLogId;
    }

    public Date getRunAt() {
        return runAt;
    }

    public void setRunAt(Date runAt) {
        this.runAt = runAt;
    }

    public User getRunBy() {
        return runBy;
    }

    public void setRunBy(User runBy) {
        this.runBy = runBy;
    }

    public SyncTransmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SyncTransmissionStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDetailedError() {
        return detailedError;
    }

    public void setDetailedError(String detailedError) {
        this.detailedError = detailedError;
    }

    public List<TransmissionLogRecord> getTransmissionLogRecords() {
        return transmissionLogRecords;
    }

    public void setTransmissionLogRecords(List<TransmissionLogRecord> transmissionLogRecords) {
        this.transmissionLogRecords = transmissionLogRecords;
    }

    public RemoteServer getRemoteServer() {
        return remoteServer;
    }

    public void setRemoteServer(RemoteServer remoteServer) {
        this.remoteServer = remoteServer;
    }

    public void setSyncRecords(List<SyncRecord> records) {
        // Clean the existing Transmission Record.
        // check for null safety
        if(records != null) {
            transmissionLogRecords = new ArrayList<TransmissionLogRecord>();
            for (SyncRecord record : records) {
                TransmissionLogRecord logRecord = new TransmissionLogRecord();
                logRecord.setRecord(record);
                logRecord.setTransmissionLog(this);
                transmissionLogRecords.add(logRecord);
            }
        }
    }

    public void addSyncRecord(SyncRecord record) {
        TransmissionLogRecord logRecord = new TransmissionLogRecord();
        logRecord.setRecord(record);
        logRecord.setTransmissionLog(this);
        transmissionLogRecords.add(logRecord);
    }
}
