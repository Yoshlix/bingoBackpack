package de.yoshlix.bingobackpack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * /up command - Teleports the player to the surface at their current X/Z
 * position.
 * Handles Nether specially to avoid the bedrock ceiling.
 */
public class UpCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("up")
                .executes(UpCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = (ServerLevel) player.level();

            int x = (int) player.getX();
            int z = (int) player.getZ();

            int safeY = findSurfaceY(level, x, z);

            if (safeY == -1) {
                player.sendSystemMessage(Component.literal("§cKeine sichere Oberfläche gefunden!"));
                return 0;
            }

            // Teleport player
            player.teleportTo(level, x + 0.5, safeY, z + 0.5,
                    java.util.Set.of(), player.getYRot(), player.getXRot(), true);

            player.sendSystemMessage(
                    Component.literal("§a§l↑ UP! §rDu stehst jetzt auf der Oberfläche! §7(Y=" + safeY + ")"));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFehler: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Find the surface Y position where the player can stand safely.
     * In the Nether, searches from bottom up to avoid the ceiling.
     */
    private static int findSurfaceY(ServerLevel level, int x, int z) {
        boolean isNether = level.dimension() == Level.NETHER;

        if (isNether) {
            return findSurfaceYNether(level, x, z);
        } else {
            return findSurfaceYNormal(level, x, z);
        }
    }

    /**
     * Find surface in normal dimensions (Overworld, End) using heightmap.
     */
    private static int findSurfaceYNormal(ServerLevel level, int x, int z) {
        // MOTION_BLOCKING returns the Y above the highest solid block
        int heightmapY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

        // The heightmap gives us the position above the ground, so player stands there
        // But we need to verify it's actually safe
        BlockPos groundPos = new BlockPos(x, heightmapY - 1, z);
        BlockPos feetPos = new BlockPos(x, heightmapY, z);
        BlockPos headPos = new BlockPos(x, heightmapY + 1, z);

        var groundState = level.getBlockState(groundPos);
        var feetState = level.getBlockState(feetPos);
        var headState = level.getBlockState(headPos);

        // Check if the position is safe
        boolean groundIsSafe = !groundState.isAir() && !groundState.liquid() && groundState.blocksMotion();
        boolean feetIsSafe = !feetState.blocksMotion() && !feetState.liquid();
        boolean headIsSafe = !headState.blocksMotion() && !headState.liquid();

        if (groundIsSafe && feetIsSafe && headIsSafe) {
            return heightmapY;
        }

        // Try searching up from heightmap for a safe spot
        for (int yOffset = 0; yOffset <= 10; yOffset++) {
            int testY = heightmapY + yOffset;
            if (isSafePosition(level, x, testY, z)) {
                return testY;
            }
        }

        // Fallback: just use heightmap
        return heightmapY;
    }

    /**
     * Find surface in the Nether by searching from the player's current Y upward,
     * but staying below the bedrock ceiling.
     */
    private static int findSurfaceYNether(ServerLevel level, int x, int z) {
        int minY = level.getMinY();
        int maxSafeY = ModConfig.getInstance().netherCeilingY - 2;

        // Start from player's current position and search upward for open sky (within
        // Nether)
        // We want to find the HIGHEST safe position below the ceiling

        // First, find the highest solid ground below ceiling
        int highestGround = -1;
        for (int y = maxSafeY; y >= minY + 1; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            var state = level.getBlockState(pos);

            if (!state.isAir() && !state.liquid() && state.blocksMotion()) {
                // Found solid ground, check if we can stand on it
                if (isSafePosition(level, x, y + 1, z)) {
                    highestGround = y + 1;
                    break;
                }
            }
        }

        if (highestGround != -1) {
            return highestGround;
        }

        // Fallback: search from bottom up
        for (int y = minY + 1; y <= maxSafeY; y++) {
            if (isSafePosition(level, x, y, z)) {
                return y;
            }
        }

        // Ultimate fallback
        return 64;
    }

    /**
     * Check if a position is safe for the player to stand at.
     */
    private static boolean isSafePosition(ServerLevel level, int x, int y, int z) {
        BlockPos groundPos = new BlockPos(x, y - 1, z);
        BlockPos feetPos = new BlockPos(x, y, z);
        BlockPos headPos = new BlockPos(x, y + 1, z);

        var groundState = level.getBlockState(groundPos);
        var feetState = level.getBlockState(feetPos);
        var headState = level.getBlockState(headPos);

        // Ground must be solid and not dangerous
        boolean groundIsSafe = !groundState.isAir()
                && !groundState.liquid()
                && groundState.blocksMotion();

        // Feet and head must be passable and not dangerous
        boolean feetIsPassable = !feetState.blocksMotion() && !feetState.liquid();
        boolean headIsPassable = !headState.blocksMotion() && !headState.liquid();

        return groundIsSafe && feetIsPassable && headIsPassable;
    }
}
