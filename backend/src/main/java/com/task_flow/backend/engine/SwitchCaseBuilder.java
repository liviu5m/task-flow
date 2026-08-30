package com.task_flow.backend.engine; 

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

class SwitchCaseBuilder {
    private final String baseName;
    private final WorkflowBuilder builder;
    private final Function<Map<String, Object>, Object> valueSelector;
    private final List<String> dependencies;
    private final int maxAttempts;
    private final List<CaseBuilder> cases = new ArrayList<>();
    private CaseBuilder defaultCase = null;

    public SwitchCaseBuilder(String baseName, WorkflowBuilder builder,
                            Function<Map<String, Object>, Object> valueSelector,
                            int maxAttempts, List<String> dependencies) {
        this.baseName = baseName;
        this.builder = builder;
        this.valueSelector = valueSelector;
        this.maxAttempts = maxAttempts;
        this.dependencies = dependencies;
    }
    public SwitchCaseBuilder when(Object expectedValue, WorkflowLambda lambda) {
        return when(expectedValue, "step", lambda);
    }

    public SwitchCaseBuilder when(Object expectedValue, String stepName, WorkflowLambda lambda) {
        Predicate<Map<String, Object>> condition = ctx -> {
            Object actualValue = valueSelector.apply(ctx);
            return actualValue != null && actualValue.equals(expectedValue);
        };
        CaseBuilder caseBuilder = new CaseBuilder(
            baseName,
            "case-" + expectedValue,
            condition,
            dependencies,
            maxAttempts
        );
        caseBuilder.then(stepName, lambda);
        cases.add(caseBuilder);
        return this;
    }

    public SwitchCaseBuilder default_(WorkflowLambda lambda) {
        return default_("step", lambda);
    }

    public SwitchCaseBuilder default_(String stepName, WorkflowLambda lambda) {
        Predicate<Map<String, Object>> defaultCondition = ctx -> {
            for (CaseBuilder caseBuilder : cases) {
                if (caseBuilder.condition.test(ctx)) {
                    return false;
                }
            }
            return true;
        };
        defaultCase = new CaseBuilder(
            baseName,
            "default",
            defaultCondition,
            dependencies,
            maxAttempts
        );
        defaultCase.then(stepName, lambda);
        return this;
    }

    public void endSwitchCase() {
        for (CaseBuilder caseBuilder : cases) {
            caseBuilder.addStepsToBuilder(builder);
        }
        if (defaultCase != null) {
            defaultCase.addStepsToBuilder(builder);
        }
    }
}
