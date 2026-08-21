package com.task_flow.backend.controller;

import com.task_flow.backend.engine.TaskFlowEngine;
import com.task_flow.backend.model.WorkflowInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final TaskFlowEngine engine;

    @Autowired
    public WorkflowController(TaskFlowEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/start/{workflowName}")
    public ResponseEntity<?> getWorkflowInstance(@PathVariable String workflowName, @RequestBody Map<String, Object> initialInput) {
        UUID workflowId = engine.start(workflowName, initialInput);
        return ResponseEntity.ok(Map.of("id", workflowId, "status", "RUNNING"));
    }

}
