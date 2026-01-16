package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * TEMPLATE: Copy this file to create a new Bingo Item
 * 
 * Steps:
 * 1. Copy this file and rename it (e.g., MyAwesomeItem.java)
 * 2. Change the class name
 * 3. Fill in all the methods
 * 4. Register in BingoItemRegistry.init():
 * register(new MyAwesomeItem());
 */
public class _ItemTemplate extends BingoItem {

    // Optional: Define constants for your item
    // private static final int BASE_VALUE = 10;

    @Override
    public String getId() {
        // Unique ID - lowercase with underscores
        // Example: "my_awesome_item"
        return "template_item";
    }

    @Override
    public String getName() {
        // Display name (will be colored by rarity)
        // Example: "Magischer Kristall"
        return "Template Item";
    }

    @Override
    public String getDescription() {
        // Short description for tooltip
        // Example: "Gibt dir magische Kräfte."
        return "Beschreibung des Items.";
    }

    @Override
    public ItemRarity getRarity() {
        // Choose one:
        // - ItemRarity.COMMON (15% base drop, white)
        // - ItemRarity.UNCOMMON (8% base drop, green)
        // - ItemRarity.RARE (4% base drop, blue)
        // - ItemRarity.EPIC (1.5% base drop, purple)
        // - ItemRarity.LEGENDARY (0.5% base drop, gold)
        return ItemRarity.COMMON;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        // Called when player right-clicks with this item
        //
        // Useful methods:
        // - player.addEffect(...) - add potion effect
        // - player.heal(amount) - heal player
        // - player.teleportTo(x, y, z) - teleport
        // - player.sendSystemMessage(...) - send message
        // - getEffectStrength() - multiplier based on rarity
        // - getDurationTicks(baseSeconds) - duration scaled by rarity
        //
        // Return true to consume the item
        // Return false to keep the item (e.g., if conditions not met)

        player.sendSystemMessage(Component.literal("§aItem wurde verwendet!"));

        return true;
    }

    // ========================================
    // OPTIONAL OVERRIDES (delete if not needed)
    // ========================================

    @Override
    public List<Component> getExtraLore() {
        // Add extra tooltip lines
        // return List.of(
        // Component.literal("Extra Info").withStyle(ChatFormatting.AQUA)
        // );
        return List.of();
    }

    @Override
    public double getDropChanceMultiplier() {
        // Modify drop chance (1.0 = normal, 2.0 = double, 0.5 = half)
        return 1.0;
    }

    @Override
    public boolean canDropFromMob() {
        // Set to false if item should only come from bingo rows
        return true;
    }
}
