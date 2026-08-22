package com.task_flow.backend.engine;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
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
    private final int maxAttempts;
    private final List<String> dependencies;
    
    public WorkflowStep(String name, WorkflowLambda lambda, int maxAttempts) {
        this.name = name;
        this.lambda = lambda;
        this.maxAttempts = maxAttempts;
        this.dependencies = new ArrayList<>();
    }

    public WorkflowStep(String name, WorkflowLambda lambda, int maxAttempts, List<String> dependencies) {
        this.name = name;
        this.lambda = lambda;
        this.maxAttempts = maxAttempts;
        this.dependencies = dependencies;
    }

    public String getName() { return name; }
    public WorkflowLambda getLambda() { return lambda; }
    public int getMaxAttempts() { return maxAttempts; }
    public List<String> getDependencies() { return dependencies; }
}

@FunctionalInterface
interface WorkflowLambda {
    Object execute(Map<String, Object> context) throws Exception;
}

class WorkflowDefinition {
    private final String name;
    private final List<WorkflowStep> steps;
    private final DirectedAcyclicGraph<String, DefaultEdge> dag;
    private final Map<String, WorkflowStep> stepMap;
    
    public WorkflowDefinition(String name, List<WorkflowStep> steps, DirectedAcyclicGraph<String, DefaultEdge> dag, Map<String, WorkflowStep> stepMap) {
        this.name = name;
        this.steps = steps;
        this.dag = dag;
        this.stepMap = stepMap;
    }

    public String getName() { return name; }
    public List<WorkflowStep> steps() { return steps; }
    public DirectedAcyclicGraph<String, DefaultEdge> dag() { return dag; }
    public Map<String, WorkflowStep> stepMap() { return stepMap; }
}

class WorkflowBuilder {
    private final String name;
    private final List<WorkflowStep> steps = new ArrayList<>();

    public WorkflowBuilder(String name) {
        this.name = name;
    }

    public WorkflowBuilder step(String stepName, WorkflowLambda lambda, int maxAttempts, List<String> dependencies) {
        steps.add(new WorkflowStep(stepName, lambda, maxAttempts, dependencies));
        return this;
    }

    public WorkflowBuilder step(String stepName, WorkflowLambda lambda, int maxAttempts) {
        steps.add(new WorkflowStep(stepName, lambda, maxAttempts));
        return this;
    }

    public WorkflowBuilder step(String stepName, WorkflowLambda lambda) {
        steps.add(new WorkflowStep(stepName, lambda, 3));
        return this;
    }

    public WorkflowDefinition build() {
        DirectedAcyclicGraph<String, DefaultEdge> g =
            new DirectedAcyclicGraph<>(DefaultEdge.class);
        Map<String, WorkflowStep> stepMap = new HashMap<>();
        for (WorkflowStep step : steps) {
            g.addVertex(step.getName());
            stepMap.put(step.getName(), step);
        }

        for (WorkflowStep step : steps) {
            for (String parent : step.getDependencies()) {
                g.addEdge(parent, step.getName());
            }
        }
        return new WorkflowDefinition(name, steps, g, stepMap);
    }
}
