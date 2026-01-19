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
            ConfigPayload payload = new ConfigPayload();
            payload.discordToken = config.discordToken;
            payload.discordGuildId = config.discordGuildId;
            payload.discordLobbyChannelName = config.discordLobbyChannelName;
            payload.discordTeamChannelFormat = config.discordTeamChannelFormat;

            sendAsync("/init", payload);
        }
    }

    public void stop() {
        sendAsync("/stop", null); // Optional
    }

    public boolean linkPlayer(UUID playerUuid, String discordId) {
        LinkPayload payload = new LinkPayload();
        payload.uuid = playerUuid;
        payload.discordId = discordId;

        // Synchronous for command feedback? Or just fire and forget?
        // The original returned boolean if format was valid.
        // We can do simple check here.
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

        StartRoundPayload payload = new StartRoundPayload();
        payload.teams = teams;

        sendAsync("/round/start", payload);
    }

    public void onRoundEnd() {
        sendAsync("/round/end", null);
    }

    private void sendAsync(String endpoint, Object payload) {
        CompletableFuture.runAsync(() -> {
            try {
                String json = payload != null ? GSON.toJson(payload) : "";
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + endpoint))
                        .header("Content-Type", "application/json");

                if (payload != null) {
                    builder.POST(HttpRequest.BodyPublishers.ofString(json));
                } else {
                    builder.POST(HttpRequest.BodyPublishers.noBody());
                }

                HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 400) {
                    BingoBackpack.LOGGER.error("Discord Service Error {}: {}", response.statusCode(), response.body());
                }
            } catch (Exception e) {
                BingoBackpack.LOGGER.error("Failed to communicate with Discord Service", e);
            }
        });
    }

    private static class ConfigPayload {
        String discordToken;
        String discordGuildId;
        String discordLobbyChannelName;
        String discordTeamChannelFormat;
    }

    private static class LinkPayload {
        UUID uuid;
        String discordId;
    }

    private static class StartRoundPayload {
        Map<String, List<UUID>> teams;
    }
}
