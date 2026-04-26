package com.sdi.gateway.repository;

import com.sdi.gateway.model.entity.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, UUID> {

    Optional<EvaluationResult> findBySessionId(UUID sessionId);
}
