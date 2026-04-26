package com.sdi.gateway.repository;

import com.sdi.gateway.model.entity.InterviewSession;
import com.sdi.gateway.model.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    List<InterviewSession> findByStatus(SessionStatus status);
}
