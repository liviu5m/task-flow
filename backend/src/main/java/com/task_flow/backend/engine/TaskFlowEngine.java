package com.task_flow.backend.engine;
import com.task_flow.backend.dto.StepContext;
import com.task_flow.backend.enums.WorkflowEventType;
import com.task_flow.backend.enums.WorkflowInstanceStatus;
import com.task_flow.backend.exception.RetryableStepException;
import com.task_flow.backend.model.WorkflowEvent;
import com.task_flow.backend.model.WorkflowInstance;
import com.task_flow.backend.model.WorkflowParentChild;
import com.task_flow.backend.model.WorkflowSequence;
import com.task_flow.backend.model.WorkflowTimer;
import com.task_flow.backend.repository.WorkflowEventRepository;
import com.task_flow.backend.repository.WorkflowInstanceRepository;
import com.task_flow.backend.repository.WorkflowParentChildRepository;
import com.task_flow.backend.repository.WorkflowSequenceRepository;
import com.task_flow.backend.repository.WorkflowTimerRepository;
import com.task_flow.backend.service.RedisTaskProducer;

import ch.qos.logback.core.joran.sanity.Pair;
import tools.jackson.databind.ObjectMapper;

import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private final WorkflowSequenceRepository sequenceRepository;
  private final LeaderElector leaderElector;

  public TaskFlowEngine(WorkflowRegistry registry,
    WorkflowInstanceRepository instanceRepository,
    WorkflowEventRepository eventRepository,
    ObjectMapper objectMapper,
    RedisTaskProducer redisTaskProducer,
    WorkflowTimerRepository timerRepository,
    WorkflowParentChildRepository parentChildRepository,
    WorkflowSequenceRepository sequenceRepository,
    LeaderElector leaderElector
    ) {  
    this.registry = registry; 
    this.instanceRepository = instanceRepository; 
    this.eventRepository = eventRepository; 
    this.objectMapper = objectMapper; 
    this.redisTaskProducer = redisTaskProducer;
    this.workflowTimerRepository = timerRepository;
    this.parentChildRepository = parentChildRepository;
    this.sequenceRepository = sequenceRepository;
    this.leaderElector = leaderElector;
  }

  @Transactional(noRollbackFor = RetryableStepException.class)
  public UUID start(String workflowName, Map<String, Object> initialInput) {
    UUID workflowId = UUID.randomUUID();

    WorkflowInstance instance = new WorkflowInstance();
    instance.setId(workflowId);
    instance.setName(workflowName);
    instance.setStatus(WorkflowInstanceStatus.valueOf("RUNNING"));
    instanceRepository.save(instance);
  
    appendEventAtomic(workflowId, WorkflowEventType.WORKFLOW_STARTED, initialInput);

    executeWorkflow(workflowId, workflowName);

    return workflowId;
  }

  public void executeWorkflow(UUID workflowId, String workflowName) {
    if(!leaderElector.isLeader()) {
      System.out.println(">>> [ENGINE] Not a leader. Skipping dispatch for workflow: " + workflowId);
      return;
    }
    WorkflowDefinition definition = registry.get(workflowName);
    Map.Entry<Map<String, Object>, Set<String>> result = loadExistingStateAndReplay(workflowId);

    Map<String, Object> context = result.getKey();
    Set<String> startedSteps = result.getValue();
    TopologicalOrderIterator<String, DefaultEdge> iterator =
        new TopologicalOrderIterator<>(definition.dag());

    List<String> orderedSteps = new ArrayList<>();
    while (iterator.hasNext()) {
        orderedSteps.add(iterator.next());
    }
    orderedSteps.sort(Comparator.naturalOrder());
    boolean allStepsCompleted = true;
    for(String stepName : orderedSteps) {
        WorkflowStep step = definition.stepMap().get(stepName);

        if (context.containsKey(stepName) || startedSteps.contains(stepName)) {
            continue;
        }
        allStepsCompleted = false;

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

        redisTaskProducer.enqueueTask(workflowId, workflowName, stepName, 1);
    }

    if (allStepsCompleted) {
        WorkflowInstance instance = instanceRepository.findById(workflowId).orElse(null);
        if (instance != null && instance.getStatus() == WorkflowInstanceStatus.RUNNING) {
            appendEventAtomic(workflowId, WorkflowEventType.WORKFLOW_COMPLETED, Map.of());
            updateInstanceStatus(workflowId, "COMPLETED");
        }
    }
  }

  @Transactional(noRollbackFor = RetryableStepException.class)
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
        if(e instanceof RetryableStepException) {
          throw (RetryableStepException) e;
        }
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

    Map<String, Object> context = loadExistingStateAndReplay(workflowId).getKey();
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
    StepContext stepContext = new StepContext(stepName, workflowId.toString() + "-" + stepName, intentCreated && !intentCompleted);
    int currentAttempt = startedAttempts + 1;
    appendEventAtomic(workflowId, WorkflowEventType.STEP_STARTED, Map.of(
        "stepName", stepName,
        "attempt", currentAttempt
    ));

    try {
      Object output = step.getLambda().execute(stepContext, context);
      
      appendEventAtomic(workflowId, WorkflowEventType.STEP_COMPLETED, Map.of(
          "stepName", stepName,
          "output", output == null ? "" : output
      ));
      
      executeWorkflow(workflowId, workflowName);

    } catch (Exception e) {

      Map<String, Object> errorData = new HashMap<>();
      errorData.put("stepName", stepName);
      errorData.put("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
      appendEventAtomic(workflowId, WorkflowEventType.STEP_FAILED, errorData);
      if (currentAttempt < step.getMaxAttempts()) {
        System.out.println(">>> [WORKER] Step " + stepName + " failed. Attempt " + currentAttempt + "/" + step.getMaxAttempts() + ". Re-enqueueing for retry.");
        redisTaskProducer.enqueueTask(workflowId, workflowName, stepName, currentAttempt);
      } else {
        System.err.println(">>> [WORKER] Step " + stepName + " failed and exhausted all attempts. Marking workflow FAILED.");
        updateInstanceStatus(workflowId, "FAILED");
      }
    }
  }


  private Map.Entry<Map<String, Object>, Set<String>> loadExistingStateAndReplay(UUID workflowId) {
    Map<String, Object> context = new HashMap<>();
    List<WorkflowEvent> events = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(workflowId);
    Set<String> startedSteps = new HashSet<>();
    Set<String> failedSteps = new HashSet<>();
    Set<String> completedSteps = new HashSet<>();
    for (WorkflowEvent event : events) {
      try {
        Map<String, Object> data = objectMapper.readValue(event.getData(), Map.class);

        if (event.getType() == WorkflowEventType.WORKFLOW_STARTED) {
          context.putAll(data);
        } else if (event.getType() == WorkflowEventType.STEP_COMPLETED) {
          String stepName = (String) data.get("stepName");
          Object output = data.get("output");
          context.put(stepName, output);
          completedSteps.add(stepName);
        } else if(event.getType() == WorkflowEventType.STEP_FAILED) {
          String stepName = (String) data.get("stepName");
          failedSteps.add(stepName);
        }else if(event.getType() == WorkflowEventType.STEP_STARTED) {
          String stepName = (String) data.get("stepName");
          startedSteps.add(stepName);
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
    return new AbstractMap.SimpleEntry<>(context, startedSteps);
  }

  @Transactional
  public void appendEventAtomic(UUID workflowId, WorkflowEventType eventType, Object dataPayload) {
    try {
      String jsonPayload = objectMapper.writeValueAsString(dataPayload);
      sequenceRepository.insertIfAbsent(workflowId);
      WorkflowSequence seq = sequenceRepository.findByWorkflowIdForUpdate(workflowId);
      if(seq == null) {
        throw new IllegalStateException("Workflow sequence not found for workflow " + workflowId);
      }
      seq.setCurrentSeq(seq.getCurrentSeq() + 1);
      sequenceRepository.save(seq);
      Long seqId = seq.getCurrentSeq();
      
      eventRepository.insertAtomicEvent(workflowId,seqId, eventType.name(), jsonPayload);
    } catch (Exception e) {
      throw new RuntimeException("Failed to atomically append the event", e);
    }
  }

  public void updateInstanceStatus(UUID workflowId, String status) {
    WorkflowInstance instance = instanceRepository.findById(workflowId)
    .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + workflowId));
    instance.setStatus(WorkflowInstanceStatus.valueOf(status));
    instanceRepository.save(instance);

    if(status.equals("COMPLETED") || status.equals("FAILED")) {
      notifyParentIfExists(workflowId, status);
    }
  }
  @Transactional(isolation = Isolation.SERIALIZABLE) 
  public void notifyParentIfExists(UUID childWorkflowId, String status) {
    Optional<WorkflowParentChild> relation = parentChildRepository.findByChildWorkflowIdForUpdate(childWorkflowId);
    if(relation.isEmpty()) return;
    
    WorkflowParentChild relationship = relation.get();
    UUID parentWorkflowId = relationship.getParentWorkflowId();
    String parentStepName = relationship.getParentStepName();
    Object childResult = getWorkflowFinalResult(childWorkflowId);

    appendEventAtomic(parentWorkflowId, WorkflowEventType.CHILD_WORKFLOW_COMPLETED, Map.of("stepName", parentStepName, "childWorkflowId", childWorkflowId,
    "status", status, "result", childResult));

    WorkflowInstance instance = instanceRepository.findById(parentWorkflowId).orElseThrow();
    if(instance.getStatus() == WorkflowInstanceStatus.PAUSED) {
      updateInstanceStatus(parentWorkflowId, "RUNNING");
      executeWorkflow(parentWorkflowId, instance.getName());
    }
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

      appendEventAtomic(workflowId, WorkflowEventType.TIMER_SCHEDULED, Map.of(
          "stepName", stepName,
          "firesAt", timer.getFiresAt().toString()
      ));

      updateInstanceStatus(workflowId, "PAUSED");
      System.out.println(">>> [ENGINE] Scheduled timer for workflow " + workflowId + " (Step: " + stepName + ") to fire at " + timer.getFiresAt());
  }

  @Transactional(noRollbackFor = RetryableStepException.class)
  public void resumeFromTimer(UUID workflowId, String stepName) {
      appendEventAtomic(workflowId, WorkflowEventType.TIMER_FIRED, Map.of("stepName", stepName));
      updateInstanceStatus(workflowId, "RUNNING");
      System.out.println(">>> [ENGINE] Timer fired! Resuming workflow " + workflowId + " at step: " + stepName);

      WorkflowInstance instance = instanceRepository.findById(workflowId)
          .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + workflowId));
      executeWorkflow(workflowId, instance.getName());
  }

  public void writeIntent(UUID workflowId, String stepName, Object intent) {
      appendEventAtomic(workflowId, WorkflowEventType.INTENT_CREATED, Map.of("stepName", stepName, "intent", intent));
  }

  public void writeIntentDone(UUID workflowId, String stepName, Object intent) {
      appendEventAtomic(workflowId, WorkflowEventType.INTENT_COMPLETED, Map.of("stepName", stepName, "intent", intent));
  }
  @Transactional(noRollbackFor = RetryableStepException.class)
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

    Map<String, Object> parentContext = loadExistingStateAndReplay(workflowId).getKey();
    Map<String, Object> childInput = step.getInputMapper().apply(parentContext);
    
    UUID childworkflowId = start(step.getChildWorkflowName(), childInput);

    WorkflowParentChild relation = new WorkflowParentChild();
    relation.setChildWorkflowId(childworkflowId);
    relation.setParentWorkflowId(workflowId);
    relation.setParentStepName(stepName);
    parentChildRepository.save(relation);

    appendEventAtomic(workflowId, WorkflowEventType.CHILD_WORKFLOW_STARTED, Map.of("stepName", stepName, "childWorkflowId", childworkflowId,
    "childWorkflowName", step.getChildWorkflowName()));
    updateInstanceStatus(workflowId, "PAUSED");
  }
  private int countStartedAttempts(UUID workflowId, String stepName) {
      int count = 0;
      List<WorkflowEvent> events = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(workflowId);
      for (WorkflowEvent e : events) {
          try {
              Map<String, Object> data = objectMapper.readValue(e.getData(), Map.class);
              if (stepName.equals(data.get("stepName")) 
                  && e.getType() == WorkflowEventType.STEP_STARTED) {
                  count++;
              }
          } catch (Exception ignored) {}
      }
      return count;
  }
  @Transactional
  public void recoverStuckWorkflows() {
    List<WorkflowInstance> running = instanceRepository.findByStatus(WorkflowInstanceStatus.RUNNING);
    for (WorkflowInstance instance : running) {
        WorkflowEvent last = eventRepository.findTopByWorkflowIdOrderBySequenceIdDesc(instance.getId());
        if (last == null || last.getType() != WorkflowEventType.STEP_FAILED) continue;
        
        try {
            Map<String, Object> data = objectMapper.readValue(last.getData(), Map.class);
            String stepName = (String) data.get("stepName");
            WorkflowDefinition def = registry.get(instance.getName());
            WorkflowStep step = def.stepMap().get(stepName);
            if (step == null) continue;
            
            int startedAttempts = countStartedAttempts(instance.getId(), stepName);
            int currentAttempt = startedAttempts + 1;
            
            if (currentAttempt < step.getMaxAttempts()) {
                System.out.println(">>> [RECOVERY] Re-enqueueing stuck step " + stepName 
                    + " for workflow " + instance.getId());
                redisTaskProducer.enqueueTask(instance.getId(), instance.getName(), stepName, currentAttempt);
            }
        } catch (Exception e) {
            System.err.println(">>> [RECOVERY] Failed to recover workflow " + instance.getId() + ": " + e.getMessage());
        }
    }
}
}
