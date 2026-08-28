package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instant Furnace - Instantly smelts ALL smeltable items in your inventory.
 * No fuel needed, no waiting!
 */
public class InstantFurnace extends BingoItem {

    @Override
    public String getId() {
        return "instant_furnace";
    }

    @Override
    public String getName() {
        return "Instant-Schmelze";
    }

    @Override
    public String getDescription() {
        return "Schmelzt SOFORT alles Schmelzbare in deinem Inventar!";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        var recipeManager = level.getServer().getRecipeManager();

        int totalSmelted = 0;
        Map<String, Integer> smeltedItems = new HashMap<>();

        // Go through entire inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty())
                continue;

            // Try to find a smelting recipe for this item
            var input = new SingleRecipeInput(stack);
            var recipeOptional = recipeManager.getRecipeFor(RecipeType.SMELTING, input, level);

            if (recipeOptional.isPresent()) {
                var recipe = recipeOptional.get();
                // MC 26.2: assemble() no longer takes a RegistryAccess
                ItemStack result = recipe.value().assemble(input);

                if (!result.isEmpty()) {
                    int count = stack.getCount();

                    // Create the smelted result with same count
                    ItemStack smeltedStack = result.copy();
                    smeltedStack.setCount(count);

                    // Replace in inventory
                    player.getInventory().setItem(i, smeltedStack);

                    totalSmelted += count;
                    String itemName = result.getHoverName().getString();
                    smeltedItems.merge(itemName, count, Integer::sum);
                }
            }
        }

        if (totalSmelted == 0) {
            player.sendSystemMessage(Component.literal("§6Nichts zum Schmelzen im Inventar!"));
            return false;
        }

        // Play smelting sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.PLAYERS, 1.0f, 1.2f);

        // Show what was smelted
        player.sendSystemMessage(Component.literal("§6§l🔥 WHOOSH! §r§6Alles geschmolzen!"));
        player.sendSystemMessage(Component.literal("§7Insgesamt §f" + totalSmelted + " §7Items geschmolzen:"));

        for (var entry : smeltedItems.entrySet()) {
            player.sendSystemMessage(Component.literal("  §7• §f" + entry.getValue() + "x " + entry.getKey()));
        }

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§6🔥 Kein Ofen nötig!"),
                Component.literal("§7Schmelzt Erze → Ingots"),
                Component.literal("§7Schmelzt Sand → Glas"),
                Component.literal("§7Schmelzt alles andere auch!"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
