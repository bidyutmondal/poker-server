package com.example.poker.controller;

import com.example.poker.model.Player;
import com.example.poker.model.Room;
import com.example.poker.service.RoomService;
import com.example.poker.websocket.GameWebSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
@Tag(name = "Rooms", description = "Room lifecycle and game control endpoints")
public class RoomController {
    private final RoomService roomService;
    private final GameWebSocketHandler gameWebSocketHandler;

    public RoomController(RoomService roomService, GameWebSocketHandler gameWebSocketHandler) {
        this.roomService = roomService;
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Room Setup
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Create a room",
        description = """
            Creates a new poker room. You can optionally set:
            - `smallBlind` (default: 10)
            - `bigBlind` (default: 20)
            - `defaultBuyIn` – chips automatically assigned when a player joins (default: 1000)
            
            The first player to **join** (`/join`) becomes the room owner.
            """,
        tags = {"Rooms"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Room created successfully",
            content = @Content(schema = @Schema(implementation = Room.class))),
    })
    @PostMapping
    public ResponseEntity<Room> createRoom(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Optional room settings. All fields have defaults.",
                content = @Content(examples = @ExampleObject(
                    value = "{\"smallBlind\": 10, \"bigBlind\": 20, \"defaultBuyIn\": 1000}"
                ))
            )
            @RequestBody(required = false) CreateRoomRequest req) {
        int sb = (req != null && req.getSmallBlind() > 0) ? req.getSmallBlind() : 0;
        int bb = (req != null && req.getBigBlind()   > 0) ? req.getBigBlind()   : 0;
        int bi = (req != null && req.getDefaultBuyIn() > 0) ? req.getDefaultBuyIn() : 0;
        Room r = roomService.createRoom(sb, bb, bi);
        return ResponseEntity.ok(r);
    }

    @Operation(
        summary = "Join a room",
        description = """
            Join an existing room using its 6-letter code. Players automatically receive `defaultBuyIn` chips.
            
            - The **first** player to join becomes the room **owner**.
            - Players can join **mid-game** — they will sit out the current hand and participate in the next.
            """,
        tags = {"Rooms"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Joined successfully – returns Player object with your `id`",
            content = @Content(schema = @Schema(implementation = Player.class))),
        @ApiResponse(responseCode = "404", description = "Room not found", content = @Content),
        @ApiResponse(responseCode = "400", description = "Could not add player", content = @Content),
    })
    @PostMapping("/{code}/join")
    public ResponseEntity<?> joinRoom(
            @Parameter(description = "6-letter room code", example = "ABCXYZ") @PathVariable String code,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(examples = @ExampleObject(value = "{\"name\": \"Alice\"}"))
            )
            @Valid @RequestBody JoinRequest req) {
        Room room = roomService.getRoom(code);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Room not found"));
        }
        Player p = roomService.addPlayer(code, req.getName());
        if (p == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Could not add player to room"));
        }
        return ResponseEntity.ok(p);
    }

    @Operation(
        summary = "Leave a room",
        description = """
            A player voluntarily leaves the room.
            
            - If the game is running, their hand is **auto-folded** and the turn advances.
            - If the leaving player was the **owner**, ownership transfers to the next player in the list.
            """,
        tags = {"Rooms"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Left successfully"),
        @ApiResponse(responseCode = "400", description = "Player not found", content = @Content),
    })
    @PostMapping("/{code}/leave")
    public ResponseEntity<?> leaveRoom(
            @Parameter(description = "6-letter room code") @PathVariable String code,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(examples = @ExampleObject(value = "{\"playerId\": \"<your-player-id>\"}"))
            )
            @Valid @RequestBody PlayerIdRequest req) {
        try {
            roomService.removePlayer(code, req.getPlayerId());
            Room room = roomService.getRoom(code);
            gameWebSocketHandler.broadcastPlayerLeft(code);
            return ResponseEntity.ok(room != null ? room : "Room empty");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }

    @Operation(
        summary = "Get room state",
        description = "Returns the full current state of the room, including player list, chip counts, game stage, community cards, and whose turn it is.",
        tags = {"Rooms"}
    )
    @ApiResponse(responseCode = "200", description = "Room state",
        content = @Content(schema = @Schema(implementation = Room.class)))
    @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
    @GetMapping("/{code}")
    public ResponseEntity<?> getRoom(
            @Parameter(description = "6-letter room code") @PathVariable String code) {
        Room room = roomService.getRoom(code);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Room not found"));
        }
        return ResponseEntity.ok(room);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin / Owner Actions
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "[Owner] Update room settings",
        description = """
            Owner can configure blind levels and default buy-in **before** the game starts.
            
            | Field | Description | Default |
            |---|---|---|
            | `smallBlind` | Small blind amount | 10 |
            | `bigBlind` | Big blind amount | 20 |
            | `defaultBuyIn` | Chips given to each player on join | 1000 |
            
            > ⚠️ Cannot be changed while a game is in progress.
            """,
        tags = {"Admin"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Settings updated"),
        @ApiResponse(responseCode = "400", description = "Not the owner, or game already started", content = @Content),
    })
    @PostMapping("/{code}/settings")
    public ResponseEntity<?> updateSettings(
            @Parameter(description = "6-letter room code") @PathVariable String code,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(examples = @ExampleObject(
                    value = "{\"playerId\": \"<owner-id>\", \"smallBlind\": 25, \"bigBlind\": 50, \"defaultBuyIn\": 2000}"
                ))
            )
            @Valid @RequestBody UpdateSettingsRequest req) {
        try {
            roomService.updateSettings(code, req.getPlayerId(),
                    req.getSmallBlind(), req.getBigBlind(), req.getDefaultBuyIn());
            return ResponseEntity.ok(roomService.getRoom(code));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }

    @Operation(
        summary = "[Owner] Give chips to a player (buy-in)",
        description = """
            Owner grants additional chips to any player. Chips are **additive** (added on top of existing chips).
            
            Works both **before** and **during** a game. If given mid-game, the extra chips are immediately available for the next hand.
            """,
        tags = {"Admin"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chips distributed"),
        @ApiResponse(responseCode = "400", description = "Not owner or invalid amount", content = @Content),
    })
    @PostMapping("/{code}/buy-in")
    public ResponseEntity<?> distributeChips(
            @Parameter(description = "6-letter room code") @PathVariable String code,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(examples = @ExampleObject(
                    value = "{\"ownerId\": \"<owner-id>\", \"playerId\": \"<target-player-id>\", \"chips\": 500}"
                ))
            )
            @Valid @RequestBody BuyInRequest req) {
        try {
            roomService.distributeChips(code, req.getOwnerId(), req.getPlayerId(), req.getChips());
            return ResponseEntity.ok(roomService.getRoom(code));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }

    @Operation(
        summary = "[Owner] Start the game",
        description = """
            Starts a new hand. Requires at least 2 players with chips.
            
            On start the server automatically:
            1. Shuffles the deck and deals 2 hole cards to each active player.
            2. Moves the dealer button.
            3. Posts small blind and big blind.
            4. Sets `currentPlayer` to Under-the-Gun (first to act).
            
            After this, game actions must be sent over **WebSocket** (`ws://host/ws/game/{code}`).
            """,
        tags = {"Admin"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Game started – returns updated room state"),
        @ApiResponse(responseCode = "400", description = "Not owner / not enough players", content = @Content),
    })
    @PostMapping("/{code}/start-game")
    public ResponseEntity<?> startGame(
            @Parameter(description = "6-letter room code") @PathVariable String code,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(examples = @ExampleObject(value = "{\"playerId\": \"<owner-id>\"}"))
            )
            @Valid @RequestBody PlayerIdRequest req) {
        try {
            roomService.startGame(code, req.getPlayerId());
            return ResponseEntity.ok(roomService.getRoom(code));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }

    @Operation(
        summary = "[Owner] Stop the game",
        description = """
            Immediately ends the current game. The room resets to `WAITING_FOR_PLAYERS`.
            
            Players keep their current chip counts. The owner can start a fresh game at any time.
            """,
        tags = {"Admin"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Game stopped"),
        @ApiResponse(responseCode = "400", description = "Not the owner", content = @Content),
    })
    @PostMapping("/{code}/stop-game")
    public ResponseEntity<?> stopGame(
            @Parameter(description = "6-letter room code") @PathVariable String code,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(examples = @ExampleObject(value = "{\"playerId\": \"<owner-id>\"}"))
            )
            @Valid @RequestBody PlayerIdRequest req) {
        try {
            roomService.stopGame(code, req.getPlayerId());
            return ResponseEntity.ok(roomService.getRoom(code));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }
    }

    // ────────────── DTOs ──────────────

    @Schema(description = "Optional room creation settings")
    public static class CreateRoomRequest {
        @Schema(description = "Small blind amount", example = "10", defaultValue = "10")
        private int smallBlind;
        @Schema(description = "Big blind amount", example = "20", defaultValue = "20")
        private int bigBlind;
        @Schema(description = "Chips auto-assigned when a player joins", example = "1000", defaultValue = "1000")
        private int defaultBuyIn;

        public int getSmallBlind() { return smallBlind; }
        public void setSmallBlind(int v) { this.smallBlind = v; }
        public int getBigBlind() { return bigBlind; }
        public void setBigBlind(int v) { this.bigBlind = v; }
        public int getDefaultBuyIn() { return defaultBuyIn; }
        public void setDefaultBuyIn(int v) { this.defaultBuyIn = v; }
    }

    @Schema(description = "Owner settings update request")
    public static class UpdateSettingsRequest {
        @NotBlank(message = "Player ID is required")
        @Schema(description = "Owner's player ID", example = "550e8400-e29b-41d4-a716-446655440000")
        private String playerId;
        @Schema(description = "New small blind", example = "25")
        private Integer smallBlind;
        @Schema(description = "New big blind", example = "50")
        private Integer bigBlind;
        @Schema(description = "New default buy-in", example = "2000")
        private Integer defaultBuyIn;

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String v) { this.playerId = v; }
        public Integer getSmallBlind() { return smallBlind; }
        public void setSmallBlind(Integer v) { this.smallBlind = v; }
        public Integer getBigBlind() { return bigBlind; }
        public void setBigBlind(Integer v) { this.bigBlind = v; }
        public Integer getDefaultBuyIn() { return defaultBuyIn; }
        public void setDefaultBuyIn(Integer v) { this.defaultBuyIn = v; }
    }

    @Schema(description = "Player join request")
    public static class JoinRequest {
        @NotBlank(message = "Player name is required")
        @Schema(description = "Display name for the player", example = "Alice")
        private String name;

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
    }

    @Schema(description = "Generic request carrying only a player ID")
    public static class PlayerIdRequest {
        @NotBlank(message = "Player ID is required")
        @Schema(description = "The player's UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        private String playerId;

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String v) { this.playerId = v; }
    }

    @Schema(description = "Owner-issued chip buy-in request")
    public static class BuyInRequest {
        @NotBlank(message = "Owner ID is required")
        @Schema(description = "Room owner's player ID", example = "550e8400-e29b-41d4-a716-446655440000")
        private String ownerId;
        @NotBlank(message = "Target player ID is required")
        @Schema(description = "ID of the player receiving chips", example = "660e8400-e29b-41d4-a716-446655440111")
        private String playerId;
        @Schema(description = "Number of chips to add (additive)", example = "500")
        private int chips;

        public String getOwnerId() { return ownerId; }
        public void setOwnerId(String v) { this.ownerId = v; }
        public String getPlayerId() { return playerId; }
        public void setPlayerId(String v) { this.playerId = v; }
        public int getChips() { return chips; }
        public void setChips(int v) { this.chips = v; }
    }

    public static class ErrorResponse {
        @Schema(description = "Error description")
        private String error;

        public ErrorResponse(String e) { this.error = e; }
        public String getError() { return error; }
        public void setError(String e) { this.error = e; }
    }
}
