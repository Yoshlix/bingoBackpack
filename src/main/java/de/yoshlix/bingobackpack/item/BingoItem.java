package de.yoshlix.bingobackpack.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all Bingo Items.
 * 
 * To create a new item, extend this class and implement:
 * - getId(): Unique identifier for this item
 * - getName(): Display name for this item
 * - getDescription(): Description shown in lore
 * - getRarity(): The rarity level
 * - onUse(): The action when right-clicked
 * 
 * Optional overrides:
 * - getDropChanceMultiplier(): Modify the drop chance from mobs
 * - canDropFromMob(): Whether this item can drop from mobs
 * - getExtraLore(): Additional lore lines
 */
public abstract class BingoItem {

    public static final String NBT_KEY = "BingoItemId";

    /**
     * Unique identifier for this item type.
     * Used for NBT storage and registry lookup.
     */
    public abstract String getId();

    /**
     * Display name for this item (without color formatting).
     */
    public abstract String getName();

    /**
     * Short description of what this item does.
     */
    public abstract String getDescription();

    /**
     * The rarity level of this item.
     */
    public abstract ItemRarity getRarity();

    /**
     * Called when a player right-clicks with this item.
     * The item will be consumed after this is called (unless you return false).
     * 
     * @param player The player who used the item
     * @return true if the item should be consumed, false to keep it
     */
    public abstract boolean onUse(ServerPlayer player);

    /**
     * Multiplier for the drop chance from mobs.
     * Default is 1.0 (uses rarity base chance).
     * Override to make specific items more or less common.
     */
    public double getDropChanceMultiplier() {
        return 1.0;
    }

    /**
     * Whether this item can drop from killing mobs.
     * Override and return false for items that should only
     * be obtained through other means (like completing bingo rows).
     */
    public boolean canDropFromMob() {
        return true;
    }

    /**
     * Additional lore lines to display.
     * Override to add custom information.
     */
    public List<Component> getExtraLore() {
        return List.of();
    }

    /**
     * Calculate the actual drop chance for this item.
     */
    public double getDropChance() {
        return getRarity().getBaseDropChance() * getDropChanceMultiplier();
    }

    /**
     * Create an ItemStack representing this Bingo Item.
     */
    public ItemStack createItemStack() {
        return createItemStack(1);
    }

    /**
     * Create an ItemStack with a specific count.
     */
    public ItemStack createItemStack(int count) {
        ItemStack stack = new ItemStack(Items.PAPER, count);

        // Set custom name with rarity color
        Component displayName = Component.literal(getName())
                .withStyle(Style.EMPTY
                        .withColor(getRarity().getColor())
                        .withItalic(false));
        stack.set(DataComponents.CUSTOM_NAME, displayName);

        // Build lore
        List<Component> loreList = new ArrayList<>();

        // Rarity line
        loreList.add(Component.literal(getRarity().getDisplayName())
                .withStyle(Style.EMPTY
                        .withColor(getRarity().getColor())
                        .withItalic(true)));

        // Empty line
        loreList.add(Component.empty());

        // Description
        loreList.add(Component.literal(getDescription())
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GRAY)
                        .withItalic(false)));

        // Extra lore from subclass
        List<Component> extraLore = getExtraLore();
        if (!extraLore.isEmpty()) {
            loreList.add(Component.empty());
            loreList.addAll(extraLore);
        }

        // Usage hint
        loreList.add(Component.empty());
        loreList.add(Component.literal("Rechtsklick zum Einlösen")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.YELLOW)
                        .withItalic(true)));

        stack.set(DataComponents.LORE, new ItemLore(loreList));

        // Store item ID in NBT
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_KEY, getId());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        return stack;
    }

    /**
     * Check if an ItemStack is this specific Bingo Item.
     */
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.PAPER)) {
            return false;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        CompoundTag tag = customData.copyTag();
        return tag.contains(NBT_KEY) && tag.getString(NBT_KEY).orElse("").equals(getId());
    }

    /**
     * Utility method to get the effect strength based on rarity.
     */
    protected double getEffectStrength() {
        return getRarity().getEffectMultiplier();
    }

    /**
     * Utility method to get the duration multiplier based on rarity.
     */
    protected double getDurationMultiplier() {
        return getRarity().getDurationMultiplier();
    }

    /**
     * Utility method to calculate a duration in ticks.
     * 
     * @param baseSeconds Base duration in seconds
     * @return Duration in ticks, scaled by rarity
     */
    protected int getDurationTicks(int baseSeconds) {
        return (int) (baseSeconds * 20 * getDurationMultiplier());
    }
}
