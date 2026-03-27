package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class TaskEscapeRoom implements BanishTask {
    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        TaskUtils.fill(level, origin.offset(-10, -5, -10), origin.offset(10, 10, 10), Blocks.AIR);
        TaskUtils.hollowBox(level, origin.offset(-3, -1, -3), origin.offset(3, 5, 3), Blocks.BEDROCK, Blocks.AIR);
        
        level.setBlock(origin.offset(0, 4, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
        
        // Wood block to start with
        level.setBlock(origin.offset(0, 0, 2), Blocks.OAK_LOG.defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 0, 2), Blocks.OAK_LOG.defaultBlockState(), 3);
        
        // Reaching the win condition: Any wooden button pressed inside the room.
        return getSpawnPos(origin);
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        // Any wooden button pressed inside the 5x5 room
        return level.getBlockState(interactedBlock).is(BlockTags.WOODEN_BUTTONS) && interactedBlock.distSqr(origin) < 100;
    }

    @Override
    public void tick(ServerPlayer player, BlockPos origin) {
        BanishTask.super.tick(player, origin);
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        return new Vec3(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
    }
}
