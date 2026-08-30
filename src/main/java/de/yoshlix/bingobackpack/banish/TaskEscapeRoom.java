package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
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
        Random rand = TaskUtils.randomFor(origin);
        int variant = rand.nextInt(5);

        // Clear and build base room (variants that need more space clear extra themselves)
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
        int size = 3 + rand.nextInt(3); // 3 to 5
        TaskUtils.hollowBox(level, origin.offset(-size, -1, -size), origin.offset(size, 5, size), Blocks.BEDROCK, Blocks.AIR);
        level.setBlock(origin.offset(0, 4, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);

        boolean useStone = rand.nextBoolean();
        if (useStone) {
            int count = 2 + rand.nextInt(3);
            for (int i = 0; i < count; i++) {
                int rx = rand.nextInt(size * 2 - 1) - (size - 1);
                int rz = rand.nextInt(size * 2 - 1) - (size - 1);
                level.setBlock(origin.offset(rx, 0, rz), Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        } else {
            int count = 1 + rand.nextInt(2);
            for (int i = 0; i < count; i++) {
                int rx = rand.nextInt(size * 2 - 1) - (size - 1);
                int rz = rand.nextInt(size * 2 - 1) - (size - 1);
                level.setBlock(origin.offset(rx, 0, rz), Blocks.OAK_LOG.defaultBlockState(), 3);
            }
        }

        int ctx = rand.nextInt(size * 2 - 1) - (size - 1);
        int ctz = rand.nextInt(size * 2 - 1) - (size - 1);
        level.setBlock(origin.offset(ctx, 0, ctz), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
    }

    /**
     * Variant 1: Room with a hidden button behind breakable walls. Material
     * and wall thickness vary per instance so "punch three times and you're
     * through" isn't a guaranteed strategy.
     */
    private void generateHiddenButtonRoom(ServerLevel level, BlockPos origin, Random rand) {
        TaskUtils.hollowBox(level, origin.offset(-5, -1, -5), origin.offset(5, 5, 5), Blocks.BEDROCK, Blocks.AIR);
        level.setBlock(origin.offset(0, 4, 0), Blocks.SEA_LANTERN.defaultBlockState(), 3);

        Block[] wallMaterials = {Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.MANGROVE_PLANKS};
        Block wallMat = wallMaterials[rand.nextInt(wallMaterials.length)];
        boolean thick = rand.nextBoolean();

        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if ((x == -2 || x == 2) && rand.nextInt(3) != 0) {
                    TaskUtils.fill(level, origin.offset(x, 0, z), origin.offset(x, 2, z), wallMat);
                    if (thick) {
                        int outer = x + (x < 0 ? -1 : 1);
                        TaskUtils.fill(level, origin.offset(outer, 0, z), origin.offset(outer, 2, z), wallMat);
                    }
                }
                if ((z == -2 || z == 2) && rand.nextInt(3) != 0) {
                    TaskUtils.fill(level, origin.offset(x, 0, z), origin.offset(x, 2, z), wallMat);
                    if (thick) {
                        int outer = z + (z < 0 ? -1 : 1);
                        TaskUtils.fill(level, origin.offset(x, 0, outer), origin.offset(x, 2, outer), wallMat);
                    }
                }
            }
        }

        int bx = rand.nextBoolean() ? (rand.nextBoolean() ? -4 : 4) : (rand.nextInt(7) - 3);
        int bz = rand.nextBoolean() ? (rand.nextBoolean() ? -4 : 4) : (rand.nextInt(7) - 3);
        level.setBlock(origin.offset(bx, 0, bz), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.offset(bx, 1, bz), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);

        level.setBlock(origin.offset(-3, 0, 3), Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(origin.offset(3, 0, -3), Blocks.BARREL.defaultBlockState(), 3);
    }

    /**
     * Variant 2: A chain of 2-4 rooms in a randomized direction sequence
     * (not always the same fixed L-shape) — button is in the last one.
     */
    private void generateMultiRoomPuzzle(ServerLevel level, BlockPos origin, Random rand) {
        int roomCount = 2 + rand.nextInt(3); // 2-4 rooms
        int roomSize = 3;
        int step = roomSize * 2 + 4;

        // Extra clearing for longer/redirected chains beyond the shared -12..12 box
        TaskUtils.fill(level, origin.offset(-14, -3, -14), origin.offset(14 + roomCount * step, 10, 14 + roomCount * step), Blocks.AIR);
        TaskUtils.fill(level, origin.offset(-14 - roomCount * step, -3, -14 - roomCount * step), origin.offset(14, 10, 14), Blocks.AIR);

        BlockPos[] centers = new BlockPos[roomCount];
        centers[0] = origin;
        for (int i = 1; i < roomCount; i++) {
            int dir = rand.nextInt(4); // 0=+X, 1=-X, 2=+Z, 3=-Z
            int dx = (dir == 0) ? step : (dir == 1) ? -step : 0;
            int dz = (dir == 2) ? step : (dir == 3) ? -step : 0;
            centers[i] = centers[i - 1].offset(dx, 0, dz);
        }

        Block[] wallMaterials = {Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS};

        for (int i = 0; i < roomCount; i++) {
            TaskUtils.hollowBox(level, centers[i].offset(-roomSize, -1, -roomSize),
                    centers[i].offset(roomSize, 5, roomSize), Blocks.BEDROCK, Blocks.AIR);
            level.setBlock(centers[i].offset(0, 4, 0),
                    (i % 2 == 0) ? Blocks.GLOWSTONE.defaultBlockState() : Blocks.SEA_LANTERN.defaultBlockState(), 3);
        }

        // Punch a breakable connection through both facing walls for each hop
        for (int i = 0; i < roomCount - 1; i++) {
            BlockPos a = centers[i];
            BlockPos b = centers[i + 1];
            Block wallMat = wallMaterials[rand.nextInt(wallMaterials.length)];
            int dx = Integer.signum(b.getX() - a.getX());
            int dz = Integer.signum(b.getZ() - a.getZ());
            if (dx != 0) {
                int wallXa = a.getX() + dx * roomSize;
                int wallXb = b.getX() - dx * roomSize;
                TaskUtils.fill(level, new BlockPos(wallXa, a.getY(), a.getZ() - 1), new BlockPos(wallXa, a.getY() + 2, a.getZ() + 1), wallMat);
                TaskUtils.fill(level, new BlockPos(wallXb, b.getY(), b.getZ() - 1), new BlockPos(wallXb, b.getY() + 2, b.getZ() + 1), wallMat);
            } else {
                int wallZa = a.getZ() + dz * roomSize;
                int wallZb = b.getZ() - dz * roomSize;
                TaskUtils.fill(level, new BlockPos(a.getX() - 1, a.getY(), wallZa), new BlockPos(a.getX() + 1, a.getY() + 2, wallZa), wallMat);
                TaskUtils.fill(level, new BlockPos(b.getX() - 1, b.getY(), wallZb), new BlockPos(b.getX() + 1, b.getY() + 2, wallZb), wallMat);
            }
        }

        // Crafting supplies in the second-to-last room
        BlockPos craftRoom = centers[Math.max(0, roomCount - 2)];
        level.setBlock(craftRoom.offset(-1, 0, 0), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        level.setBlock(craftRoom.offset(1, 0, 1), Blocks.OAK_LOG.defaultBlockState(), 3);
        level.setBlock(craftRoom.offset(1, 0, -1), Blocks.COBBLESTONE.defaultBlockState(), 3);

        // Win button in the last room
        BlockPos finalRoom = centers[roomCount - 1];
        int bx = rand.nextInt(roomSize * 2 - 1) - (roomSize - 1);
        int bz = rand.nextInt(roomSize * 2 - 1) - (roomSize - 1);
        level.setBlock(finalRoom.offset(bx, 0, bz), Blocks.BEDROCK.defaultBlockState(), 3);
        level.setBlock(finalRoom.offset(bx, 1, bz), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);
    }

    /**
     * Variant 3: Must pillar up using blocks to reach a button. Ceiling
     * height and available material count vary per instance.
     */
    private void generatePillarPuzzle(ServerLevel level, BlockPos origin, Random rand) {
        int height = 6 + rand.nextInt(3); // 6-8
        TaskUtils.hollowBox(level, origin.offset(-4, -1, -4), origin.offset(4, height, 4), Blocks.BEDROCK, Blocks.AIR);
        level.setBlock(origin.offset(0, height - 1, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);

        int cobbleCount = 6 + rand.nextInt(5);
        for (int i = 0; i < cobbleCount; i++) {
            int rx = rand.nextInt(7) - 3;
            int rz = rand.nextInt(7) - 3;
            level.setBlock(origin.offset(rx, 0, rz), Blocks.COBBLESTONE.defaultBlockState(), 3);
        }

        int dirtCount = 4 + rand.nextInt(4);
        for (int i = 0; i < dirtCount; i++) {
            int rx = rand.nextInt(7) - 3;
            int rz = rand.nextInt(7) - 3;
            level.setBlock(origin.offset(rx, 0, rz), Blocks.DIRT.defaultBlockState(), 3);
        }

        int side = rand.nextInt(4);
        BlockPos buttonPos = switch (side) {
            case 0 -> origin.offset(rand.nextInt(5) - 2, height - 2, -3);
            case 1 -> origin.offset(rand.nextInt(5) - 2, height - 2, 3);
            case 2 -> origin.offset(-3, height - 2, rand.nextInt(5) - 2);
            default -> origin.offset(3, height - 2, rand.nextInt(5) - 2);
        };
        level.setBlock(buttonPos, Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);
    }

    /**
     * Variant 4: Dark, twisty corridors — find the button by feel. Room size
     * varies per instance.
     */
    private void generateDarkMazePuzzle(ServerLevel level, BlockPos origin, Random rand) {
        int half = 5 + rand.nextInt(3); // 5-7
        TaskUtils.hollowBox(level, origin.offset(-half, -1, -half), origin.offset(half, 4, half), Blocks.BEDROCK, Blocks.AIR);

        for (int x = -half + 1; x <= half - 1; x += 2) {
            boolean open = rand.nextBoolean();
            int gapZ = rand.nextInt(half * 2 - 1) - (half - 1);
            for (int z = -half + 1; z <= half - 1; z++) {
                if (z == gapZ || (open && rand.nextInt(4) == 0)) continue;
                TaskUtils.fill(level, origin.offset(x, 0, z), origin.offset(x, 2, z), Blocks.BLACKSTONE);
            }
        }

        int bx = rand.nextInt(half * 2 - 1) - (half - 1);
        int bz = rand.nextInt(half * 2 - 1) - (half - 1);
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
        return isButton && interactedBlock.distSqr(origin) < 5000;
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
