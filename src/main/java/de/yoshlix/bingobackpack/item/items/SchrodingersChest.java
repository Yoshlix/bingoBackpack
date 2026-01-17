package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Schrödingers Kiste - Spawns a chest with completely random loot.
 * Could be diamonds, could be dirt, could be another Bingo item, could be
 * empty!
 */
public class SchrodingersChest extends BingoItem {

    private final Random random = new Random();

    @Override
    public String getId() {
        return "schrodingers_chest";
    }

    @Override
    public String getName() {
        return "Schrödingers Kiste";
    }

    @Override
    public String getDescription() {
        return "Spawnt eine Kiste mit... irgendwas? Diamanten? Dirt? Nichts?";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        // Find a suitable position for the chest (at player's feet or in front)
        BlockPos chestPos = player.blockPosition();

        // Try to find air block nearby
        if (!level.getBlockState(chestPos).isAir()) {
            chestPos = chestPos.above();
        }
        if (!level.getBlockState(chestPos).isAir()) {
            // Try in front of player
            int facing = (int) ((player.getYRot() + 180) / 90) % 4;
            chestPos = player.blockPosition().relative(
                    facing == 0 ? net.minecraft.core.Direction.SOUTH
                            : facing == 1 ? net.minecraft.core.Direction.WEST
                                    : facing == 2 ? net.minecraft.core.Direction.NORTH
                                            : net.minecraft.core.Direction.EAST);
        }

        // Place the chest
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);

        // Get the chest block entity
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            // Generate random loot
            List<ItemStack> loot = generateRandomLoot();

            // Put loot in random slots
            for (ItemStack stack : loot) {
                int slot = random.nextInt(27);
                // Find empty slot if occupied
                for (int i = 0; i < 27 && !chest.getItem(slot).isEmpty(); i++) {
                    slot = (slot + 1) % 27;
                }
                if (chest.getItem(slot).isEmpty()) {
                    chest.setItem(slot, stack);
                }
            }
        }

        // Play mysterious sound
        level.playSound(null, chestPos, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 1.0f, 0.5f);

        // Dramatic message
        String[] messages = {
                "§5§l✦ §dDie Kiste materialisiert sich... §5§l✦",
                "§5§l✦ §dQuantenfluktuationen stabilisieren sich... §5§l✦",
                "§5§l✦ §dDas Schicksal ist besiegelt... §5§l✦"
        };
        player.sendSystemMessage(Component.literal(messages[random.nextInt(messages.length)]));
        player.sendSystemMessage(Component.literal("§7Eine mysteriöse Kiste erscheint bei §f" +
                chestPos.getX() + ", " + chestPos.getY() + ", " + chestPos.getZ()));

        return true;
    }

    private List<ItemStack> generateRandomLoot() {
        List<ItemStack> loot = new ArrayList<>();

        // Roll for loot tier (weighted)
        int roll = random.nextInt(100);

        if (roll < 5) {
            // 5% - EMPTY! Unlucky!
            // No items added
        } else if (roll < 25) {
            // 20% - Trash tier
            loot.add(new ItemStack(Items.DIRT, 32 + random.nextInt(32)));
            if (random.nextBoolean()) {
                loot.add(new ItemStack(Items.COBBLESTONE, 16 + random.nextInt(48)));
            }
            if (random.nextBoolean()) {
                loot.add(new ItemStack(Items.ROTTEN_FLESH, 8 + random.nextInt(16)));
            }
        } else if (roll < 50) {
            // 25% - Common tier
            loot.add(new ItemStack(Items.IRON_INGOT, 4 + random.nextInt(12)));
            loot.add(new ItemStack(Items.COAL, 8 + random.nextInt(24)));
            if (random.nextBoolean()) {
                loot.add(new ItemStack(Items.GOLD_INGOT, 2 + random.nextInt(6)));
            }
            if (random.nextBoolean()) {
                loot.add(new ItemStack(Items.BREAD, 8 + random.nextInt(8)));
            }
        } else if (roll < 75) {
            // 25% - Good tier
            loot.add(new ItemStack(Items.DIAMOND, 2 + random.nextInt(4)));
            loot.add(new ItemStack(Items.IRON_INGOT, 8 + random.nextInt(16)));
            if (random.nextBoolean()) {
                loot.add(new ItemStack(Items.ENDER_PEARL, 2 + random.nextInt(4)));
            }
            if (random.nextBoolean()) {
                loot.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 4 + random.nextInt(12)));
            }
        } else if (roll < 90) {
            // 15% - Great tier
            loot.add(new ItemStack(Items.DIAMOND, 6 + random.nextInt(10)));
            loot.add(new ItemStack(Items.NETHERITE_SCRAP, 1 + random.nextInt(2)));
            loot.add(new ItemStack(Items.GOLDEN_APPLE, 2 + random.nextInt(3)));
            if (random.nextBoolean()) {
                loot.add(new ItemStack(Items.BLAZE_ROD, 4 + random.nextInt(8)));
            }
        } else if (roll < 98) {
            // 8% - Jackpot tier
            loot.add(new ItemStack(Items.DIAMOND_BLOCK, 1 + random.nextInt(3)));
            loot.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
            loot.add(new ItemStack(Items.NETHERITE_INGOT, 1));
            loot.add(new ItemStack(Items.ELYTRA, 1));
        } else {
            // 2% - Bingo Item Jackpot!
            // Add a random Bingo item
            var items = BingoItemRegistry.getDroppableItems();
            if (!items.isEmpty()) {
                var randomItem = items.get(random.nextInt(items.size()));
                loot.add(randomItem.createItemStack());
                // Maybe add a second one
                if (random.nextBoolean()) {
                    randomItem = items.get(random.nextInt(items.size()));
                    loot.add(randomItem.createItemStack());
                }
            }
            // Plus some diamonds
            loot.add(new ItemStack(Items.DIAMOND, 4 + random.nextInt(8)));
        }

        return loot;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§d§oWas ist in der Kiste?"),
                Component.literal("§7💎 Diamanten?"),
                Component.literal("§7🪨 Dirt?"),
                Component.literal("§7📦 Ein Bingo-Item?"),
                Component.literal("§7🕳️ ...Nichts?"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
