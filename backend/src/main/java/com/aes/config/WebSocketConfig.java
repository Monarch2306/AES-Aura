package com.aes.config;

import com.aes.evaluations.EvaluationProgressBroadcaster;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@SuppressWarnings("null")
public class WebSocketConfig implements WebSocketConfigurer {

    private final EvaluationProgressBroadcaster evaluationProgressBroadcaster;

    public WebSocketConfig(EvaluationProgressBroadcaster evaluationProgressBroadcaster) {
        this.evaluationProgressBroadcaster = evaluationProgressBroadcaster;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(evaluationProgressBroadcaster, "/ws/evaluations", "/ws/evaluations/{sheetId}")
                .setAllowedOriginPatterns("*");
    }
}
