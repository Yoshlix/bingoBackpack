package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class TaskArena implements BanishTask {
    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        // Clear area
        TaskUtils.fill(level, origin.offset(-15, -5, -15), origin.offset(15, 10, 15), Blocks.AIR);
        // Build hollow bedrock box
        TaskUtils.hollowBox(level, origin.offset(-10, -1, -10), origin.offset(10, 6, 10), Blocks.BEDROCK, Blocks.AIR);
        // Add torches
        level.setBlock(origin.offset(0, 4, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
        
        return getSpawnPos(origin);
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        return level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON) && interactedBlock.distSqr(origin) < 400;
    }

    @Override
    public void tick(ServerPlayer player, BlockPos origin) {
        BanishData data = BanishManager.getInstance().getBanishData(player);
        if (data == null) return;
        
        data.taskTime++;
        ServerLevel level = (ServerLevel) player.level();
        
        if (data.taskTime == 40) { // 2 seconds in
            player.getInventory().clearContent();
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            player.getInventory().add(new ItemStack(Items.COOKED_BEEF, 16));
            
            // Spawn 10 zombies
            for (int i=0; i<10; i++) {
                BlockPos spawnPos = new BlockPos((int)(origin.getX() - 5 + Math.random()*10), (int)origin.getY() + 1, (int)(origin.getZ() - 5 + Math.random()*10));
                EntityType.ZOMBIE.spawn(level, spawnPos, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            }
            player.sendSystemMessage(Component.literal("§eÜberlebe und besiege die Zombies! Der Button erscheint in 3 Minuten oder wenn alle besiegt sind."));
        }
        
        // Appear win button after 3 minutes (180 secs = 3600 ticks) OR if time > 100 ticks and no zombies left
        boolean giveButton = false;
        if (data.taskTime > 3600) {
            giveButton = true;
        } else if (data.taskTime > 100 && data.taskTime % 40 == 0) { // check every 2 seconds
            long zombieCount = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(15)).stream().filter(e -> e.getType() == EntityType.ZOMBIE).count();
            if (zombieCount == 0) {
                giveButton = true;
            }
        }
        
        if (giveButton && level.getBlockState(origin).isAir()) {
            level.setBlock(origin, Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);
            player.sendSystemMessage(Component.literal("§aDu hast es geschafft! Der Flucht-Button befindet sich jetzt in der Mitte!"));
        }
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        return new Vec3(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
    }
}
