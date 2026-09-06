package com.task_flow.backend.repository;

import com.task_flow.backend.model.ApiKey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
  List<ApiKey> findByUserId(String userId);

}
