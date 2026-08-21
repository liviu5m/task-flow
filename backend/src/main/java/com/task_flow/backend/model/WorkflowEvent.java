package com.task_flow.backend.model;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.task_flow.backend.enums.WorkflowEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "workflow_events")
@Data
@IdClass(WorkflowEventId.class)
public class WorkflowEvent extends BaseEntity {
    @Id
    private UUID workflowId;
    
    @Id
    @Column(nullable = false)
    private Long sequenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private WorkflowEventType type;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String data;
}
    
