package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TaskEpsteinIsland implements BanishTask {
    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        int r = 15;
        TaskUtils.fill(level, origin.offset(-r, -5, -r), origin.offset(r, 10, r), Blocks.AIR);
        
        // Deep bedrock bounding box
        TaskUtils.hollowBox(level, origin.offset(-r, -15, -r), origin.offset(r, -1, r), Blocks.BEDROCK, Blocks.AIR);
        
        // The island surface
        for (int x = -r+1; x < r; x++) {
            for (int z = -r+1; z < r; z++) {
                if (x*x + z*z < r*r) {
                    level.setBlock(origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                    level.setBlock(origin.offset(x, -2, z), Blocks.DIRT.defaultBlockState(), 3);
                    level.setBlock(origin.offset(x, -3, z), Blocks.SAND.defaultBlockState(), 3);
                }
            }
        }
        
        // A little hut
        TaskUtils.hollowBox(level, origin.offset(-3, 0, -3), origin.offset(3, 4, 3), Blocks.BIRCH_PLANKS, Blocks.AIR);
        level.setBlock(origin.offset(0, 0, 3), Blocks.AIR.defaultBlockState(), 3); // door
        level.setBlock(origin.offset(0, 1, 3), Blocks.AIR.defaultBlockState(), 3); // door
        
        // A sign
        // level.setBlock(origin.offset(2, 1, 4), Blocks.OAK_SIGN.defaultBlockState(), 3);
        
        // A hole in the hut leading to the bunker
        TaskUtils.fill(level, origin.offset(0, -10, 0), origin.offset(0, -1, 0), Blocks.AIR);
        
        // The bunker maze
        Random rand = new Random(origin.asLong());
        for (int x = -r+2; x < r-1; x+=2) {
            for (int z = -r+2; z < r-1; z+=2) {
                if (rand.nextBoolean()) {
                    TaskUtils.fill(level, origin.offset(x, -10, z), origin.offset(x, -6, z), Blocks.QUARTZ_BLOCK);
                }
            }
        }
        
        // The flight log (win button)
        level.setBlock(origin.offset(r-3, -10, r-3), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);
        
        return getSpawnPos(origin);
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        return level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON) && interactedBlock.distSqr(origin) < 4000;
    }

    @Override
    public void tick(ServerPlayer player, BlockPos origin) {
        BanishTask.super.tick(player, origin);
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        return new Vec3(origin.getX() + 0.5, origin.getY() + 1, origin.getZ() + 0.5);
    }
}
