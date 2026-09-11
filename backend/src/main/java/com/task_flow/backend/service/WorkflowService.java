package com.task_flow.backend.service;

import com.task_flow.backend.dto.PendingSignalDto;
import com.task_flow.backend.dto.WorkflowEventDto;
import com.task_flow.backend.dto.WorkflowTimerDto;
import com.task_flow.backend.engine.WorkflowRegistry;
import com.task_flow.backend.model.ApiKey;
import com.task_flow.backend.model.PendingSignal;
import com.task_flow.backend.model.WorkflowEvent;
import com.task_flow.backend.model.WorkflowInstance;
import com.task_flow.backend.model.WorkflowTimer;
import com.task_flow.backend.reponses.WorkflowResponse;
import com.task_flow.backend.repository.ApiKeyRepository;
import com.task_flow.backend.repository.PendingSignalRepository;
import com.task_flow.backend.repository.WorkflowEventRepository;
import com.task_flow.backend.repository.WorkflowInstanceRepository;
import com.task_flow.backend.repository.WorkflowTimerRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class WorkflowService {

    private final WorkflowRegistry registry;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final WorkflowEventRepository eventRepository;
    private final WorkflowTimerRepository workflowTimerRepository;
    private final PendingSignalRepository pendingSignalRepository;

    public WorkflowService(
        WorkflowRegistry registry,
        WorkflowInstanceRepository workflowInstanceRepository,
        ApiKeyRepository apiKeyRepository,
        WorkflowEventRepository eventRepository,
        WorkflowTimerRepository workflowTimerRepository,
        PendingSignalRepository pendingSignalRepository
    ) {
        this.registry = registry;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.eventRepository = eventRepository;
        this.workflowTimerRepository = workflowTimerRepository;
        this.pendingSignalRepository = pendingSignalRepository;
    }

    public List<String> getWorkflows(Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId).orElse(null);
        if (apiKey == null) {
            throw new IllegalArgumentException("No API key found for id: " + apiKeyId);
        }
        return registry.listWorkflows(apiKey);
    }

    public List<WorkflowResponse> getWorkflowInstances(Long id) {
        String userId = registry.getUserId();
        List<WorkflowInstance> instances = workflowInstanceRepository.findByUserId(userId);

        List<WorkflowResponse> response = instances.stream().map(instance -> {
        UUID workflowId = instance.getId();
        List<WorkflowEvent> events = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(workflowId);
        List<WorkflowTimer> timers = workflowTimerRepository.findByWorkflowId(workflowId);
        List<PendingSignal> signals = pendingSignalRepository.findByWorkflowId(workflowId);

        WorkflowResponse dto = new WorkflowResponse();
        dto.setId(workflowId);
        dto.setName(instance.getName());
        dto.setStatus(instance.getStatus());
        dto.setUserId(instance.getUserId());
        dto.setCreatedAt(instance.getCreatedAt() != null ? instance.getCreatedAt().toString() : "");
        dto.setEvents(events.stream().map(e -> new WorkflowEventDto(e.getWorkflowId(), e.getSequenceId(), e.getType().name(), e.getData(), e.getCreatedAt() != null ? e.getCreatedAt().toString() : "")).toList());
        dto.setTimers(timers.stream().map(t -> new WorkflowTimerDto(t.getId(), t.getWorkflowId(), t.getStepName(), t.getFiresAt() != null ? t.getFiresAt().toString() : "", t.isFired())).toList());
        dto.setSignals(signals.stream().map(s -> new PendingSignalDto(s.getWorkflowId(), s.getSignalName())).toList());
        return dto;
    }).toList();

      return response;
    }
}
