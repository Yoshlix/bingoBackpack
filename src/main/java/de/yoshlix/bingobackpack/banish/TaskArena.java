package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TaskArena implements BanishTask {

    // Which mob type this arena uses (stored per-generation via origin seed)
    private static final EntityType<?>[] MOB_TYPES = {
        EntityType.ZOMBIE,
        EntityType.SKELETON,
        EntityType.SPIDER,
        EntityType.HUSK,
        EntityType.STRAY,
        EntityType.CAVE_SPIDER,
    };

    private static final String[] MOB_NAMES = {
        "Zombies", "Skelette", "Spinnen", "Husks", "Strays", "Höhlenspinnen"
    };

    @Override
    public String getTaskDescription() {
        return "Besiege alle Monster in der Arena! Danach erscheint ein Flucht-Button.";
    }

    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        // Clear area
        TaskUtils.fill(level, origin.offset(-15, -5, -15), origin.offset(15, 10, 15), Blocks.AIR);
        // Build hollow bedrock box
        TaskUtils.hollowBox(level, origin.offset(-10, -1, -10), origin.offset(10, 6, 10), Blocks.BEDROCK, Blocks.AIR);
        // Add light
        level.setBlock(origin.offset(0, 4, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
        level.setBlock(origin.offset(-5, 4, -5), Blocks.GLOWSTONE.defaultBlockState(), 3);
        level.setBlock(origin.offset(5, 4, -5), Blocks.GLOWSTONE.defaultBlockState(), 3);
        level.setBlock(origin.offset(-5, 4, 5), Blocks.GLOWSTONE.defaultBlockState(), 3);
        level.setBlock(origin.offset(5, 4, 5), Blocks.GLOWSTONE.defaultBlockState(), 3);
        // Additional lighting along walls
        level.setBlock(origin.offset(0, 4, -5), Blocks.GLOWSTONE.defaultBlockState(), 3);
        level.setBlock(origin.offset(0, 4, 5), Blocks.GLOWSTONE.defaultBlockState(), 3);
        level.setBlock(origin.offset(-5, 4, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
        level.setBlock(origin.offset(5, 4, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);

        // Place a solid block under origin for the button later
        level.setBlock(origin, Blocks.BEDROCK.defaultBlockState(), 3);

        return getSpawnPos(origin);
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        return level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON) && interactedBlock.distSqr(origin) < 400;
    }

    private int getMobTypeIndex(BlockPos origin) {
        return Math.abs(new Random(origin.asLong()).nextInt()) % MOB_TYPES.length;
    }

    @Override
    public void tick(ServerPlayer player, BlockPos origin) {
        BanishData data = BanishManager.getInstance().getBanishData(player);
        if (data == null) return;
        
        data.taskTime++; // incremented once per second
        ServerLevel level = (ServerLevel) player.level();

        // Hunger works naturally for banished players (HungerMixin exception)
        // However, fighting doesn't drain enough exhaustion in 3 mins.
        // We artificially give them exhaustion so they *have* to eat the steak!
        if (data.taskTime > 2 && data.taskTime % 2 == 0) { // every 2 seconds
            // 0.5 per second -> ~22.5 food points over 3 mins
            player.causeFoodExhaustion(1.0f);
        }
        
        // Give gear and spawn mobs after 2 seconds
        if (data.taskTime == 2) {
            giveArenaGear(player);

            int mobIndex = getMobTypeIndex(origin);
            EntityType<?> mobType = MOB_TYPES[mobIndex];
            String mobName = MOB_NAMES[mobIndex];
            int count = (mobType == EntityType.CAVE_SPIDER) ? 15 : 10;

            for (int i = 0; i < count; i++) {
                BlockPos spawnPos = new BlockPos(
                    (int)(origin.getX() - 5 + Math.random() * 10),
                    origin.getY() + 1,
                    (int)(origin.getZ() - 5 + Math.random() * 10)
                );
                mobType.spawn(level, spawnPos, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            }
            player.sendSystemMessage(Component.literal("§e" + count + " " + mobName + " spawnen! Besiege sie alle!"));
        }
        
        // Check win condition: button appears after 180 seconds (3 min) OR all mobs dead
        boolean giveButton = false;
        if (data.taskTime > 180) {
            giveButton = true;
        } else if (data.taskTime > 5 && data.taskTime % 2 == 0) {
            int mobIndex = getMobTypeIndex(origin);
            EntityType<?> mobType = MOB_TYPES[mobIndex];
            long mobCount = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(15)
            ).stream().filter(e -> e.getType() == mobType).count();
            if (mobCount == 0) {
                giveButton = true;
            }
        }
        
        if (giveButton && !level.getBlockState(origin.above()).is(Blocks.POLISHED_BLACKSTONE_BUTTON)) {
            level.setBlock(origin.above(), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);
            player.sendSystemMessage(Component.literal("§aGeschafft! Der Flucht-Button ist in der Mitte!"));
        }
    }

    @Override
    public void onRespawn(ServerPlayer player, BlockPos origin) {
        giveArenaGear(player);
    }

    private void giveArenaGear(ServerPlayer player) {
        player.getInventory().clearContent();
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        // Shield for extra defense
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        // Food since hunger is active for banished players
        player.getInventory().add(new ItemStack(Items.COOKED_BEEF, 16));
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        return new Vec3(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
    }
}
