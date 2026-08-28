package com.task_flow.backend.service;

import com.task_flow.backend.engine.TaskFlowEngine;
import com.task_flow.backend.model.WorkflowTimer;
import com.task_flow.backend.repository.WorkflowTimerRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimerPollerService {

    private final WorkflowTimerRepository timerRepository;
    private final TaskFlowEngine taskFlowEngine;


    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void pollTimers() {
        List<WorkflowTimer> dueTimers = timerRepository.findDueTimersForUpdate();
        for (WorkflowTimer timer : dueTimers) {

            try {
                taskFlowEngine.resumeFromTimer(timer.getWorkflowId(), timer.getStepName());
                timer.setFired(true);
                timerRepository.save(timer);
            } catch (Exception e) {
                System.err.println(">>> [POLLER] Failed to resume workflow from timer: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void sweep() {
        taskFlowEngine.recoverStuckWorkflows();
    }
}
