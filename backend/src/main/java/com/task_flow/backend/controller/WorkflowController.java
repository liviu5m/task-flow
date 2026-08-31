package com.task_flow.backend.controller;

import com.task_flow.backend.engine.TaskFlowEngine;
import com.task_flow.backend.enums.WorkflowEventType;
import com.task_flow.backend.model.PendingSignal;
import com.task_flow.backend.model.PendingSignalId;
import com.task_flow.backend.model.WorkflowEvent;
import com.task_flow.backend.model.WorkflowInstance;
import com.task_flow.backend.repository.PendingSignalRepository;
import com.task_flow.backend.repository.WorkflowEventRepository;
import com.task_flow.backend.repository.WorkflowInstanceRepository;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final TaskFlowEngine engine;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final PendingSignalRepository pendingSignalRepository;
    private final WorkflowEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public WorkflowController(
        TaskFlowEngine engine,
        WorkflowInstanceRepository workflowInstanceRepository,
        PendingSignalRepository pendingSignalRepository,
        WorkflowEventRepository eventRepository,
        ObjectMapper objectMapper
    ) {
        this.engine = engine;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.pendingSignalRepository = pendingSignalRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/start/{workflowName}")
    public ResponseEntity<?> getWorkflowInstance(@PathVariable String workflowName, @RequestBody Map<String, Object> initialInput) {
        UUID workflowId = engine.start(workflowName, initialInput);
        return ResponseEntity.ok(Map.of("id", workflowId, "status", "RUNNING"));
    }
    @PostMapping("/{workflowId}/signal/{signalName}")
    @Transactional
    public ResponseEntity<String> receiveSignal(
            @PathVariable UUID workflowId,
            @PathVariable String signalName,
            @RequestBody Map<String, Object> payload) {

        PendingSignalId signalId = new PendingSignalId(workflowId, signalName);
        PendingSignal pendingSignal = pendingSignalRepository.findById(signalId).orElse(null);
        WorkflowInstance workflowInstance = workflowInstanceRepository.findById(workflowId).orElse(null);

        if (workflowInstance == null) {
            return ResponseEntity.badRequest().body("No workflow instance found: " + workflowId);
        }

        if (pendingSignal == null) {
            return ResponseEntity.badRequest().body("No workflow awaiting signal: " + signalName);
        }

        pendingSignalRepository.delete(pendingSignal);

        try {
            String eventData = objectMapper.writeValueAsString(Map.of(
                "signalName", signalName,
                "payload", payload,
                "receivedAt", Instant.now().toString()
            ));

            engine.appendEventAtomic(workflowId, WorkflowEventType.SIGNAL_RECEIVED,
                Map.of("signalName", signalName, "payload", payload, "receivedAt", Instant.now().toString()));

        } catch (Exception e) {
            throw new RuntimeException("Failed to save signal event", e);
        }

        engine.executeWorkflow(workflowId, workflowInstance.getName());

        return ResponseEntity.ok().body("Signal received");
    }
}
