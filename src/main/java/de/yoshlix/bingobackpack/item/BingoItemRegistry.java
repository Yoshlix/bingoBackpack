package de.yoshlix.bingobackpack.item;

import de.yoshlix.bingobackpack.BingoBackpack;
import de.yoshlix.bingobackpack.item.items.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.*;

/**
 * Registry for all Bingo Items.
 * 
 * To add a new item:
 * 1. Create a new class extending BingoItem
 * 2. Register it in the static block below using register()
 */
public class BingoItemRegistry {

    private static final Map<String, BingoItem> ITEMS = new HashMap<>();
    private static final List<BingoItem> DROPPABLE_ITEMS = new ArrayList<>();
    private static boolean initialized = false;

    /**
     * Initialize and register all items.
     * Call this once during mod initialization.
     */
    public static void init() {
        if (initialized)
            return;

        // ========================================
        // REGISTER YOUR ITEMS HERE
        // ========================================

        // Example items - uncomment or add your own:
        // register(new SpeedBoostItem());
        // register(new HealingItem());
        // register(new TeleportHomeItem());
        // register(new ExtraHeartsItem());
        // register(new InvisibilityItem());

        // ========================================
        register(new ExampleSpeedBoostItem());
        register(new CompleteRandomBingoField());
        initialized = true;
        BingoBackpack.LOGGER.info("Registered {} Bingo Items ({} droppable)",
                ITEMS.size(), DROPPABLE_ITEMS.size());
    }

    /**
     * Register a new Bingo Item.
     */
    public static void register(BingoItem item) {
        if (ITEMS.containsKey(item.getId())) {
            BingoBackpack.LOGGER.warn("Duplicate Bingo Item ID: {}", item.getId());
            return;
        }

        ITEMS.put(item.getId(), item);

        if (item.canDropFromMob()) {
            DROPPABLE_ITEMS.add(item);
        }

        BingoBackpack.LOGGER.debug("Registered Bingo Item: {} ({})",
                item.getName(), item.getRarity().getDisplayName());
    }

    /**
     * Get a Bingo Item by its ID.
     */
    public static Optional<BingoItem> getById(String id) {
        return Optional.ofNullable(ITEMS.get(id));
    }

    /**
     * Get the Bingo Item from an ItemStack, if it is one.
     */
    public static Optional<BingoItem> fromItemStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.PAPER)) {
            return Optional.empty();
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return Optional.empty();
        }

        CompoundTag tag = customData.copyTag();
        if (!tag.contains(BingoItem.NBT_KEY)) {
            return Optional.empty();
        }

        String id = tag.getString(BingoItem.NBT_KEY).orElse("");
        return getById(id);
    }

    /**
     * Check if an ItemStack is a Bingo Item.
     */
    public static boolean isBingoItem(ItemStack stack) {
        return fromItemStack(stack).isPresent();
    }

    /**
     * Get all registered items.
     */
    public static Collection<BingoItem> getAllItems() {
        return Collections.unmodifiableCollection(ITEMS.values());
    }

    /**
     * Get all items that can drop from mobs.
     */
    public static List<BingoItem> getDroppableItems() {
        return Collections.unmodifiableList(DROPPABLE_ITEMS);
    }

    /**
     * Get all items of a specific rarity.
     */
    public static List<BingoItem> getItemsByRarity(ItemRarity rarity) {
        return ITEMS.values().stream()
                .filter(item -> item.getRarity() == rarity)
                .toList();
    }

    /**
     * Get a random droppable item, or empty if none available.
     */
    public static Optional<BingoItem> getRandomDroppableItem(Random random) {
        if (DROPPABLE_ITEMS.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(DROPPABLE_ITEMS.get(random.nextInt(DROPPABLE_ITEMS.size())));
    }
}
