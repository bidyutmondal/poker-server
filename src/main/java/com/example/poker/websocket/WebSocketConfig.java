package com.example.poker.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final GameWebSocketHandler gameWebSocketHandler;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler) {
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Plain WebSocket — works with wscat, Postman, and native browser WebSocket API
        // Single wildcard captures the room code: /ws/game/ABCXYZ
        registry.addHandler(gameWebSocketHandler, "/ws/game/*")
                .setAllowedOrigins("*");

        // SockJS — for browser clients needing HTTP long-polling fallback
        // Do NOT add /** here; SockJS appends its own path segments internally
        registry.addHandler(gameWebSocketHandler, "/sockjs/game/*")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
