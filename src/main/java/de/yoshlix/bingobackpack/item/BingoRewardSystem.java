package de.yoshlix.bingobackpack.item;

import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.BingoBackpack;
import de.yoshlix.bingobackpack.ModConfig;
import me.jfenn.bingo.api.data.BingoGameStatus;
import me.jfenn.bingo.api.ext.ICardEntryView;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Manages the reward system for Bingo Items.
 * 
 * Rewards are given in the following situations:
 * 1. When a player completes a bingo row -> All team members get a weighted
 * random item, with the best LEGENDARY odds of any reward path (15%)
 * 2. Every 30 seconds -> Small chance for a random player to receive any item
 * 3. When a player completes a single task -> Chance for item based on task
 * difficulty
 */
public class BingoRewardSystem {
    private static BingoRewardSystem instance;

    private final Random random = new Random();
    private MinecraftServer server;

    // Tracking
    private int tickCounter = 0;

    // Track completed lines per team to detect new completions
    private final Map<String, Integer> teamCompletedLines = new HashMap<>();

    // Track completed objectives to detect new completions
    private final Map<String, Set<String>> teamCompletedObjectives = new HashMap<>();

    // Track objective count for milestone rewards
    private final Map<String, Integer> teamObjectiveCount = new HashMap<>();

    public static BingoRewardSystem getInstance() {
        if (instance == null) {
            instance = new BingoRewardSystem();
        }
        return instance;
    }

    private BingoRewardSystem() {
    }

    public void init(MinecraftServer server) {
        this.server = server;
        reset();
    }

    public void reset() {
        teamCompletedLines.clear();
        teamCompletedObjectives.clear();
        teamObjectiveCount.clear();
        tickCounter = 0;
    }

    /**
     * Called every server tick to check for rewards.
     */
    public void tick(MinecraftServer server) {
        if (server == null)
            return;

        var game = BingoBridge.game();
        if (game == null || !game.getStatus().equals(BingoGameStatus.PLAYING)) {
            return;
        }

        tickCounter++;

        // Check for random gifts every configured interval
        if (tickCounter >= ModConfig.getInstance().randomGiftIntervalTicks) {
            tickCounter = 0;
            checkRandomGift(server);
        }

        // Check for row completions and task completions
        checkBingoProgress(server);
    }

    /**
     * Random gift system - every 30 seconds there's a chance for a random player to
     * get an item.
     */
    private void checkRandomGift(MinecraftServer server) {
        if (random.nextDouble() >= ModConfig.getInstance().randomGiftChance) {
            return;
        }

        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty())
            return;

        // Select random player
        ServerPlayer luckyPlayer = players.get(random.nextInt(players.size()));

