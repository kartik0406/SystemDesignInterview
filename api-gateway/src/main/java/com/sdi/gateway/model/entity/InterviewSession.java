package com.sdi.gateway.model.entity;

import com.sdi.gateway.model.enums.CompanyMode;
import com.sdi.gateway.model.enums.DifficultyLevel;
import com.sdi.gateway.model.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "interview_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyMode companyMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel currentDifficulty;

    @Column(nullable = false)
    private int currentRound;

    @Column(nullable = false)
    private int maxRounds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("roundNumber ASC")
    @Builder.Default
    private List<InterviewRound> rounds = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SessionStatus.IN_PROGRESS;
        }
        if (currentDifficulty == null) {
            currentDifficulty = DifficultyLevel.MEDIUM;
        }
    }

    public void addRound(InterviewRound round) {
        rounds.add(round);
        round.setSession(this);
    }
}
