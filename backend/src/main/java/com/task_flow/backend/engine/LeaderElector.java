package com.task_flow.backend.engine;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.task_flow.backend.model.LeaderEpoch;
import com.task_flow.backend.repository.LeaderEpochRepository;

import jakarta.annotation.PostConstruct;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LeaderElector {
    private static final long LOCK_ID = 123456789L; 
    private final JdbcTemplate jdbcTemplate;
    private final LeaderEpochRepository leaderEpochRepository;
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private final AtomicLong currentEpoch = new AtomicLong(0);

    
    public LeaderElector(JdbcTemplate jdbcTemplate, LeaderEpochRepository leaderEpochRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.leaderEpochRepository = leaderEpochRepository;
    }

    @PostConstruct
    public void init() {
        Long epoch = leaderEpochRepository.getCurrentEpoch();
        currentEpoch.set(epoch != null ? epoch : 0L);
    }

    @Scheduled(fixedRate = 5000) 
    public void tryAcquireLeadership() {
        Boolean acquired = jdbcTemplate.queryForObject(
            "SELECT pg_try_advisory_lock(?);", Boolean.class, LOCK_ID
        );
        if (acquired != null && acquired) {
            if (!isLeader.getAndSet(true)) {
                leaderEpochRepository.incrementEpoch();
                Long newEpoch = leaderEpochRepository.getCurrentEpoch();
                currentEpoch.set(newEpoch != null ? newEpoch : 0L);
                System.out.println(">>> [LEADER] I am now the leader! Epoch: " + currentEpoch.get());
            }
         } else {
            if (isLeader.getAndSet(false)) {
                System.out.println(">>> [LEADER] I lost leadership!");
            }
            Long epoch = leaderEpochRepository.getCurrentEpoch();
            currentEpoch.set(epoch != null ? epoch : 0L);
        }
    }

    public boolean isLeader() {
        return isLeader.get();
    }

    public long getCurrentEpoch() {
        return currentEpoch.get();
    }
}
