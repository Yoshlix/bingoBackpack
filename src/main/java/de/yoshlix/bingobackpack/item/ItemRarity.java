package de.yoshlix.bingobackpack.item;

import de.yoshlix.bingobackpack.ModConfig;
import net.minecraft.ChatFormatting;

/**
 * Rarity levels for Bingo Items.
 * Each rarity has different drop chances and visual styling.
 */
public enum ItemRarity {
    COMMON("Common", ChatFormatting.WHITE),
    UNCOMMON("Uncommon", ChatFormatting.GREEN),
    RARE("Rare", ChatFormatting.BLUE),
    EPIC("Epic", ChatFormatting.DARK_PURPLE),
    LEGENDARY("Legendary", ChatFormatting.GOLD);

    private final String displayName;
    private final ChatFormatting color;

    ItemRarity(String displayName, ChatFormatting color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getColor() {
        return color;
    }

    /**
     * Base drop chance when killing mobs (0.0 - 1.0)
     * Values are loaded from ModConfig for easy customization.
     */
    public double getBaseDropChance() {
        ModConfig config = ModConfig.getInstance();
        return switch (this) {
            case COMMON -> config.dropChanceCommon;
            case UNCOMMON -> config.dropChanceUncommon;
            case RARE -> config.dropChanceRare;
            case EPIC -> config.dropChanceEpic;
            case LEGENDARY -> config.dropChanceLegendary;
        };
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
