package com.example.medieval_melee;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private final Queue<WebSocketSession> waitingPlayers = new ConcurrentLinkedQueue<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static class Player {
        WebSocketSession session;
        double x;
        double y;
        double velocity;
        double attackTimer;
        int percent;
        int playerId;

        Player(WebSocketSession session) {
            this.session = session;
            this.percent = 0;
        }
    }

    private static class Game {
        Player player1;
        Player player2;
        Hammer hammer;
        Bow bow;
        int timeLeft = 180;
        ScheduledFuture<?> timerTask;

        Game(Player player1, Player player2) {
            this.player1 = player1;
            this.player2 = player2;
        }
    }

    private static class Hammer {
        int x;
        int y;

        Hammer() {
            Random rand = new Random();
            this.x = rand.nextInt(930 - 350 + 1) + 350;
            this.y = rand.nextInt(400 - 150 + 1) + 150;
        }
    }

    private static class Bow {
        int x;
        int y;

        Bow() {
            Random rand = new Random();
            this.x = rand.nextInt(930 - 350 + 1) + 350;
            this.y = rand.nextInt(400 - 150 + 1) + 150;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        waitingPlayers.add(session);
        players.put(session.getId(), new Player(session));

        synchronized (this) {
            checkAndCreateGame();
        }
    }

    private synchronized void checkAndCreateGame() {
        if (waitingPlayers.size() >= 2) {
            WebSocketSession session1 = waitingPlayers.poll();
            WebSocketSession session2 = waitingPlayers.poll();

            if (session1 != null && session2 != null) {

                Player player1 = players.get(session1.getId());
                Player player2 = players.get(session2.getId());

                player1.playerId = 1;
                player2.playerId = 2;
                player1.x = 440;
                player1.y = 300; 
                player2.x = 840; 
                player2.y = 300; 
                player1.velocity = 0;
                player2.velocity = 0;
                player1.attackTimer = 0;
                player2.attackTimer = 0;

                Game game = new Game(player1, player2);
                games.put(session1.getId(), game);
                games.put(session2.getId(), game);
                startGame(game);
            }
        }
    }

    private void startGame(Game game) {
        List<List<Object>> playersData = Arrays.asList(
                Arrays.asList(game.player1.x, game.player1.y, 1),
                Arrays.asList(game.player2.x, game.player2.y, 2)
        );

        sendToPlayer(game.player1, "i", Map.of("id", 1, "p", playersData));
        sendToPlayer(game.player2, "i", Map.of("id", 2, "p", playersData));

        game.timerTask = scheduler.scheduleAtFixedRate(() -> {
            gameLoop(game);
        }, 0, 1, TimeUnit.SECONDS);

        spawnItem(game);
    }

    private void gameLoop(Game game) {
        game.timeLeft--;
        
        sendToPlayer(game.player1, "t", game.timeLeft);
        sendToPlayer(game.player2, "t", game.timeLeft);

        if (game.timeLeft % 20 == 0) {
            spawnItem(game);
        }

        if (game.timeLeft <= 0) {
            endGame(game); 
        }
    }

    private void spawnItem(Game game) {
        Random rand = new Random();

        if(rand.nextBoolean()) {
            game.hammer = new Hammer();
            sendToPlayer(game.player1, "h", Arrays.asList(game.hammer.x, game.hammer.y));
            sendToPlayer(game.player2, "h", Arrays.asList(game.hammer.x, game.hammer.y));
        } else {
            game.bow = new Bow();
            sendToPlayer(game.player1, "b", Arrays.asList(game.bow.x, game.bow.y));
            sendToPlayer(game.player2, "b", Arrays.asList(game.bow.x, game.bow.y));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {

            Game game = games.get(session.getId());

            if (game == null)
                return;

            Player currentPlayer = players.get(session.getId());
            Player otherPlayer = game.player1 == currentPlayer ? game.player2 : game.player1;

            String payload = message.getPayload();
            char type = payload.charAt(0);
            String data = payload.length() > 1 ? payload.substring(1) : "";

            switch (type) {
                case 'p':
                    List<Integer> pos = mapper.readValue(data, List.class);

                    currentPlayer.x = pos.get(0);
                    currentPlayer.y = pos.get(1);
                    currentPlayer.velocity = pos.get(2);
                    currentPlayer.attackTimer = pos.get(3);

                    sendToPlayer(otherPlayer, "p",
                            Arrays.asList(currentPlayer.playerId, currentPlayer.x, currentPlayer.y, currentPlayer.velocity, currentPlayer.attackTimer));
                    break;

                case 'j':
                    if (game.hammer != null) {
                        game.hammer = null;
                            sendToPlayer(game.player1, "j", Arrays.asList(
                                currentPlayer.playerId));
                            sendToPlayer(game.player2, "j", Arrays.asList(
                                currentPlayer.playerId));
                    }
                    break;
                
                case 'n':
                    if (game.bow != null) {
                        game.bow = null;
                            sendToPlayer(game.player1, "n", Arrays.asList(
                                currentPlayer.playerId));
                            sendToPlayer(game.player2, "n", Arrays.asList(
                                currentPlayer.playerId));
                    }
                    break;
                
                case 'f':
                    sendToPlayer(game.player1, "f", Arrays.asList(
                        currentPlayer.playerId));
                    sendToPlayer(game.player2, "f", Arrays.asList(
                        currentPlayer.playerId));
                    break;
                case 'a':
                    sendToPlayer(game.player1, "a", Arrays.asList(
                        currentPlayer.playerId));
                    sendToPlayer(game.player2, "a", Arrays.asList(
                        currentPlayer.playerId));
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void endGame(Game game) {
        // Send final scores to both players
        List<Integer> finalScores = Arrays.asList(game.player1.percent, game.player2.percent);

        if (this.players.containsKey(game.player1.session.getId())) {
            sendToPlayer(game.player1, "o", finalScores);
        }

        if (this.players.containsKey(game.player2.session.getId())) {
            sendToPlayer(game.player2, "o", finalScores);
        }

        if (game.timerTask != null) {
            game.timerTask.cancel(false);
        }

        games.remove(game.player1.session.getId());
        games.remove(game.player2.session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        players.remove(session.getId());
        waitingPlayers.remove(session);

        Game game = games.remove(session.getId());
        if (game != null) {
            endGame(game);
        }

    }

    private void sendToPlayer(Player player, String type, Object data) {
        try {
            String message = type;
            if (data != null) {
                message += mapper.writeValueAsString(data);
            }
            synchronized (player.session) {
                player.session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}