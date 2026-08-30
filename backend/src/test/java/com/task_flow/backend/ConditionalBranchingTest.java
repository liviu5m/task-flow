package com.task_flow.backend;

import com.task_flow.backend.engine.TaskFlowEngine;
import com.task_flow.backend.engine.WorkflowRegistry;
import com.task_flow.backend.enums.WorkflowEventType;
import com.task_flow.backend.enums.WorkflowInstanceStatus;
import com.task_flow.backend.model.WorkflowEvent;
import com.task_flow.backend.model.WorkflowInstance;
import com.task_flow.backend.repository.WorkflowEventRepository;
import com.task_flow.backend.repository.WorkflowInstanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConditionalBranchingTest {

    @Autowired
    private WorkflowRegistry registry;

    @Autowired
    private TaskFlowEngine engine;

    @Autowired
    private WorkflowInstanceRepository instanceRepository;

    @Autowired
    private WorkflowEventRepository eventRepository;

    @Test
    void testSimpleConditionalStep_SkipsWhenConditionFalse() {
        // Define workflow with a conditional step
        registry.register("test-conditional", builder -> {
            builder.step("validate", (ctx, input) -> false, 3);  // Always returns false
            builder.step("charge", (ctx, input) -> "charged", 3, List.of("validate"),
                ctx -> Boolean.TRUE.equals(ctx.get("validate")));  // Condition: validate == true
        });

        UUID workflowId = engine.start("test-conditional", Map.of());

        // Wait for workflow to complete
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            WorkflowInstance instance = instanceRepository.findById(workflowId).orElse(null);
            return instance != null && instance.getStatus() == WorkflowInstanceStatus.COMPLETED;
        });

        // Verify STEP_SKIPPED event for "charge"
        List<WorkflowEvent> events = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(workflowId);
        assertTrue(events.stream().anyMatch(e -> e.getType() == WorkflowEventType.STEP_SKIPPED));
        assertTrue(events.stream().anyMatch(e -> {
            try {
                Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(e.getData(), Map.class);
                return "charge".equals(data.get("stepName")) && e.getType() == WorkflowEventType.STEP_SKIPPED;
            } catch (Exception ex) {
                return false;
            }
        }));
    }

    @Test
    void testIfElseBranch_ExecutesCorrectPath() {
        registry.register("test-ifelse", builder -> {
            builder.step("validate", (ctx, input) -> true, 3);  // Returns true
            builder.ifElse(
                "handle",
                ctx -> Boolean.TRUE.equals(ctx.get("validate")),
                (ctx, input) -> "charged",
                (ctx, input) -> "notified",
                3,
                List.of("validate")
            );
        });

        UUID workflowId = engine.start("test-ifelse", Map.of());

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            WorkflowInstance instance = instanceRepository.findById(workflowId).orElse(null);
            return instance != null && instance.getStatus() == WorkflowInstanceStatus.COMPLETED;
        });

        List<WorkflowEvent> events = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(workflowId);
        // Verify "handle-then-step" completed and "handle-else-step" was skipped
        assertTrue(events.stream().anyMatch(e -> {
            try {
                Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(e.getData(), Map.class);
                return "handle-then-step".equals(data.get("stepName")) && e.getType() == WorkflowEventType.STEP_COMPLETED;
            } catch (Exception ex) {
                return false;
            }
        }));
        assertTrue(events.stream().anyMatch(e -> e.getType() == WorkflowEventType.STEP_SKIPPED));
    }

    @Test
    void testSwitchCaseBranch_ExecutesCorrectCase() {
        registry.register("test-switch", builder -> {
            builder.step("get-status", (ctx, input) -> "valid", 3);
            builder.switchCase(
                "handle-status",
                ctx -> ctx.get("get-status"),
                sw -> sw.when("valid", (ctx, input) -> "processed")
                      .when("invalid", (ctx, input) -> "rejected")
                      .default_((ctx, input) -> "unknown"),
                3,
                List.of("get-status")
            );
        });

        UUID workflowId = engine.start("test-switch", Map.of());

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            WorkflowInstance instance = instanceRepository.findById(workflowId).orElse(null);
            return instance != null && instance.getStatus() == WorkflowInstanceStatus.COMPLETED;
        });

        List<WorkflowEvent> events = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(workflowId);
        // Verify "handle-status-case-valid-step" completed
        assertTrue(events.stream().anyMatch(e -> {
            try {
                Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(e.getData(), Map.class);
                return "handle-status-case-valid-step".equals(data.get("stepName")) && e.getType() == WorkflowEventType.STEP_COMPLETED;
            } catch (Exception ex) {
                return false;
            }
        }));
        // Verify other cases were skipped
        assertEquals(1, events.stream().filter(e -> e.getType() == WorkflowEventType.STEP_COMPLETED).count());
    }

    @Test
    void testBranchWithMultipleSteps() {
        registry.register("test-branch-multi", builder -> {
            builder.step("validate", (ctx, input) -> true, 3);
            builder.branch("handle")
                .if_("valid", ctx -> Boolean.TRUE.equals(ctx.get("validate")))
                    .then("charge", (ctx, input) -> "charged")
                    .then("ship", (ctx, input) -> "shipped")
                .else_()
                    .then("notify", (ctx, input) -> "notified")
                .endBranch();
        });

        UUID workflowId = engine.start("test-branch-multi", Map.of());

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            WorkflowInstance instance = instanceRepository.findById(workflowId).orElse(null);
            return instance != null && instance.getStatus() == WorkflowInstanceStatus.COMPLETED;
        });

        List<WorkflowEvent> events = eventRepository.findByWorkflowIdOrderBySequenceIdAsc(workflowId);
        // Verify both "charge" and "ship" completed
        assertTrue(events.stream().anyMatch(e -> {
            try {
                Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(e.getData(), Map.class);
                return "handle-valid-charge".equals(data.get("stepName")) && e.getType() == WorkflowEventType.STEP_COMPLETED;
            } catch (Exception ex) {
                return false;
            }
        }));
        assertTrue(events.stream().anyMatch(e -> {
            try {
                Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(e.getData(), Map.class);
                return "handle-valid-ship".equals(data.get("stepName")) && e.getType() == WorkflowEventType.STEP_COMPLETED;
            } catch (Exception ex) {
                return false;
            }
        }));
        // Verify "notify" was skipped
        assertTrue(events.stream().anyMatch(e -> {
            try {
                Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(e.getData(), Map.class);
                return "handle-else-notify".equals(data.get("stepName")) && e.getType() == WorkflowEventType.STEP_SKIPPED;
            } catch (Exception ex) {
                return false;
            }
        }));
    }
}
