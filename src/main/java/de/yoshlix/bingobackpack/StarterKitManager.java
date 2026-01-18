package de.yoshlix.bingobackpack;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager for the Starter Kit system.
 * Provides players with essential tools and armor at the start of a round.
 */
public class StarterKitManager {
    private static StarterKitManager instance;

    public static final String STARTERKIT_TAG = "bingobackpack_starterkit";

    public static StarterKitManager getInstance() {
        if (instance == null) {
            instance = new StarterKitManager();
        }
        return instance;
    }

    private StarterKitManager() {
    }

    /**
     * Gives the full starter kit to a player.
     * Removes any existing starter kit items first.
     */
    @SuppressWarnings("null")
    public void giveStarterKit(ServerPlayer player) {
        // Remove old starter kit items
        removeStarterKitItems(player);

        // Create and give new starter kit items
        List<ItemStack> kitItems = createStarterKitItems(player);

        // Give leather boots (equipped)
        ItemStack boots = kitItems.get(0);
        player.setItemSlot(EquipmentSlot.FEET, boots);

        // Give tools to inventory
        for (int i = 1; i < kitItems.size(); i++) {
            ItemStack item = kitItems.get(i);
            if (!player.getInventory().add(item)) {
                // If inventory is full, drop the item
                player.drop(item, false);
            }
        }

        player.sendSystemMessage(Component.literal("§a§l✓ §rStarterkit erhalten!"));
    }

    /**
     * Removes all starter kit items from a player's inventory and equipment.
     */
    private void removeStarterKitItems(ServerPlayer player) {
        // Check and remove from armor slots
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = player.getItemBySlot(slot);
            if (isStarterKitItem(equipped)) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
        }

        // Check and remove from inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isStarterKitItem(stack)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Checks if an item is a starter kit item.
     * All starter kit items are marked as UNBREAKABLE.
     */
    private boolean isStarterKitItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Check if item is unbreakable (all starter kit items are unbreakable)
        return stack.has(DataComponents.UNBREAKABLE);
    }

    /**
     * Creates all starter kit items.
     */
    private List<ItemStack> createStarterKitItems(ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>();

        // 1. Leather Boots (Unbreakable)
        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        boots = makeUnbreakable(boots);
        items.add(boots);

        // 2. Netherite Axe (Unbreakable, Efficiency 5, Looting 2)
        ItemStack axe = new ItemStack(Items.NETHERITE_AXE);
        axe = makeUnbreakable(axe);
        axe = addEnchantments(axe, player);
        items.add(axe);

        // 3. Netherite Pickaxe (Unbreakable, Efficiency 5, Fortune 3)
        ItemStack pickaxe = new ItemStack(Items.NETHERITE_PICKAXE);
        pickaxe = makeUnbreakable(pickaxe);
        pickaxe = addEnchantments(pickaxe, player);
        items.add(pickaxe);

        // 4. Netherite Shovel (Unbreakable, Efficiency 5)
        ItemStack shovel = new ItemStack(Items.NETHERITE_SHOVEL);
        shovel = makeUnbreakable(shovel);
        shovel = addEnchantments(shovel, player);
        items.add(shovel);

        // 5. Netherite Hoe (Unbreakable, Efficiency 5)
        ItemStack hoe = new ItemStack(Items.NETHERITE_HOE);
        hoe = makeUnbreakable(hoe);
        hoe = addEnchantments(hoe, player);
        items.add(hoe);

        return items;
    }

    /**
     * Makes an ItemStack unbreakable (won't lose durability).
     */
    @SuppressWarnings("null")
    private ItemStack makeUnbreakable(ItemStack stack) {
        // Set unbreakable component
        // In Minecraft 1.21+, UNBREAKABLE uses Unit type
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        return stack;
    }

    /**
     * Adds enchantments to tools based on their type.
     */
    @SuppressWarnings("null")
    private ItemStack addEnchantments(ItemStack stack, ServerPlayer player) {
        // Get current enchantments (if any)
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));

        var registry = player.level().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

        // Add Efficiency 5 to all tools
        registry.get(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY)
                .ifPresent(enchantment -> enchantments.set(enchantment, 5));

        // Add specific enchantments based on tool type
        if (stack.is(Items.NETHERITE_AXE)) {
            // Looting 2 for Axe
            registry.get(net.minecraft.world.item.enchantment.Enchantments.LOOTING)
                    .ifPresent(enchantment -> enchantments.set(enchantment, 2));
        } else if (stack.is(Items.NETHERITE_PICKAXE)) {
            // Fortune 3 for Pickaxe
            registry.get(net.minecraft.world.item.enchantment.Enchantments.FORTUNE)
                    .ifPresent(enchantment -> enchantments.set(enchantment, 3));
        }

        stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        return stack;
    }

}
