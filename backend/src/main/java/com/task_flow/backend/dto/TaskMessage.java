package com.task_flow.backend.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class TaskMessage {
    private final UUID workflowId;
    private final String workflowName;
    private final String stepName;

    public TaskMessage(UUID workflowId, String workflowName, String stepName) {
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.stepName = stepName;
    }
}
