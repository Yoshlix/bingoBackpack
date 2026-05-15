package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TaskEpsteinIsland implements BanishTask {
    @Override
    public String getTaskDescription() {
        return "Erkunde die Insel, finde den Geheimgang im Haus und suche das Fluglogbuch im Bunker!";
    }

    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        int r = 15;
        // Big clear first
        TaskUtils.fill(level, origin.offset(-r, -15, -r), origin.offset(r, 10, r), Blocks.AIR);
        
        // Bedrock bounds for safety
        TaskUtils.fill(level, origin.offset(-r, -16, -r), origin.offset(r, -16, r), Blocks.BEDROCK);
        TaskUtils.fill(level, origin.offset(-r, -16, -r), origin.offset(-r, 10, r), Blocks.BEDROCK);
        TaskUtils.fill(level, origin.offset(r, -16, -r), origin.offset(r, 10, r), Blocks.BEDROCK);
        TaskUtils.fill(level, origin.offset(-r, -16, -r), origin.offset(r, 10, -r), Blocks.BEDROCK);
        TaskUtils.fill(level, origin.offset(-r, -16, r), origin.offset(r, 10, r), Blocks.BEDROCK);
        TaskUtils.fill(level, origin.offset(-r, 10, -r), origin.offset(r, 10, r), Blocks.BEDROCK);
        
        // Solid dirt down to bunker ceiling (y=-5)
        for (int x = -r + 1; x < r; x++) {
            for (int z = -r + 1; z < r; z++) {
                if (x * x + z * z < r * r) {
                    level.setBlock(origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                    TaskUtils.fill(level, origin.offset(x, -5, z), origin.offset(x, -2, z), Blocks.DIRT);
                }
            }
        }

        // Bunker outer shell (y=-11 to y=-6)
        int br = 10; // bunker radius smaller than island
        TaskUtils.fill(level, origin.offset(-br, -11, -br), origin.offset(br, -6, br), Blocks.QUARTZ_BLOCK);
        // Bunker inner empty space
        TaskUtils.fill(level, origin.offset(-br + 1, -10, -br + 1), origin.offset(br - 1, -7, br - 1), Blocks.AIR);
        
        // Bunker internal maze/pillars
        Random rand = new Random(origin.asLong() + 12345);
        for (int x = -br + 3; x < br - 2; x += 3) {
            for (int z = -br + 3; z < br - 2; z += 3) {
                if (rand.nextBoolean()) {
                    TaskUtils.fill(level, origin.offset(x, -10, z), origin.offset(x, -7, z), Blocks.QUARTZ_BLOCK);
                }
            }
        }

        // Bunker lighting
        for (int x = -br + 2; x < br - 1; x += 4) {
            for (int z = -br + 2; z < br - 1; z += 4) {
                level.setBlock(origin.offset(x, -7, z), Blocks.SEA_LANTERN.defaultBlockState(), 3);
            }
        }

        // Hut on the surface
        TaskUtils.hollowBox(level, origin.offset(-3, 0, -3), origin.offset(3, 4, 3), Blocks.BIRCH_PLANKS, Blocks.AIR);
        
        // Doorway
        level.setBlock(origin.offset(0, 1, 3), Blocks.AIR.defaultBlockState(), 3); // door bottom
        level.setBlock(origin.offset(0, 2, 3), Blocks.AIR.defaultBlockState(), 3); // door top
        // Inside lighting
        level.setBlock(origin.offset(0, 3, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
        
        // The secret shaft down to the bunker
        // Clear a 2x1 hole from inside the hut all the way down to the bunker floor
        TaskUtils.fill(level, origin.offset(0, -10, 0), origin.offset(1, 0, 0), Blocks.AIR);
        
        // The wall for the ladder to attach to (X=-1)
        TaskUtils.fill(level, origin.offset(-1, -10, 0), origin.offset(-1, 0, 0), Blocks.OAK_PLANKS);
        
        // Place ladder on the wall in the shaft
        for (int y = -10; y <= 0; y++) {
            // In 1.21.x, we just place the ladder state. If direction is completely off, it might pop, 
            // but usually attaching it automatically or relying on block tick suspension works. 
            // Let's use scaffolding instead for a foolproof vertical climb!
            level.setBlock(origin.offset(0, y, 0), Blocks.SCAFFOLDING.defaultBlockState(), 3);
        }
        
        // The flight log (win button) hidden inside the bunker
        // Pick a random corner or spot
        int wx = rand.nextBoolean() ? (br - 2) : (-br + 2);
        int wz = rand.nextBoolean() ? (br - 2) : (-br + 2);
        
        level.setBlock(origin.offset(wx, -10, wz), Blocks.LECTERN.defaultBlockState(), 3);
        level.setBlock(origin.offset(wx, -9, wz), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);

        return getSpawnPos(origin);
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
