package de.yoshlix.bingobackpack.bounty;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.BingoItemManager;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Rotating world objectives ("Bounties"), independent of the item-drop RNG
 * loop. One bounty is active at a time; whichever team completes it first
 * gets a reward sized to its difficulty tier. Mirrors the structure of
 * {@link de.yoshlix.bingobackpack.item.ChaosHour} and
 * {@link de.yoshlix.bingobackpack.banish.BanishManager}.
 */
public class BountyManager {
    private static BountyManager instance;

    private final Random random = new Random();
    private MinecraftServer server;

    private BountyDefinition active;
    private long activeSinceMillis;
    private final Map<UUID, Integer> killProgress = new HashMap<>();

    public static BountyManager getInstance() {
        if (instance == null) {
            instance = new BountyManager();
        }
        return instance;
    }

    private BountyManager() {
    }

    public void init(MinecraftServer server) {
        this.server = server;
    }

    public void onRoundStarted() {
        reset();
        if (ModConfig.getInstance().bountyBoardEnabled) {
            selectNewBounty();
        }
    }

    public void reset() {
        active = null;
        killProgress.clear();
    }

    public BountyDefinition getActive() {
        return active;
    }

    /** Call every server tick. Internally throttled to once a second. */
    public void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        if (!ModConfig.getInstance().bountyBoardEnabled) {
            return;
        }
        if (active == null) {
            selectNewBounty();
            return;
        }

        long rotationMs = Math.max(1, ModConfig.getInstance().bountyRotationMinutes) * 60_000L;
        if (System.currentTimeMillis() - activeSinceMillis >= rotationMs) {
            selectNewBounty();
            return;
        }

        if (active.getType() == BountyType.LOCATION) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (active.getLocationCheck().test(player)) {
                    complete(player);
                    return;
                }
            }
        } else if (active.getType() == BountyType.COLLECT) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (tryConsumeCollectTarget(player, active)) {
                    complete(player);
                    return;
                }
            }
        }
    }

    /** Called from {@code MobDeathMixin} on every player kill. */
    public void onMobKilled(LivingEntity killed, Player killer) {
        if (!ModConfig.getInstance().bountyBoardEnabled) {
            return;
        }
        if (active == null || active.getType() != BountyType.KILL) {
            return;
        }
        if (!(killer instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (killed.getType() != active.getMobType()) {
            return;
        }

        int progress = killProgress.merge(serverPlayer.getUUID(), 1, Integer::sum);
        if (progress >= active.getKillCount()) {
            complete(serverPlayer);
        }
    }

    /**
     * Checks whether the player owns enough of the active COLLECT bounty's
     * target item, and if so consumes it. Explicitly skips any stack that is
     * a {@code BingoItem} — a bounty must never be satisfiable by consuming
     * one of those, since that would remove it from the team's actual bingo
     * toolkit instead of just spending spare vanilla materials.
     */
    private boolean tryConsumeCollectTarget(ServerPlayer player, BountyDefinition def) {
        var inventory = player.getInventory();
        int owned = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || BingoItemRegistry.isBingoItem(stack)) {
                continue;
            }
            if (stack.is(def.getCollectItem())) {
                owned += stack.getCount();
            }
        }

        if (owned < def.getCollectCount()) {
            return false;
        }

        int remaining = def.getCollectCount();
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || BingoItemRegistry.isBingoItem(stack) || !stack.is(def.getCollectItem())) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return true;
    }

    private void complete(ServerPlayer player) {
        if (active == null || server == null) {
            return;
        }
        BountyDefinition def = active;

        boolean teamReward = def.getTier() == ItemRarity.RARE || def.getTier() == ItemRarity.EPIC
                || def.getTier() == ItemRarity.LEGENDARY;

        List<BingoItem> pool = BingoItemRegistry.getItemsByRarity(def.getTier());
        if (pool.isEmpty()) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§6⚑ Bounty '" + def.getName() + "' erfüllt, aber kein passendes Item registriert."),
                    false);
            active = null;
            killProgress.clear();
            selectNewBounty();
            return;
        }

        if (teamReward) {
            IBingoTeam team = BingoBridge.getTeamForPlayer(player.getUUID());
            if (team != null) {
                for (UUID memberId : team.getPlayers()) {
                    ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                    if (member != null) {
                        BingoItem item = pool.get(random.nextInt(pool.size()));
                        BingoItemManager.getInstance().giveItem(member, item);
                    }
                }
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§6⚑ §eTeam " + team.getId() + " §6hat die Bounty '" + def.getName()
                                + "' erfüllt! §7(" + def.getTier().getDisplayName() + ")"),
                        false);
            }
        } else {
            BingoItem item = pool.get(random.nextInt(pool.size()));
            BingoItemManager.getInstance().giveItem(player, item);
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§6⚑ §e" + player.getName().getString() + " §6hat die Bounty '" + def.getName()
                            + "' erfüllt! §7(" + def.getTier().getDisplayName() + ")"),
                    false);
        }

        active = null;
        killProgress.clear();
        selectNewBounty();
    }

    private void selectNewBounty() {
        if (server == null) {
            return;
        }

        ItemRarity tier = rollTier();
        List<BountyDefinition> candidates = BountyRegistry.getByTier(tier);
        if (candidates.isEmpty()) {
            candidates = BountyRegistry.getAll();
        }
        if (candidates.isEmpty()) {
            return;
        }

        active = candidates.get(random.nextInt(candidates.size()));
        activeSinceMillis = System.currentTimeMillis();
        killProgress.clear();

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6⚑§l NEUE BOUNTY: §e" + active.getName() + " §7(" + active.getTier().getDisplayName() + ")\n"
                        + "§7" + active.getDescription()),
                false);
    }

    /** Same 35/30/20/10/5 weighting as {@code BingoRewardSystem.getRandomItemAnyRarity()}. */
    private ItemRarity rollTier() {
        double roll = random.nextDouble();
        if (roll < 0.35) {
            return ItemRarity.COMMON;
        } else if (roll < 0.65) {
            return ItemRarity.UNCOMMON;
        } else if (roll < 0.85) {
            return ItemRarity.RARE;
        } else if (roll < 0.95) {
            return ItemRarity.EPIC;
        } else {
            return ItemRarity.LEGENDARY;
        }
    }

    /** Admin/testing override — force a specific bounty to become active immediately. */
    public boolean forceBounty(String id) {
        var def = BountyRegistry.getById(id);
        if (def.isEmpty() || server == null) {
            return false;
        }
        active = def.get();
        activeSinceMillis = System.currentTimeMillis();
        killProgress.clear();
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6⚑§l BOUNTY (erzwungen): §e" + active.getName() + " §7(" + active.getTier().getDisplayName() + ")\n"
                        + "§7" + active.getDescription()),
                false);
        return true;
    }
}
