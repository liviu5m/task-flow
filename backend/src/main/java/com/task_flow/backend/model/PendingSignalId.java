package com.task_flow.backend.model;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class PendingSignalId implements Serializable {
  private UUID workflowId;
  private String signalName;
}
