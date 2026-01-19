package de.yoshlix.bingobackpack;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.*;

import me.jfenn.bingo.api.BingoApi;
import me.jfenn.bingo.api.data.BingoGameStatus;

/**
 * Integration with Bingo mods that use Minecraft's scoreboard teams.
 * This will automatically sync scoreboard teams to backpack teams
 * and give players their backpack bundle when they join a team.
 */
public class BingoIntegration {
    private static BingoIntegration instance;

    private MinecraftServer server;
    private boolean enabled = true;
    private boolean backpackGiven = false;

    // Track which players have already received their backpack for their current
    // team
    private final Map<UUID, String> playersWithBackpack = new HashMap<>();

    private int tickCounter = 0;

    public static BingoIntegration getInstance() {
        if (instance == null) {
            instance = new BingoIntegration();
        }
        return instance;
    }

    private BingoIntegration() {
    }

    public void init(MinecraftServer server) {
        this.server = server;
        this.playersWithBackpack.clear();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Called every server tick to check for team changes
     */
    public void tick(MinecraftServer server) {
        if (!enabled || server == null)
            return;

        tickCounter++;
        if (tickCounter < ModConfig.getInstance().bingoCheckIntervalTicks)
            return;
        tickCounter = 0;

        syncScoreboardTeams();

        if (!backpackGiven && BingoApi.getGame().getStatus().equals(BingoGameStatus.PLAYING)) {
            for (PlayerTeam scoPlayerTeam : server.getScoreboard().getPlayerTeams()) {
                for (String name : scoPlayerTeam.getPlayers()) {
                    ServerPlayer player = server.getPlayerList().getPlayerByName(name);
                    if (player != null) {
                        giveBackpackToPlayer(player, scoPlayerTeam.getName());
                        // Give starter kit to player at round start
                        StarterKitManager.getInstance().giveStarterKit(player);
                    }
                }
            }

            if (ModConfig.getInstance().spawnTeleportEnabled) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    SpawnManager.getInstance().setSpawn(player);
                }
            }

            backpackGiven = true;
            // Discord Round Start
            DiscordService.getInstance().onRoundStart();
        }

        if (BingoApi.getGame().getStatus().equals(BingoGameStatus.POSTGAME)) {
            backpackGiven = false;
            // Discord Round End
            DiscordService.getInstance().onRoundEnd();
        }
    }

    /**
     * Sync Minecraft scoreboard teams with backpack teams
     */
    private void syncScoreboardTeams() {
        if (server == null)
            return;

        Scoreboard scoreboard = server.getScoreboard();
        Collection<PlayerTeam> scoreboardTeams = scoreboard.getPlayerTeams();

        // Build a view of scoreboard teams -> player UUIDs (online players only)
        Map<String, Set<UUID>> scoreboardState = new HashMap<>();

        for (PlayerTeam scoreboardTeam : scoreboardTeams) {
            String teamName = scoreboardTeam.getName();
            Set<UUID> members = new HashSet<>();

            for (String playerName : scoreboardTeam.getPlayers()) {
                ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
                if (player != null) {
                    members.add(player.getUUID());
                }
            }

            scoreboardState.put(teamName, members);

            // Ensure backpack team exists
            if (!TeamManager.getInstance().teamExists(teamName)) {
                TeamManager.getInstance().createTeam(teamName);
                BingoBackpack.LOGGER.info("Created backpack team from scoreboard: {}", teamName);
            }

            // Add/move members and give bundles
            for (UUID playerUUID : members) {
                String currentBackpackTeam = TeamManager.getInstance().getPlayerTeam(playerUUID);
                if (!teamName.equals(currentBackpackTeam)) {
                    TeamManager.getInstance().addPlayerToTeam(teamName, playerUUID);
                    BingoBackpack.LOGGER.info("Added {} to backpack team {} from scoreboard", playerUUID, teamName);
                }

                ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
                if (player != null) {
                    String backpackTeam = playersWithBackpack.get(playerUUID);
                    if (!teamName.equals(backpackTeam)) {
                        // giveBackpackToPlayer(player, teamName);
                        playersWithBackpack.put(playerUUID, teamName);
                    }
                }
            }
        }

        // Remove players that are no longer in a scoreboard team
        for (String team : TeamManager.getInstance().getAllTeams()) {
            Set<UUID> expected = scoreboardState.getOrDefault(team, Collections.emptySet());
            Set<UUID> currentMembers = new HashSet<>(TeamManager.getInstance().getTeamMembers(team));
            for (UUID uuid : currentMembers) {
                if (!expected.contains(uuid)) {
                    TeamManager.getInstance().removePlayerFromTeam(team, uuid);
                    playersWithBackpack.remove(uuid);
                }
            }
        }

        // Remove teams that no longer exist on the scoreboard
        Set<String> scoreboardTeamNames = scoreboardState.keySet();
        for (String team : new HashSet<>(TeamManager.getInstance().getAllTeams())) {
            if (!scoreboardTeamNames.contains(team)) {
                TeamManager.getInstance().deleteTeam(team);
                BackpackManager.getInstance().clearBackpack(team);
                playersWithBackpack.entrySet().removeIf(entry -> team.equals(entry.getValue()));
            }
        }
    }

    /**
     * Give a backpack bundle to a player
     */
    private void giveBackpackToPlayer(ServerPlayer player, String teamName) {
        // Check if player already has a backpack bundle for this team
        if (playerHasBackpack(player, teamName)) {
            return;
        }

        // Create the backpack bundle
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.CUSTOM_NAME, Component.literal("§6Team Backpack: §e" + teamName));

        // Add custom NBT to identify it as a backpack item
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putBoolean("bingobackpack_item", true);
        tag.putString("team", teamName);
        bundle.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        // Give item to player
        if (!player.getInventory().add(bundle)) {
            player.drop(bundle, false);
        }

        player.sendSystemMessage(Component.literal("§aYou received your team backpack!"));
    }

    /**
     * Check if a player already has a backpack bundle for a specific team
     */
    private boolean playerHasBackpack(ServerPlayer player, String teamName) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isBackpackBundle(stack, teamName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBackpackBundle(ItemStack stack, String teamName) {
        if (stack.isEmpty() || !stack.is(Items.BUNDLE))
            return false;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            if (tag.contains("bingobackpack_item") && tag.getBoolean("bingobackpack_item").orElse(false)) {
                if (teamName == null)
                    return true;
                return teamName.equals(tag.getString("team").orElse(""));
            }
        }
        return false;
    }

    /**
     * Called when all teams should be reset (e.g., new bingo round)
     */
    public void onBingoReset() {
        playersWithBackpack.clear();
    }

    /**
     * Manually trigger team sync (can be called from command)
     */
    public int manualSync() {
        if (server == null)
            return 0;

        syncScoreboardTeams();
        return TeamManager.getInstance().getAllTeams().size();
    }
}
