package com.task_flow.backend.controller;

import com.task_flow.backend.dto.ApiKeyDto;
import com.task_flow.backend.model.ApiKey;
import com.task_flow.backend.service.ApiKeyService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<ApiKey> createApiKey(@RequestBody ApiKeyDto apiKeyDto) {
        return ResponseEntity.ok(apiKeyService.createApiKey(apiKeyDto));
    }

    @DeleteMapping("/{id}")
    public void deleteApiKeyFunc(@PathVariable Long id) {
        apiKeyService.deleteApiKey(id);
    }

    @GetMapping
    public ResponseEntity<List<ApiKey>> getApiKeys(@RequestParam(required = false) String userId) {
        return ResponseEntity.ok(apiKeyService.getApiKeys(userId));
    }

} 
