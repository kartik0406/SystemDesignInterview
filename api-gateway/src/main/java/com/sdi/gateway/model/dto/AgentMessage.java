package com.sdi.gateway.model.dto;

import lombok.*;

import java.util.Map;

/**
 * A2A-inspired agent message envelope following JSON-RPC 2.0 pattern.
 * Used for inter-agent communication within the orchestrator.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentMessage {

    @Builder.Default
    private String jsonrpc = "2.0";

    private String method;        // e.g., "generate_question", "evaluate_answer", "provide_hint"
    private Map<String, Object> params;
    private String id;
    private Object result;
    private AgentError error;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgentError {
        private int code;
        private String message;
        private Object data;
    }

    public static AgentMessage request(String method, Map<String, Object> params, String id) {
        return AgentMessage.builder()
                .method(method)
                .params(params)
                .id(id)
                .build();
    }

    public static AgentMessage response(String id, Object result) {
        return AgentMessage.builder()
                .id(id)
                .result(result)
                .build();
    }

    public static AgentMessage error(String id, int code, String message) {
        return AgentMessage.builder()
                .id(id)
                .error(AgentError.builder().code(code).message(message).build())
                .build();
    }
}
