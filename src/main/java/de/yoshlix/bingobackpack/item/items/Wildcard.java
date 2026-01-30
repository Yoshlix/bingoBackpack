package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.BingoItemManager;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Lets the player choose any Rare item from the registry.
 * The ultimate utility item!
 */
public class Wildcard extends BingoItem {

    private static final Map<UUID, List<BingoItem>> pendingSelections = new HashMap<>();

    @Override
    public String getId() {
        return "wildcard";
    }

    @Override
    public String getName() {
        return "Wildcard";
    }

    @Override
    public String getDescription() {
        return "Wähle ein beliebiges Rare-Item aus.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        // Get all Rare items
        List<BingoItem> rareItems = BingoItemRegistry.getItemsByRarity(ItemRarity.RARE);

        if (rareItems.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKeine Rare-Items verfügbar!"));
            return false;
        }

        // Store pending selection
        pendingSelections.put(player.getUUID(), rareItems);

        // Show selection menu
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§9§l═══════ Wildcard: Wähle Item ═══════"));
        player.sendSystemMessage(Component.literal(""));

        int index = 1;
        for (BingoItem item : rareItems) {
            Component message = Component.literal("  §e[" + index + "] ")
                    .append(Component.literal(item.getName()).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.BLUE)
                            .withBold(true)
                            .withClickEvent(new ClickEvent.RunCommand("/backpack perks wildcard " + item.getId()))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("§9§l" + item.getName() + "\n§7" + item.getDescription()
                                            + "\n\n§eKlicke zum Auswählen!")))));
            player.sendSystemMessage(message);
            index++;
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Klicke auf ein Item zum Auswählen"));
        player.sendSystemMessage(Component.literal("§9§l═══════════════════════════════════"));

        return false; // Don't consume yet - wait for selection
    }

    /**
     * Called when a player selects an item.
     */
    public static boolean selectItem(ServerPlayer player, String itemId) {
        List<BingoItem> validItems = pendingSelections.get(player.getUUID());
        if (validItems == null) {
            player.sendSystemMessage(Component.literal("§cKeine ausstehende Wildcard-Auswahl!"));
            return false;
        }

        BingoItem selectedItem = null;
        for (BingoItem item : validItems) {
            if (item.getId().equals(itemId)) {
                selectedItem = item;
                break;
            }
        }

        if (selectedItem == null) {
            player.sendSystemMessage(Component.literal("§cUngültiges Item!"));
            return false;
        }

        // Give the selected item
        BingoItemManager.getInstance().giveItem(player, selectedItem);

        player.sendSystemMessage(Component.literal("§6§l✦ WILDCARD! §r§6Du hast ")
                .append(Component.literal(selectedItem.getName()).withStyle(selectedItem.getRarity().getColor()))
                .append(Component.literal(" §6gewählt!")));

        // Consume the Wildcard item
        consumeItem(player);

        // Clean up
        pendingSelections.remove(player.getUUID());

        return true;
    }

    private static void consumeItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            var itemOpt = de.yoshlix.bingobackpack.item.BingoItemRegistry.fromItemStack(stack);
            if (itemOpt.isPresent() && itemOpt.get().getId().equals("wildcard")) {
                stack.shrink(1);
                return;
            }
        }
    }

    /**
     * Check if player has a pending wildcard selection.
     */
    public static boolean hasPendingSelection(UUID playerId) {
        return pendingSelections.containsKey(playerId);
    }

    /**
     * Cancel a pending selection.
     */
    public static void cancelSelection(UUID playerId) {
        pendingSelections.remove(playerId);
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Wähle ein beliebiges §9Rare §7Item!"),
                Component.literal("§7Die ultimative Flexibilität"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
