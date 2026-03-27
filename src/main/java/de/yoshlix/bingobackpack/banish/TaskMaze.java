package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TaskMaze implements BanishTask {
    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        // Simple manually generated maze or empty room with pillars for now.
        // A true random maze generator is a bit complex for a one-file task. Let's make a grid maze.
        int width = 21;
        int height = 21;
        
        TaskUtils.fill(level, origin.offset(-1, -1, -1), origin.offset(width, 4, height), Blocks.AIR);
        TaskUtils.fill(level, origin.offset(0, -1, 0), origin.offset(width-1, -1, height-1), Blocks.BEDROCK);
        TaskUtils.fill(level, origin.offset(0, 3, 0), origin.offset(width-1, 3, height-1), Blocks.BEDROCK);
        
        Random rand = new Random(origin.asLong());

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                if (x == 0 || x == width-1 || z == 0 || z == height-1 || (x%2 == 0 && z%2 == 0)) {
                    TaskUtils.fill(level, origin.offset(x, 0, z), origin.offset(x, 2, z), Blocks.BEDROCK);
                } else if (x%2 == 0 || z%2 == 0) {
                    if (rand.nextBoolean()) {
                        TaskUtils.fill(level, origin.offset(x, 0, z), origin.offset(x, 2, z), Blocks.BEDROCK);
                    }
                }
            }
        }
        
        // Ensure starting and ending points are open
        TaskUtils.fill(level, origin.offset(1, 0, 1), origin.offset(1, 2, 1), Blocks.AIR);
        TaskUtils.fill(level, origin.offset(width-2, 0, height-2), origin.offset(width-2, 2, height-2), Blocks.AIR);
        
        // Win button at the end
        level.setBlock(origin.offset(width-2, 1, height-2), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);

        return getSpawnPos(origin);
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        return level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON) && interactedBlock.distSqr(origin) < 2000;
    }

    @Override
    public void tick(ServerPlayer player, BlockPos origin) {
        BanishTask.super.tick(player, origin);
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        return new Vec3(origin.getX() + 1.5, origin.getY() + 0.1, origin.getZ() + 1.5);
    }
}
