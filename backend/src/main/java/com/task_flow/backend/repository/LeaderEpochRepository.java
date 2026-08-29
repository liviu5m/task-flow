package com.task_flow.backend.repository;

import com.task_flow.backend.model.LeaderEpoch;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaderEpochRepository extends CrudRepository<LeaderEpoch, Long> {
  @Transactional
  @Modifying
  @Query("UPDATE LeaderEpoch SET epoch = epoch + 1 WHERE id = 1")
  void incrementEpoch();

  @Query("SELECT e.epoch FROM LeaderEpoch e WHERE e.id = 1")
  Long getCurrentEpoch();
}
