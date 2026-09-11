package com.task_flow.backend.reponses;

import java.util.List;
import java.util.UUID;

import com.task_flow.backend.dto.PendingSignalDto;
import com.task_flow.backend.dto.WorkflowEventDto;
import com.task_flow.backend.dto.WorkflowTimerDto;
import com.task_flow.backend.enums.WorkflowInstanceStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {
    private UUID id;
    private String name;
    private WorkflowInstanceStatus status;
    private String userId;
    private String createdAt;
    private List<WorkflowEventDto> events;
    private List<WorkflowTimerDto> timers;
    private List<PendingSignalDto> signals;
}
