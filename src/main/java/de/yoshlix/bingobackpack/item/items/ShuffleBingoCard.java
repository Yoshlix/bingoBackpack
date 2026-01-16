package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Shuffles the entire bingo card - extremely rare and powerful!
 */
public class ShuffleBingoCard extends BingoItem {

    @Override
    public String getId() {
        return "shuffle_bingo_card";
    }

    @Override
    public String getName() {
        return "Bingo Neu Mischen";
    }

    @Override
    public String getDescription() {
        return "Mischt die gesamte Bingo-Karte neu! EXTREM SELTEN!";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var cardService = BingoApi.getCardService();
        var game = BingoApi.getGameExtended();

        if (cardService == null || game == null) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        var oldCard = game.getActiveCard();
        if (oldCard == null) {
            player.sendSystemMessage(Component.literal("§cKeine Bingo-Karte vorhanden!"));
            return false;
        }

        // Generate new seed for complete reroll
        long newSeed = System.currentTimeMillis();

        var newCard = cardService.rerollCard(null, newSeed);

        if (newCard != null) {
            player.sendSystemMessage(Component.literal("§6§l★★★ BINGO KARTE NEU GEMISCHT! ★★★"));

            var server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();

            // Announce to everyone
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§4§l⚠ ACHTUNG! ⚠")
                            .append(Component.literal("\n§e" + player.getName().getString()))
                            .append(Component.literal(" §6hat die §c§lGESAMTE BINGO KARTE §6neu gemischt!"))
                            .append(Component.literal("\n§7Alle Felder wurden zurückgesetzt!")),
                    false);

            // Play dramatic sound to all players
            for (var p : server.getPlayerList().getPlayers()) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                        net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN,
                        net.minecraft.sounds.SoundSource.MASTER,
                        1.0f, 0.5f);
            }

            return true;
        } else {
            player.sendSystemMessage(Component.literal("§cFehler beim Neu-Mischen!"));
            return false;
        }
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§4§lEXTREM MÄCHTIG!"),
                Component.literal("§cMischt ALLE Felder neu!"),
                Component.literal("§7Für alle Teams!"));
    }

    @Override
    public boolean canDropFromMob() {
        return false; // Too powerful - only from special events
    }

    @Override
    public double getDropChanceMultiplier() {
        return 0.1; // Very rare
    }
}
