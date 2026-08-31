package com.task_flow.backend.model;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pending_signals")
@IdClass(PendingSignalId.class)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PendingSignal extends BaseEntity {
  
  @Id
  private UUID workflowId;
  @Id
  private String signalName;

}

