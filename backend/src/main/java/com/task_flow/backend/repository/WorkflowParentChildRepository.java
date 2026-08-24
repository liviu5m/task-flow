package com.task_flow.backend.repository;

import com.task_flow.backend.model.WorkflowParentChild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowParentChildRepository extends JpaRepository<WorkflowParentChild, UUID> {
  Optional<WorkflowParentChild> findByChildWorkflowId(UUID childWorkflowId);
}
