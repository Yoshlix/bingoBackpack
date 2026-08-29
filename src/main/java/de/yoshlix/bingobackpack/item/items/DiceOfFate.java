package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Würfel des Schicksals - a craps-style bet. Pick "Unter 7" or "Über 7" for a
 * ~42% chance at a solid buff, or the longshot "Genau 7" (~17%) for a
 * guaranteed Rare item. Missing your bet still gives a small consolation, so
 * no roll feels wasted.
 */
public class DiceOfFate extends BingoItem {

    private static final Set<UUID> pendingBets = new HashSet<>();

    @Override
    public String getId() {
        return "dice_of_fate";
    }

    @Override
    public String getName() {
        return "Würfel des Schicksals";
    }

    @Override
    public String getDescription() {
        return "Wette auf 2 Würfel: Unter/Über 7 für einen Boost, Genau 7 für den Jackpot.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        pendingBets.add(player.getUUID());

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l═══════ Würfel des Schicksals ═══════"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(betOption("unter", "Unter 7", "§7~42% Chance, guter Boost"));
        player.sendSystemMessage(betOption("ueber", "Über 7", "§7~42% Chance, guter Boost"));
        player.sendSystemMessage(betOption("genau", "Genau 7", "§7~17% Chance, aber Jackpot!"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Klicke eine Wette oder schreibe §f/backpack perks dice <unter|ueber|genau>"));
        player.sendSystemMessage(Component.literal("§6§l══════════════════════════════════"));

        return false; // Don't consume yet - wait for the bet
    }

    private Component betOption(String command, String label, String hover) {
        return Component.literal("  §e» ")
                .append(Component.literal(label).withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GOLD)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/backpack perks dice " + command))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover)))));
    }

    public static boolean hasPendingBet(UUID playerId) {
        return pendingBets.contains(playerId);
    }

    public static boolean processBet(ServerPlayer player, String choice) {
        if (!pendingBets.remove(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cKeine ausstehende Wette!"));
            return false;
        }

        String bet = choice.toLowerCase(Locale.ROOT);
        if (!bet.equals("unter") && !bet.equals("ueber") && !bet.equals("genau")) {
            player.sendSystemMessage(Component.literal("§cUngültige Wette! Nutze unter, ueber oder genau."));
            return false;
        }

        // Consume before rolling: the item may have been moved to the team
        // backpack or handed off while the menu was open.
        if (!consumeOrWarn(player, "dice_of_fate")) {
            return false;
        }

        int die1 = RANDOM.nextInt(6) + 1;
        int die2 = RANDOM.nextInt(6) + 1;
        int sum = die1 + die2;
        player.sendSystemMessage(Component.literal("§7[ 🎲" + die1 + " §7| 🎲" + die2 + " §7] = §f" + sum));

        boolean jackpot = bet.equals("genau") && sum == 7;
        boolean won = switch (bet) {
            case "unter" -> sum < 7;
            case "ueber" -> sum > 7;
            default -> sum == 7;
        };

        if (jackpot) {
            var pool = BingoItemRegistry.getItemsByRarity(ItemRarity.RARE);
            if (!pool.isEmpty()) {
                var prize = pool.get(RANDOM.nextInt(pool.size()));
                player.getInventory().add(prize.createItemStack());
            }
            player.sendSystemMessage(Component.literal("§6§l✦✦✦ JACKPOT! GENAU 7! ✦✦✦"));
        } else if (won) {
            int ticks = ModConfig.getInstance().diceOfFateGoodDurationSeconds * 20;
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 0, false, true, true));
            player.sendSystemMessage(Component.literal("§a§lWette gewonnen! §rBoost erhalten."));
        } else {
            player.heal(2.0f);
            player.sendSystemMessage(Component.literal("§8Wette verloren. Immerhin ein bisschen geheilt."));
        }

        return true;
    }

    public static void clearAllPendingBets() {
        pendingBets.clear();
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Unter/Über 7: guter Boost bei Erfolg"),
                Component.literal("§7Genau 7: Longshot auf ein Rare-Item"),
                Component.literal("§7Verloren? Kleine Trostheilung."));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
