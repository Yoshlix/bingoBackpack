package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

/**
 * Teleports the player to the Nether dimension.
 */
public class NetherTeleport extends BingoItem {

    private final Random random = new Random();
    private static final int NETHER_CEILING_Y = 127;

    @Override
    public String getId() {
        return "nether_teleport";
    }

    @Override
    public String getName() {
        return "Nether-Portal";
    }

    @Override
    public String getDescription() {
        return "Teleportiert dich sofort in den Nether.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        ServerLevel currentLevel = (ServerLevel) player.level();

        // Check if already in Nether
        if (currentLevel.dimension() == Level.NETHER) {
            player.sendSystemMessage(Component.literal("§cDu bist bereits im Nether!"));
            return false;
        }

        // Get the Nether dimension
        ServerLevel nether = currentLevel.getServer().getLevel(Level.NETHER);
        if (nether == null) {
            player.sendSystemMessage(Component.literal("§cNether nicht verfügbar!"));
            return false;
        }

        // Calculate Nether coordinates (Overworld / 8)
        int netherX = (int) (player.getX() / 8);
        int netherZ = (int) (player.getZ() / 8);

        // Force chunk generation
        nether.getChunk(netherX >> 4, netherZ >> 4);

        // Find safe Y position in Nether
        int safeY = findSafeYNether(nether, netherX, netherZ);

        if (safeY == -1) {
            // Try a few random positions nearby
            for (int i = 0; i < 10; i++) {
                int offsetX = netherX + random.nextInt(32) - 16;
                int offsetZ = netherZ + random.nextInt(32) - 16;
                nether.getChunk(offsetX >> 4, offsetZ >> 4);
                safeY = findSafeYNether(nether, offsetX, offsetZ);
                if (safeY != -1) {
                    netherX = offsetX;
                    netherZ = offsetZ;
                    break;
                }
            }
        }

        if (safeY == -1) {
            // Fallback to Y=64
            safeY = 64;
        }

        // Teleport to Nether
        player.teleportTo(nether, netherX + 0.5, safeY, netherZ + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);

        player.sendSystemMessage(Component.literal("§c§l🔥 NETHER! §rDu wurdest in den Nether teleportiert!"));
        player.sendSystemMessage(Component.literal("§7Position: §f" + netherX + ", " + safeY + ", " + netherZ));

        return true;
    }

    private int findSafeYNether(ServerLevel level, int x, int z) {
        int minY = level.getMinY();
        int maxSafeY = NETHER_CEILING_Y - 2;

        // Scan from bottom to top
        for (int y = minY + 1; y <= maxSafeY; y++) {
            if (isSafePosition(level, x, y, z)) {
                return y;
            }
        }

        return -1;
    }

    private boolean isSafePosition(ServerLevel level, int x, int y, int z) {
        BlockPos groundPos = new BlockPos(x, y - 1, z);
        BlockPos feetPos = new BlockPos(x, y, z);
        BlockPos headPos = new BlockPos(x, y + 1, z);

        var groundState = level.getBlockState(groundPos);
        var feetState = level.getBlockState(feetPos);
        var headState = level.getBlockState(headPos);

        // Ground must be solid and not lava
        boolean groundIsSafe = !groundState.isAir()
                && !groundState.liquid()
                && groundState.blocksMotion();

        // Feet and head must be passable and not lava
        boolean feetIsPassable = !feetState.blocksMotion() && !feetState.liquid();
        boolean headIsPassable = !headState.blocksMotion() && !headState.liquid();

        return groundIsSafe && feetIsPassable && headIsPassable;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Koordinaten werden durch 8 geteilt"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
