package com.task_flow.backend.engine;
import com.task_flow.backend.engine.TaskFlowEngine;
import com.task_flow.backend.model.WorkflowEvent;
import com.task_flow.backend.model.WorkflowInstance;
import com.task_flow.backend.repository.WorkflowEventRepository;
import com.task_flow.backend.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkflowRecoveryScheduler {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowEventRepository eventRepository;
    private final TaskFlowEngine taskFlowEngine;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void recoverStuckWorkflows() {
        List<WorkflowInstance> runningInstances = instanceRepository.findByStatus("RUNNING");
        
        for (WorkflowInstance instance : runningInstances) {
            WorkflowEvent lastEvent = eventRepository.findTopByWorkflowIdOrderBySequenceIdDesc(instance.getId());
            if (lastEvent != null && "STEP_FAILED".equals(lastEvent.getEventType())) {
                taskFlowEngine.executeWorkflow(instance.getId());
            }
        }
    }
}
