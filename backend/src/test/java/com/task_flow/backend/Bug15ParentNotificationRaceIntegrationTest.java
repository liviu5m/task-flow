package com.task_flow.backend;

import com.task_flow.backend.engine.TaskFlowEngine;
import com.task_flow.backend.enums.WorkflowEventType;
import com.task_flow.backend.enums.WorkflowInstanceStatus;
import com.task_flow.backend.model.WorkflowEvent;
import com.task_flow.backend.model.WorkflowInstance;
import com.task_flow.backend.model.WorkflowParentChild;
import com.task_flow.backend.repository.WorkflowEventRepository;
import com.task_flow.backend.repository.WorkflowInstanceRepository;
import com.task_flow.backend.repository.WorkflowParentChildRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Bug15ParentNotificationRaceIntegrationTest {

    @Autowired
    private TaskFlowEngine engine;

    @Autowired
    private WorkflowInstanceRepository instanceRepository;

    @Autowired
    private WorkflowParentChildRepository parentChildRepository;

    @Autowired
    private WorkflowEventRepository eventRepository;

    @Test
    void notifyParentIfExists_shouldNotDuplicateExecuteWorkflowCalls() throws Exception {
        UUID parentWorkflowId = UUID.randomUUID();
        UUID childWorkflowId1 = UUID.randomUUID();
        UUID childWorkflowId2 = UUID.randomUUID();

        WorkflowInstance parentInstance = new WorkflowInstance();
        parentInstance.setId(parentWorkflowId);
        parentInstance.setName("parent-workflow");
        parentInstance.setStatus(WorkflowInstanceStatus.PAUSED);
        instanceRepository.save(parentInstance);

        WorkflowParentChild relation1 = new WorkflowParentChild();
        relation1.setChildWorkflowId(childWorkflowId1);
        relation1.setParentWorkflowId(parentWorkflowId);
        relation1.setParentStepName("childStep");
        parentChildRepository.save(relation1);

        WorkflowParentChild relation2 = new WorkflowParentChild();
        relation2.setChildWorkflowId(childWorkflowId2);
        relation2.setParentWorkflowId(parentWorkflowId);
        relation2.setParentStepName("childStep");
        parentChildRepository.save(relation2);

        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger executeWorkflowCallCount = new AtomicInteger(0);

        executor.submit(() -> {
            engine.notifyParentIfExists(childWorkflowId1, "COMPLETED");
            latch.countDown();
        });

        executor.submit(() -> {
            engine.notifyParentIfExists(childWorkflowId2, "COMPLETED");
            latch.countDown();
        });

        latch.await();
        executor.shutdown();

        List<WorkflowEvent> parentEvents = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(parentWorkflowId);
        long childCompletedCount = parentEvents.stream()
            .filter(e -> e.getType() == WorkflowEventType.CHILD_WORKFLOW_COMPLETED)
            .count();

        WorkflowInstance updatedParent = instanceRepository.findById(parentWorkflowId).orElseThrow();

        assertEquals(2, childCompletedCount, "Both child completions should be recorded");
        assertEquals(WorkflowInstanceStatus.RUNNING, updatedParent.getStatus(), "Parent should be RUNNING");
    }
}
