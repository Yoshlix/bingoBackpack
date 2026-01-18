package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Random;

/**
 * Teleports the player to a random location in the world.
 */
public class RandomTeleport extends BingoItem {

    private final Random random = new Random();

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
        int distance = ModConfig.getInstance().randomTeleportMinDistance + random.nextInt(
                ModConfig.getInstance().randomTeleportMaxDistance - ModConfig.getInstance().randomTeleportMinDistance);

        int newX = (int) (player.getX() + Math.cos(angle) * distance);
        int newZ = (int) (player.getZ() + Math.sin(angle) * distance);

        // Force chunk generation at target location
        level.getChunk(newX >> 4, newZ >> 4);

        // Find safe Y position
        BlockPos targetPos = new BlockPos(newX, 0, newZ);
        int safeY = findSafeY(level, targetPos);

        if (safeY == -1) {
            // Try a few more times with different positions
            for (int i = 0; i < 10; i++) {
                angle = random.nextDouble() * 2 * Math.PI;
                distance = ModConfig.getInstance().randomTeleportMinDistance
                        + random.nextInt(ModConfig.getInstance().randomTeleportMaxDistance
                                - ModConfig.getInstance().randomTeleportMinDistance);
                newX = (int) (player.getX() + Math.cos(angle) * distance);
                newZ = (int) (player.getZ() + Math.sin(angle) * distance);

                // Force chunk generation
                level.getChunk(newX >> 4, newZ >> 4);

                targetPos = new BlockPos(newX, 0, newZ);
                safeY = findSafeY(level, targetPos);
                if (safeY != -1)
                    break;
            }
        }

        if (safeY == -1) {
            // Fallback: use heightmap + 1 but ensure it's above sea level
            // In the Nether, cap at ceiling level to avoid spawning on top of bedrock
            boolean isNether = level.dimension() == Level.NETHER;
            int heightmapY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, newX, newZ);

            if (isNether) {
                // In Nether, don't use heightmap (would put us on ceiling)
                // Instead, use configured nether fallback Y
                safeY = ModConfig.getInstance().netherFallbackY;
            } else {
                safeY = Math.max(heightmapY + 1, level.getSeaLevel() + 1);
            }
        }

        // Teleport player
        player.teleportTo(level, newX + 0.5, safeY, newZ + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);

        player.sendSystemMessage(Component.literal("§a§lWOOSH! §rDu wurdest teleportiert!"));
        player.sendSystemMessage(Component.literal("§7Neue Position: §f" + newX + ", " + safeY + ", " + newZ));

        return true;
    }

    private int findSafeY(ServerLevel level, BlockPos pos) {
        boolean isNether = level.dimension() == Level.NETHER;
        int minY = level.getMinY();

        if (isNether) {
            // In the Nether, we need to search from bottom to ceiling, avoiding the bedrock
            // roof
            return findSafeYNether(level, pos);
        }

        // Normal dimension logic using heightmap
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());

        // Ensure we're above the minimum build height and have a reasonable surface
        if (surfaceY <= minY + 5) {
            // Probably in void or ungenerated - not safe
            return -1;
        }

        // Check multiple positions around the surface
        for (int yOffset = 0; yOffset <= ModConfig.getInstance().safePosSearchRange; yOffset++) {
            int testY = surfaceY + yOffset;

            if (isSafePosition(level, pos.getX(), testY, pos.getZ())) {
                return testY;
            }
        }

        return -1;
    }

    /**
     * Find a safe Y position in the Nether by scanning from bottom up,
     * but staying below the bedrock ceiling.
     */
    private int findSafeYNether(ServerLevel level, BlockPos pos) {
        int minY = level.getMinY();
        int maxSafeY = ModConfig.getInstance().netherCeilingY - 2; // Stay well below the ceiling

        // Scan from bottom to top, looking for the first safe spot
        for (int y = minY + 1; y <= maxSafeY; y++) {
            if (isSafePosition(level, pos.getX(), y, pos.getZ())) {
                return y;
            }
        }

        // If no safe spot found scanning up, try scanning down from ceiling
        for (int y = maxSafeY; y >= minY + 1; y--) {
            if (isSafePosition(level, pos.getX(), y, pos.getZ())) {
                return y;
            }
        }

        return -1;
    }

    /**
     * Check if a position is safe for the player to stand at.
     * 
     * @param y The Y position where the player's feet would be
     */
    private boolean isSafePosition(ServerLevel level, int x, int y, int z) {
        BlockPos groundPos = new BlockPos(x, y - 1, z);
        BlockPos feetPos = new BlockPos(x, y, z);
        BlockPos headPos = new BlockPos(x, y + 1, z);

        var groundState = level.getBlockState(groundPos);
        var feetState = level.getBlockState(feetPos);
        var headState = level.getBlockState(headPos);

        // Ground must be solid and not dangerous (not lava, not fire)
        boolean groundIsSafe = !groundState.isAir()
                && groundState.getFluidState().isEmpty()
                && !groundState.getCollisionShape(level, groundPos).isEmpty();

        // Feet and head must be passable (air or non-blocking) and not dangerous
        boolean feetIsPassable = feetState.getCollisionShape(level, feetPos).isEmpty()
                && feetState.getFluidState().isEmpty();
        boolean headIsPassable = headState.getCollisionShape(level, headPos).isEmpty()
                && headState.getFluidState().isEmpty();

        return groundIsSafe && feetIsPassable && headIsPassable;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Distanz: " + ModConfig.getInstance().randomTeleportMinDistance + "-"
                        + ModConfig.getInstance().randomTeleportMaxDistance + " Blöcke"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
