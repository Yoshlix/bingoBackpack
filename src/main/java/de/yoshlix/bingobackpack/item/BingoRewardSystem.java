package de.yoshlix.bingobackpack.item;

import de.yoshlix.bingobackpack.BingoBackpack;
import me.jfenn.bingo.api.BingoApi;
import me.jfenn.bingo.api.data.BingoGameStatus;
import me.jfenn.bingo.api.data.IBingoObjective;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Manages the reward system for Bingo Items.
 * 
 * Rewards are given in the following situations:
 * 1. When a player completes a bingo row -> All team members get a random item
 * (up to RARE)
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
    private static final int RANDOM_GIFT_INTERVAL = 30 * 20; // 30 seconds in ticks
    private static final double RANDOM_GIFT_CHANCE = 0.05; // 5% chance every 30 seconds

    // Track completed lines per team to detect new completions
    private final Map<String, Integer> teamCompletedLines = new HashMap<>();

    // Track completed objectives to detect new completions
    private final Map<String, Set<String>> teamCompletedObjectives = new HashMap<>();

    // Track objective count for milestone rewards (every 5 tasks = 1 item)
    private final Map<String, Integer> teamObjectiveCount = new HashMap<>();
    private static final int MILESTONE_INTERVAL = 5; // Every 5 tasks = 1 reward

    // Difficulty mapping (bingo fields have implicit difficulty based on
    // position/type)
    // Since the API doesn't expose difficulty directly, we'll use a random approach
    private static final double TASK_COMPLETE_ITEM_CHANCE = 0.15; // 15% chance on task complete

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

        var game = BingoApi.getGame();
        if (game == null || !game.getStatus().equals(BingoGameStatus.PLAYING)) {
            return;
        }

        tickCounter++;

        // Check for random gifts every 30 seconds
        if (tickCounter >= RANDOM_GIFT_INTERVAL) {
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
        if (random.nextDouble() >= RANDOM_GIFT_CHANCE) {
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
                            " §6hat ein zufälliges Geschenk erhalten: §" +
                            item.getRarity().getColor().getChar() + item.getName() + "§6!"),
                    false);

            BingoBackpack.LOGGER.info("Random gift given to {}: {}",
                    luckyPlayer.getName().getString(), item.getName());
        }
    }

    /**
     * Check for bingo row completions and individual task completions.
     */
    private void checkBingoProgress(MinecraftServer server) {
        var teams = BingoApi.getTeams();
        var gameExtended = BingoApi.getGameExtended();

        if (teams == null || gameExtended == null)
            return;

        var card = gameExtended.getActiveCard();
        if (card == null)
            return;

        for (var team : teams) {
            String teamId = team.getId();

            // Check for new row completions
            int currentLines = card.countLines(teamId);
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
            Set<String> currentCompleted = new HashSet<>();
            for (var objective : card.getObjectives()) {
                if (objective.hasAchieved(teamId)) {
                    currentCompleted.add(objective.getId());
                }
            }

            Set<String> previousCompleted = teamCompletedObjectives.getOrDefault(teamId, new HashSet<>());
            int newCompletions = 0;

            for (String objId : currentCompleted) {
                if (!previousCompleted.contains(objId)) {
                    // New objective completed!
                    newCompletions++;
                    var objective = card.getObjectives().stream()
                            .filter(o -> o.getId().equals(objId))
                            .findFirst()
                            .orElse(null);

                    if (objective != null) {
                        onObjectiveCompleted(server, team, objective);
                    }
                }
            }

            // Check for milestone rewards (every 5 tasks)
            if (newCompletions > 0) {
                int previousCount = teamObjectiveCount.getOrDefault(teamId, 0);
                int newCount = currentCompleted.size();

                // Calculate milestones crossed
                int previousMilestone = previousCount / MILESTONE_INTERVAL;
                int newMilestone = newCount / MILESTONE_INTERVAL;

                if (newMilestone > previousMilestone) {
                    int milestonesReached = newMilestone - previousMilestone;
                    for (int i = 0; i < milestonesReached; i++) {
                        int milestoneNumber = (previousMilestone + i + 1) * MILESTONE_INTERVAL;
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
     * All team members receive a random item (up to RARE rarity).
     */
    private void onRowCompleted(MinecraftServer server, IBingoTeam team) {
        BingoBackpack.LOGGER.info("Team {} completed a row!", team.getId());

        for (UUID playerId : team.getPlayers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                // Give random item up to RARE rarity
                var item = getRandomItemUpToRarity(ItemRarity.RARE);
                if (item != null) {
                    BingoItemManager.getInstance().giveItem(player, item);
                    player.sendSystemMessage(
                            Component.literal("§a§l★ §aReihe abgeschlossen! §fDu hast §" +
                                    item.getRarity().getColor().getChar() + item.getName() + " §ferhalten!"));
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
                            Component.literal("§d§l✦ §d" + tasksCompleted + " Aufgaben erledigt! §fDu hast §" +
                                    item.getRarity().getColor().getChar() + item.getName() + " §ferhalten!"));
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
    private void onObjectiveCompleted(MinecraftServer server, IBingoTeam team, IBingoObjective objective) {
        // Random chance to get an item
        if (random.nextDouble() >= TASK_COMPLETE_ITEM_CHANCE) {
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

        String objectiveName = objective.getDisplayName() != null ? objective.getDisplayName() : objective.getId();

        luckyPlayer.sendSystemMessage(
                Component.literal("§6✓ §eBonus für §f" + objectiveName + "§e: §" +
                        item.getRarity().getColor().getChar() + item.getName()));
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
     * Get a random item up to the specified rarity (inclusive).
     */
    private BingoItem getRandomItemUpToRarity(ItemRarity maxRarity) {
        List<BingoItem> eligibleItems = new ArrayList<>();

        for (ItemRarity rarity : ItemRarity.values()) {
            if (rarity.ordinal() <= maxRarity.ordinal()) {
                eligibleItems.addAll(BingoItemRegistry.getItemsByRarity(rarity));
            }
        }

        if (eligibleItems.isEmpty()) {
            return BingoItemRegistry.getRandomDroppableItem(random).orElse(null);
        }

        // Weight towards higher rarities within the range
        // Simple approach: just pick randomly
        return eligibleItems.get(random.nextInt(eligibleItems.size()));
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
