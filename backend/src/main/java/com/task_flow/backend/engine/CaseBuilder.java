package com.task_flow.backend.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import lombok.Data;

class CaseBuilder {
    private final String baseName;
    private final String caseName;
    final Predicate<Map<String, Object>> condition;
    private final List<String> dependencies;
    private final int maxAttempts;
    private final List<WorkflowStep> steps = new ArrayList<>();

    public CaseBuilder(String baseName, String caseName, Predicate<Map<String, Object>> condition,
                       List<String> dependencies, int maxAttempts) {
        this.baseName = baseName;
        this.caseName = caseName;
        this.condition = condition;
        this.dependencies = dependencies;
        this.maxAttempts = maxAttempts;
    }

    public CaseBuilder then(String stepName, WorkflowLambda lambda) {
        steps.add(new WorkflowStep(
            buildStepName(stepName),
            lambda,
            maxAttempts,
            new ArrayList<>(dependencies),
            null,
            null,
            null,
            condition
        ));
        return this;
    }

    public CaseBuilder then(String stepName, WorkflowLambda lambda, int stepMaxAttempts) {
        steps.add(new WorkflowStep(
            buildStepName(stepName),
            lambda,
            stepMaxAttempts,
            new ArrayList<>(dependencies),
            null,
            null,
            null,
            condition
        ));
        return this;
    }

    private String buildStepName(String stepName) {
        return baseName + "-" + caseName + "-" + stepName;
    }

    public void addStepsToBuilder(WorkflowBuilder builder) {
        builder.steps.addAll(steps);
    }
}
