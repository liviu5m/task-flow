package com.task_flow.backend.engine;

import com.task_flow.backend.enums.WorkflowInstanceStatus;
import com.task_flow.backend.model.WorkflowInstance;
import com.task_flow.backend.repository.WorkflowInstanceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LeaderElectionScheduler {
    private final TaskFlowEngine taskFlowEngine;
    private final LeaderElector leaderElector;
    private final WorkflowInstanceRepository instanceRepository;

    public LeaderElectionScheduler(
        TaskFlowEngine taskFlowEngine,
        LeaderElector leaderElector,
        WorkflowInstanceRepository instanceRepository
    ) {
        this.taskFlowEngine = taskFlowEngine;
        this.leaderElector = leaderElector;
        this.instanceRepository = instanceRepository;
    }

    @Scheduled(fixedRate = 1000) 
    public void dispatchReadySteps() {
        if (!leaderElector.isLeader()) {
            return; // Only leader dispatches
        }
        System.out.println(">>> [SCHEDULER] Dispatching ready steps (leader epoch: " + leaderElector.getCurrentEpoch() + ")");
        List<WorkflowInstance> runningInstances = instanceRepository.findByStatus(WorkflowInstanceStatus.RUNNING);
        for (WorkflowInstance instance : runningInstances) {
            taskFlowEngine.executeWorkflow(instance.getId(), instance.getName());
        }
    }
}
