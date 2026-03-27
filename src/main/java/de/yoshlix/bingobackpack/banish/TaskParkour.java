package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TaskParkour implements BanishTask {
    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        Random rand = new Random(origin.asLong());
        
        // Clear a big area just in case
        TaskUtils.fill(level, origin.offset(-10, -5, -10), origin.offset(100, 20, 100), Blocks.AIR);

        // Start platform
        TaskUtils.fill(level, origin.offset(-1, -1, -1), origin.offset(1, -1, 1), Blocks.PURPUR_BLOCK);
        
        BlockPos current = origin.offset(3, -1, 0);
        
        // Generate 40 jumps
        for (int i = 0; i < 40; i++) {
            level.setBlock(current, Blocks.END_STONE_BRICKS.defaultBlockState(), 3);
            
            // Random next jump
            int dx = rand.nextInt(3) + 2; // 2 to 4 dx
            int dy = rand.nextInt(3) - 1; // -1 to 1 dy
            int dz = rand.nextInt(3) - 1; // -1 to 1 dz
            
            // keep it moving forward roughly
            current = current.offset(dx, dy, dz);
        }
        
        // End platform
        TaskUtils.fill(level, current.offset(-2, 0, -2), current.offset(2, 0, 2), Blocks.OBSIDIAN);
        
        // Win button at the center of end platform
        BlockPos winButtonPos = current.offset(0, 1, 0);
        level.setBlock(winButtonPos, Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);

        return getSpawnPos(origin);
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        // The win button is always exactly where we placed it.
        // But to be robust, we just check if it's a stone button in a large radius
        if (level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON)) {
            // Check if within 200 blocks of origin
            return interactedBlock.distSqr(origin) < 40000;
        }
        return false;
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        return new Vec3(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
    }
}
