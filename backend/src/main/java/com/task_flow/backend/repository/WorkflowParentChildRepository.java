package com.task_flow.backend.repository;

import com.task_flow.backend.model.WorkflowParentChild;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowParentChildRepository extends JpaRepository<WorkflowParentChild, UUID> {
  Optional<WorkflowParentChild> findByChildWorkflowId(UUID childWorkflowId);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT w FROM WorkflowParentChild w WHERE w.childWorkflowId = :childWorkflowId")
  Optional<WorkflowParentChild> findByChildWorkflowIdForUpdate(UUID childWorkflowId);
}
