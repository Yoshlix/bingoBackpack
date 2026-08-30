package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TaskUtils {
    /**
     * A single Random per generated task instance, seeded from both the
     * (always-unique) origin and wall-clock time. Use this instead of ad hoc
     * seeding so every banish instance gets a genuinely different layout —
     * a fixed/predictable seed here is how a task's "solution" ends up
     * memorizable across repeat visits.
     */
    public static Random randomFor(BlockPos origin) {
        return new Random(origin.asLong() ^ System.nanoTime());
    }

    /**
     * Random-walks an irregular cavity starting at {@code start} for
     * {@code steps} steps, carving a small sphere at each stop. The bias
     * vector nudges the walk direction each step — e.g. (0,-1,0) produces a
     * roughly vertical shaft, (1,0,0) a roughly horizontal tunnel, (0,0,0) a
     * compact blob — while the exact path still varies per call, so the
     * general digging/traversal strategy isn't the same every time.
     *
     * @return the final position reached, useful for placing the win
     *         condition at the "end" of the carved path.
     */
    public static BlockPos carveBlob(ServerLevel level, BlockPos start, Random rand, int steps, int radius,
            int biasX, int biasY, int biasZ) {
        BlockPos current = start;
        for (int i = 0; i < steps; i++) {
            carveSphere(level, current, radius);
            int dx = biasX + rand.nextInt(3) - 1;
            int dy = biasY + rand.nextInt(3) - 1;
            int dz = biasZ + rand.nextInt(3) - 1;
            current = current.offset(dx, dy, dz);
        }
        return current;
    }

    /** All positions visited by {@link #carveBlob}, in walk order — for callers that need the whole path, not just the end. */
    public static List<BlockPos> carveBlobPath(ServerLevel level, BlockPos start, Random rand, int steps, int radius,
            int biasX, int biasY, int biasZ) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos current = start;
        path.add(current);
        for (int i = 0; i < steps; i++) {
            carveSphere(level, current, radius);
            int dx = biasX + rand.nextInt(3) - 1;
            int dy = biasY + rand.nextInt(3) - 1;
            int dz = biasZ + rand.nextInt(3) - 1;
            current = current.offset(dx, dy, dz);
            path.add(current);
        }
        return path;
    }

    private static void carveSphere(ServerLevel level, BlockPos center, int radius) {
        int rSq = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= rSq) {
                        level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

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
