package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

public class TaskEpsteinIsland implements BanishTask {
    @Override
    public String getTaskDescription() {
        return "Erkunde die Insel, finde den geheimen Einstieg und suche das Fluglogbuch im Bunker!";
    }

    private static final int R = 15; // island radius
    private static final int BR = 10; // bunker radius
    private static final int BUNKER_FLOOR_Y = -9;

    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        Random rand = TaskUtils.randomFor(origin);

        // Big clear first
        TaskUtils.fill(level, origin.offset(-R, -15, -R), origin.offset(R, 10, R), Blocks.AIR);

        // Bedrock bounds for safety
        TaskUtils.fill(level, origin.offset(-R, -16, -R), origin.offset(R, -16, R), Blocks.BEDROCK);
        TaskUtils.fill(level, origin.offset(-R, -16, -R), origin.offset(-R, 10, R), Blocks.BEDROCK);
        TaskUtils.fill(level, origin.offset(R, -16, -R), origin.offset(R, 10, R), Blocks.BEDROCK);
        TaskUtils.fill(level, origin.offset(-R, -16, -R), origin.offset(R, 10, -R), Blocks.BEDROCK);
        TaskUtils.fill(level, origin.offset(-R, -16, R), origin.offset(R, 10, R), Blocks.BEDROCK);
        TaskUtils.fill(level, origin.offset(-R, 10, -R), origin.offset(R, 10, R), Blocks.BEDROCK);

        // Solid dirt down to bunker ceiling (y=-5)
        for (int x = -R + 1; x < R; x++) {
            for (int z = -R + 1; z < R; z++) {
                if (x * x + z * z < R * R) {
                    level.setBlock(origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                    TaskUtils.fill(level, origin.offset(x, -5, z), origin.offset(x, -2, z), Blocks.DIRT);
                }
            }
        }

        // Bunker outer shell (y=-11 to y=-6)
        TaskUtils.fill(level, origin.offset(-BR, -11, -BR), origin.offset(BR, -6, BR), Blocks.QUARTZ_BLOCK);
        // Bunker inner empty space
        TaskUtils.fill(level, origin.offset(-BR + 1, -10, -BR + 1), origin.offset(BR - 1, -7, BR - 1), Blocks.AIR);

        // Bunker internal maze/pillars
        for (int x = -BR + 3; x < BR - 2; x += 3) {
            for (int z = -BR + 3; z < BR - 2; z += 3) {
                if (rand.nextBoolean()) {
                    TaskUtils.fill(level, origin.offset(x, -10, z), origin.offset(x, -7, z), Blocks.QUARTZ_BLOCK);
                }
            }
        }

        // Bunker lighting
        for (int x = -BR + 2; x < BR - 1; x += 4) {
            for (int z = -BR + 2; z < BR - 1; z += 4) {
                level.setBlock(origin.offset(x, -7, z), Blocks.SEA_LANTERN.defaultBlockState(), 3);
            }
        }

        // Hut on the surface — a landmark, but no longer necessarily where the
        // way down actually is.
        TaskUtils.hollowBox(level, origin.offset(-3, 0, -3), origin.offset(3, 4, 3), Blocks.BIRCH_PLANKS, Blocks.AIR);
        level.setBlock(origin.offset(0, 1, 3), Blocks.AIR.defaultBlockState(), 3); // door bottom
        level.setBlock(origin.offset(0, 2, 3), Blocks.AIR.defaultBlockState(), 3); // door top
        level.setBlock(origin.offset(0, 3, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);

        // The entry point varies per instance: about half the time it's
        // somewhere on the hut floor (not always dead center), the other half
        // it's hidden elsewhere on the island entirely, away from the hut —
        // so "beeline to the hut" isn't a reliable strategy.
        boolean entryInHut = rand.nextBoolean();
        int ex, ez;
        if (entryInHut) {
            ex = rand.nextInt(5) - 2; // within the hut floor, clear of the walls
            ez = rand.nextInt(5) - 2;
        } else {
            int ix, iz;
            do {
                double angle = rand.nextDouble() * Math.PI * 2;
                double dist = 5 + rand.nextDouble() * (R - 7);
                ix = (int) Math.round(Math.cos(angle) * dist);
                iz = (int) Math.round(Math.sin(angle) * dist);
            } while (Math.abs(ix) <= 4 && Math.abs(iz) <= 4); // stay clear of the hut footprint
            ex = ix;
            ez = iz;
        }

        BlockPos entryPoint = origin.offset(ex, -1, ez);
        // Mark the entry with a subtle surface tell — one block to break
        // through, not an open hole you can just walk into.
        level.setBlock(entryPoint, Blocks.COARSE_DIRT.defaultBlockState(), 3);

        BlockPos bunkerArrival = carveDescent(level, entryPoint.below(), rand);

        // The flight log (win button), hidden inside the bunker in a random corner
        int wx = rand.nextBoolean() ? (BR - 2) : (-BR + 2);
        int wz = rand.nextBoolean() ? (BR - 2) : (-BR + 2);
        level.setBlock(origin.offset(wx, -10, wz), Blocks.LECTERN.defaultBlockState(), 3);
        level.setBlock(origin.offset(wx, -9, wz), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);

        return getSpawnPos(origin);
    }

    /**
     * Random-walks a winding, mostly-downward tunnel from {@code start} into
     * the bunker, marking every step with scaffolding to climb. Falls back to
     * a straight scaffold column if the walk doesn't reach bunker depth
     * within its step budget, so the descent always connects.
     */
    private BlockPos carveDescent(ServerLevel level, BlockPos start, Random rand) {
        int depth = start.getY() - BUNKER_FLOOR_Y;
        int steps = depth + 4 + rand.nextInt(4);
        List<BlockPos> path = TaskUtils.carveBlobPath(level, start, rand, steps, 1, 0, -1, 0);
        for (BlockPos p : path) {
            level.setBlock(p, Blocks.SCAFFOLDING.defaultBlockState(), 3);
        }

        BlockPos cursor = path.get(path.size() - 1);
        while (cursor.getY() > BUNKER_FLOOR_Y) {
            level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(cursor, Blocks.SCAFFOLDING.defaultBlockState(), 3);
            cursor = cursor.below();
        }
        return cursor;
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        return level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON) && interactedBlock.distSqr(origin) < 4000;
    }

    @Override
    public void tick(ServerPlayer player, BlockPos origin) {
        // Don't call super.tick — player goes underground to Y < 150 intentionally
        // Only teleport back if they fall below the bedrock floor
        if (player.getY() < origin.getY() - 20) {
            Vec3 spawn = getSpawnPos(origin);
            player.teleportTo(spawn.x, spawn.y, spawn.z);
        }
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        return new Vec3(origin.getX() + 0.5, origin.getY() + 1, origin.getZ() + 0.5);
    }
}
