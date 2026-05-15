package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class TaskMaze implements BanishTask {

    // Maze cell size: each cell is 2 blocks wide with 1 block wall between
    private static final int MAZE_W = 9;  // grid cells width
    private static final int MAZE_H = 9;  // grid cells height  
    private static final int CELL = 2;    // cell interior size

    @Override
    public String getTaskDescription() {
        return "Finde den Weg durch das Labyrinth und drücke den Button am Ende!";
    }

    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        Random rand = new Random(origin.asLong() + System.nanoTime());

        int totalW = MAZE_W * (CELL + 1) + 1;
        int totalH = MAZE_H * (CELL + 1) + 1;

        // Clear area
        TaskUtils.fill(level, origin.offset(-2, -2, -2), origin.offset(totalW + 2, 6, totalH + 2), Blocks.AIR);

        // Floor
        TaskUtils.fill(level, origin.offset(0, -1, 0), origin.offset(totalW - 1, -1, totalH - 1), Blocks.BEDROCK);
        // Ceiling
        TaskUtils.fill(level, origin.offset(0, 3, 0), origin.offset(totalW - 1, 3, totalH - 1), Blocks.BEDROCK);

        // Fill entire maze with walls first
        for (int x = 0; x < totalW; x++) {
            for (int z = 0; z < totalH; z++) {
                TaskUtils.fill(level, origin.offset(x, 0, z), origin.offset(x, 2, z), Blocks.DEEPSLATE_BRICKS);
            }
        }

        // Generate maze using DFS (recursive backtracker)
        boolean[][] visited = new boolean[MAZE_W][MAZE_H];
        Stack<int[]> stack = new Stack<>();
        
        // Start at top-left
        visited[0][0] = true;
        stack.push(new int[]{0, 0});
        carveCell(level, origin, 0, 0);

        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            int cx = current[0];
            int cz = current[1];

            // Get unvisited neighbors
            List<int[]> neighbors = new ArrayList<>();
            if (cx > 0 && !visited[cx - 1][cz]) neighbors.add(new int[]{cx - 1, cz});
            if (cx < MAZE_W - 1 && !visited[cx + 1][cz]) neighbors.add(new int[]{cx + 1, cz});
            if (cz > 0 && !visited[cx][cz - 1]) neighbors.add(new int[]{cx, cz - 1});
            if (cz < MAZE_H - 1 && !visited[cx][cz + 1]) neighbors.add(new int[]{cx, cz + 1});

            if (!neighbors.isEmpty()) {
                int[] next = neighbors.get(rand.nextInt(neighbors.size()));
                int nx = next[0];
                int nz = next[1];

                // Carve wall between current and next
                carveWall(level, origin, cx, cz, nx, nz);
                // Carve next cell
                carveCell(level, origin, nx, nz);

                visited[nx][nz] = true;
                stack.push(next);
            } else {
                stack.pop();
            }
        }

        // Lighting along corridors
        for (int cx = 0; cx < MAZE_W; cx++) {
            for (int cz = 0; cz < MAZE_H; cz++) {
                if ((cx + cz) % 3 == 0) {
                    int bx = 1 + cx * (CELL + 1);
                    int bz = 1 + cz * (CELL + 1);
                    level.setBlock(origin.offset(bx, 2, bz), Blocks.SEA_LANTERN.defaultBlockState(), 3);
                }
            }
        }

        // Win button at the end cell (bottom-right)
        int endX = 1 + (MAZE_W - 1) * (CELL + 1);
        int endZ = 1 + (MAZE_H - 1) * (CELL + 1);
        level.setBlock(origin.offset(endX, 0, endZ), Blocks.BEDROCK.defaultBlockState(), 3);
        level.setBlock(origin.offset(endX, 1, endZ), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);

        return getSpawnPos(origin);
    }

    private void carveCell(ServerLevel level, BlockPos origin, int cx, int cz) {
        int bx = 1 + cx * (CELL + 1);
        int bz = 1 + cz * (CELL + 1);
        // Clear the cell interior
        for (int dx = 0; dx < CELL; dx++) {
            for (int dz = 0; dz < CELL; dz++) {
                TaskUtils.fill(level, origin.offset(bx + dx, 0, bz + dz), origin.offset(bx + dx, 2, bz + dz), Blocks.AIR);
            }
        }
    }

    private void carveWall(ServerLevel level, BlockPos origin, int cx1, int cz1, int cx2, int cz2) {
        // Wall position is between the two cells
        int wallX, wallZ;
        if (cx2 > cx1) {
            // Wall to the right of cell 1
            wallX = 1 + cx1 * (CELL + 1) + CELL;
            wallZ = 1 + cz1 * (CELL + 1);
        } else if (cx2 < cx1) {
            // Wall to the left of cell 1
            wallX = 1 + cx2 * (CELL + 1) + CELL;
            wallZ = 1 + cz2 * (CELL + 1);
        } else if (cz2 > cz1) {
            // Wall below cell 1
            wallX = 1 + cx1 * (CELL + 1);
            wallZ = 1 + cz1 * (CELL + 1) + CELL;
        } else {
            // Wall above cell 1
            wallX = 1 + cx2 * (CELL + 1);
            wallZ = 1 + cz2 * (CELL + 1) + CELL;
        }

        // Carve opening in the wall (CELL blocks wide)
        for (int d = 0; d < CELL; d++) {
            if (cx1 != cx2) {
                // Horizontal wall — carve along Z
                TaskUtils.fill(level, origin.offset(wallX, 0, wallZ + d), origin.offset(wallX, 2, wallZ + d), Blocks.AIR);
            } else {
                // Vertical wall — carve along X
                TaskUtils.fill(level, origin.offset(wallX + d, 0, wallZ), origin.offset(wallX + d, 2, wallZ), Blocks.AIR);
            }
        }
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        return level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON) && interactedBlock.distSqr(origin) < 5000;
    }

    @Override
    public void tick(ServerPlayer player, BlockPos origin) {
        // Origin-relative void check
        if (player.getY() < origin.getY() - 5) {
            Vec3 spawn = getSpawnPos(origin);
            player.teleportTo(spawn.x, spawn.y, spawn.z);
        }
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        // Spawn in the first cell (top-left)
        return new Vec3(origin.getX() + 1.5, origin.getY() + 0.1, origin.getZ() + 1.5);
    }
}
