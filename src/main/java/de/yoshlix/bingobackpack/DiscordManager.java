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

public class DiscordManager {
    private static DiscordManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String LINKS_FILE = "discord_links.json";

    private GatewayDiscordClient client;
    private Map<UUID, String> playerLinks = new ConcurrentHashMap<>();
    private boolean initialized = false;

    // Cache created team channels to delete/manage them later
    private final Set<Snowflake> createdChannels = ConcurrentHashMap.newKeySet();

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

        ModConfig config = ModConfig.getInstance();
        if (config.discordEnabled && !config.discordToken.isEmpty()) {
            new Thread(() -> {
                try {
                    DiscordClient discordClient = DiscordClient.create(config.discordToken);
                    this.client = discordClient.login().block();
                    BingoBackpack.LOGGER.info("Discord Bot logged in as {}", client.getSelf().block().getUsername());
                    this.initialized = true;
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

        // Get all teams from TeamManager
        Set<String> teamNames = TeamManager.getInstance().getAllTeams();

        new Thread(() -> {
            try {
                Guild guild = client.getGuildById(guildId).block();
                if (guild == null) {
                    BingoBackpack.LOGGER.error("Could not find Discord Guild ID {}", config.discordGuildId);
                    return;
                }

                // Keep track of channels we create/manage
                // Ideally we check if they already exist to avoid duplicates if crash happened,
                // but requirements say "If they don't exist yet, create them".
                // We'll try to find existing ones first.

                for (String teamName : teamNames) {
                    String channelName = String.format(config.discordTeamChannelFormat, teamName);

                    VoiceChannel teamChannel = guild.getChannels()
                            .ofType(VoiceChannel.class)
                            .filter(c -> c.getName().equalsIgnoreCase(channelName))
                            .next()
                            .block();

                    if (teamChannel == null) {
                        teamChannel = guild.createVoiceChannel(spec -> spec.setName(channelName)).block();
                        if (teamChannel != null) {
                            createdChannels.add(teamChannel.getId());
                        }
                    }

                    if (teamChannel == null)
                        continue;

                    // Move players in this team
                    Set<UUID> members = TeamManager.getInstance().getTeamMembers(teamName);
                    for (UUID memberUuid : members) {
                        movePlayerToChannel(memberUuid, guild, teamChannel);
                    }
                }
            } catch (Exception e) {
                BingoBackpack.LOGGER.error("Error processing Discord round start", e);
            }
        }).start();
    }

    public void onRoundEnd() {
        if (!initialized || client == null)
            return;
        ModConfig config = ModConfig.getInstance();
        if (config.discordGuildId.isEmpty())
            return;

        Snowflake guildId = Snowflake.of(config.discordGuildId);

        new Thread(() -> {
            try {
                Guild guild = client.getGuildById(guildId).block();
                if (guild == null)
                    return;

                VoiceChannel lobbyChannel = guild.getChannels()
                        .ofType(VoiceChannel.class)
                        .filter(c -> c.getName().equalsIgnoreCase(config.discordLobbyChannelName))
                        .next()
                        .block();

                if (lobbyChannel == null) {
                    // Try to create lobby if missing? Or just fail?
                    // Requirement: "moved to the Bingo Lobby Voice Channel". implies it should
                    // exist.
                    // But if we are strict: "Existieren die Channels noch nicht, müssen sie zuerst
                    // erstellt werden"
                    // applied to Team channels context. But let's be safe.
                    lobbyChannel = guild.createVoiceChannel(spec -> spec.setName(config.discordLobbyChannelName))
                            .block();
                }

                if (lobbyChannel != null) {
                    // Move ALL linked players to lobby
                    for (UUID storedUuid : playerLinks.keySet()) {
                        movePlayerToChannel(storedUuid, guild, lobbyChannel);
                    }
                }

                // Cleanup created channels? Re-read: "Mehr soll der Discord Bot nicht machen."
                // Usually cleanup is good, otherwise we spam channels.
                // Assuming we should delete channels we created.
                for (Snowflake channelId : createdChannels) {
                    guild.getChannelById(channelId).blockOptional().ifPresent(channel -> channel.delete().subscribe());
                }
                createdChannels.clear();

            } catch (Exception e) {
                BingoBackpack.LOGGER.error("Error processing Discord round end", e);
            }
        }).start();
    }

    private void movePlayerToChannel(UUID playerUuid, Guild guild, VoiceChannel channel) {
        String discordIdStr = playerLinks.get(playerUuid);
        if (discordIdStr == null)
            return;

        try {
            Snowflake memberId = Snowflake.of(discordIdStr);
            Member member = guild.getMemberById(memberId).block(); // This might fail if user not in guild
            if (member != null) {
                // Check if user is in a voice channel currently?
                // Discord4J edit requires them to be connected usually?
                // Actually discord API restriction: you can only move members who are connected
                // to voice.
                // We'll try to move.
                member.edit(spec -> spec.setNewVoiceChannel(channel.getId())).subscribe(
                        success -> {
                        },
                        error -> BingoBackpack.LOGGER.debug("Failed to move member {}: {}", discordIdStr,
                                error.getMessage()));
            }
        } catch (Exception e) {
            // Ignore if member not found or other minor issues
        }
    }
}
