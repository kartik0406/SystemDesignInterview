package com.sdi.gateway.agent;

import lombok.*;

import java.util.List;

/**
 * A2A-compatible agent card for service discovery.
 * Each agent advertises its capabilities.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentCard {

    private String name;
    private String description;
    private String version;
    private List<String> capabilities;
    private List<String> supportedMethods;

    public static AgentCard questionAgent() {
        return AgentCard.builder()
                .name("QuestionAgent")
                .description("Generates adaptive system design interview questions")
                .version("1.0.0")
                .capabilities(List.of("question_generation", "difficulty_adjustment", "topic_selection"))
                .supportedMethods(List.of("generate_question"))
                .build();
    }

    public static AgentCard evaluationAgent() {
        return AgentCard.builder()
                .name("EvaluationAgent")
                .description("Evaluates candidate answers with structured rubric scoring")
                .version("1.0.0")
                .capabilities(List.of("answer_evaluation", "rubric_scoring", "feedback_generation"))
                .supportedMethods(List.of("evaluate_answer"))
                .build();
    }

    public static AgentCard hintAgent() {
        return AgentCard.builder()
                .name("HintAgent")
                .description("Provides progressive hints without revealing full solutions")
                .version("1.0.0")
                .capabilities(List.of("hint_generation", "progressive_disclosure"))
                .supportedMethods(List.of("provide_hint"))
                .build();
    }
}
