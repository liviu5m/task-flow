package com.task_flow.backend.service;

import com.task_flow.backend.engine.WorkflowRegistry;
import com.task_flow.backend.model.ApiKey;
import com.task_flow.backend.repository.ApiKeyRepository;
import com.task_flow.backend.repository.WorkflowInstanceRepository;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class WorkflowService {

    private final WorkflowRegistry registry;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final ApiKeyRepository apiKeyRepository;

    public WorkflowService(
        WorkflowRegistry registry,
        WorkflowInstanceRepository workflowInstanceRepository,
        ApiKeyRepository apiKeyRepository
    ) {
        this.registry = registry;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    public List<String> getWorkflows(Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId).orElse(null);
        if (apiKey == null) {
            throw new IllegalArgumentException("No API key found for id: " + apiKeyId);
        }
        return registry.listWorkflows(apiKey);
    }
}
