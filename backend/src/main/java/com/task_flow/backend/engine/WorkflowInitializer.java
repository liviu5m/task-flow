package com.task_flow.backend.engine;

import com.task_flow.backend.engine.WorkflowBuilder;
import com.task_flow.backend.engine.WorkflowRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowInitializer implements CommandLineRunner {

    private final WorkflowRegistry registry;

    public WorkflowInitializer(WorkflowRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run(String... args) {
        registry.register("demo-workflow", (WorkflowBuilder builder) -> {
            builder.step("step1", (ctx, input) -> {
                System.out.println("Executing step1");
                return Map.of("result1", "success");
            })
            .step("step2", (ctx, input) -> {
                System.out.println("Executing step2");
                return "completed";
            }, 3, List.of("step1"));
        });
    }
}
