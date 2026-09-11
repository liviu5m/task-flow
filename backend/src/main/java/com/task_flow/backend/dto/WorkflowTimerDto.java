package com.task_flow.backend.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTimerDto {
    private UUID id;
    private UUID workflowId;
    private String stepName;
    private String firesAt;
    private boolean fired;
}
