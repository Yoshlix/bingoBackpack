package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import me.jfenn.bingo.api.data.IBingoObjective;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resets the progress of a specific bingo field.
 * Can target the enemy team's completed fields to uncomplete them.
 */
public class ResetFieldProgress extends BingoItem {

    private static final Map<UUID, PendingReset> pendingResets = new HashMap<>();

    @Override
    public String getId() {
        return "reset_field_progress";
    }

    @Override
    public String getName() {
        return "Fortschritt Zurücksetzen";
    }

    @Override
    public String getDescription() {
        return "Setzt den Fortschritt eines gegnerischen Feldes zurück.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var teams = BingoApi.getTeams();
        if (teams == null) {
            player.sendSystemMessage(Component.literal("§cKein Bingo-Spiel aktiv!"));
            return false;
        }

        var playerTeam = teams.getTeamForPlayer(player.getUUID());
        if (playerTeam == null) {
            player.sendSystemMessage(Component.literal("§cDu bist in keinem Team!"));
            return false;
        }

        var game = BingoApi.getGameExtended();
        if (game == null) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        var card = game.getActiveCard();
        if (card == null) {
            player.sendSystemMessage(Component.literal("§cKeine Bingo-Karte vorhanden!"));
            return false;
        }

        // Find enemy teams and their completed objectives
        var enemyCompletions = new ArrayList<EnemyCompletion>();

        for (var team : teams) {
            if (team.getId().equals(playerTeam.getId()))
                continue; // Skip own team

            for (var objective : card.getObjectives()) {
                if (objective.hasAchieved(team.getId())) {
                    enemyCompletions.add(new EnemyCompletion(team.getId(), objective));
                }
            }
        }

        if (enemyCompletions.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Keine gegnerischen abgeschlossenen Felder!"));
            return false;
        }

        // Store pending reset
        pendingResets.put(player.getUUID(), new PendingReset(enemyCompletions));

        // Show selection menu
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§c§l═══ Gegnerische Felder zurücksetzen ═══"));
        player.sendSystemMessage(Component.literal(""));

        int index = 1;
        for (var completion : enemyCompletions) {
            String name = completion.objective.getDisplayName() != null ? completion.objective.getDisplayName()
                    : completion.objective.getId();

            Component message = Component.literal("  §e[" + index + "] §c" + completion.teamId + "§7: ")
                    .append(Component.literal(name).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.WHITE)
                            .withClickEvent(new ClickEvent.RunCommand("/backpack perks reset " + index))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("Klicke um diesen Fortschritt zurückzusetzen")))));

            player.sendSystemMessage(message);
            index++;
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Klicke oder schreibe §f/backpack perks reset <nummer>"));
        player.sendSystemMessage(Component.literal("§c§l════════════════════════════════"));

        return false;
    }

    public static boolean processReset(ServerPlayer player, String selection) {
        PendingReset pending = pendingResets.remove(player.getUUID());
        if (pending == null) {
            player.sendSystemMessage(Component.literal("§cKeine ausstehende Reset-Auswahl!"));
            return false;
        }

        int index;
        try {
            index = Integer.parseInt(selection) - 1;
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        if (index < 0 || index >= pending.completions.size()) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        EnemyCompletion completion = pending.completions.get(index);

        var scoringService = BingoApi.getScoringService();
        if (scoringService == null) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        String name = completion.objective.getDisplayName() != null ? completion.objective.getDisplayName()
                : completion.objective.getId();

        boolean success = scoringService.uncompleteObjective(
                completion.objective.getId(),
                completion.teamId);

        if (success) {
            player.sendSystemMessage(Component.literal("§a✓ Fortschritt zurückgesetzt: §f" + name +
                    " §7(Team: §c" + completion.teamId + "§7)"));

            ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getPlayerList()
                    .broadcastSystemMessage(
                            Component.literal("§c§l⚠ §e" + player.getName().getString() +
                                    " §chat den Fortschritt von §f" + name + " §c(Team: " + completion.teamId
                                    + ") zurückgesetzt!"),
                            false);

            consumeItem(player);
            return true;
        } else {
            player.sendSystemMessage(Component.literal("§cFehler beim Zurücksetzen!"));
            return false;
        }
    }

    private static void consumeItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            var itemOpt = de.yoshlix.bingobackpack.item.BingoItemRegistry.fromItemStack(stack);
            if (itemOpt.isPresent() && itemOpt.get().getId().equals("reset_field_progress")) {
                stack.shrink(1);
                return;
            }
        }
    }

    public static boolean hasPendingReset(UUID playerId) {
        return pendingResets.containsKey(playerId);
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§cSabotiere das gegnerische Team!"));
    }

    @Override
    public boolean canDropFromMob() {
        return false; // Too powerful
    }

    private static class EnemyCompletion {
        final String teamId;
        final IBingoObjective objective;

        EnemyCompletion(String teamId, IBingoObjective objective) {
            this.teamId = teamId;
            this.objective = objective;
        }
    }

    private static class PendingReset {
        final List<EnemyCompletion> completions;

        PendingReset(List<EnemyCompletion> completions) {
            this.completions = completions;
        }
    }
}
