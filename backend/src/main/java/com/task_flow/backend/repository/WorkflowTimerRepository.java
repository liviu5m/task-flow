package com.task_flow.backend.repository;

import com.task_flow.backend.model.WorkflowTimer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowTimerRepository extends JpaRepository<WorkflowTimer, UUID> {
    @Query(value = "SELECT * FROM \"task-flow\".workflow_timers WHERE fires_at <= NOW() AND NOT fired FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<WorkflowTimer> findDueTimersForUpdate();
}
