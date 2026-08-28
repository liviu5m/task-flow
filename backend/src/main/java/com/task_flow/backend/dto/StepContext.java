package com.task_flow.backend.dto;

import com.task_flow.backend.engine.TaskFlowEngine;

import lombok.Data;
@Data
public class StepContext {
    private final String stepName;
    private final String idempotencyKey;
    private final boolean intentCreatedButNotCompleted;
}
