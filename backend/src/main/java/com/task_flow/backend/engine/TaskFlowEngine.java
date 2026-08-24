package com.task_flow.backend.engine;
import com.task_flow.backend.enums.WorkflowEventType;
import com.task_flow.backend.enums.WorkflowInstanceStatus;
import com.task_flow.backend.model.WorkflowEvent;
import com.task_flow.backend.model.WorkflowInstance;
import com.task_flow.backend.model.WorkflowParentChild;
import com.task_flow.backend.model.WorkflowTimer;
import com.task_flow.backend.repository.WorkflowEventRepository;
import com.task_flow.backend.repository.WorkflowInstanceRepository;
import com.task_flow.backend.repository.WorkflowParentChildRepository;
import com.task_flow.backend.repository.WorkflowTimerRepository;
import com.task_flow.backend.service.RedisTaskProducer;

import tools.jackson.databind.ObjectMapper;

import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskFlowEngine {

  private final WorkflowRegistry registry;
  private final WorkflowInstanceRepository instanceRepository;
  private final WorkflowEventRepository eventRepository;
  private final ObjectMapper objectMapper; 
  private final RedisTaskProducer redisTaskProducer;
  private final WorkflowTimerRepository workflowTimerRepository;
  private final WorkflowParentChildRepository parentChildRepository;

  public TaskFlowEngine(WorkflowRegistry registry,
    WorkflowInstanceRepository instanceRepository,
    WorkflowEventRepository eventRepository,
    ObjectMapper objectMapper,
    RedisTaskProducer redisTaskProducer,
    WorkflowTimerRepository timerRepository,
    WorkflowParentChildRepository parentChildRepository
    ) {  
    this.registry = registry; 
    this.instanceRepository = instanceRepository; 
    this.eventRepository = eventRepository; 
    this.objectMapper = objectMapper; 
    this.redisTaskProducer = redisTaskProducer;
    this.workflowTimerRepository = timerRepository;
    this.parentChildRepository = parentChildRepository;
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
    Map<String, Object> context = loadExistingStateAndReplay(workflowId);
    TopologicalOrderIterator<String, org.jgrapht.graph.DefaultEdge> iterator = 
        new TopologicalOrderIterator<>(definition.dag());
    
    boolean allStepsCompleted = true;
    while (iterator.hasNext()) {
        String stepName = iterator.next();
        WorkflowStep step = definition.stepMap().get(stepName);

        

        if (context.containsKey(stepName)) {
          continue;
        }else {
          allStepsCompleted = false;
        }

        if (step == null) {
            throw new IllegalStateException("Step definition missing for: " + stepName);
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

        redisTaskProducer.enqueueTask(workflowId, workflowName, stepName);
    }

    if (allStepsCompleted) {
        WorkflowInstance instance = instanceRepository.findById(workflowId).orElse(null);
        if (instance != null && instance.getStatus() == WorkflowInstanceStatus.RUNNING) {

            Long maxSeq = eventRepository.findMaxSequenceIdByWorkflowId(workflowId);
            long finalSeq = (maxSeq == null ? 0L : maxSeq) + 1L;
            appendEvent(workflowId, finalSeq, WorkflowEventType.WORKFLOW_COMPLETED, Map.of());
            updateInstanceStatus(workflowId, "COMPLETED");
        }
    }
  }

  @Transactional(noRollbackFor = RuntimeException.class)
  public void executeSingleStep(UUID workflowId, String workflowName, String stepName) {
    WorkflowDefinition definition = registry.get(workflowName);
    WorkflowStep step = definition.stepMap().get(stepName);
    if (step == null) {
      throw new IllegalArgumentException("Step " + stepName + " not found in workflow " + workflowName);
    }

    List<WorkflowEvent> events = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(workflowId);
    
    boolean alreadyCompleted = false;
    boolean intentCreated = false;
    boolean intentCompleted = false;
    int startedAttempts = 0;
    
    for (WorkflowEvent event : events) {
      try {
        Map<String, Object> data = objectMapper.readValue(event.getData(), Map.class);
        String eventStepName = (String) data.get("stepName");
        if (stepName.equals(eventStepName)) {
          if (event.getType() == WorkflowEventType.STEP_COMPLETED) {
            alreadyCompleted = true;
          } else if (event.getType() == WorkflowEventType.STEP_STARTED) {
            startedAttempts++;
          } else if (event.getType() == WorkflowEventType.INTENT_CREATED) {
            intentCreated = true;
          } else if (event.getType() == WorkflowEventType.INTENT_COMPLETED) {
            intentCompleted = true;
          }
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to read event data during executeSingleStep", e);
      }
    }

    if (alreadyCompleted) {
      System.out.println(">>> [WORKER] Step " + stepName + " is already completed for " + workflowId + ". Skipping.");
      return;
    }

    if(step.hasChildWorkflow()) {
      executeChildWorkflow(workflowId, stepName, step);
      return;
    }

    Map<String, Object> context = loadExistingStateAndReplay(workflowId);
    if (step.hasDelay() && !context.containsKey("timerFired-" + stepName)) {
      boolean alreadyScheduled = false;
      for (WorkflowEvent event : events) {
          if (event.getType() == WorkflowEventType.TIMER_SCHEDULED) {
              try {
                  Map<String, Object> data = objectMapper.readValue(event.getData(), Map.class);
                  if (stepName.equals(data.get("stepName"))) {
                      alreadyScheduled = true;
                      break;
                  }
              } catch (Exception ignored) {}
          }
      }

      if (!alreadyScheduled) {
          scheduleTimer(workflowId, stepName, step.getDelay());
      }
      
      return;
    }
    
    context.put("workflowId", workflowId);
    context.put("stepName", stepName);
    context.put("idempotencyKey", workflowId.toString() + "-" + stepName);
    context.put("intentCreatedButNotCompleted", intentCreated && !intentCompleted);
    context.put("engine", this);

    int currentAttempt = startedAttempts + 1;
    Long maxSeq = eventRepository.findMaxSequenceIdByWorkflowId(workflowId);
    long startSeq = (maxSeq == null ? 0L : maxSeq) + 1L;
    appendEvent(workflowId, startSeq, WorkflowEventType.STEP_STARTED, Map.of(
        "stepName", stepName,
        "attempt", currentAttempt
    ));


    try {
      Object output = step.getLambda().execute(context);
      
      maxSeq = eventRepository.findMaxSequenceIdByWorkflowId(workflowId);
      long completeSeq = (maxSeq == null ? 0L : maxSeq) + 1L;
      appendEvent(workflowId, completeSeq, WorkflowEventType.STEP_COMPLETED, Map.of(
          "stepName", stepName,
          "output", output == null ? "" : output
      ));
      
      executeWorkflow(workflowId, workflowName);

    } catch (Exception e) {

      maxSeq = eventRepository.findMaxSequenceIdByWorkflowId(workflowId);
      long failSeq = (maxSeq == null ? 0L : maxSeq) + 1L;
      Map<String, Object> errorData = new HashMap<>();
      errorData.put("stepName", stepName);
      errorData.put("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
      appendEvent(workflowId, failSeq, WorkflowEventType.STEP_FAILED, errorData);

      if (currentAttempt < step.getMaxAttempts()) {
        System.out.println(">>> [WORKER] Step " + stepName + " failed. Attempt " + currentAttempt + "/" + step.getMaxAttempts() + ". Re-enqueueing for retry.");
        redisTaskProducer.enqueueTask(workflowId, workflowName, stepName, currentAttempt);
      } else {
        System.err.println(">>> [WORKER] Step " + stepName + " failed and exhausted all attempts. Marking workflow FAILED.");
        updateInstanceStatus(workflowId, "FAILED");
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
        } else if (event.getType() == WorkflowEventType.TIMER_FIRED) {
          String stepName = (String) data.get("stepName");
          context.put("timerFired-" + stepName, true);
        } else if (event.getType() == WorkflowEventType.CHILD_WORKFLOW_COMPLETED) {
          String stepName = (String) data.get("stepName");
          context.put(stepName, data.get("result"));
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

    if(status.equals("COMPLETED") || status.equals("FAILED")) {
      notifyParentIfExists(workflowId, status);
    }
  }
  
  private void notifyParentIfExists(UUID childWorkflowId, String status) {
    Optional<WorkflowParentChild> relation = parentChildRepository.findByChildWorkflowId(childWorkflowId);
    if(relation.isEmpty()) return;
    
    WorkflowParentChild relationship = relation.get();
    UUID parentWorkflowId = relationship.getParentWorkflowId();
    String parentStepName = relationship.getParentStepName();
    Object childResult = getWorkflowFinalResult(childWorkflowId);

    Long maxSeq = eventRepository.findMaxSequenceIdByWorkflowId(parentWorkflowId);
    long nextSeq = (maxSeq == null ? 0L : maxSeq) + 1L;
    appendEvent(parentWorkflowId, nextSeq, WorkflowEventType.CHILD_WORKFLOW_COMPLETED, Map.of("stepName", parentStepName, "childWorkflowId", childWorkflowId,
    "status", status, "result", childResult));

    WorkflowInstance instance = instanceRepository.findById(parentWorkflowId).orElseThrow();
    updateInstanceStatus(parentWorkflowId, "RUNNING");
    executeWorkflow(parentWorkflowId, instance.getName());
  }

  public Object getWorkflowFinalResult(UUID workflowId) {

    List<WorkflowEvent> events = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(workflowId);
    Map<String, Object> context = new HashMap<>();
    WorkflowDefinition definition = null;

    for (WorkflowEvent event : events) {
      try {
        Map<String, Object> data = objectMapper.readValue(event.getData(), Map.class);
        if(event.getType() == WorkflowEventType.WORKFLOW_STARTED) {
          context.putAll(data);
        }else if(event.getType() == WorkflowEventType.STEP_COMPLETED) {
          String stepName = (String) data.get("stepName");
          Object output = data.get("output");
          context.put(stepName, output);
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to parse event data for crash recovery", e);
      }
    }

    WorkflowInstance instance = instanceRepository.findById(workflowId).orElse(null);
    if(instance != null) {
      definition = registry.get(instance.getName());
      List<WorkflowStep> steps = definition.steps();
      if(!steps.isEmpty()) {
        WorkflowStep lastStep = steps.get(steps.size() - 1);
        return context.get(lastStep.getName());
      }
    }
    return Map.of();
  }

  @Transactional
  public void scheduleTimer(UUID workflowId, String stepName, Duration delay) {
      WorkflowTimer timer = new WorkflowTimer();
      timer.setWorkflowId(workflowId);
      timer.setStepName(stepName);
      timer.setFiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(delay));
      timer.setFired(false);
      workflowTimerRepository.save(timer);
      System.out.println(">>> [ENGINE] Timer saved to DB with ID: " + timer.getId() + ", fires_at: " + timer.getFiresAt());

      Long maxSeq = eventRepository.findMaxSequenceIdByWorkflowId(workflowId);
      long nextSeq = (maxSeq == null ? 0L : maxSeq) + 1L;
      appendEvent(workflowId, nextSeq, WorkflowEventType.TIMER_SCHEDULED, Map.of(
          "stepName", stepName,
          "firesAt", timer.getFiresAt().toString()
      ));

      updateInstanceStatus(workflowId, "PAUSED");
      System.out.println(">>> [ENGINE] Scheduled timer for workflow " + workflowId + " (Step: " + stepName + ") to fire at " + timer.getFiresAt());
  }

  @Transactional(noRollbackFor = RuntimeException.class)
  public void resumeFromTimer(UUID workflowId, String stepName) {
      Long maxSeq = eventRepository.findMaxSequenceIdByWorkflowId(workflowId);
      long nextSeq = (maxSeq == null ? 0L : maxSeq) + 1L;
      appendEvent(workflowId, nextSeq, WorkflowEventType.TIMER_FIRED, Map.of("stepName", stepName));

      updateInstanceStatus(workflowId, "RUNNING");
      System.out.println(">>> [ENGINE] Timer fired! Resuming workflow " + workflowId + " at step: " + stepName);

      WorkflowInstance instance = instanceRepository.findById(workflowId)
          .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + workflowId));
      executeWorkflow(workflowId, instance.getName());
  }

  public void writeIntent(UUID workflowId, String stepName, Object intent) {
      Long maxSeq = eventRepository.findMaxSequenceIdByWorkflowId(workflowId);
      long nextSeq = (maxSeq == null ? 0L : maxSeq) + 1L;
      appendEvent(workflowId, nextSeq, WorkflowEventType.INTENT_CREATED, Map.of("stepName", stepName, "intent", intent));
  }

  public void writeIntentDone(UUID workflowId, String stepName, Object intent) {
      Long maxSeq = eventRepository.findMaxSequenceIdByWorkflowId(workflowId);
      long nextSeq = (maxSeq == null ? 0L : maxSeq) + 1L;
      appendEvent(workflowId, nextSeq, WorkflowEventType.INTENT_COMPLETED, Map.of("stepName", stepName, "intent", intent));
  }
  @Transactional(noRollbackFor = RuntimeException.class)
  public void executeChildWorkflow(UUID workflowId, String stepName, WorkflowStep step) {
    List<WorkflowEvent> events = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(workflowId);

    for(WorkflowEvent event : events) {
      if(event.getType() != WorkflowEventType.CHILD_WORKFLOW_STARTED) continue;
      try {
        Map<String, Object> data = objectMapper.readValue(event.getData(), Map.class);
        if(stepName.equals(data.get("stepName"))) return;
      } catch (Exception e) {
        throw new RuntimeException("Failed to read event data during executeChildWorkflow", e);
      }
    }

    Map<String, Object> parentContext = loadExistingStateAndReplay(workflowId);
    Map<String, Object> childInput = step.getInputMapper().apply(parentContext);
    
    UUID childworkflowId = start(step.getChildWorkflowName(), childInput);

    WorkflowParentChild relation = new WorkflowParentChild();
    relation.setChildWorkflowId(childworkflowId);
    relation.setParentWorkflowId(workflowId);
    relation.setParentStepName(stepName);
    parentChildRepository.save(relation);

    Long maxSeq = eventRepository.findMaxSequenceIdByWorkflowId(workflowId);
    long nextSeq = (maxSeq == null ? 0L : maxSeq) + 1L;
    appendEvent(workflowId, nextSeq, WorkflowEventType.CHILD_WORKFLOW_STARTED, Map.of("stepName", stepName, "childWorkflowId", childworkflowId,
    "childWorkflowName", step.getChildWorkflowName()));
    updateInstanceStatus(workflowId, "PAUSED");
  }
}
