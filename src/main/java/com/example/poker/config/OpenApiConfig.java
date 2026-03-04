package com.example.poker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pokerPlaygroundApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Poker Playground API")
                        .description("""
                                REST API for the Poker Playground backend.
                                
                                **Game Flow:**
                                1. `POST /api/rooms` – Create a room (set blinds & buy-in).
                                2. `POST /api/rooms/{code}/join` – First joiner becomes the **owner**.
                                3. `POST /api/rooms/{code}/settings` – Owner adjusts blinds/buy-in before game starts.
                                4. `POST /api/rooms/{code}/start-game` – Owner starts the game.
                                5. In-game actions are done over **WebSocket** at `ws://localhost:8080/ws/game/{code}`.
                                6. `POST /api/rooms/{code}/stop-game` – Owner stops the game at any time.
                                
                                **WebSocket Actions:** BET, CHECK, CALL, RAISE, FOLD, ALL_IN
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Poker Playground")))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local")))
                .tags(List.of(
                        new Tag().name("Rooms").description("Room lifecycle – create, join, leave"),
                        new Tag().name("Admin").description("Owner-only actions – settings, start/stop, chip distribution")
                ));
    }
}
