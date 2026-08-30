package com.task_flow.backend.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

class BranchBuilder {
    private final String baseName;
    private final WorkflowBuilder builder;
    private final List<CaseBuilder> cases = new ArrayList<>();
    private CaseBuilder defaultCase = null;
    private final List<String> dependencies;
    private final int maxAttempts;

    public BranchBuilder(String baseName, WorkflowBuilder builder, List<String> dependencies, int maxAttempts) {
        this.baseName = baseName;
        this.builder = builder;
        this.dependencies = dependencies;
        this.maxAttempts = maxAttempts;
    }

    public CaseBuilder if_(String caseName, Predicate<Map<String, Object>> condition) {
        CaseBuilder caseBuilder = new CaseBuilder(baseName, caseName, condition, dependencies, maxAttempts);
        cases.add(caseBuilder);
        return caseBuilder;
    }

    public CaseBuilder elseIf_(String caseName, Predicate<Map<String, Object>> condition) {
        return if_(caseName, condition);
    }

    public CaseBuilder else_() {
        Predicate<Map<String, Object>> defaultCondition = ctx -> {
            for (CaseBuilder caseBuilder : cases) {
                if (caseBuilder.condition.test(ctx)) {
                    return false;
                }
            }
            return true;
        };
        defaultCase = new CaseBuilder(baseName, "else", defaultCondition, dependencies, maxAttempts);
        return defaultCase;
    }

    public WorkflowBuilder endBranch() {
        for (CaseBuilder caseBuilder : cases) {
            caseBuilder.addStepsToBuilder(builder);
        }
        if (defaultCase != null) {
            defaultCase.addStepsToBuilder(builder);
        }
        return builder;
    }
}
