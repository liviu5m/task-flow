package com.task_flow.backend.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workflow_sequences")
@Data
@NoArgsConstructor
public class WorkflowSequence extends BaseEntity {
    @Id
    private UUID workflowId;
    
    @Column(nullable = false)
    private Long currentSeq = 0L;

    public WorkflowSequence(UUID workflowId, Long currentSeq) {
        this.workflowId = workflowId;
        this.currentSeq = currentSeq;
    }

}
