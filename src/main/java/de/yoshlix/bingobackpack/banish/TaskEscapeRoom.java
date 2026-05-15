package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TaskEscapeRoom implements BanishTask {
    @Override
    public String getTaskDescription() {
        return "Finde einen Weg, aus diesem Raum zu entkommen!";
    }

    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        Random rand = new Random(origin.asLong() + System.currentTimeMillis() / 60000);
        int variant = rand.nextInt(5);

        // Clear and build base room
        TaskUtils.fill(level, origin.offset(-12, -3, -12), origin.offset(12, 10, 12), Blocks.AIR);

        switch (variant) {
            case 0 -> generateCraftingPuzzle(level, origin, rand);
            case 1 -> generateHiddenButtonRoom(level, origin, rand);
            case 2 -> generateMultiRoomPuzzle(level, origin, rand);
            case 3 -> generatePillarPuzzle(level, origin, rand);
            case 4 -> generateDarkMazePuzzle(level, origin, rand);
        }

        return getSpawnPos(origin);
    }

    /**
     * Variant 0: Random crafting materials — must figure out what to craft.
     * Sometimes logs, sometimes cobblestone. Crafting table hidden or visible.
     */
    private void generateCraftingPuzzle(ServerLevel level, BlockPos origin, Random rand) {
        // Random room size
        int size = 3 + rand.nextInt(3); // 3 to 5
        TaskUtils.hollowBox(level, origin.offset(-size, -1, -size), origin.offset(size, 5, size), Blocks.BEDROCK, Blocks.AIR);
        level.setBlock(origin.offset(0, 4, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);

        // Randomly choose material set
        boolean useStone = rand.nextBoolean();
        if (useStone) {
            // Cobblestone blocks — player breaks them, crafts stone button
            int count = 2 + rand.nextInt(3);
            for (int i = 0; i < count; i++) {
                int rx = rand.nextInt(size * 2 - 1) - (size - 1);
                int rz = rand.nextInt(size * 2 - 1) - (size - 1);
                level.setBlock(origin.offset(rx, 0, rz), Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        } else {
            // Oak logs — player breaks them, crafts planks, then button
            int count = 1 + rand.nextInt(2);
            for (int i = 0; i < count; i++) {
                int rx = rand.nextInt(size * 2 - 1) - (size - 1);
                int rz = rand.nextInt(size * 2 - 1) - (size - 1);
                level.setBlock(origin.offset(rx, 0, rz), Blocks.OAK_LOG.defaultBlockState(), 3);
            }
        }

        // Crafting table at random position
        int ctx = rand.nextInt(size * 2 - 1) - (size - 1);
        int ctz = rand.nextInt(size * 2 - 1) - (size - 1);
        level.setBlock(origin.offset(ctx, 0, ctz), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
    }

    /**
     * Variant 1: Room with hidden button behind breakable walls.
     */
    private void generateHiddenButtonRoom(ServerLevel level, BlockPos origin, Random rand) {
        TaskUtils.hollowBox(level, origin.offset(-5, -1, -5), origin.offset(5, 5, 5), Blocks.BEDROCK, Blocks.AIR);
        level.setBlock(origin.offset(0, 4, 0), Blocks.SEA_LANTERN.defaultBlockState(), 3);

        // Build internal walls from breakable material
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if ((x == -2 || x == 2) && rand.nextInt(3) != 0) {
                    TaskUtils.fill(level, origin.offset(x, 0, z), origin.offset(x, 2, z), Blocks.OAK_PLANKS);
                }
                if ((z == -2 || z == 2) && rand.nextInt(3) != 0) {
                    TaskUtils.fill(level, origin.offset(x, 0, z), origin.offset(x, 2, z), Blocks.OAK_PLANKS);
                }
            }
        }

        // Hidden button behind one of the walls
        int bx = rand.nextBoolean() ? (rand.nextBoolean() ? -4 : 4) : (rand.nextInt(7) - 3);
        int bz = rand.nextBoolean() ? (rand.nextBoolean() ? -4 : 4) : (rand.nextInt(7) - 3);
        level.setBlock(origin.offset(bx, 0, bz), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.offset(bx, 1, bz), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);

        // Some decoy items
        level.setBlock(origin.offset(-3, 0, 3), Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(origin.offset(3, 0, -3), Blocks.BARREL.defaultBlockState(), 3);
    }

    /**
     * Variant 2: Multiple connected rooms — button is in the last one.
     */
    private void generateMultiRoomPuzzle(ServerLevel level, BlockPos origin, Random rand) {
        // Room 1 (spawn room)
        TaskUtils.hollowBox(level, origin.offset(-3, -1, -3), origin.offset(3, 5, 3), Blocks.BEDROCK, Blocks.AIR);
        level.setBlock(origin.offset(0, 4, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);

        // Room 2 (connected via breakable wall)
        TaskUtils.hollowBox(level, origin.offset(4, -1, -3), origin.offset(10, 5, 3), Blocks.BEDROCK, Blocks.AIR);
        level.setBlock(origin.offset(7, 4, 0), Blocks.SEA_LANTERN.defaultBlockState(), 3);
        // Breakable connection
        TaskUtils.fill(level, origin.offset(3, 0, -1), origin.offset(3, 2, 1), Blocks.OAK_PLANKS);

        // Room 3 (connected from room 2, different direction)
        TaskUtils.hollowBox(level, origin.offset(4, -1, 4), origin.offset(10, 5, 10), Blocks.BEDROCK, Blocks.AIR);
        level.setBlock(origin.offset(7, 4, 7), Blocks.GLOWSTONE.defaultBlockState(), 3);
        // Breakable connection
        TaskUtils.fill(level, origin.offset(5, 0, 3), origin.offset(8, 2, 3), Blocks.SPRUCE_PLANKS);

        // Place crafting table and wood in room 2
        level.setBlock(origin.offset(5, 0, 0), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        level.setBlock(origin.offset(6, 0, 1), Blocks.OAK_LOG.defaultBlockState(), 3);
        level.setBlock(origin.offset(8, 0, -1), Blocks.COBBLESTONE.defaultBlockState(), 3);

        // Win button in room 3
        int bx = 5 + rand.nextInt(4);
        int bz = 5 + rand.nextInt(4);
        level.setBlock(origin.offset(bx, 0, bz), Blocks.BEDROCK.defaultBlockState(), 3);
        level.setBlock(origin.offset(bx, 1, bz), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);
    }

    /**
     * Variant 3: Must pillar up using blocks to reach a button on the ceiling.
     */
    private void generatePillarPuzzle(ServerLevel level, BlockPos origin, Random rand) {
        // Tall room
        TaskUtils.hollowBox(level, origin.offset(-4, -1, -4), origin.offset(4, 8, 4), Blocks.BEDROCK, Blocks.AIR);
        level.setBlock(origin.offset(0, 7, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);

        // Some blocks scattered to pillar up with
        for (int i = 0; i < 8; i++) {
            int rx = rand.nextInt(7) - 3;
            int rz = rand.nextInt(7) - 3;
            level.setBlock(origin.offset(rx, 0, rz), Blocks.COBBLESTONE.defaultBlockState(), 3);
        }

        // Additional dirt blocks
        for (int i = 0; i < 6; i++) {
            int rx = rand.nextInt(7) - 3;
            int rz = rand.nextInt(7) - 3;
            level.setBlock(origin.offset(rx, 0, rz), Blocks.DIRT.defaultBlockState(), 3);
        }

        // Button high up on a wall
        int side = rand.nextInt(4);
        BlockPos buttonPos = switch (side) {
            case 0 -> origin.offset(rand.nextInt(5) - 2, 6, -3);
            case 1 -> origin.offset(rand.nextInt(5) - 2, 6, 3);
            case 2 -> origin.offset(-3, 6, rand.nextInt(5) - 2);
            default -> origin.offset(3, 6, rand.nextInt(5) - 2);
        };
        level.setBlock(buttonPos, Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);
    }

    /**
     * Variant 4: Dark, twisty corridors — find the button by feel.
     */
    private void generateDarkMazePuzzle(ServerLevel level, BlockPos origin, Random rand) {
        // No light at all
        TaskUtils.hollowBox(level, origin.offset(-6, -1, -6), origin.offset(6, 4, 6), Blocks.BEDROCK, Blocks.AIR);

        // Internal walls creating corridors
        for (int x = -5; x <= 5; x += 2) {
            boolean open = rand.nextBoolean();
            int gapZ = rand.nextInt(9) - 4;
            for (int z = -5; z <= 5; z++) {
                if (z == gapZ || (open && rand.nextInt(4) == 0)) continue;
                TaskUtils.fill(level, origin.offset(x, 0, z), origin.offset(x, 2, z), Blocks.BLACKSTONE);
            }
        }

        // Button somewhere in the maze
        int bx = rand.nextInt(9) - 4;
        int bz = rand.nextInt(9) - 4;
        level.setBlock(origin.offset(bx, 0, bz), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.offset(bx, 1, bz), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);

        // One single torch so player gets their bearings
        level.setBlock(origin.offset(0, 1, 0), Blocks.SOUL_TORCH.defaultBlockState(), 3);
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        // Accept both wooden buttons (crafted) and polished blackstone buttons (pre-placed)
        boolean isButton = level.getBlockState(interactedBlock).is(BlockTags.WOODEN_BUTTONS)
                || level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON);
        return isButton && interactedBlock.distSqr(origin) < 600;
    }

    @Override
    public void tick(ServerPlayer player, BlockPos origin) {
        // Origin-relative void check
        if (player.getY() < origin.getY() - 10) {
            Vec3 spawn = getSpawnPos(origin);
            player.teleportTo(spawn.x, spawn.y, spawn.z);
        }
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        return new Vec3(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
    }
}
