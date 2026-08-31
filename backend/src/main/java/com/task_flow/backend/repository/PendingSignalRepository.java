package com.task_flow.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.task_flow.backend.model.PendingSignal;
import com.task_flow.backend.model.PendingSignalId;

@Repository
public interface PendingSignalRepository extends JpaRepository<PendingSignal, PendingSignalId> {
  
  PendingSignal findByWorkflowIdAndSignalName(UUID workflowId, String signalName);
  boolean existsById(PendingSignalId id);

}
