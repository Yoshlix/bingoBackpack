package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class TaskUtils {
    public static void fill(ServerLevel level, BlockPos start, BlockPos end, Block block) {
        int minX = Math.min(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxX = Math.max(start.getX(), end.getX());
        int maxY = Math.max(start.getY(), end.getY());
        int maxZ = Math.max(start.getZ(), end.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
                }
            }
        }
    }

    public static void hollowBox(ServerLevel level, BlockPos start, BlockPos end, Block wallBlock, Block airBlock) {
        fill(level, start, end, wallBlock);
        
        int minX = Math.min(start.getX(), end.getX()) + 1;
        int minY = Math.min(start.getY(), end.getY()) + 1;
        int minZ = Math.min(start.getZ(), end.getZ()) + 1;
        int maxX = Math.max(start.getX(), end.getX()) - 1;
        int maxY = Math.max(start.getY(), end.getY()) - 1;
        int maxZ = Math.max(start.getZ(), end.getZ()) - 1;

        if (minX <= maxX && minY <= maxY && minZ <= maxZ) {
            fill(level, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), airBlock);
        }
    }
}
