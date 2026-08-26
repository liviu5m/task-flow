package com.task_flow.backend.repository;

import com.task_flow.backend.model.WorkflowSequence;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkflowSequenceRepository extends JpaRepository<WorkflowSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WorkflowSequence w WHERE w.workflowId = :workflowId")
    WorkflowSequence findByWorkflowIdForUpdate(@Param("workflowId") UUID workflowId);

    @Modifying
    @Query(value = """
        INSERT INTO "task-flow".workflow_sequences 
            (workflow_id, current_seq, created_at, updated_at)
        VALUES (:workflowId, 0, NOW(), NOW())
        ON CONFLICT (workflow_id) DO NOTHING
        """, nativeQuery = true)
    void insertIfAbsent(@Param("workflowId") UUID workflowId);
} 
