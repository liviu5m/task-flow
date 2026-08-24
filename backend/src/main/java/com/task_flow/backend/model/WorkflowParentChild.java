package com.task_flow.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "workflow_parent_child")
@Data
@NoArgsConstructor
public class WorkflowParentChild extends BaseEntity {

    @Id
    private UUID childWorkflowId;

    @Column(nullable = false)
    private UUID parentWorkflowId;

    @Column(nullable = false)
    private String parentStepName;

}
