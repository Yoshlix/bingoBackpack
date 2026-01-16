package de.yoshlix.bingobackpack.item;

import net.minecraft.ChatFormatting;

/**
 * Rarity levels for Bingo Items.
 * Each rarity has different drop chances and visual styling.
 */
public enum ItemRarity {
    COMMON("Common", ChatFormatting.WHITE, 0.15), // 15% base drop chance
    UNCOMMON("Uncommon", ChatFormatting.GREEN, 0.08), // 8% base drop chance
    RARE("Rare", ChatFormatting.BLUE, 0.04), // 4% base drop chance
    EPIC("Epic", ChatFormatting.DARK_PURPLE, 0.025), // 2.5% base drop chance
    LEGENDARY("Legendary", ChatFormatting.GOLD, 0.05); // 0.5% base drop chance

    private final String displayName;
    private final ChatFormatting color;
    private final double baseDropChance;

    ItemRarity(String displayName, ChatFormatting color, double baseDropChance) {
        this.displayName = displayName;
        this.color = color;
        this.baseDropChance = baseDropChance;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getColor() {
        return color;
    }

    /**
     * Base drop chance when killing mobs (0.0 - 1.0)
     */
    public double getBaseDropChance() {
        return baseDropChance;
    }

    /**
     * Get a multiplier for effect strength based on rarity
     */
    public double getEffectMultiplier() {
        return switch (this) {
            case COMMON -> 1.0;
            case UNCOMMON -> 1.5;
            case RARE -> 2.0;
            case EPIC -> 3.0;
            case LEGENDARY -> 5.0;
        };
    }

    /**
     * Get duration multiplier for timed effects
     */
    public double getDurationMultiplier() {
        return switch (this) {
            case COMMON -> 1.0;
            case UNCOMMON -> 1.25;
            case RARE -> 1.5;
            case EPIC -> 2.0;
            case LEGENDARY -> 3.0;
        };
    }
}
