package com.task_flow.backend.engine;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.task_flow.backend.repository.LeaderEpochRepository;

import jakarta.annotation.PostConstruct;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LeaderElector {
    private static final long LOCK_ID = 123456789L; 
    private final JdbcTemplate jdbcTemplate;
    private final LeaderEpochRepository leaderEpochRepository;
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private final AtomicLong currentEpoch = new AtomicLong(0);
    private Connection dedicatedConn;

    
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
      try {
        if(dedicatedConn == null || dedicatedConn.isClosed()) {
          dedicatedConn = jdbcTemplate.getDataSource().getConnection();
        }
        boolean acquired = false;
        try (Statement stmt = dedicatedConn.createStatement()) {
          ResultSet rs = stmt.executeQuery("SELECT pg_try_advisory_lock(" + LOCK_ID + ")");
          if (rs.next()) {
            acquired = rs.getBoolean(1);
          }
        }
        if (acquired) {
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
      } catch (Exception e) {
        System.err.println(">>> [LEADER] Failed to acquire leadership: " + e.getMessage());
        isLeader.set(false);
        try {
          if (dedicatedConn != null) {
            dedicatedConn.close();
          }
        } catch (Exception ignored) {}
        dedicatedConn = null;
      }
    }

    public boolean isLeader() {
        return isLeader.get();
    }

    public long getCurrentEpoch() {
        return currentEpoch.get();
    }
}
