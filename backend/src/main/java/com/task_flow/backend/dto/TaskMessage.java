package com.task_flow.backend.dto;

import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskMessage {
    private UUID workflowId;
    private String workflowName;
    private String stepName;
    private int attempt = 1;
    
    public TaskMessage(UUID workflowId, String workflowName, String stepName, int attempt) {
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.stepName = stepName;
        this.attempt = attempt;
    }

    public TaskMessage(UUID workflowId, String workflowName, String stepName) {
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.stepName = stepName;
        this.attempt = 1;
    }
}
