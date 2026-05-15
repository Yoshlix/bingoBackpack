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
     * Returns the task description shown to the player.
     */
    String getTaskDescription();

    /**
     * Check if the player has won (e.g. they pressed the win button).
     * This is called when the player interacts with a block.
     */
    boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin);

    /**
     * Called every second (20 ticks) for banished players.
     * Use data.taskTime as seconds, not ticks.
     */
    default void tick(ServerPlayer player, BlockPos origin) {
        // Fallback for falling into the void
        if (player.getY() < 150) {
            Vec3 spawn = getSpawnPos(origin);
            player.teleportTo(spawn.x, spawn.y, spawn.z);
        }
    }

    /**
     * Called when a banished player respawns after death.
     * Override to re-give equipment etc.
     */
    default void onRespawn(ServerPlayer player, BlockPos origin) {
        // default: do nothing
    }

    /**
     * Given the origin, returns the same spawn pos as generate() did.
     */
    Vec3 getSpawnPos(BlockPos origin);
}
