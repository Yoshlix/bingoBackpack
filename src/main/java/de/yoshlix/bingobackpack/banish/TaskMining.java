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
        // Clear area
        TaskUtils.fill(level, origin.offset(-10, -8, -10), origin.offset(10, 12, 10), Blocks.AIR);

        // Bedrock floor so player doesn't fall into void
        TaskUtils.fill(level, origin.offset(-7, -8, -7), origin.offset(7, -8, 7), Blocks.BEDROCK);

        // Obsidian outer shell (hard but breakable — not bedrock!)
        TaskUtils.fill(level, origin.offset(-6, -7, -6), origin.offset(6, 6, 6), Blocks.OBSIDIAN);
        // Stone inner fill (the actual mining part)
        TaskUtils.fill(level, origin.offset(-5, -6, -5), origin.offset(5, 5, 5), Blocks.STONE);

        // Mix in some ores and different blocks for variety
        Random decorRand = new Random(origin.asLong() + 1);
        for (int i = 0; i < 30; i++) {
            int rx = decorRand.nextInt(9) - 4;
            int ry = decorRand.nextInt(9) - 4;
            int rz = decorRand.nextInt(9) - 4;
            level.setBlock(origin.offset(rx, ry, rz), Blocks.DEEPSLATE.defaultBlockState(), 3);
        }
        for (int i = 0; i < 15; i++) {
            int rx = decorRand.nextInt(9) - 4;
            int ry = decorRand.nextInt(9) - 4;
            int rz = decorRand.nextInt(9) - 4;
            level.setBlock(origin.offset(rx, ry, rz), Blocks.ANDESITE.defaultBlockState(), 3);
        }

        // Spawn area: small air pocket at the top of the stone cube
        TaskUtils.fill(level, origin.offset(-1, 4, -1), origin.offset(1, 5, 1), Blocks.AIR);
        // Light in spawn pocket
        level.setBlock(origin.offset(0, 5, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);

        // Place win button randomly deep inside the stone
        Random rand = new Random(origin.asLong());
        int rx = rand.nextInt(7) - 3; // -3 to 3
        int ry = rand.nextInt(5) - 4; // -4 to 0 (bottom half)
        int rz = rand.nextInt(7) - 3; // -3 to 3

        // Create a small cavity for the button
        level.setBlock(origin.offset(rx, ry + 1, rz), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(origin.offset(rx, ry, rz), Blocks.STONE.defaultBlockState(), 3); // solid base
        level.setBlock(origin.offset(rx, ry + 1, rz), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);

        return getSpawnPos(origin);
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        return level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON) && interactedBlock.distSqr(origin) < 400;
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