        // Give random item of any rarity
        var item = getRandomItemAnyRarity();
        if (item != null) {
            BingoItemManager.getInstance().giveItem(luckyPlayer, item);

            // Announce to all players
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§6✦ §e" + luckyPlayer.getName().getString() +
                            " §6hat ein zufälliges Geschenk erhalten: " +
                            item.getRarity().getColorCode() + item.getName() + "§6!"),
                    false);

            BingoBackpack.LOGGER.info("Random gift given to {}: {}",
                    luckyPlayer.getName().getString(), item.getName());
        }
    }

    /**
     * Check for bingo row completions and individual task completions.
     */
    private void checkBingoProgress(MinecraftServer server) {
        if (!BingoBridge.isAvailable())
            return;

        for (var team : BingoBridge.getAllTeams()) {
            String teamId = team.getId();

            var card = BingoBridge.getCardForTeam(teamId);
            if (card == null)
                continue;

            // Line count now comes from the team's own score, which upstream tracks
            int currentLines = team.getScore().getLines();
            int previousLines = teamCompletedLines.getOrDefault(teamId, 0);

            if (currentLines > previousLines) {
                // New row(s) completed!
                int newRows = currentLines - previousLines;
                for (int i = 0; i < newRows; i++) {
                    onRowCompleted(server, team);
                }
                teamCompletedLines.put(teamId, currentLines);
            }

            // Check for new objective completions
            var completedEntries = BingoBridge.getCompletedEntries(card, teamId);
            Set<String> currentCompleted = new HashSet<>();
            for (var entry : completedEntries) {
                currentCompleted.add(entry.getObjectiveId());
            }

            Set<String> previousCompleted = teamCompletedObjectives.getOrDefault(teamId, new HashSet<>());
            int newCompletions = 0;

            for (var entry : completedEntries) {
                if (!previousCompleted.contains(entry.getObjectiveId())) {
                    // New objective completed!
                    newCompletions++;
                    onObjectiveCompleted(server, team, entry);
                }
            }

            // Check for milestone rewards (every 5 tasks)
            if (newCompletions > 0) {
                int previousCount = teamObjectiveCount.getOrDefault(teamId, 0);
                int newCount = currentCompleted.size();

                // Calculate milestones crossed
                int milestoneInterval = ModConfig.getInstance().milestoneInterval;
                int previousMilestone = previousCount / milestoneInterval;
                int newMilestone = newCount / milestoneInterval;

                if (newMilestone > previousMilestone) {
                    int milestonesReached = newMilestone - previousMilestone;
                    for (int i = 0; i < milestonesReached; i++) {
                        int milestoneNumber = (previousMilestone + i + 1) * milestoneInterval;
                        onMilestoneReached(server, team, milestoneNumber);
                    }
                }

                teamObjectiveCount.put(teamId, newCount);
            }

            teamCompletedObjectives.put(teamId, currentCompleted);
        }
    }

    /**
     * Called when a team completes a bingo row.
     * All team members receive a weighted random item.
     */
    private void onRowCompleted(MinecraftServer server, IBingoTeam team) {
        BingoBackpack.LOGGER.info("Team {} completed a row!", team.getId());

        for (UUID playerId : team.getPlayers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                var item = getRowCompletionReward();
                if (item != null) {
                    BingoItemManager.getInstance().giveItem(player, item);
                    player.sendSystemMessage(
                            Component.literal("§a§l★ §aReihe abgeschlossen! §fDu hast " +
                                    item.getRarity().getColorCode() + item.getName() + " §ferhalten!"));
                }
            }
        }
    }

    /**
     * Called when a team reaches a milestone (every 5 tasks completed).
     * All team members receive a random item based on rarity weights.
     */
    private void onMilestoneReached(MinecraftServer server, IBingoTeam team, int tasksCompleted) {
        BingoBackpack.LOGGER.info("Team {} reached milestone: {} tasks completed!", team.getId(), tasksCompleted);

        for (UUID playerId : team.getPlayers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                // Give random item with rarity based on weights
                var item = getRandomItemAnyRarity();
                if (item != null) {
                    BingoItemManager.getInstance().giveItem(player, item);
                    player.sendSystemMessage(
                            Component.literal("§d§l✦ §d" + tasksCompleted + " Aufgaben erledigt! §fDu hast " +
                                    item.getRarity().getColorCode() + item.getName() + " §ferhalten!"));
                }
            }
        }

        // Broadcast milestone to all players
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§d§l✦ §eTeam " + team.getId() + " §dhat " + tasksCompleted + " Aufgaben erledigt!"),
                false);
    }

    /**
     * Called when a player/team completes an individual objective.
     * There's a chance to receive an item based on the task difficulty.
     */
    private void onObjectiveCompleted(MinecraftServer server, IBingoTeam team, ICardEntryView objective) {
        // Random chance to get an item
        if (random.nextDouble() >= ModConfig.getInstance().taskCompleteItemChance) {
            return;
        }

        // Determine rarity based on objective type
        // Since we don't have explicit difficulty, we'll randomize with weights
        ItemRarity rarity = getRandomRarityForTaskCompletion();

        // Give to a random team member who is online
        List<ServerPlayer> onlinePlayers = new ArrayList<>();
        for (UUID playerId : team.getPlayers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                onlinePlayers.add(player);
            }
        }

        if (onlinePlayers.isEmpty())
            return;

        ServerPlayer luckyPlayer = onlinePlayers.get(random.nextInt(onlinePlayers.size()));

        var items = BingoItemRegistry.getItemsByRarity(rarity);
        if (items.isEmpty()) {
            // Fallback to any item
            BingoItemRegistry.getRandomDroppableItem(random).ifPresent(item -> {
                BingoItemManager.getInstance().giveItem(luckyPlayer, item);
            });
            return;
        }

        var item = items.get(random.nextInt(items.size()));
        BingoItemManager.getInstance().giveItem(luckyPlayer, item);

        String objectiveName = BingoBridge.nameOf(objective);

        luckyPlayer.sendSystemMessage(
                Component.literal("§6✓ §eBonus für §f" + objectiveName + "§e: " +
                        item.getRarity().getColorCode() + item.getName()));
    }

    /**
     * Get a random item of any rarity, weighted towards lower rarities.
     */
    private BingoItem getRandomItemAnyRarity() {
        // Weight distribution: COMMON 35%, UNCOMMON 30%, RARE 20%, EPIC 10%, LEGENDARY
        // 5%
        double roll = random.nextDouble();
        ItemRarity rarity;

        if (roll < 0.35) {
            rarity = ItemRarity.COMMON;
        } else if (roll < 0.65) {
            rarity = ItemRarity.UNCOMMON;
        } else if (roll < 0.85) {
            rarity = ItemRarity.RARE;
        } else if (roll < 0.95) {
            rarity = ItemRarity.EPIC;
        } else {
            rarity = ItemRarity.LEGENDARY;
        }

        var items = BingoItemRegistry.getItemsByRarity(rarity);
        if (items.isEmpty()) {
            // Fallback
            return BingoItemRegistry.getRandomDroppableItem(random).orElse(null);
        }

        return items.get(random.nextInt(items.size()));
    }

    /**
     * Reward for completing a bingo row.
     *
     * Completing a row is the main achievement in a round, so this carries the
     * highest LEGENDARY chance of any reward path (15%, against 5% for
     * milestones and random gifts). Drawing uniformly from a combined pool
     * would ignore rarity entirely — with pools of 5/9/7/12/8 a COMMON would be
     * exactly as likely as a LEGENDARY — so the tier is rolled first.
     */
    private BingoItem getRowCompletionReward() {
        double roll = random.nextDouble();
        ItemRarity rarity;

        if (roll < 0.15) {
            rarity = ItemRarity.COMMON;
        } else if (roll < 0.40) {
            rarity = ItemRarity.UNCOMMON;
        } else if (roll < 0.70) {
            rarity = ItemRarity.RARE;
        } else if (roll < 0.85) {
            rarity = ItemRarity.EPIC;
        } else {
            rarity = ItemRarity.LEGENDARY;
        }

        var items = BingoItemRegistry.getItemsByRarity(rarity);
        if (items.isEmpty()) {
            return BingoItemRegistry.getRandomDroppableItem(random).orElse(null);
        }
        return items.get(random.nextInt(items.size()));
    }

    /**
     * Get a random rarity for task completion rewards.
     * Weighted towards COMMON/UNCOMMON.
     */
    private ItemRarity getRandomRarityForTaskCompletion() {
        double roll = random.nextDouble();

        if (roll < 0.40) {
            return ItemRarity.COMMON;
        } else if (roll < 0.70) {
            return ItemRarity.UNCOMMON;
        } else if (roll < 0.90) {
            return ItemRarity.RARE;
        } else {
            return ItemRarity.EPIC;
        }
        // LEGENDARY not given for single task completion
    }
}
