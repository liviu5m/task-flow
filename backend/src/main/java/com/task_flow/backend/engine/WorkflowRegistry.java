package com.task_flow.backend.engine;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.stereotype.Component;

import com.task_flow.backend.dto.StepContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
public class WorkflowRegistry {
    
    private final Map<String, WorkflowDefinition> registry = new HashMap<>();

    public WorkflowRegistry() {
      register("test-workflow", builder -> {
            builder.step("step1", (context, input) -> {
                System.out.println(">>> [STEP] Executing step1 for workflow: " + input.get("workflowId"));
                return "step1-output";
            }, 3);
        });
    }

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
    private final Duration delay;
    private final String childWorkflowName;
    private final java.util.function.Function<Map<String, Object>, Map<String, Object>> inputMapper;
    private final Predicate<Map<String, Object>> condition;

    
    public WorkflowStep(String name, WorkflowLambda lambda, int maxAttempts) {
        this.name = name;
        this.lambda = lambda;
        this.maxAttempts = maxAttempts;
        this.dependencies = new ArrayList<>();
        this.delay = null;
        this.childWorkflowName = null;
        this.inputMapper = null;
        this.condition = null;
    }

    public WorkflowStep(String name, WorkflowLambda lambda, int maxAttempts, List<String> dependencies) {
        this.name = name;
        this.lambda = lambda;
        this.maxAttempts = maxAttempts;
        this.dependencies = dependencies;
        this.delay = null;
        this.childWorkflowName = null;
        this.inputMapper = null;
        this.condition = null;
    }

    public WorkflowStep(String name, WorkflowLambda lambda, int maxAttempts, List<String> dependencies, Duration delay) {
        this.name = name;
        this.lambda = lambda;
        this.maxAttempts = maxAttempts;
        this.dependencies = dependencies;
        this.delay = delay;
        this.childWorkflowName = null;
        this.inputMapper = null;
        this.condition = null;
    }

    public WorkflowStep(
        String name,
        WorkflowLambda lambda,
        int maxAttempts,
        List<String> dependencies,
        Duration delay,
        String childWorkflowName,
        Function<Map<String, Object>, Map<String, Object>> inputMapper,
        Predicate<Map<String, Object>> condition
    ) {
        this.name = name;
        this.lambda = lambda;
        this.maxAttempts = maxAttempts;
        this.dependencies = dependencies;
        this.delay = delay;
        this.childWorkflowName = childWorkflowName;
        this.inputMapper = inputMapper;
        this.condition = condition;
    }

    public WorkflowStep(String name,String childWorkflowName, java.util.function.Function<Map<String, Object>, Map<String, Object>> inputMapper, List<String> dependencies) {
        this.name = name;
        this.lambda = null;
        this.maxAttempts = 1;
        this.dependencies = dependencies;
        this.delay = null;
        this.childWorkflowName = childWorkflowName;
        this.inputMapper = inputMapper;
        this.condition = null;
    }

    public WorkflowStep(String name, WorkflowLambda lambda, int maxAttempts, List<String> dependencies, Predicate<Map<String, Object>> condition) {
        this.name = name;
        this.lambda = lambda;
        this.maxAttempts = maxAttempts;
        this.dependencies = dependencies;
        this.delay = null;
        this.childWorkflowName = null;
        this.inputMapper = null;
        this.condition = condition;
    }

    public String getName() { return name; }
    public WorkflowLambda getLambda() { return lambda; }
    public int getMaxAttempts() { return maxAttempts; }
    public List<String> getDependencies() { return dependencies; } 
    public boolean hasDelay() { return delay != null; }
    public Duration getDelay() { return delay; }
    public boolean hasChildWorkflow() { return childWorkflowName != null; }
    public String getChildWorkflowName() { return childWorkflowName; }
    public java.util.function.Function<Map<String, Object>, Map<String, Object>> getInputMapper() { 
        return inputMapper; 
    }
    public boolean hasCondition() { return condition != null; }
    public Predicate<Map<String, Object>> getCondition() { return condition; }
}

@FunctionalInterface
interface WorkflowLambda {
    Object execute(StepContext stepContext, Map<String, Object> context) throws Exception;
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
    final List<WorkflowStep> steps = new ArrayList<>();

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

    public WorkflowBuilder stepWithDelay(String stepName, Duration delay, WorkflowLambda lambda, int maxAttempts, List<String> dependencies) {
        steps.add(new WorkflowStep(stepName, lambda, maxAttempts, dependencies, delay));
        return this;
    }

    public WorkflowBuilder stepWithDelay(String stepName, Duration delay, WorkflowLambda lambda, int maxAttempts) {
        steps.add(new WorkflowStep(stepName, lambda, maxAttempts,new ArrayList<>(), delay));
        return this;
    }

    public WorkflowBuilder childWorkflow(String stepName, String childWorkflowName, java.util.function.Function<Map<String, Object>, Map<String, Object>> inputMapper, List<String> dependencies) {
        steps.add(new WorkflowStep(stepName, childWorkflowName, inputMapper, dependencies));
        return this;
    }


    public WorkflowBuilder step(String stepName, WorkflowLambda lambda, int maxAttempts, List<String> dependencies, Predicate<Map<String, Object>> condition) {
        steps.add(new WorkflowStep(stepName, lambda, maxAttempts, dependencies, condition));
        return this;
    }

    public BranchBuilder branch(String name, List<String> dependencies, int maxAttempts) {
        return new BranchBuilder(name, this, dependencies, maxAttempts);
    }

    public BranchBuilder branch(String name, List<String> dependencies) {
        return branch(name, dependencies, 3);
    }

    public BranchBuilder branch(String name) {
        return branch(name, new ArrayList<>(), 3);
    }

    public WorkflowBuilder ifElse(
        String baseName,
        Predicate<Map<String, Object>> condition,
        WorkflowLambda thenLambda,
        WorkflowLambda elseLambda,
        int maxAttempts,
        List<String> dependencies
    ) {
      BranchBuilder branchBuilder = branch(baseName, dependencies, maxAttempts);
      branchBuilder.if_("then", condition)
          .then("step", thenLambda);
      branchBuilder.else_()
          .then("step", elseLambda);
      branchBuilder.endBranch();
      return this;
    }

    public WorkflowBuilder ifElse(
        String baseName,
        Predicate<Map<String, Object>> condition,
        WorkflowLambda thenLambda,
        WorkflowLambda elseLambda,
        List<String> dependencies
    ) {
        return ifElse(baseName, condition, thenLambda, elseLambda, 3, dependencies);
    }

    public WorkflowBuilder switchCase(
        String baseName,
        Function<Map<String, Object>, Object> valueSelector,
        Consumer<SwitchCaseBuilder> caseConfigurer,
        int maxAttempts,
        List<String> dependencies
    ) {
        SwitchCaseBuilder switchBuilder = new SwitchCaseBuilder(baseName, this, valueSelector, maxAttempts, dependencies);
        caseConfigurer.accept(switchBuilder);
        switchBuilder.endSwitchCase();
        return this;
    }

    public WorkflowBuilder switchCase(
        String baseName,
        Function<Map<String, Object>, Object> valueSelector,
        Consumer<SwitchCaseBuilder> caseConfigurer,
        List<String> dependencies
    ) {
        return switchCase(baseName, valueSelector, caseConfigurer, 3, dependencies);
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

