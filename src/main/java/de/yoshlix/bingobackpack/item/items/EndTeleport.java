package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Teleports the player to the End dimension.
 */
public class EndTeleport extends BingoItem {

    // End spawn platform location
    private static final int END_SPAWN_X = 100;
    private static final int END_SPAWN_Y = 49;
    private static final int END_SPAWN_Z = 0;

    @Override
    public String getId() {
        return "end_teleport";
    }

    @Override
    public String getName() {
        return "End-Portal";
    }

    @Override
    public String getDescription() {
        return "Teleportiert dich sofort ins End.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        ServerLevel currentLevel = (ServerLevel) player.level();

        // Check if already in End
        if (currentLevel.dimension() == Level.END) {
            player.sendSystemMessage(Component.literal("§cDu bist bereits im End!"));
            return false;
        }

        // Get the End dimension
        ServerLevel end = currentLevel.getServer().getLevel(Level.END);
        if (end == null) {
            player.sendSystemMessage(Component.literal("§cEnd nicht verfügbar!"));
            return false;
        }

        // Force chunk generation at spawn platform
        end.getChunk(END_SPAWN_X >> 4, END_SPAWN_Z >> 4);

        // Create obsidian platform for safe landing (like vanilla end portal behavior)
        createSpawnPlatform(end, END_SPAWN_X, END_SPAWN_Y, END_SPAWN_Z);

        // Teleport to End spawn platform
        player.teleportTo(end, END_SPAWN_X + 0.5, END_SPAWN_Y + 1, END_SPAWN_Z + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);

        player.sendSystemMessage(Component.literal("§5§l✦ END! §rDu wurdest ins End teleportiert!"));
        player.sendSystemMessage(Component.literal("§7Viel Glück beim Drachenkampf!"));

        return true;
    }

    /**
     * Creates a 5x5 obsidian platform like vanilla does when entering the End.
     */
    private void createSpawnPlatform(ServerLevel level, int centerX, int y, int centerZ) {
        // Create 5x5 obsidian platform
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                BlockPos platformPos = new BlockPos(x, y, z);
                level.setBlock(platformPos, Blocks.OBSIDIAN.defaultBlockState(), 3);

                // Clear 3 blocks above for player space
                for (int yOffset = 1; yOffset <= 3; yOffset++) {
                    BlockPos clearPos = new BlockPos(x, y + yOffset, z);
                    level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Erstellt eine sichere Landeplattform"),
                Component.literal("§7Vorsicht: Der Enderdrache wartet!"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
