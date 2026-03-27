package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TaskMining implements BanishTask {
    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        TaskUtils.fill(level, origin.offset(-10, -5, -10), origin.offset(10, 10, 10), Blocks.AIR);
        TaskUtils.fill(level, origin.offset(-5, -5, -5), origin.offset(5, 5, 5), Blocks.BEDROCK);
        TaskUtils.fill(level, origin.offset(-4, -4, -4), origin.offset(4, 4, 4), Blocks.STONE);
        
        // Spawn is outside the stone ball
        TaskUtils.hollowBox(level, origin.offset(-5, 6, -5), origin.offset(5, 10, 5), Blocks.BEDROCK, Blocks.AIR);
        level.setBlock(origin.offset(0, 9, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
        
        // Let player dig down through bedrock gap
        TaskUtils.fill(level, origin.offset(-1, 5, -1), origin.offset(1, 5, 1), Blocks.STONE);
        
        // Place win button randomly inside
        Random rand = new Random(origin.asLong());
        int rx = rand.nextInt(7) - 3;
        int ry = rand.nextInt(7) - 3;
        int rz = rand.nextInt(7) - 3;
        
        // Give space
        level.setBlock(origin.offset(rx, ry, rz), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.offset(rx, ry-1, rz), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);
        
        return getSpawnPos(origin);
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        return level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON) && interactedBlock.distSqr(origin) < 400;
    }

    @Override
    public void tick(ServerPlayer player, BlockPos origin) {
        BanishData data = BanishManager.getInstance().getBanishData(player);
        if (data != null && data.taskTime == 0) {
            player.getInventory().clearContent();
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            player.setItemSlot(EquipmentSlot.MAINHAND, pick);
            data.taskTime = 1; // Mark as given gear
        }
        BanishTask.super.tick(player, origin);
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        return new Vec3(origin.getX() + 0.5, origin.getY() + 7, origin.getZ() + 0.5);
    }
}
