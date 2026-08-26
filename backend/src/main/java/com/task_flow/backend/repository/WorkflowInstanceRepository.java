package com.task_flow.backend.repository;

import com.task_flow.backend.enums.WorkflowInstanceStatus;
import com.task_flow.backend.model.WorkflowInstance;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {
  List<WorkflowInstance> findByStatus(WorkflowInstanceStatus status);
  

}
