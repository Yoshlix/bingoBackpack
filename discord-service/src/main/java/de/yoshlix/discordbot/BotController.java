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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                this.client = discordClient.login().block();
                LOGGER.info("Discord Bot logged in as {}", client.getSelf().block().getUsername());

                // Initialize Lobby
                ensureLobbyChannel();

                // Initial Move: If we have no active team channels, move everyone to lobby
                if (teamChannelIds.isEmpty()) {
                    moveAllToLobby();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to login to Discord", e);
            }
        }
    }

    public void stop() {
        if (client != null) {
            client.logout().block();
            client = null;
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
        if (client == null || config == null || config.discordGuildId == null)
            return;

        Snowflake guildId = Snowflake.of(config.discordGuildId);

        new Thread(() -> {
            try {
                Guild guild = client.getGuildById(guildId).block();
                if (guild == null) {
                    LOGGER.error("Could not find Discord Guild ID {}", config.discordGuildId);
                    return;
                }

                ensureLobbyChannel();

                for (Map.Entry<String, List<UUID>> entry : teams.entrySet()) {
                    String teamName = entry.getKey();
                    List<UUID> members = entry.getValue();
                    String channelName = String.format(config.discordTeamChannelFormat, teamName);
                    VoiceChannel teamChannel = null;

                    if (teamChannelIds.containsKey(teamName)) {
                        try {
                            teamChannel = guild.getChannelById(teamChannelIds.get(teamName))
                                    .ofType(VoiceChannel.class)
                                    .block();
                        } catch (Exception ignored) {
                        }
                    }

                    if (teamChannel == null) {
                        VoiceChannel finalTeamChannel = guild.getChannels()
                                .ofType(VoiceChannel.class)
                                .filter(c -> c.getName().equalsIgnoreCase(channelName))
                                .next()
                                .block();

                        teamChannel = finalTeamChannel;

                        if (teamChannel == null) {
                            teamChannel = guild.createVoiceChannel(spec -> spec.setName(channelName)).block();
                        }
                    }

                    if (teamChannel != null) {
                        teamChannelIds.put(teamName, teamChannel.getId());
                        for (UUID memberUuid : members) {
                            movePlayerToChannel(memberUuid, guild, teamChannel);
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
        if (client == null || config == null)
            return;

        new Thread(() -> {
            try {
                ensureLobbyChannel();
                moveAllToLobby();

                Snowflake guildId = Snowflake.of(config.discordGuildId);
                Guild guild = client.getGuildById(guildId).block();

                if (guild != null) {
                    for (Snowflake channelId : teamChannelIds.values()) {
                        try {
                            guild.getChannelById(channelId)
                                    .blockOptional()
                                    .ifPresent(channel -> channel.delete().subscribe());
                        } catch (Exception ignored) {
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
        if (client == null || config == null || config.discordGuildId == null)
            return;

        try {
            Snowflake guildId = Snowflake.of(config.discordGuildId);
            Guild guild = client.getGuildById(guildId).block();
            if (guild == null)
                return;

            VoiceChannel lobby = null;
            if (lobbyChannelId.get() != null) {
                try {
                    lobby = guild.getChannelById(lobbyChannelId.get())
                            .ofType(VoiceChannel.class)
                            .block();
                } catch (Exception ignored) {
                }
            }

            if (lobby == null) {
                String lname = config.discordLobbyChannelName;
                lobby = guild.getChannels()
                        .ofType(VoiceChannel.class)
                        .filter(c -> c.getName().equalsIgnoreCase(lname))
                        .next()
                        .block();
            }

            if (lobby == null) {
                VoiceChannel finalLobby = lobby;
                lobby = guild.createVoiceChannel(spec -> spec.setName(config.discordLobbyChannelName)).block();
            }

            if (lobby != null) {
                lobbyChannelId.set(lobby.getId());
                saveChannels();
            }

        } catch (Exception e) {
            LOGGER.error("Failed to ensure lobby channel", e);
        }
    }

    private void moveAllToLobby() {
        if (client == null || config == null || config.discordGuildId == null)
            return;

        try {
            if (lobbyChannelId.get() == null) {
                ensureLobbyChannel();
                if (lobbyChannelId.get() == null)
                    return;
            }

            Snowflake guildId = Snowflake.of(config.discordGuildId);
            Guild guild = client.getGuildById(guildId).block();
            VoiceChannel lobby = guild.getChannelById(lobbyChannelId.get()).ofType(VoiceChannel.class).block();

            if (guild != null && lobby != null) {
                for (UUID storedUuid : playerLinks.keySet()) {
                    movePlayerToChannel(storedUuid, guild, lobby);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to move all to lobby", e);
        }
    }

    private void movePlayerToChannel(UUID playerUuid, Guild guild, VoiceChannel channel) {
        String discordIdStr = playerLinks.get(playerUuid);
        if (discordIdStr == null || client == null)
            return;

        try {
            Snowflake memberId = Snowflake.of(discordIdStr);
            Member member = guild.getMemberById(memberId).block();
            if (member == null)
                return;

            var voiceStateOpt = member.getVoiceState().blockOptional();
            if (voiceStateOpt.isEmpty() || voiceStateOpt.get().getChannelId().isEmpty())
                return;

            if (voiceStateOpt.get().getChannelId().get().equals(channel.getId()))
                return;

            LOGGER.info("Moving Discord member {} to channel {}", discordIdStr, channel.getName());
            member.edit(spec -> spec.setNewVoiceChannel(channel.getId())).subscribe();
        } catch (Exception e) {
            LOGGER.error("Error moving player {}", playerUuid, e);
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
