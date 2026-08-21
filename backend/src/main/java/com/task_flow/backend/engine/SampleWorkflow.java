package com.task_flow.backend.engine;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class SampleWorkflow {

    private final WorkflowRegistry registry;
  
    public SampleWorkflow(WorkflowRegistry registry) {  
        this.registry = registry; 
    }

    @PostConstruct 
    public void init() {
        registry.register("sample-workflow", builder -> {
            builder.step("step-1", step1 -> {
                System.out.println("Step 1 executed");
                return "Step 1 output";
            });
            builder.step("step-2", step2 -> {
                System.out.println("Step 2 executed");
                return "Step 2 output";
            });
            builder.step("step-3", step3 -> {
                System.out.println("Step 3 executed");
                return "Step 3 output";
            });
        });
    }
}

