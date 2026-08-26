package com.task_flow.backend;
import com.task_flow.backend.engine.TaskFlowEngine;
import com.task_flow.backend.enums.WorkflowEventType;
import com.task_flow.backend.model.WorkflowEvent;
import com.task_flow.backend.model.WorkflowInstance;
import com.task_flow.backend.model.WorkflowEventId;
import com.task_flow.backend.repository.WorkflowEventRepository;
import com.task_flow.backend.repository.WorkflowInstanceRepository;
import com.task_flow.backend.enums.WorkflowInstanceStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ConcurrentAppendEventTest {

    @Autowired
    private TaskFlowEngine engine;

    @Autowired
    private WorkflowEventRepository eventRepository;

    @Autowired
    private WorkflowInstanceRepository instanceRepository;

    @Test
    void concurrentAppends_produceUniqueSequenceIds() throws Exception {
        UUID workflowId = UUID.randomUUID();
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(workflowId);
        instance.setName("race-test");
        instance.setStatus(WorkflowInstanceStatus.RUNNING);
        instanceRepository.save(instance);

        int threadCount = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new java.util.ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    engine.appendEventAtomic(
                        workflowId,
                        WorkflowEventType.STEP_COMPLETED,
                        java.util.Map.of("stepName", "race-step", "thread", Thread.currentThread().getId())
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }

        start.countDown();
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(threadCount, successCount.get(), "all threads should succeed");

        List<WorkflowEvent> events = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(workflowId);
        long distinctSequences = events.stream()
            .map(WorkflowEvent::getSequenceId)
            .distinct()
            .count();

        assertEquals(threadCount, events.size(), "all events persisted");
        assertEquals(threadCount, distinctSequences, "all sequence IDs are unique");
    }
}
