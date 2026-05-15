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
        return "Springe über die Blöcke bis zum Ende und drücke den Button auf der Obsidian-Plattform!";
    }

    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        Random rand = new Random(origin.asLong() + System.nanoTime());
        
        // Clear a big area
        TaskUtils.fill(level, origin.offset(-5, -5, -5), origin.offset(120, 25, 50), Blocks.AIR);

        // Bedrock floor far below to catch falls
        TaskUtils.fill(level, origin.offset(-5, -5, -5), origin.offset(120, -5, 50), Blocks.BEDROCK);

        // Start platform (3x3)
        TaskUtils.fill(level, origin.offset(-1, -1, -1), origin.offset(1, -1, 1), Blocks.PURPUR_BLOCK);
        
        BlockPos current = origin.offset(3, -1, 0);
        
        // Generate 30 jumps with guaranteed reachable distances
        for (int i = 0; i < 30; i++) {
            // Place a 1x1 block
            level.setBlock(current, Blocks.END_STONE_BRICKS.defaultBlockState(), 3);
            
            // Generate next jump within reachable bounds:
            // Max sprint-jump: 4 blocks horizontal at same/lower level, 3 at +1 height
            int dy = rand.nextInt(3) - 1; // -1, 0, or +1
            int maxHorizontal = (dy <= 0) ? 4 : 3; // Shorter hops when going up
            int dx = rand.nextInt(maxHorizontal - 1) + 2; // 2 to maxHorizontal
            int dz = rand.nextInt(3) - 1; // -1, 0, or +1 sideways
            
            current = current.offset(dx, dy, dz);
        }
        
        // End platform (3x3)
        TaskUtils.fill(level, current.offset(-1, 0, -1), current.offset(1, 0, 1), Blocks.OBSIDIAN);
        // Win button on center of end platform
        level.setBlock(current.offset(0, 1, 0), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);

        return getSpawnPos(origin);
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        return level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON) && interactedBlock.distSqr(origin) < 80000;
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
