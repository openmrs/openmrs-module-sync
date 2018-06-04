package org.openmrs.module.sync.web;

import org.openmrs.scheduler.TaskDefinition;

public class TasksDTO {
    private TaskDefinition cleanupSyncTablesTask = new TaskDefinition();
    private TaskDefinition cleanupTransmissionLogsTask = new TaskDefinition();

    public TaskDefinition getCleanupSyncTablesTask() {
        return cleanupSyncTablesTask;
    }

    public void setCleanupSyncTablesTask(TaskDefinition taskDefinition) {
        this.cleanupSyncTablesTask = taskDefinition;
    }

    public TaskDefinition getCleanupTransmissionLogsTask() {
        return cleanupTransmissionLogsTask;
    }

    public void setCleanupTransmissionLogsTask(TaskDefinition taskDefinition) {
        this.cleanupTransmissionLogsTask = taskDefinition;
    }
}
