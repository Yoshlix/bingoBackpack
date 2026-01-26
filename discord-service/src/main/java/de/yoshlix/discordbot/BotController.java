package de.yoshlix.discordbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class BotController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotController.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String LINKS_FILE = "discord_links.json";
    private static final String CHANNELS_FILE = "discord_channels.json";

    private GatewayDiscordClient client;
    private final Map<UUID, String> playerLinks = new ConcurrentHashMap<>();
    private final AtomicReference<Snowflake> lobbyChannelId = new AtomicReference<>();
    private final Map<String, Snowflake> teamChannelIds = new ConcurrentHashMap<>();

    private ConfigData config;

    public BotController() {
        loadLinks();
        loadChannels();
    }

    public void init(ConfigData config) {
        this.config = config;
        if (config.discordToken != null && !config.discordToken.isEmpty()) {
            try {
                DiscordClient discordClient = DiscordClient.create(config.discordToken);
                this.client = discordClient.login()
                        .timeout(Duration.ofSeconds(30))
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                                .doBeforeRetry(retrySignal -> LOGGER.warn("Retrying Discord login, attempt {}", retrySignal.totalRetries() + 1)))
                        .block();
                
                if (this.client == null) {
                    LOGGER.error("Failed to login to Discord: client is null");
                    return;
                }
                
                try {
                    var self = this.client.getSelf().timeout(Duration.ofSeconds(10)).block();
                    if (self != null) {
                        LOGGER.info("Discord Bot logged in as {}", self.getUsername());
                    } else {
                        LOGGER.warn("Discord Bot logged in but could not retrieve self information");
                    }
                } catch (Exception e) {
                    LOGGER.warn("Could not retrieve Discord bot username", e);
                }

                // Initialize Lobby
                ensureLobbyChannel();

                // Initial Move: If we have no active team channels, move everyone to lobby
                if (teamChannelIds.isEmpty()) {
                    moveAllToLobby();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to login to Discord", e);
                this.client = null;
            }
        }
    }

    public void stop() {
        if (client != null) {
            try {
                client.logout()
                        .timeout(Duration.ofSeconds(10))
                        .block();
            } catch (Exception e) {
                LOGGER.error("Error during Discord logout", e);
            } finally {
                client = null;
            }
        }
        saveLinks();
        saveChannels();
    }

    public boolean linkPlayer(UUID playerUuid, String discordId) {
        if (!discordId.matches("\\d+")) {
            return false;
        }
        playerLinks.put(playerUuid, discordId);
        saveLinks();
        return true;
    }

    public void onRoundStart(Map<String, List<UUID>> teams) {
        if (client == null || config == null || config.discordGuildId == null) {
            LOGGER.warn("Cannot process round start: client={}, config={}, guildId={}", 
                    client != null, config != null, config != null ? config.discordGuildId : null);
            return;
        }

        if (teams == null || teams.isEmpty()) {
            LOGGER.warn("Round start called with empty teams map");
            return;
        }

        Snowflake guildId;
        try {
            guildId = Snowflake.of(config.discordGuildId);
        } catch (Exception e) {
            LOGGER.error("Invalid Discord Guild ID: {}", config.discordGuildId, e);
            return;
        }

        new Thread(() -> {
            try {
                Guild guild = client.getGuildById(guildId)
                        .timeout(Duration.ofSeconds(15))
                        .block();
                if (guild == null) {
                    LOGGER.error("Could not find Discord Guild ID {}", config.discordGuildId);
                    return;
                }

                ensureLobbyChannel();

                for (Map.Entry<String, List<UUID>> entry : teams.entrySet()) {
                    String teamName = entry.getKey();
                    List<UUID> members = entry.getValue();
                    
                    if (teamName == null || teamName.isEmpty()) {
                        LOGGER.warn("Skipping team with null or empty name");
                        continue;
                    }
                    
                    if (members == null || members.isEmpty()) {
                        LOGGER.debug("Team {} has no members, skipping", teamName);
                        continue;
                    }
                    
                    String channelName = String.format(config.discordTeamChannelFormat, teamName);
                    VoiceChannel teamChannel = null;

                    // Try to find existing channel by ID
                    if (teamChannelIds.containsKey(teamName)) {
                        try {
                            teamChannel = guild.getChannelById(teamChannelIds.get(teamName))
                                    .ofType(VoiceChannel.class)
                                    .timeout(Duration.ofSeconds(10))
                                    .block();
                        } catch (Exception e) {
                            LOGGER.debug("Could not find team channel by ID for team {}, will search by name", teamName, e);
                        }
                    }

                    // Try to find existing channel by name
                    if (teamChannel == null) {
                        try {
                            teamChannel = guild.getChannels()
                                    .ofType(VoiceChannel.class)
                                    .filter(c -> c.getName().equalsIgnoreCase(channelName))
                                    .next()
                                    .timeout(Duration.ofSeconds(10))
                                    .block();
                        } catch (Exception e) {
                            LOGGER.debug("Could not find team channel by name: {}", channelName, e);
                        }
                    }

                    // Create new channel if not found
                    if (teamChannel == null) {
                        try {
                            teamChannel = guild.createVoiceChannel(spec -> spec.setName(channelName))
                                    .timeout(Duration.ofSeconds(15))
                                    .block();
                            LOGGER.info("Created new team voice channel: {}", channelName);
                        } catch (Exception e) {
                            LOGGER.error("Failed to create team voice channel: {}", channelName, e);
                            continue;
                        }
                    }

                    if (teamChannel != null) {
                        teamChannelIds.put(teamName, teamChannel.getId());
                        for (UUID memberUuid : members) {
                            if (memberUuid != null) {
                                movePlayerToChannel(memberUuid, guild, teamChannel);
                            }
                        }
                    }
                }
                saveChannels();

            } catch (Exception e) {
                LOGGER.error("Error processing Discord round start", e);
            }
        }).start();
    }

    public void onRoundEnd() {
        if (client == null || config == null || config.discordGuildId == null) {
            LOGGER.warn("Cannot process round end: client={}, config={}", client != null, config != null);
            return;
        }

        new Thread(() -> {
            try {
                ensureLobbyChannel();
                moveAllToLobby();

                Snowflake guildId;
                try {
                    guildId = Snowflake.of(config.discordGuildId);
                } catch (Exception e) {
                    LOGGER.error("Invalid Discord Guild ID: {}", config.discordGuildId, e);
                    teamChannelIds.clear();
                    saveChannels();
                    return;
                }
                
                Guild guild = client.getGuildById(guildId)
                        .timeout(Duration.ofSeconds(15))
                        .block();

                if (guild != null) {
                    // Create a copy of channel IDs to avoid concurrent modification
                    List<Snowflake> channelIdsToDelete = new ArrayList<>(teamChannelIds.values());
                    for (Snowflake channelId : channelIdsToDelete) {
                        try {
                            guild.getChannelById(channelId)
                                    .timeout(Duration.ofSeconds(10))
                                    .blockOptional()
                                    .ifPresent(channel -> {
                                        try {
                                            channel.delete()
                                                    .timeout(Duration.ofSeconds(10))
                                                    .subscribe(
                                                            null,
                                                            error -> LOGGER.warn("Failed to delete team channel {}", channelId, error)
                                                    );
                                        } catch (Exception e) {
                                            LOGGER.warn("Error deleting team channel {}", channelId, e);
                                        }
                                    });
                        } catch (Exception e) {
                            LOGGER.debug("Could not delete team channel {}", channelId, e);
                        }
                    }
                }

                teamChannelIds.clear();
                saveChannels();

            } catch (Exception e) {
                LOGGER.error("Error processing Discord round end", e);
            }
        }).start();
    }

    private void ensureLobbyChannel() {
        if (client == null || config == null || config.discordGuildId == null) {
            LOGGER.debug("Cannot ensure lobby channel: missing client or config");
            return;
        }

        try {
            Snowflake guildId = Snowflake.of(config.discordGuildId);
            Guild guild = client.getGuildById(guildId)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            if (guild == null) {
                LOGGER.warn("Could not find guild for lobby channel");
                return;
            }

            VoiceChannel lobby = null;
            
            // Try to find lobby by stored ID
            if (lobbyChannelId.get() != null) {
                try {
                    lobby = guild.getChannelById(lobbyChannelId.get())
                            .ofType(VoiceChannel.class)
                            .timeout(Duration.ofSeconds(10))
                            .block();
                } catch (Exception e) {
                    LOGGER.debug("Could not find lobby channel by ID, will search by name", e);
                }
            }

            // Try to find lobby by name
            if (lobby == null && config.discordLobbyChannelName != null && !config.discordLobbyChannelName.isEmpty()) {
                try {
                    lobby = guild.getChannels()
                            .ofType(VoiceChannel.class)
                            .filter(c -> c.getName().equalsIgnoreCase(config.discordLobbyChannelName))
                            .next()
                            .timeout(Duration.ofSeconds(10))
                            .block();
                } catch (Exception e) {
                    LOGGER.debug("Could not find lobby channel by name: {}", config.discordLobbyChannelName, e);
                }
            }

            // Create lobby if not found
            if (lobby == null && config.discordLobbyChannelName != null && !config.discordLobbyChannelName.isEmpty()) {
                try {
                    lobby = guild.createVoiceChannel(spec -> spec.setName(config.discordLobbyChannelName))
                            .timeout(Duration.ofSeconds(15))
                            .block();
                    LOGGER.info("Created new lobby voice channel: {}", config.discordLobbyChannelName);
                } catch (Exception e) {
                    LOGGER.error("Failed to create lobby voice channel: {}", config.discordLobbyChannelName, e);
                }
            }

            if (lobby != null) {
                lobbyChannelId.set(lobby.getId());
                saveChannels();
            } else {
                LOGGER.error("Could not find or create lobby channel");
            }

        } catch (Exception e) {
            LOGGER.error("Failed to ensure lobby channel", e);
        }
    }

    private void moveAllToLobby() {
        if (client == null || config == null || config.discordGuildId == null) {
            LOGGER.debug("Cannot move all to lobby: missing client or config");
            return;
        }

        try {
            if (lobbyChannelId.get() == null) {
                ensureLobbyChannel();
                if (lobbyChannelId.get() == null) {
                    LOGGER.warn("Could not ensure lobby channel, cannot move players");
                    return;
                }
            }

            Snowflake guildId = Snowflake.of(config.discordGuildId);
            Guild guild = client.getGuildById(guildId)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            
            if (guild == null) {
                LOGGER.warn("Could not find guild to move players to lobby");
                return;
            }
            
            VoiceChannel lobby = guild.getChannelById(lobbyChannelId.get())
                    .ofType(VoiceChannel.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (lobby != null) {
                // Create a copy to avoid concurrent modification
                List<UUID> playerUuids = new ArrayList<>(playerLinks.keySet());
                for (UUID storedUuid : playerUuids) {
                    if (storedUuid != null) {
                        movePlayerToChannel(storedUuid, guild, lobby);
                    }
                }
            } else {
                LOGGER.warn("Lobby channel not found, cannot move players");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to move all to lobby", e);
        }
    }

    private void movePlayerToChannel(UUID playerUuid, Guild guild, VoiceChannel channel) {
        if (playerUuid == null || guild == null || channel == null || client == null) {
            LOGGER.debug("Cannot move player: invalid parameters");
            return;
        }
        
        String discordIdStr = playerLinks.get(playerUuid);
        if (discordIdStr == null || discordIdStr.isEmpty()) {
            LOGGER.debug("No Discord ID linked for player {}", playerUuid);
            return;
        }

        try {
            Snowflake memberId = Snowflake.of(discordIdStr);
            Member member = guild.getMemberById(memberId)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (member == null) {
                LOGGER.debug("Discord member {} not found in guild", discordIdStr);
                return;
            }

            // Prüfe, ob der Nutzer mit Voice verbunden ist
            var voiceStateOpt = member.getVoiceState()
                    .timeout(Duration.ofSeconds(5))
                    .blockOptional();
            if (voiceStateOpt.isEmpty() || voiceStateOpt.get().getChannelId().isEmpty()) {
                LOGGER.debug("Discord member {} is not connected to voice, skipping move.", discordIdStr);
                return;
            }

            // Skip if already in target channel
            if (voiceStateOpt.get().getChannelId().get().equals(channel.getId())) {
                LOGGER.debug("Discord member {} already in target channel {}", discordIdStr, channel.getName());
                return;
            }

            LOGGER.info("Moving Discord member {} to channel {}", discordIdStr, channel.getName());
            member.edit(spec -> spec.setNewVoiceChannel(channel.getId()))
                    .timeout(Duration.ofSeconds(10))
                    .subscribe(
                            null,
                            error -> LOGGER.warn("Failed to move Discord member {} to channel {}", discordIdStr, channel.getName(), error)
                    );
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid Discord ID format for player {}: {}", playerUuid, discordIdStr, e);
        } catch (Exception e) {
            LOGGER.error("Error moving player {} (Discord ID: {})", playerUuid, discordIdStr, e);
        }
    }

    private void loadLinks() {
        File file = new File(LINKS_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<UUID, String>>() {
                }.getType();
                Map<UUID, String> loaded = GSON.fromJson(reader, type);
                if (loaded != null)
                    playerLinks.putAll(loaded);
            } catch (IOException e) {
                LOGGER.error("Failed to load discord links", e);
            }
        }
    }

    private void saveLinks() {
        try (FileWriter writer = new FileWriter(LINKS_FILE)) {
            GSON.toJson(playerLinks, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save discord links", e);
        }
    }

    private void loadChannels() {
        File file = new File(CHANNELS_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                ChannelData data = GSON.fromJson(reader, ChannelData.class);
                if (data != null) {
                    if (data.lobbyId != null)
                        lobbyChannelId.set(Snowflake.of(data.lobbyId));
                    if (data.teamChannels != null) {
                        for (Map.Entry<String, String> entry : data.teamChannels.entrySet()) {
                            teamChannelIds.put(entry.getKey(), Snowflake.of(entry.getValue()));
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load discord channels", e);
            }
        }
    }

    private void saveChannels() {
        ChannelData data = new ChannelData();
        if (lobbyChannelId.get() != null)
            data.lobbyId = lobbyChannelId.get().asString();
        for (Map.Entry<String, Snowflake> entry : teamChannelIds.entrySet()) {
            data.teamChannels.put(entry.getKey(), entry.getValue().asString());
        }
        try (FileWriter writer = new FileWriter(CHANNELS_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save discord channels", e);
        }
    }

    private static class ChannelData {
        String lobbyId;
        Map<String, String> teamChannels = new HashMap<>();
    }

    public static class ConfigData {
        public String discordToken;
        public String discordGuildId;
        public String discordLobbyChannelName;
        public String discordTeamChannelFormat;
    }
}
