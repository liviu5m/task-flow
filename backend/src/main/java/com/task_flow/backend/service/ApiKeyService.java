package com.task_flow.backend.service;

import com.task_flow.backend.dto.ApiKeyDto;
import com.task_flow.backend.model.ApiKey;
import com.task_flow.backend.repository.ApiKeyRepository;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public ApiKey createApiKey(ApiKeyDto apiKeyDto) {
        ApiKey apiKey = new ApiKey();
        apiKey.setName(apiKeyDto.getName());
        apiKey.setKeyHash(generateKeyHash());
        apiKey.setUserId(apiKeyDto.getUserId());
        return apiKeyRepository.save(apiKey);
    }
    private String generateKeyHash() {
        return "tf_live_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public void deleteApiKey(Long id) {
        apiKeyRepository.deleteById(id);
    }

    public List<ApiKey> getApiKeys(String userId) {
        return apiKeyRepository.findByUserId(userId);
    }
} 
