package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Random;

/**
 * Teleports the player to a random location in the world.
 */
public class RandomTeleport extends BingoItem {

    private final Random random = new Random();
    private static final int MIN_DISTANCE = 500;
    private static final int MAX_DISTANCE = 5000;

    @Override
    public String getId() {
        return "random_teleport";
    }

    @Override
    public String getName() {
        return "Zufälliger Teleport";
    }

    @Override
    public String getDescription() {
        return "Teleportiert dich an einen zufälligen Ort.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.UNCOMMON;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        // Calculate random offset
        double angle = random.nextDouble() * 2 * Math.PI;
        int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);

        int newX = (int) (player.getX() + Math.cos(angle) * distance);
        int newZ = (int) (player.getZ() + Math.sin(angle) * distance);

        // Find safe Y position
        BlockPos targetPos = new BlockPos(newX, 0, newZ);
        int safeY = findSafeY(level, targetPos);

        if (safeY == -1) {
            // Try a few more times
            for (int i = 0; i < 5; i++) {
                angle = random.nextDouble() * 2 * Math.PI;
                distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);
                newX = (int) (player.getX() + Math.cos(angle) * distance);
                newZ = (int) (player.getZ() + Math.sin(angle) * distance);
                targetPos = new BlockPos(newX, 0, newZ);
                safeY = findSafeY(level, targetPos);
                if (safeY != -1)
                    break;
            }
        }

        if (safeY == -1) {
            safeY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, newX, newZ) + 1;
        }

        // Teleport player
        player.teleportTo(level, newX + 0.5, safeY, newZ + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);

        player.sendSystemMessage(Component.literal("§a§lWOOSH! §rDu wurdest teleportiert!"));
        player.sendSystemMessage(Component.literal("§7Neue Position: §f" + newX + ", " + safeY + ", " + newZ));

        return true;
    }

    private int findSafeY(ServerLevel level, BlockPos pos) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());

        // Check if the surface is safe (not in lava, has air above)
        BlockPos surfacePos = new BlockPos(pos.getX(), surfaceY, pos.getZ());

        if (level.getBlockState(surfacePos).isAir() &&
                level.getBlockState(surfacePos.above()).isAir()) {
            return surfaceY;
        }

        // Search for safe spot
        for (int y = surfaceY; y < level.getMaxY() - 2; y++) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (level.getBlockState(checkPos).isAir() &&
                    level.getBlockState(checkPos.above()).isAir() &&
                    !level.getBlockState(checkPos.below()).isAir()) {
                return y;
            }
        }

        return -1;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Distanz: " + MIN_DISTANCE + "-" + MAX_DISTANCE + " Blöcke"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
