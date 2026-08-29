package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.ext.ICardEntryView;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pokerhand - draws 5 cards from a standard deck; the reward tier scales with
 * the poker hand you draw, from a guaranteed Uncommon item for a plain High
 * Card/Pair all the way up to a Legendary item (plus an instant field
 * completion) for the vanishingly rare Straight Flush.
 */
public class PokerHand extends BingoItem {

    private enum HandRank {
        HIGH_CARD, PAIR, TWO_PAIR, THREE_OF_A_KIND, STRAIGHT, FLUSH, FULL_HOUSE, FOUR_OF_A_KIND, STRAIGHT_FLUSH
    }

    private static final String[] SUIT_SYMBOLS = {"♠", "♥", "♦", "♣"};

    @Override
    public String getId() {
        return "poker_hand";
    }

    @Override
    public String getName() {
        return "Pokerhand";
    }

    @Override
    public String getDescription() {
        return "Zieht 5 Karten — die Belohnung skaliert mit deiner Pokerhand.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        List<Integer> deck = new ArrayList<>(52);
        for (int i = 0; i < 52; i++) {
            deck.add(i);
        }
        Collections.shuffle(deck, RANDOM);

        int[] ranks = new int[5];
        int[] suits = new int[5];
        for (int i = 0; i < 5; i++) {
            int card = deck.get(i);
            ranks[i] = card / 4 + 2; // 2..14 (14 = Ace)
            suits[i] = card % 4;
        }

        player.sendSystemMessage(Component.literal("§7" + renderHand(ranks, suits)));

        HandRank hand = evaluate(ranks, suits);
        ItemRarity rewardRarity;
        String handName;

        switch (hand) {
            case STRAIGHT_FLUSH -> {
                rewardRarity = ItemRarity.LEGENDARY;
                handName = "§6§lSTRAIGHT FLUSH!";
                completeRandomField(player);
            }
            case FOUR_OF_A_KIND, FULL_HOUSE -> {
                rewardRarity = ItemRarity.LEGENDARY;
                handName = hand == HandRank.FOUR_OF_A_KIND ? "Vierling" : "Full House";
            }
            case STRAIGHT, FLUSH -> {
                rewardRarity = ItemRarity.EPIC;
                handName = hand == HandRank.STRAIGHT ? "Straße" : "Flush";
            }
            case TWO_PAIR, THREE_OF_A_KIND -> {
                rewardRarity = ItemRarity.RARE;
                handName = hand == HandRank.TWO_PAIR ? "Zwei Paare" : "Drilling";
            }
            default -> {
                rewardRarity = ItemRarity.UNCOMMON;
                handName = hand == HandRank.PAIR ? "Ein Paar" : "High Card";
            }
        }

        var pool = BingoItemRegistry.getItemsByRarity(rewardRarity);
        if (!pool.isEmpty()) {
            var prize = pool.get(RANDOM.nextInt(pool.size()));
            player.getInventory().add(prize.createItemStack());
        }

        player.sendSystemMessage(Component.literal(
                "§e" + handName + " §7— Belohnung: §f" + rewardRarity.getDisplayName()));

        return true;
    }

    private void completeRandomField(ServerPlayer player) {
        var playerTeam = requireTeam(player);
        if (playerTeam == null) {
            return;
        }
        var card = BingoBridge.getCardForTeam(playerTeam.getId());
        if (card == null) {
            return;
        }
        List<ICardEntryView> incomplete = BingoBridge.getIncompleteEntries(card, playerTeam.getId());
        if (incomplete.isEmpty()) {
            return;
        }
        var target = incomplete.get(RANDOM.nextInt(incomplete.size()));
        if (BingoBridge.completeObjective(target.getObjectiveId(), playerTeam.getId(), player.getUUID())) {
            player.sendSystemMessage(Component.literal(
                    "§a✓ Zusätzlich abgeschlossen: §f" + BingoBridge.nameOf(target)));
        }
    }

    private HandRank evaluate(int[] ranks, int[] suits) {
        int[] sorted = ranks.clone();
        java.util.Arrays.sort(sorted);

        boolean hasFlush = suits[0] == suits[1] && suits[1] == suits[2] && suits[2] == suits[3] && suits[3] == suits[4];
        Map<Integer, Integer> counts = new HashMap<>();
        for (int r : sorted) {
            counts.merge(r, 1, Integer::sum);
        }
        List<Integer> countValues = new ArrayList<>(counts.values());
        countValues.sort(Collections.reverseOrder());

        boolean straight = true;
        for (int i = 1; i < 5; i++) {
            if (sorted[i] != sorted[i - 1] + 1) {
                straight = false;
                break;
            }
        }
        boolean wheel = sorted[0] == 2 && sorted[1] == 3 && sorted[2] == 4 && sorted[3] == 5 && sorted[4] == 14;
        if (wheel) {
            straight = true;
        }

        boolean hasFour = countValues.get(0) == 4;
        boolean hasThree = countValues.get(0) == 3;
        boolean hasPairAfterThree = countValues.size() > 1 && countValues.get(1) == 2;
        boolean hasTwoPairs = countValues.get(0) == 2 && countValues.size() > 1 && countValues.get(1) == 2;
        boolean hasOnePair = countValues.get(0) == 2 && !hasTwoPairs;

        if (hasFlush && straight) {
            return HandRank.STRAIGHT_FLUSH;
        }
        if (hasFour) {
            return HandRank.FOUR_OF_A_KIND;
        }
        if (hasThree && hasPairAfterThree) {
            return HandRank.FULL_HOUSE;
        }
        if (hasFlush) {
            return HandRank.FLUSH;
        }
        if (straight) {
            return HandRank.STRAIGHT;
        }
        if (hasThree) {
            return HandRank.THREE_OF_A_KIND;
        }
        if (hasTwoPairs) {
            return HandRank.TWO_PAIR;
        }
        if (hasOnePair) {
            return HandRank.PAIR;
        }
        return HandRank.HIGH_CARD;
    }

    private String renderHand(int[] ranks, int[] suits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            boolean red = suits[i] == 1 || suits[i] == 2;
            sb.append(red ? "§c" : "§f").append(rankName(ranks[i])).append(SUIT_SYMBOLS[suits[i]]);
        }
        return sb.toString();
    }

    private String rankName(int rank) {
        return switch (rank) {
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            case 14 -> "A";
            default -> String.valueOf(rank);
        };
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7High Card/Paar: Uncommon-Item"),
                Component.literal("§72 Paare/Drilling: Rare-Item"),
                Component.literal("§7Straße/Flush: Epic-Item"),
                Component.literal("§7Full House/Vierling: Legendary-Item"),
                Component.literal("§6Straight Flush: Legendary-Item + Feldabschluss!"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
