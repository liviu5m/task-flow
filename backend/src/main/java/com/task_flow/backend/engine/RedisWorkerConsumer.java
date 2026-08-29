package com.task_flow.backend.engine;

import com.task_flow.backend.dto.TaskMessage;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RedisWorkerConsumer implements InitializingBean {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskFlowEngine taskFlowEngine;
    private final LeaderElector leaderElector;
    private final String consumerName;

    private static final String STREAM_KEY = "taskflow-task-stream";
    private static final String GROUP_NAME = "taskflow-workers";

    public RedisWorkerConsumer(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            TaskFlowEngine taskFlowEngine,
            LeaderElector leaderElector,
            @Value("${HOSTNAME:#{T(java.util.UUID).randomUUID().toString()}}") String hostId) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.taskFlowEngine = taskFlowEngine;
        this.leaderElector = leaderElector;
        this.consumerName = "worker-" + hostId;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
        } catch (Exception ignored) {
        }
        claimStaleTasks();
        Thread.ofVirtual().start(this::consumeLoop);
    }

    @Scheduled(fixedRate = 30000)
    public void claimStaleTasks() {
        try {
            System.out.println(">>> [JANITOR] Checking for stale tasks...");
            List<MapRecord<String, Object, Object>> staleMessages = redisTemplate.opsForStream().read(
                Consumer.from(GROUP_NAME, consumerName),
                StreamReadOptions.empty().count(100),
                StreamOffset.create(STREAM_KEY, ReadOffset.from("0"))
            );

            if (staleMessages != null && !staleMessages.isEmpty()) {
                System.out.println(">>> [JANITOR] Found " + staleMessages.size() + " stale tasks. Reclaiming...");
                for (MapRecord<String, Object, Object> message : staleMessages) {
                    redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, message.getId());
                    System.out.println(">>> [JANITOR] Reclaimed task ID: " + message.getId());
                }
            }
        } catch (Exception e) {
            System.err.println(">>> [JANITOR] Failed to reclaim messages: " + e.getMessage());
        }
    }

    private void consumeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream().read(
                    Consumer.from(GROUP_NAME, consumerName),
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
            if(task.getLeaderEpoch() < leaderElector.getCurrentEpoch()) {
                System.out.println(">>> [WORKER] Rejecting stale task from epoch " + task.getLeaderEpoch() + " (current: " + leaderElector.getCurrentEpoch() + ")");
                redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, message.getId());
                return;
            }
            taskFlowEngine.executeSingleStep(task.getWorkflowId(), task.getWorkflowName(), task.getStepName());

            redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, message.getId());
            System.out.println(">>> [WORKER] Successfully acknowledged message ID: " + message.getId());

        } catch (Exception e) {
            System.err.println(">>> [WORKER] Failed to process task, leaving in PEL for recovery: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
