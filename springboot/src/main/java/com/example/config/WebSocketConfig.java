package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AiChatWebSocketHandler aiChatWebSocketHandler;

    public WebSocketConfig(AiChatWebSocketHandler aiChatWebSocketHandler) {
        this.aiChatWebSocketHandler = aiChatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(aiChatWebSocketHandler, "/ws/ai/chat").setAllowedOrigins("*");
    }
}
