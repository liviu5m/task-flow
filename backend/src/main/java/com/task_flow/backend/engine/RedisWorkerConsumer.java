package com.task_flow.backend.engine;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.task_flow.backend.dto.TaskMessage;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class RedisWorkerConsumer implements InitializingBean {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskFlowEngine taskFlowEngine;

    private static final String STREAM_KEY = "taskflow-task-stream";
    private static final String GROUP_NAME = "taskflow-workers";
    private static final String CONSUMER_NAME = "worker-" + UUID.randomUUID();

    public RedisWorkerConsumer(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, TaskFlowEngine taskFlowEngine) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.taskFlowEngine = taskFlowEngine;
        System.out.println(">>> [WORKER] RedisWorkerConsumer bean was successfully created by Spring!");
        System.out.flush(); // Forces the terminal to display it instantly
    }

    @Override
    public void afterPropertiesSet() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
        } catch (Exception ignored) {
        }

        Thread.ofVirtual().start(this::consumeLoop);
    }
    private void consumeLoop() {
        System.out.println(">>> [WORKER] Consumer loop started and polling stream: " + STREAM_KEY);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream().read(
                    Consumer.from(GROUP_NAME, CONSUMER_NAME),
                    StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                    StreamOffset.create(STREAM_KEY, ReadOffset.from(">"))
                );

                if (messages != null && !messages.isEmpty()) {
                    System.out.println(">>> [WORKER] Pulled " + messages.size() + " messages from Redis!");
                    for (MapRecord<String, Object, Object> message : messages) {
                        processMessage(message);
                    }
                }
            } catch (org.springframework.data.redis.RedisSystemException | org.springframework.data.redis.RedisConnectionFailureException e) {
                System.err.println(">>> [WORKER] Redis connection lost or factory destroyed, stopping worker consumer loop.");
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processMessage(MapRecord<String, Object, Object> message) {
        try {
            System.out.println(">>> [WORKER] Processing message ID: " + message.getId());
            String jsonPayload = (String) message.getValue().get("payload");
            TaskMessage task = objectMapper.readValue(jsonPayload, TaskMessage.class);

            System.out.println(">>> [WORKER] Executing step: " + task.getStepName() + " for workflow: " + task.getWorkflowId());
            taskFlowEngine.executeSingleStep(task.getWorkflowId(), task.getWorkflowName(), task.getStepName());

            redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, message.getId());
            System.out.println(">>> [WORKER] Successfully acknowledged message ID: " + message.getId());

        } catch (Exception e) {
            System.err.println(">>> [WORKER] Failed to process task, leaving in PEL for recovery: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
