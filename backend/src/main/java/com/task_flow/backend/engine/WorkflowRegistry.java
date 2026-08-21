package com.task_flow.backend.engine;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class WorkflowRegistry {
    
    private final Map<String, WorkflowDefinition> registry = new HashMap<>();

    public void register(String name, Consumer<WorkflowBuilder> configurer) {
        WorkflowBuilder builder = new WorkflowBuilder(name);
        configurer.accept(builder);
        registry.put(name, builder.build());
    }

    public WorkflowDefinition get(String name) {
        WorkflowDefinition def = registry.get(name);
        if (def == null) {
            throw new IllegalArgumentException("Workflow not found in registry: " + name);
        }
        return def;
    }
}

class WorkflowStep {
    private final String name;
    private final WorkflowLambda lambda;

    public WorkflowStep(String name, WorkflowLambda lambda) {
        this.name = name;
        this.lambda = lambda;
    }

    public String getName() { return name; }
    public WorkflowLambda getLambda() { return lambda; }
}

@FunctionalInterface
interface WorkflowLambda {
    Object execute(Map<String, Object> context) throws Exception;
}

class WorkflowDefinition {
    private final String name;
    private final List<WorkflowStep> steps;

    public WorkflowDefinition(String name, List<WorkflowStep> steps) {
        this.name = name;
        this.steps = steps;
    }

    public String getName() { return name; }
    public List<WorkflowStep> steps() { return steps; }
}

class WorkflowBuilder {
    private final String name;
    private final List<WorkflowStep> steps = new ArrayList<>();

    public WorkflowBuilder(String name) {
        this.name = name;
    }

    public WorkflowBuilder step(String stepName, WorkflowLambda lambda) {
        steps.add(new WorkflowStep(stepName, lambda));
        return this;
    }

    public WorkflowDefinition build() {
        return new WorkflowDefinition(name, steps);
    }
}
