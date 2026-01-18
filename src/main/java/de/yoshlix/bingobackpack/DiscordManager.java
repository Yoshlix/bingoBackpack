package de.yoshlix.bingobackpack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class DiscordManager {
    private static DiscordManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String LINKS_FILE = "discord_links.json";
    private static final String CHANNELS_FILE = "discord_channels.json";

    private GatewayDiscordClient client;
    private final Map<UUID, String> playerLinks = new ConcurrentHashMap<>();
    private boolean initialized = false;

    // Cache channel IDs
    private final AtomicReference<Snowflake> lobbyChannelId = new AtomicReference<>();
    private final Map<String, Snowflake> teamChannelIds = new ConcurrentHashMap<>();

    public static DiscordManager getInstance() {
        if (instance == null) {
            instance = new DiscordManager();
        }
        return instance;
    }

    private DiscordManager() {
    }

    public void init(MinecraftServer server) {
        loadLinks();
        loadChannels();

        ModConfig config = ModConfig.getInstance();
        if (config.discordEnabled && !config.discordToken.isEmpty()) {
            new Thread(() -> {
                try {
                    DiscordClient discordClient = DiscordClient.create(config.discordToken);
                    this.client = discordClient.login().block();
                    BingoBackpack.LOGGER.info("Discord Bot logged in as {}", client.getSelf().block().getUsername());
                    this.initialized = true;

                    // Initialize Lobby
                    ensureLobbyChannel();

                    // Initial Move: If we have no active team channels, move everyone to lobby
                    if (teamChannelIds.isEmpty()) {
                        moveAllToLobby();
                    }

                } catch (Exception e) {
                    BingoBackpack.LOGGER.error("Failed to login to Discord", e);
                }
            }, "Bingo-Discord-Login").start();
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
        // Simple validation: is it a number?
        if (!discordId.matches("\\d+")) {
            return false;
        }
        playerLinks.put(playerUuid, discordId);
        saveLinks();
        return true;
    }

    public String getLinkedDiscordId(UUID playerUuid) {
        return playerLinks.get(playerUuid);
    }

    private void loadLinks() {
        File file = FabricLoader.getInstance().getConfigDir().resolve(LINKS_FILE).toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<UUID, String>>() {
                }.getType();
                Map<UUID, String> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    playerLinks.putAll(loaded);
                }
            } catch (IOException e) {
                BingoBackpack.LOGGER.error("Failed to load discord links", e);
            }
        }
    }

    private void saveLinks() {
        File file = FabricLoader.getInstance().getConfigDir().resolve(LINKS_FILE).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(playerLinks, writer);
        } catch (IOException e) {
            BingoBackpack.LOGGER.error("Failed to save discord links", e);
        }
    }

    public void onRoundStart() {
        if (!initialized || client == null)
            return;
        ModConfig config = ModConfig.getInstance();
        if (config.discordGuildId.isEmpty())
            return;

        Snowflake guildId = Snowflake.of(config.discordGuildId);
        Set<String> teamNames = TeamManager.getInstance().getAllTeams();

        new Thread(() -> {
            try {
                Guild guild = client.getGuildById(guildId).block();
                if (guild == null) {
                    BingoBackpack.LOGGER.error("Could not find Discord Guild ID {}", config.discordGuildId);
                    return;
                }

                // Ensure Lobby exists just in case
                ensureLobbyChannel();

                for (String teamName : teamNames) {
                    String channelName = String.format(config.discordTeamChannelFormat, teamName);
                    VoiceChannel teamChannel = null;

                    // Check if we already have a valid channel ID for this team
                    if (teamChannelIds.containsKey(teamName)) {
                        try {
                            teamChannel = guild.getChannelById(teamChannelIds.get(teamName))
                                    .ofType(VoiceChannel.class)
                                    .block();
                        } catch (Exception ignored) {
                            // Channel might have been deleted manually
                        }
                    }

                    // If not found by ID, look by name or create
                    if (teamChannel == null) {
                        // Look by name
                        teamChannel = guild.getChannels()
                                .ofType(VoiceChannel.class)
                                .filter(c -> c.getName().equalsIgnoreCase(channelName))
                                .next()
                                .block();

                        // Create if not found
                        if (teamChannel == null) {
                            teamChannel = guild.createVoiceChannel(spec -> spec.setName(channelName)).block();
                        }
                    }

                    if (teamChannel != null) {
                        teamChannelIds.put(teamName, teamChannel.getId());

                        // Move players in this team
                        Set<UUID> members = TeamManager.getInstance().getTeamMembers(teamName);
                        for (UUID memberUuid : members) {
                            movePlayerToChannel(memberUuid, guild, teamChannel);
                        }
                    }
                }
                saveChannels();

            } catch (Exception e) {
                BingoBackpack.LOGGER.error("Error processing Discord round start", e);
            }
        }).start();
    }

    public void onRoundEnd() {
        if (!initialized || client == null)
            return;

        // Clear stored team channels as game ended
        // But we need them one last time to deleted them?
        // Logic: Move everyone to Lobby -> Delete Team Channels -> Clear Map -> Save

        new Thread(() -> {
            try {
                ensureLobbyChannel();
                moveAllToLobby();

                ModConfig config = ModConfig.getInstance();
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
                BingoBackpack.LOGGER.error("Error processing Discord round end", e);
            }
        }).start();
    }

    private void ensureLobbyChannel() {
        if (client == null)
            return;
        ModConfig config = ModConfig.getInstance();
        if (config.discordGuildId.isEmpty())
            return;

        try {
            Snowflake guildId = Snowflake.of(config.discordGuildId);
            Guild guild = client.getGuildById(guildId).block();
            if (guild == null)
                return;

            VoiceChannel lobby = null;

            // 1. Try ID
            if (lobbyChannelId.get() != null) {
                try {
                    lobby = guild.getChannelById(lobbyChannelId.get())
                            .ofType(VoiceChannel.class)
                            .block();
                } catch (Exception ignored) {
                }
            }

            // 2. Try Name
            if (lobby == null) {
                String lname = config.discordLobbyChannelName;
                lobby = guild.getChannels()
                        .ofType(VoiceChannel.class)
                        .filter(c -> c.getName().equalsIgnoreCase(lname))
                        .next()
                        .block();
            }

            // 3. Create
            if (lobby == null) {
                lobby = guild.createVoiceChannel(spec -> spec.setName(config.discordLobbyChannelName)).block();
            }

            if (lobby != null) {
                lobbyChannelId.set(lobby.getId());
                saveChannels();
            }

        } catch (Exception e) {
            BingoBackpack.LOGGER.error("Failed to ensure lobby channel", e);
        }
    }

    private void moveAllToLobby() {
        if (client == null)
            return;
        ModConfig config = ModConfig.getInstance();
        if (config.discordGuildId.isEmpty())
            return;

        try {
            if (lobbyChannelId.get() == null) {
                ensureLobbyChannel(); // Try again
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
            BingoBackpack.LOGGER.error("Failed to move all to lobby", e);
        }
    }

    private static class ChannelData {
        String lobbyId;
        Map<String, String> teamChannels = new HashMap<>();
    }

    private void loadChannels() {
        File file = FabricLoader.getInstance().getConfigDir().resolve(CHANNELS_FILE).toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                ChannelData data = GSON.fromJson(reader, ChannelData.class);
                if (data != null) {
                    if (data.lobbyId != null) {
                        lobbyChannelId.set(Snowflake.of(data.lobbyId));
                    }
                    if (data.teamChannels != null) {
                        for (Map.Entry<String, String> entry : data.teamChannels.entrySet()) {
                            teamChannelIds.put(entry.getKey(), Snowflake.of(entry.getValue()));
                        }
                    }
                }
            } catch (IOException e) {
                BingoBackpack.LOGGER.error("Failed to load discord channels", e);
            }
        }
    }

    private void saveChannels() {
        ChannelData data = new ChannelData();
        if (lobbyChannelId.get() != null) {
            data.lobbyId = lobbyChannelId.get().asString();
        }
        for (Map.Entry<String, Snowflake> entry : teamChannelIds.entrySet()) {
            data.teamChannels.put(entry.getKey(), entry.getValue().asString());
        }

        File file = FabricLoader.getInstance().getConfigDir().resolve(CHANNELS_FILE).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            BingoBackpack.LOGGER.error("Failed to save discord channels", e);
        }
    }

    private void movePlayerToChannel(UUID playerUuid, Guild guild, VoiceChannel channel) {
        String discordIdStr = playerLinks.get(playerUuid);
        if (discordIdStr == null) {
            BingoBackpack.LOGGER.debug("No Discord link found for player {}", playerUuid);
            return;
        }

        try {
            Snowflake memberId = Snowflake.of(discordIdStr);
            Member member = guild.getMemberById(memberId).block();
            if (member == null) {
                BingoBackpack.LOGGER.warn("Discord member {} not found in guild", discordIdStr);
                return;
            }

            // Check if user is currently in a voice channel - Discord only allows moving
            // connected users
            var voiceStateOpt = member.getVoiceState().blockOptional();
            if (voiceStateOpt.isEmpty() || voiceStateOpt.get().getChannelId().isEmpty()) {
                // BingoBackpack.LOGGER.info("Discord member {} is not in a voice channel,
                // cannot move", discordIdStr);
                return;
            }

            // Don't move if already in target channel
            if (voiceStateOpt.get().getChannelId().get().equals(channel.getId())) {
                return;
            }

            BingoBackpack.LOGGER.info("Moving Discord member {} to channel {}", discordIdStr, channel.getName());
            member.edit(spec -> spec.setNewVoiceChannel(channel.getId()))
                    .doOnSuccess(v -> BingoBackpack.LOGGER.info("Successfully moved member {} to {}", discordIdStr,
                            channel.getName()))
                    .doOnError(error -> BingoBackpack.LOGGER.error("Failed to move member {}: {}", discordIdStr,
                            error.getMessage()))
                    .block();
        } catch (Exception e) {
            BingoBackpack.LOGGER.error("Error moving player {} (Discord: {}): {}", playerUuid, discordIdStr,
                    e.getMessage());
        }
    }
}
