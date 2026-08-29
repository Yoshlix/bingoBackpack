package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.ext.ICardEntryView;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Kettenreaktion - completes a random open field on your team's card. Each
 * time, there's a 40% chance the reaction jumps to an open neighboring field
 * and completes that too, then a 40% chance to jump again from there, and so
 * on — usually stops after 0-1 jumps, but can rarely chain much further.
 */
public class ChainReaction extends BingoItem {

    private static final double CONTINUE_CHANCE = 0.4;
    private static final int CARD_SIZE = 5;

    @Override
    public String getId() {
        return "chain_reaction";
    }

    @Override
    public String getName() {
        return "Kettenreaktion";
    }

    @Override
    public String getDescription() {
        return "Schließt ein zufälliges Feld ab — mit Chance, auf Nachbarfelder überzuspringen.";
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

        List<ICardEntryView> entries = BingoBridge.getEntries(card);
        if (entries.size() < CARD_SIZE * CARD_SIZE) {
            player.sendSystemMessage(Component.literal("§cKeine Bingo-Karte vorhanden!"));
            return false;
        }

        List<ICardEntryView> incomplete = BingoBridge.getIncompleteEntries(card, playerTeam.getId());
        if (incomplete.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Alle Felder wurden bereits abgeschlossen!"));
            return false;
        }

        String teamId = playerTeam.getId();
        ICardEntryView current = incomplete.get(RANDOM.nextInt(incomplete.size()));
        List<String> completedNames = new ArrayList<>();

        while (current != null) {
            boolean success = BingoBridge.completeObjective(current.getObjectiveId(), teamId, player.getUUID());
            if (!success) {
                break;
            }
            completedNames.add(BingoBridge.nameOf(current));

            if (RANDOM.nextDouble() >= CONTINUE_CHANCE) {
                break;
            }
            current = randomIncompleteNeighbor(entries, current, teamId);
        }

        if (completedNames.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cFehler beim Abschließen des Feldes!"));
            return false;
        }

        if (completedNames.size() == 1) {
            player.sendSystemMessage(Component.literal("§a✓ Feld abgeschlossen: §f" + completedNames.get(0)));
        } else {
            player.sendSystemMessage(Component.literal(
                    "§6§l⚡ KETTENREAKTION! §r§a" + completedNames.size() + " Felder abgeschlossen:"));
            for (String name : completedNames) {
                player.sendSystemMessage(Component.literal("  §7• §f" + name));
            }
        }

        return true;
    }

    /** A random still-open neighbor (up/down/left/right) of the given entry, or null if none. */
    private ICardEntryView randomIncompleteNeighbor(List<ICardEntryView> entries, ICardEntryView entry, String teamId) {
        int x = entry.getX();
        int y = entry.getY();
        List<ICardEntryView> candidates = new ArrayList<>(4);
        addIfIncomplete(entries, candidates, x - 1, y, teamId);
        addIfIncomplete(entries, candidates, x + 1, y, teamId);
        addIfIncomplete(entries, candidates, x, y - 1, teamId);
        addIfIncomplete(entries, candidates, x, y + 1, teamId);

        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(RANDOM.nextInt(candidates.size()));
    }

    private void addIfIncomplete(List<ICardEntryView> entries, List<ICardEntryView> out, int x, int y, String teamId) {
        if (x < 0 || x >= CARD_SIZE || y < 0 || y >= CARD_SIZE) {
            return;
        }
        ICardEntryView neighbor = entries.get(x + y * CARD_SIZE);
        if (!neighbor.hasTeamAchieved(teamId)) {
            out.add(neighbor);
        }
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Kann auf offene Nachbarfelder überspringen"),
                Component.literal("§740% Chance pro Sprung — meist 1-2, selten mehr"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
