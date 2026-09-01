package com.task_flow.backend.exception;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StepExecutionFailedException extends RuntimeException {
    private final UUID workflowId;
    private final String stepName;
    private final int attempt;

}
