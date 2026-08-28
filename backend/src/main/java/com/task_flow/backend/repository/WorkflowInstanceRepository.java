package com.task_flow.backend.repository;

import com.task_flow.backend.enums.WorkflowInstanceStatus;
import com.task_flow.backend.model.WorkflowInstance;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {
  List<WorkflowInstance> findByStatus(WorkflowInstanceStatus status);
  @Query("SELECT w FROM WorkflowInstance w WHERE w.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<WorkflowInstance> findByIdForUpdate(@Param("id") UUID id);

}
