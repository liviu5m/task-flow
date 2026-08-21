package com.task_flow.backend.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import lombok.Data;

@Data
public class WorkflowEventId implements Serializable{

    private UUID workflowId;
    private Long sequenceId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowEventId that = (WorkflowEventId) o;
        return Objects.equals(workflowId, that.workflowId) && Objects.equals(sequenceId, that.sequenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workflowId, sequenceId);
    }
}
