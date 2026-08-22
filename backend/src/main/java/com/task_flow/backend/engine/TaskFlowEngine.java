package com.task_flow.backend.engine;

import com.task_flow.backend.enums.WorkflowEventType;
import com.task_flow.backend.enums.WorkflowInstanceStatus;
import com.task_flow.backend.model.WorkflowEvent;
import com.task_flow.backend.model.WorkflowInstance;
import com.task_flow.backend.repository.WorkflowEventRepository;
import com.task_flow.backend.repository.WorkflowInstanceRepository;

import tools.jackson.databind.ObjectMapper;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
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


  @Transactional(noRollbackFor = RuntimeException.class)
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
    DirectedAcyclicGraph<String, DefaultEdge> dag = definition.dag();

    boolean progress = true;
    while (progress) {
      progress = false;
      TopologicalOrderIterator<String, DefaultEdge> iterator = new TopologicalOrderIterator<>(dag);
      while (iterator.hasNext()) {
        String stepName = iterator.next();
        WorkflowStep step = definition.stepMap().get(stepName);

        if (context.containsKey(stepName)) {
          continue;
        }

        boolean dependenciesMet = true;
        for (String dependency : step.getDependencies()) {
          if (!context.containsKey(dependency)) {
            dependenciesMet = false;
            break;
          }
        }

        if (!dependenciesMet) {
          continue;
        }

        progress = true;
        long nextSequenceId = eventRepository.countByWorkflowId(workflowId) + 1L;
        appendEvent(workflowId, nextSequenceId++, WorkflowEventType.STEP_STARTED, Map.of("stepName", stepName));

        int maxAttempts = step.getMaxAttempts();
        int attempts = 0;
        boolean success = false;
        Object output = null;

        while(attempts < maxAttempts && !success) {
          attempts++;
          try {
            output = step.getLambda().execute(context);
            success = true;
          } catch (Exception e) {
            long failSeq = eventRepository.countByWorkflowId(workflowId) + 1L;
            appendEvent(workflowId,failSeq, WorkflowEventType.STEP_FAILED, Map.of(
              "stepName", stepName,
              "error", e.getMessage()
            ));

            if (attempts == maxAttempts) {
              throw new RuntimeException("Step '" + stepName + "' failed after " + maxAttempts + " attempts.", e);
            }
          }
        }
        long completeSeq = eventRepository.countByWorkflowId(workflowId) + 1L;
        context.put(stepName, output);
        appendEvent(workflowId, completeSeq, WorkflowEventType.STEP_COMPLETED, Map.of(
          "stepName", stepName,
          "output", output
        ));
      }

      if (context.size() >= definition.steps().size()) {
        long finalSeq = eventRepository.countByWorkflowId(workflowId) + 1L;
        appendEvent(workflowId, finalSeq, WorkflowEventType.WORKFLOW_COMPLETED, Map.of());
        updateInstanceStatus(workflowId, "COMPLETED");
        break;
      }
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
