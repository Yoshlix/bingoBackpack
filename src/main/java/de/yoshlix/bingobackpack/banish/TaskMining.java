package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TaskMining implements BanishTask {
    @Override
    public String getTaskDescription() {
        return "Grabe dich mit der Spitzhacke durch den Stein und finde den versteckten Button!";
    }

    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        Random rand = TaskUtils.randomFor(origin);

        int r = 6 + rand.nextInt(4); // 6-9: horizontal size varies per instance

        // Clear area
        TaskUtils.fill(level, origin.offset(-r - 5, -8, -r - 5), origin.offset(r + 5, 12, r + 5), Blocks.AIR);

        // Bedrock floor so player doesn't fall into void
        TaskUtils.fill(level, origin.offset(-r - 2, -8, -r - 2), origin.offset(r + 2, -8, r + 2), Blocks.BEDROCK);

        // Obsidian outer shell (hard but breakable - not bedrock!)
        TaskUtils.fill(level, origin.offset(-r - 1, -7, -r - 1), origin.offset(r + 1, 6, r + 1), Blocks.OBSIDIAN);
        // Stone inner fill (the actual mining part)
        TaskUtils.fill(level, origin.offset(-r, -6, -r), origin.offset(r, 5, r), Blocks.STONE);

        // Mix in some ores and different blocks for variety
        Random decorRand = new Random(rand.nextLong());
        for (int i = 0; i < 30 + r * 2; i++) {
            int rx = decorRand.nextInt(2 * r - 1) - (r - 1);
            int ry = decorRand.nextInt(11) - 6;
            int rz = decorRand.nextInt(2 * r - 1) - (r - 1);
            level.setBlock(origin.offset(rx, ry, rz), Blocks.DEEPSLATE.defaultBlockState(), 3);
        }
        for (int i = 0; i < 15 + r; i++) {
            int rx = decorRand.nextInt(2 * r - 1) - (r - 1);
            int ry = decorRand.nextInt(11) - 6;
            int rz = decorRand.nextInt(2 * r - 1) - (r - 1);
            level.setBlock(origin.offset(rx, ry, rz), Blocks.ANDESITE.defaultBlockState(), 3);
        }

        // Spawn area: small air pocket at the top of the stone cube. Fixed height,
        // independent of r, so getSpawnPos() below stays correct without needing
        // to know which r was rolled for this instance.
        TaskUtils.fill(level, origin.offset(-1, 4, -1), origin.offset(1, 5, 1), Blocks.AIR);
        level.setBlock(origin.offset(0, 5, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);

        // Place the win button using one of three orientation strategies, so the
        // general digging approach that worked last time (e.g. "straight down
        // from spawn") doesn't reliably work again.
        int orientation = rand.nextInt(3);
        int bx, by, bz;
        switch (orientation) {
            case 0 -> { // vertical shaft: deep, roughly centered under spawn
                bx = rand.nextInt(5) - 2;
                bz = rand.nextInt(5) - 2;
                by = -6 + rand.nextInt(2);
            }
            case 1 -> { // horizontal: off to one side, mid depth
                int dir = rand.nextInt(4);
                int dist = r - 1;
                int perp = rand.nextInt(5) - 2;
                by = -1 + rand.nextInt(3);
                bx = switch (dir) {
                    case 0 -> dist;
                    case 1 -> -dist;
                    default -> perp;
                };
                bz = switch (dir) {
                    case 2 -> dist;
                    case 3 -> -dist;
                    default -> perp;
                };
            }
            default -> { // blob: anywhere in the volume
                bx = rand.nextInt(2 * (r - 1) + 1) - (r - 1);
                bz = rand.nextInt(2 * (r - 1) + 1) - (r - 1);
                by = -6 + rand.nextInt(11);
            }
        }

        // Create a small cavity for the button
        level.setBlock(origin.offset(bx, by + 1, bz), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.offset(bx, by, bz), Blocks.STONE.defaultBlockState(), 3); // solid base
        level.setBlock(origin.offset(bx, by + 1, bz), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);

        return getSpawnPos(origin);
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        return level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON) && interactedBlock.distSqr(origin) < 900;
    }

    @Override
    public void tick(ServerPlayer player, BlockPos origin) {
        BanishData data = BanishManager.getInstance().getBanishData(player);
        if (data != null && data.taskTime == 0) {
            giveMiningGear(player);
            data.taskTime = 1;
        }
        // Don't call super.tick — player works underground
    }

    @Override
    public void onRespawn(ServerPlayer player, BlockPos origin) {
        giveMiningGear(player);
    }

    private void giveMiningGear(ServerPlayer player) {
        player.getInventory().clearContent();
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        player.getInventory().add(new ItemStack(Items.TORCH, 32));
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        return new Vec3(origin.getX() + 0.5, origin.getY() + 4.1, origin.getZ() + 0.5);
    }
}
