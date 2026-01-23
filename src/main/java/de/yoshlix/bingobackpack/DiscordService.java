package de.yoshlix.bingobackpack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DiscordService {
    private static DiscordService instance;
    private static final Gson GSON = new GsonBuilder().create();
    private static final String API_URL = "http://localhost:8081/api/v1";
    private final HttpClient httpClient;

    public static DiscordService getInstance() {
        if (instance == null) {
            instance = new DiscordService();
        }
        return instance;
    }

    private DiscordService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public void init(MinecraftServer server) {
        ModConfig config = ModConfig.getInstance();
        if (config.discordEnabled && !config.discordToken.isEmpty()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("discordToken", config.discordToken);
            payload.put("discordGuildId", config.discordGuildId);
            payload.put("discordLobbyChannelName", config.discordLobbyChannelName);
            payload.put("discordTeamChannelFormat", config.discordTeamChannelFormat);
            sendAsync("/init", payload);
        }
    }

    public void stop() {
        sendAsync("/stop", null); // Optional
    }

    public boolean linkPlayer(UUID playerUuid, String discordId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("uuid", playerUuid);
        payload.put("discordId", discordId);
        if (!discordId.matches("\\d+"))
            return false;
        sendAsync("/link", payload);
        return true;
    }

    public void onRoundStart() {
        Map<String, List<UUID>> teams = new HashMap<>();
        Set<String> teamNames = TeamManager.getInstance().getAllTeams();
        for (String teamName : teamNames) {
            Set<UUID> members = TeamManager.getInstance().getTeamMembers(teamName);
            teams.put(teamName, new ArrayList<>(members));
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("teams", teams);
        sendAsync("/round/start", payload);
    }

    public void onRoundEnd() {
        sendAsync("/round/end", null);
    }

    private void sendAsync(String endpoint, Object payload) {
        CompletableFuture.runAsync(() -> {
            int maxRetries = 3;
            int attempt = 0;
            long backoff = 1000L; // 1 Sekunde
            boolean success = false;
            Exception lastException = null;
            while (attempt < maxRetries && !success) {
                attempt++;
                try {
                    String json = payload != null ? GSON.toJson(payload) : "";
                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                            .uri(URI.create(API_URL + endpoint))
                            .header("Content-Type", "application/json")
                            .timeout(java.time.Duration.ofSeconds(5));

                    if (payload != null) {
                        builder.POST(HttpRequest.BodyPublishers.ofString(json));
                    } else {
                        builder.POST(HttpRequest.BodyPublishers.noBody());
                    }

                    HttpResponse<String> response = httpClient.send(builder.build(),
                            HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() >= 400) {
                        BingoBackpack.LOGGER.error("Discord Service Error {}: {} (Attempt {}/{})",
                                response.statusCode(), response.body(), attempt, maxRetries);
                        lastException = new RuntimeException("HTTP Status: " + response.statusCode());
                        Thread.sleep(backoff * attempt); // Exponentielles Backoff
                    } else {
                        if (attempt > 1) {
                            BingoBackpack.LOGGER.info("Discord Service call {} erfolgreich nach {} Versuchen.",
                                    endpoint, attempt);
                        }
                        success = true;
                    }
                } catch (Exception e) {
                    lastException = e;
                    BingoBackpack.LOGGER.error("Fehler bei Discord Service Call ({}), Versuch {}/{}", endpoint, attempt,
                            maxRetries, e);
                    try {
                        Thread.sleep(backoff * attempt);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            if (!success) {
                BingoBackpack.LOGGER.error(
                        "Discord Service Call {} fehlgeschlagen nach {} Versuchen! Letzter Fehler: {}", endpoint,
                        maxRetries, lastException != null ? lastException.getMessage() : "unbekannt");
            }
        });
    }

    // Payload-Klassen entfernt, da sie nicht benötigt werden
}
