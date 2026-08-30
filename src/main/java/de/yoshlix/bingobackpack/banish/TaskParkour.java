package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TaskParkour implements BanishTask {
    @Override
    public String getTaskDescription() {
        return "Springe über die Blöcke bis zum Ende! Der Weg gabelt sich - wähle eine der beiden Routen und drücke den Button auf der Plattform.";
    }

    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        Random rand = TaskUtils.randomFor(origin);

        // Clear a big area, wide enough for two diverging branches
        TaskUtils.fill(level, origin.offset(-10, -6, -40), origin.offset(160, 30, 40), Blocks.AIR);
        // Bedrock floor far below to catch falls
        TaskUtils.fill(level, origin.offset(-10, -7, -40), origin.offset(160, -7, 40), Blocks.BEDROCK);

        // Start platform (3x3)
        TaskUtils.fill(level, origin.offset(-1, -1, -1), origin.offset(1, -1, 1), Blocks.PURPUR_BLOCK);

        int splitAfter = 5 + rand.nextInt(4); // 5-8 shared jumps before the fork
        int branchLength = 10 + rand.nextInt(8); // 10-17 jumps per branch after the fork

        // Shared opening stretch, then the path forks into two independent
        // branches with their own randomized jump style and their own end
        // platform + button - either one completes the task. Which branch
        // ends up easier/shorter varies every time, so there's no single
        // memorizable "correct" route.
        BlockPos forkPoint = buildChain(level, origin.offset(3, -1, 0), rand, splitAfter, 0);

        buildBranch(level, forkPoint, new Random(rand.nextLong()), branchLength, 1);
        buildBranch(level, forkPoint, new Random(rand.nextLong()), branchLength, -1);

        return getSpawnPos(origin);
    }

    private void buildBranch(ServerLevel level, BlockPos start, Random rand, int jumps, int zDrift) {
        BlockPos end = buildChain(level, start, rand, jumps, zDrift);
        // End platform (3x3)
        TaskUtils.fill(level, end.offset(-1, 0, -1), end.offset(1, 0, 1), Blocks.OBSIDIAN);
        // Win button on center of end platform
        level.setBlock(end.offset(0, 1, 0), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);
    }

    /**
     * Places a chain of jumpable blocks. {@code zDrift} nudges the path
     * sideways (used to make the two branches diverge); each jump also rolls
     * a style: a wider landing pad (breather) or the hardest legal sprint gap.
     */
    private BlockPos buildChain(ServerLevel level, BlockPos start, Random rand, int jumps, int zDrift) {
        BlockPos current = start;
        for (int i = 0; i < jumps; i++) {
            level.setBlock(current, Blocks.END_STONE_BRICKS.defaultBlockState(), 3);

            double styleRoll = rand.nextDouble();
            if (styleRoll < 0.15) {
                // Wider landing pad - a brief breather
                level.setBlock(current.offset(1, 0, 0), Blocks.END_STONE_BRICKS.defaultBlockState(), 3);
            }

            // Max sprint-jump: 4 blocks horizontal at same/lower level, 3 at +1 height
            int dy = rand.nextInt(3) - 1; // -1, 0, or +1
            int maxHorizontal = (dy <= 0) ? 4 : 3;
            int dx = (styleRoll > 0.85) ? maxHorizontal : rand.nextInt(maxHorizontal - 1) + 2;
            int dz = (zDrift != 0 && rand.nextBoolean()) ? zDrift : rand.nextInt(3) - 1;

            current = current.offset(dx, dy, dz);
        }
        return current;
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        return level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON) && interactedBlock.distSqr(origin) < 250000;
    }

    @Override
    public void tick(ServerPlayer player, BlockPos origin) {
        // Teleport back to start if they fall off (below origin - 4)
        if (player.getY() < origin.getY() - 4) {
            Vec3 spawn = getSpawnPos(origin);
            player.teleportTo(spawn.x, spawn.y, spawn.z);
        }
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        return new Vec3(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
    }
}
