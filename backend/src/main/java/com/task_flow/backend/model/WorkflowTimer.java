package com.task_flow.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_timers")
@Data
@NoArgsConstructor
public class WorkflowTimer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID workflowId;

    @Column(nullable = false, length = 100)
    private String stepName;

    @Column(nullable = false)
    private LocalDateTime firesAt;

    @Column(nullable = false)
    private boolean fired = false;
}
