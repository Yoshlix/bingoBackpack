package de.yoshlix.bingobackpack.item;

import de.yoshlix.bingobackpack.BingoBackpack;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Creative Mode Tab for Bingo Items.
 * Allows testing all registered items in creative mode.
 */
public class BingoItemCreativeTab {

    public static final Identifier TAB_ID = Identifier.parse(BingoBackpack.MOD_ID + ":bingo_items");

    public static void register() {
        CreativeModeTab tab = FabricItemGroup.builder()
                .icon(() -> new ItemStack(Items.PAPER))
                .title(Component.literal("§6Bingo Items"))
                .displayItems((displayContext, entries) -> {
                    // Add all registered Bingo Items
                    for (BingoItem item : BingoItemRegistry.getAllItems()) {
                        entries.accept(item.createItemStack());
                    }

                    // If no items registered, show a placeholder info
                    if (BingoItemRegistry.getAllItems().isEmpty()) {
                        BingoBackpack.LOGGER.warn("No Bingo Items registered for Creative Tab!");
                    }
                })
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_ID, tab);
        BingoBackpack.LOGGER.info("Registered Bingo Items Creative Tab");
    }
}
