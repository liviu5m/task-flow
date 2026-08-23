package com.task_flow.backend.service;

import tools.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.task_flow.backend.dto.TaskMessage;

import java.util.Map;
import java.util.UUID;

@Service
public class RedisTaskProducer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    public static final String STREAM_KEY = "taskflow-task-stream";

    public RedisTaskProducer(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void enqueueTask(UUID workflowId, String workflowName, String stepName) {
        try {
            TaskMessage task = new TaskMessage(workflowId, workflowName, stepName);
            String jsonPayload = objectMapper.writeValueAsString(task);

            redisTemplate.opsForStream().add(STREAM_KEY, Map.of("payload", jsonPayload));
        } catch (Exception e) {
            throw new RuntimeException("Failed to enqueue task to Redis", e);
        }
    }
}
