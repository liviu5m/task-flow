package com.task_flow.backend.engine;

import com.task_flow.backend.enums.WorkflowEventType;
import com.task_flow.backend.enums.WorkflowInstanceStatus;
import com.task_flow.backend.model.WorkflowEvent;
import com.task_flow.backend.model.WorkflowInstance;
import com.task_flow.backend.repository.WorkflowEventRepository;
import com.task_flow.backend.repository.WorkflowInstanceRepository;

import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TaskFlowEngine {

    private final WorkflowRegistry registry;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowEventRepository eventRepository;
    private final ObjectMapper objectMapper; 

    public TaskFlowEngine(WorkflowRegistry registry,
                          WorkflowInstanceRepository instanceRepository,
                          WorkflowEventRepository eventRepository,
                          ObjectMapper objectMapper) {  
        this.registry = registry; 
        this.instanceRepository = instanceRepository; 
        this.eventRepository = eventRepository; 
        this.objectMapper = objectMapper; 
    }


    @Transactional
    public UUID start(String workflowName, Map<String, Object> initialInput) {
        UUID workflowId = UUID.randomUUID();

        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(workflowId);
        instance.setName(workflowName);
        instance.setStatus(WorkflowInstanceStatus.valueOf("RUNNING"));
        instanceRepository.save(instance);

        appendEvent(workflowId, 1L, WorkflowEventType.WORKFLOW_STARTED, initialInput);

        executeWorkflow(workflowId, workflowName);

        return workflowId;
    }

    public void executeWorkflow(UUID workflowId, String workflowName) {
        WorkflowDefinition definition = registry.get(workflowName);
        eventRepository.flush();
        Map<String, Object> context = loadExistingStateAndReplay(workflowId);
        long nextSequenceId = eventRepository.countByWorkflowId(workflowId) + 1L;

        try {
            for (WorkflowStep step : definition.steps()) {

                if (context.containsKey(step.getName())) {
                    continue; 
                }

                appendEvent(workflowId, nextSequenceId++, WorkflowEventType.STEP_STARTED, Map.of("stepName", step.getName()));

                Object output = step.getLambda().execute(context);

                context.put(step.getName(), output);

                appendEvent(workflowId, nextSequenceId++, WorkflowEventType.STEP_COMPLETED, Map.of(
                    "stepName", step.getName(),
                    "output", output
                ));
            }

            appendEvent(workflowId, nextSequenceId++, WorkflowEventType.WORKFLOW_COMPLETED, Map.of());
            updateInstanceStatus(workflowId, "COMPLETED");

        } catch (Exception e) {
            try {
                appendEvent(workflowId, nextSequenceId, WorkflowEventType.STEP_FAILED, Map.of("error", e.getMessage()));
                updateInstanceStatus(workflowId, "FAILED");
            } catch (Exception innerEx) {
                throw new RuntimeException("Workflow failed and failed to log error event", e);
            }
            throw new RuntimeException("Workflow execution failed for ID: " + workflowId, e);
        }
    }

    private Map<String, Object> loadExistingStateAndReplay(UUID workflowId) {
        Map<String, Object> context = new HashMap<>();
        List<WorkflowEvent> events = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(workflowId);

        for (WorkflowEvent event : events) {
            try {
                Map<String, Object> data = objectMapper.readValue(event.getData(), Map.class);

                if (event.getType() == WorkflowEventType.WORKFLOW_STARTED) {
                    context.putAll(data);
                } else if (event.getType() == WorkflowEventType.STEP_COMPLETED) {
                    String stepName = (String) data.get("stepName");
                    Object output = data.get("output");
                    context.put(stepName, output);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse event data for crash recovery", e);
            }
        }

        return context;
    }

    private void appendEvent(UUID workflowId, long sequenceId, WorkflowEventType eventType, Object dataPayload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(dataPayload);

            WorkflowEvent event = new WorkflowEvent();
            event.setWorkflowId(workflowId);
            event.setSequenceId(sequenceId);
            event.setType(eventType);
            event.setData(jsonPayload);

            eventRepository.save(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event payload to JSON", e);
        }
    }

    private void updateInstanceStatus(UUID workflowId, String status) {
        WorkflowInstance instance = instanceRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + workflowId));
        instance.setStatus(WorkflowInstanceStatus.valueOf(status));
        instanceRepository.save(instance);
    }
}
