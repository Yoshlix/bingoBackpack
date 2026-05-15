package de.yoshlix.bingobackpack.item;

import de.yoshlix.bingobackpack.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;

public class TeleportSafety {

    private TeleportSafety() {
    }

    public static Optional<BlockPos> findSafeSurface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);

        if (level.dimension() == Level.NETHER) {
            return findSafeNetherPosition(level, x, z);
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        int minY = level.getMinY();
        int maxY = level.getMaxY();

        if (surfaceY <= minY + 5 || surfaceY >= maxY - 2) {
            return Optional.empty();
        }

        for (int offset = 0; offset <= ModConfig.getInstance().safePosSearchRange; offset++) {
            Optional<BlockPos> above = safeAt(level, x, surfaceY + offset, z);
            if (above.isPresent()) {
                return above;
            }

            Optional<BlockPos> below = safeAt(level, x, surfaceY - offset, z);
            if (below.isPresent()) {
                return below;
            }
        }

        return Optional.empty();
    }

    public static Optional<BlockPos> findSafeSurfaceAround(ServerLevel level, BlockPos center, int radius) {
        Optional<BlockPos> exact = findSafeSurface(level, center.getX(), center.getZ());
        if (exact.isPresent()) {
            return exact;
        }

        for (int distance = 4; distance <= radius; distance += 4) {
            for (int dx = -distance; dx <= distance; dx += 4) {
                Optional<BlockPos> north = findSafeSurface(level, center.getX() + dx, center.getZ() - distance);
                if (north.isPresent()) return north;

                Optional<BlockPos> south = findSafeSurface(level, center.getX() + dx, center.getZ() + distance);
                if (south.isPresent()) return south;
            }

            for (int dz = -distance + 4; dz <= distance - 4; dz += 4) {
                Optional<BlockPos> west = findSafeSurface(level, center.getX() - distance, center.getZ() + dz);
                if (west.isPresent()) return west;

                Optional<BlockPos> east = findSafeSurface(level, center.getX() + distance, center.getZ() + dz);
                if (east.isPresent()) return east;
            }
        }

        return Optional.empty();
    }

    public static boolean isSafeTeleportPosition(ServerLevel level, double x, double y, double z) {
        return safeAt(level, BlockPos.containing(x, y, z)).isPresent();
    }

    private static Optional<BlockPos> findSafeNetherPosition(ServerLevel level, int x, int z) {
        int minY = level.getMinY() + 1;
        int maxY = Math.min(ModConfig.getInstance().netherCeilingY - 2, level.getMaxY() - 2);

        for (int y = minY; y <= maxY; y++) {
            Optional<BlockPos> pos = safeAt(level, x, y, z);
            if (pos.isPresent()) {
                return pos;
            }
        }

        for (int y = maxY; y >= minY; y--) {
            Optional<BlockPos> pos = safeAt(level, x, y, z);
            if (pos.isPresent()) {
                return pos;
            }
        }

        return Optional.empty();
    }

    private static Optional<BlockPos> safeAt(ServerLevel level, int x, int y, int z) {
        return safeAt(level, new BlockPos(x, y, z));
    }

    private static Optional<BlockPos> safeAt(ServerLevel level, BlockPos feetPos) {
        BlockPos groundPos = feetPos.below();
        BlockPos headPos = feetPos.above();

        if (feetPos.getY() <= level.getMinY() || headPos.getY() >= level.getMaxY()) {
            return Optional.empty();
        }

        if (!level.getWorldBorder().isWithinBounds(feetPos)) {
            return Optional.empty();
        }

        if (!level.isLoaded(feetPos)) {
            level.getChunk(feetPos.getX() >> 4, feetPos.getZ() >> 4);
        }

        BlockState ground = level.getBlockState(groundPos);
        BlockState feet = level.getBlockState(feetPos);
        BlockState head = level.getBlockState(headPos);

        if (!isSafeGround(level, groundPos, ground)) {
            return Optional.empty();
        }

        if (!isSafeBodySpace(level, feetPos, feet) || !isSafeBodySpace(level, headPos, head)) {
            return Optional.empty();
        }

        return Optional.of(feetPos);
    }

    private static boolean isSafeGround(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.getCollisionShape(level, pos).isEmpty()
                && !isDangerous(state);
    }

    private static boolean isSafeBodySpace(ServerLevel level, BlockPos pos, BlockState state) {
        return state.getCollisionShape(level, pos).isEmpty()
                && state.getFluidState().isEmpty()
                && !isDangerous(state);
    }

    private static boolean isDangerous(BlockState state) {
        return state.is(Blocks.LAVA)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.POWDER_SNOW);
    }
}
