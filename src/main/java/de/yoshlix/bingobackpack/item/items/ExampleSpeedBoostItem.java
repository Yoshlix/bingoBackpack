package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.ChatFormatting;

import java.util.List;

/**
 * EXAMPLE ITEM: Speed Boost
 * 
 * Gives the player a speed boost for a duration based on rarity.
 * This is a complete example showing all the features of the item system.
 * 
 * Copy this file and modify it to create your own items!
 */
public class ExampleSpeedBoostItem extends BingoItem {

    private static final int BASE_DURATION_SECONDS = 30; // Base duration
    private static final int BASE_AMPLIFIER = 0; // Speed I

    @Override
    public String getId() {
        // Unique ID - use lowercase with underscores
        return "speed_boost";
    }

    @Override
    public String getName() {
        // Display name - will be colored by rarity automatically
        return "Geschwindigkeits-Boost";
    }

    @Override
    public String getDescription() {
        // Short description of what the item does
        return "Gibt dir einen Geschwindigkeitsboost.";
    }

    @Override
    public ItemRarity getRarity() {
        // Choose: COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
        return ItemRarity.UNCOMMON;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        // This is called when the player right-clicks with the item

        // Calculate duration based on rarity
        int durationTicks = getDurationTicks(BASE_DURATION_SECONDS);

        // Calculate amplifier based on rarity (higher rarity = stronger effect)
        int amplifier = BASE_AMPLIFIER + (int) (getEffectStrength() - 1);
        amplifier = Math.min(amplifier, 2); // Cap at Speed III

        // Apply the effect
        player.addEffect(new MobEffectInstance(
                MobEffects.SPEED,
                durationTicks,
                amplifier,
                false, // ambient
                true, // show particles
                true // show icon
        ));

        // Send a message to the player
        player.sendSystemMessage(Component.literal("§a§lWOOSH! §rDu bist jetzt schneller!"));

        // Return true to consume the item
        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        // Optional: Add extra information to the item tooltip
        int durationSeconds = (int) (BASE_DURATION_SECONDS * getDurationMultiplier());
        int level = BASE_AMPLIFIER + (int) (getEffectStrength() - 1) + 1;

        return List.of(
                Component.literal("Schnelligkeit " + toRoman(level))
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withItalic(false)),
                Component.literal("Dauer: " + durationSeconds + " Sekunden")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)));
    }

    @Override
    public double getDropChanceMultiplier() {
        // Optional: Modify drop chance (1.0 = normal, 2.0 = double, 0.5 = half)
        return 1.0;
    }

    @Override
    public boolean canDropFromMob() {
        // Set to false if this item should only come from bingo rows
        return true;
    }

    // Helper method for roman numerals
    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }
}
