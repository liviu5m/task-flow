package com.task_flow.backend.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEventDto {
    private UUID workflowId;
    private Long sequenceId;
    private String type;
    private Object data;
    private String createdAt;
}
