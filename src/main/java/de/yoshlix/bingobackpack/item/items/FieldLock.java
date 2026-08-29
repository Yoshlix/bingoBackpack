package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.ext.ICardEntryView;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feld-Sperre - freezes a chosen field for everyone (including your own
 * team) for 2 minutes. Nobody can complete it while it's locked; the bingo
 * API only exposes completion as a mutation, not as a cancellable event, so
 * this is enforced by reverting any completion of the locked objective on
 * the next tick check, for every team.
 */
public class FieldLock extends BingoItem {

    private static final Map<UUID, List<ICardEntryView>> pendingSelections = new HashMap<>();

    // objectiveId -> lock end time (epoch millis)
    private static final Map<String, Long> activeLocks = new HashMap<>();

    private static int tickCounter = 0;

    @Override
    public String getId() {
        return "field_lock";
    }

    @Override
    public String getName() {
        return "Feld-Sperre";
    }

    @Override
    public String getDescription() {
        return "Friert ein Feld für alle Teams für 2 Minuten ein — niemand kann es abschließen.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var playerTeam = requireTeam(player);
        if (playerTeam == null) {
            return false;
        }

        var card = BingoBridge.getCardForTeam(playerTeam.getId());
        if (card == null) {
            player.sendSystemMessage(Component.literal("§cKeine Bingo-Karte vorhanden!"));
            return false;
        }

        List<ICardEntryView> incomplete = BingoBridge.getIncompleteEntries(card, playerTeam.getId());
        if (incomplete.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Alle Felder wurden bereits abgeschlossen!"));
            return false;
        }

        pendingSelections.put(player.getUUID(), incomplete);

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§b§l═══════ Wähle ein Feld zum Sperren ═══════"));
        player.sendSystemMessage(Component.literal(""));

        int index = 1;
        for (var entry : incomplete) {
            String name = BingoBridge.nameOf(entry);
            Component message = Component.literal("  §e[" + index + "] ")
                    .append(Component.literal(name).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent.RunCommand("/backpack perks lock " + index))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("§bKlicke um dieses Feld für alle zu sperren")))));
            player.sendSystemMessage(message);
            index++;

            if (index > 20) {
                player.sendSystemMessage(Component.literal("  §7... und mehr"));
                break;
            }
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Klicke oder schreibe §f/backpack perks lock <nummer>"));
        player.sendSystemMessage(Component.literal("§b§l═══════════════════════════════════════"));

        return false; // Don't consume yet - wait for selection
    }

    public static boolean hasPendingSelection(UUID playerId) {
        return pendingSelections.containsKey(playerId);
    }

    public static boolean processSelection(ServerPlayer player, String selection) {
        List<ICardEntryView> options = pendingSelections.remove(player.getUUID());
        if (options == null) {
            player.sendSystemMessage(Component.literal("§cKeine ausstehende Auswahl!"));
            return false;
        }

        int index;
        try {
            index = Integer.parseInt(selection) - 1;
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        if (index < 0 || index >= options.size()) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        ICardEntryView entry = options.get(index);

        if (!consumeOrWarn(player, "field_lock")) {
            return false;
        }

        int durationSeconds = ModConfig.getInstance().fieldLockDurationSeconds;
        activeLocks.put(entry.getObjectiveId(), System.currentTimeMillis() + durationSeconds * 1000L);

        String name = BingoBridge.nameOf(entry);
        player.sendSystemMessage(Component.literal("§b§l🔒 §rFeld §f" + name + " §rfür " + durationSeconds
                + " Sekunden gesperrt — auch für dein eigenes Team!"));

        ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getPlayerList()
                .broadcastSystemMessage(
                        Component.literal("§b§l❄ FELD EINGEFROREN! §e" + player.getName().getString()
                                + " §7hat §f" + name + " §7für alle für " + durationSeconds
                                + " Sekunden gesperrt!"),
                        false);

        return true;
    }

    /**
     * Reverts any completion of a currently locked objective, for every team.
     * The bingo API has no way to intercept completion before it happens, so
     * this polls and un-does it instead — a short flash of "completed" is the
     * tradeoff for not needing an upstream API change.
     */
    public static void tickFieldLocks(MinecraftServer server) {
        if (activeLocks.isEmpty()) {
            return;
        }
        // Checking every tick is unnecessary; every quarter-second is plenty
        // responsive for a denial effect and far cheaper.
        if (++tickCounter % 5 != 0) {
            return;
        }

        long now = System.currentTimeMillis();
        activeLocks.entrySet().removeIf(lock -> {
            String objectiveId = lock.getKey();
            if (now >= lock.getValue()) {
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§7Die Feld-Sperre ist ausgelaufen — Felder sind wieder frei."), false);
                return true;
            }

            for (var team : BingoBridge.getAllTeams()) {
                var card = BingoBridge.getCardForTeam(team.getId());
                if (card == null) {
                    continue;
                }
                for (var entry : BingoBridge.getEntries(card)) {
                    if (!entry.getObjectiveId().equals(objectiveId)) {
                        continue;
                    }
                    if (entry.hasTeamAchieved(team.getId())) {
                        BingoBridge.uncompleteObjective(objectiveId, team.getId());
                        for (UUID memberId : team.getPlayers()) {
                            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                            if (member != null) {
                                member.sendSystemMessage(Component.literal(
                                        "§c§lGesperrt! §rDieses Feld ist gerade eingefroren und wurde zurückgesetzt."));
                            }
                        }
                    }
                }
            }
            return false;
        });
    }

    public static void clearAll() {
        pendingSelections.clear();
        activeLocks.clear();
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Sperrt ein Feld für alle Teams"),
                Component.literal("§7Dauer: " + ModConfig.getInstance().fieldLockDurationSeconds + " Sekunden"),
                Component.literal("§7Trifft auch dein eigenes Team!"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
