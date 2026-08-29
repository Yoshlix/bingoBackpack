package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Alles auf eine Karte - a push-your-luck climb up the entire rarity ladder.
 * Stage 1 through 5 map to Common through Legendary; cash out any time you
 * hold a stage, or push for the next one and risk losing everything. The
 * item itself is spent the moment you start; everything past that is pure
 * tracked state, not something you can dodge by moving items around.
 */
public class PushYourLuck extends BingoItem {

    private static final ItemRarity[] STAGE_RARITY = {
            ItemRarity.COMMON, ItemRarity.UNCOMMON, ItemRarity.RARE, ItemRarity.EPIC, ItemRarity.LEGENDARY
    };
    // Chance to advance from stage i+1 to stage i+2 (index 0 = stage 1->2).
    private static final double[] ADVANCE_CHANCE = {0.90, 0.75, 0.60, 0.45};

    private static final Map<UUID, Integer> stages = new HashMap<>();

    @Override
    public String getId() {
        return "push_your_luck";
    }

    @Override
    public String getName() {
        return "Alles auf eine Karte";
    }

    @Override
    public String getDescription() {
        return "Klettere die Rarity-Leiter hoch — jederzeit aussteigen oder weiterziehen und alles riskieren.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        stages.put(player.getUUID(), 1);
        announceStage(player, 1);
        return true; // the item is spent the moment the climb starts
    }

    private static void announceStage(ServerPlayer player, int stage) {
        ItemRarity rarity = STAGE_RARITY[stage - 1];
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "§6§lStufe " + stage + ": §f" + rarity.getDisplayName() + "§6-Preis in der Hand!"));

        if (stage >= STAGE_RARITY.length) {
            return; // handled by the caller — no further choice at the top
        }

        int nextStage = stage + 1;
        double chance = ADVANCE_CHANCE[stage - 1] * 100;
        player.sendSystemMessage(pushOption("keep", "Behalten",
                "§aSicher: nimm den " + rarity.getDisplayName() + "-Preis"));
        player.sendSystemMessage(pushOption("push", "Weiterziehen",
                "§e" + (int) chance + "% auf Stufe " + nextStage + " (" + STAGE_RARITY[nextStage - 1].getDisplayName()
                        + "), sonst alles weg"));
        player.sendSystemMessage(Component.literal("§7Klicke oder schreibe §f/backpack perks pushluck <keep|push>"));
    }

    private static Component pushOption(String command, String label, String hover) {
        return Component.literal("  §e» ")
                .append(Component.literal(label).withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GOLD)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/backpack perks pushluck " + command))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover)))));
    }

    public static boolean hasActiveClimb(UUID playerId) {
        return stages.containsKey(playerId);
    }

    public static boolean processChoice(ServerPlayer player, String choiceRaw) {
        Integer stage = stages.get(player.getUUID());
        if (stage == null) {
            player.sendSystemMessage(Component.literal("§cKein laufender Versuch!"));
            return false;
        }

        String choice = choiceRaw.toLowerCase(Locale.ROOT);
        if (choice.equals("keep")) {
            stages.remove(player.getUUID());
            payout(player, stage);
            player.sendSystemMessage(Component.literal("§a§lAusgestiegen! §rPreis gesichert."));
            return true;
        }

        if (!choice.equals("push")) {
            player.sendSystemMessage(Component.literal("§cUngültige Wahl! Nutze keep oder push."));
            return false;
        }

        if (stage >= STAGE_RARITY.length) {
            player.sendSystemMessage(Component.literal("§cDu bist schon auf der höchsten Stufe!"));
            return false;
        }

        double chance = ADVANCE_CHANCE[stage - 1];
        if (RANDOM.nextDouble() < chance) {
            int nextStage = stage + 1;
            stages.put(player.getUUID(), nextStage);
            if (nextStage == STAGE_RARITY.length) {
                stages.remove(player.getUUID());
                payout(player, nextStage);
                player.sendSystemMessage(Component.literal(
                        "§6§l★★★ GANZ NACH OBEN GEZOGEN! ★★★ §rDer Legendary-Jackpot ist dein!"));
            } else {
                player.sendSystemMessage(Component.literal("§a§lGeschafft! §rEine Stufe höher."));
                announceStage(player, nextStage);
            }
        } else {
            stages.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("§4§l✗ ALLES VERLOREN! §rDas Glück hat dich verlassen."));
        }

        return true;
    }

    private static void payout(ServerPlayer player, int stage) {
        ItemRarity rarity = STAGE_RARITY[stage - 1];
        var pool = BingoItemRegistry.getItemsByRarity(rarity);
        if (!pool.isEmpty()) {
            var prize = pool.get(RANDOM.nextInt(pool.size()));
            player.getInventory().add(prize.createItemStack());
        }
    }

    public static void clearAllClimbs() {
        stages.clear();
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Common → Uncommon → Rare → Epic → Legendary"),
                Component.literal("§7Jede Stufe: aussteigen oder alles riskieren"),
                Component.literal("§cVerloren = nichts, auch nicht die erste Stufe!"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
