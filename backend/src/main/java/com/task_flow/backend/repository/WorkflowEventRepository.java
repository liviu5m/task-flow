package com.task_flow.backend.repository;

import com.task_flow.backend.model.WorkflowEvent;
import com.task_flow.backend.model.WorkflowEventId;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowEventRepository extends JpaRepository<WorkflowEvent, WorkflowEventId> {
    int countByWorkflowId(UUID workflowId);
    List<WorkflowEvent> findByWorkflowIdOrderBySequenceIdAsc(UUID workflowId);
    @Query("SELECT MAX(e.sequenceId) FROM WorkflowEvent e WHERE e.workflowId = :workflowId")
    Long findMaxSequenceIdByWorkflowId(@Param("workflowId") UUID workflowId);
}
