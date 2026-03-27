package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public interface BanishTask {
    /**
     * Generates the task structure at the given origin.
     * @param level The End dimension
     * @param origin A unique bedrock platform center for this player
     * @return The exact spawn location for the player inside the task
     */
    Vec3 generate(ServerLevel level, BlockPos origin);

    /**
     * Check if the player has won (e.g. they pressed the win button).
     * This is called when the player interacts with a block.
     */
    boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin);

    /**
     * Called every tick for the player. Can be used for custom logic (e.g. failing parkour, waves).
     */
    default void tick(ServerPlayer player, BlockPos origin) {
        // Fallback for parkour or falling into the void
        if (player.getY() < 180) {
            Vec3 spawn = getSpawnPos(origin);
            player.teleportTo(spawn.x, spawn.y, spawn.z);
        }
    }

    /**
     * Given the origin, returns the same spawn pos as generate() did.
     */
    Vec3 getSpawnPos(BlockPos origin);
}
