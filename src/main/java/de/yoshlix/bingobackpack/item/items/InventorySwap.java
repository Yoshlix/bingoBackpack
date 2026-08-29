package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Swaps the entire inventory with a random enemy player.
 */
public class InventorySwap extends BingoItem {

    @Override
    public String getId() {
        return "inventory_swap";
    }

    @Override
    public String getName() {
        return "Inventar-Tausch";
    }

    @Override
    public String getDescription() {
        return "Tauscht dein komplettes Inventar mit einem zufälligen Gegner.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var playerTeam = requireTeam(player);
        if (playerTeam == null) {
            return false;
        }

        var enemies = onlineEnemies(player, playerTeam);
        if (enemies.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Keine gegnerischen Spieler online! (Oder alle geschützt)"));
            return false;
        }

        ServerPlayer target = enemies.get(RANDOM.nextInt(enemies.size()));
        var server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();

        // Swap slot-for-slot instead of clearing both inventories and refilling
        // them from saved copies: that approach wrote each side's items back by
        // raw index, which silently overwrote (and lost) whatever unbreakable
        // item the other player had at the same index. A slot only moves here if
        // neither side needs to keep what's already in it.
        int size = Math.min(player.getInventory().getContainerSize(), target.getInventory().getContainerSize());
        int swapped = 0;
        for (int i = 0; i < size; i++) {
            ItemStack playerStack = player.getInventory().getItem(i);
            ItemStack targetStack = target.getInventory().getItem(i);

            if (isUnbreakable(playerStack) || isUnbreakable(targetStack)
                    || isThisItem(playerStack) || isThisItem(targetStack)) {
                continue;
            }
            if (playerStack.isEmpty() && targetStack.isEmpty()) {
                continue;
            }

            player.getInventory().setItem(i, targetStack.copy());
            target.getInventory().setItem(i, playerStack.copy());
            swapped++;
        }

        player.sendSystemMessage(Component.literal("§a§lSWAP! §rDu hast das Inventar mit §e" +
                target.getName().getString() + " §rgetauscht! §7(" + swapped + " Slots)"));
        target.sendSystemMessage(Component.literal("§c§lSWAP! §r" + player.getName().getString() +
                " §rhat dein Inventar gestohlen!"));

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6§l⚡ §e" + player.getName().getString() + " §6und §e" +
                        target.getName().getString() + " §6haben Inventare getauscht!"),
                false);

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§c§lEXTREM RISKANT!"),
                Component.literal("§7Dein Inventar geht auch weg..."));
    }

    @Override
    public boolean canDropFromMob() {
        return true; // Too powerful
    }

    /**
     * Check if an item is unbreakable (part of starter kit).
     */
    private boolean isUnbreakable(ItemStack stack) {
        return stack.has(DataComponents.UNBREAKABLE);
    }

    /** Whether this stack is another copy of the swap item itself. */
    private boolean isThisItem(ItemStack stack) {
        return BingoItemRegistry.fromItemStack(stack)
                .map(item -> item.getId().equals(getId()))
                .orElse(false);
    }
}
