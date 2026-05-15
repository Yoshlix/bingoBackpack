package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.item.TeleportSafety;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Teleports the player to the Nether dimension.
 */
public class NetherTeleport extends BingoItem {

    private final Random random = new Random();

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

        Optional<BlockPos> safePos = TeleportSafety.findSafeSurface(nether, netherX, netherZ);

        if (safePos.isEmpty()) {
            // Try a few random positions nearby
            for (int i = 0; i < 10; i++) {
                int offsetX = netherX + random.nextInt(32) - 16;
                int offsetZ = netherZ + random.nextInt(32) - 16;
                safePos = TeleportSafety.findSafeSurface(nether, offsetX, offsetZ);
                if (safePos.isPresent()) {
                    netherX = offsetX;
                    netherZ = offsetZ;
                    break;
                }
            }
        }

        if (safePos.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKein sicherer Ort im Nether gefunden. Item wurde nicht verbraucht."));
            return false;
        }

        BlockPos targetPos = safePos.get();

        // Teleport to Nether
        player.teleportTo(nether, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);

        player.sendSystemMessage(Component.literal("§c§l🔥 NETHER! §rDu wurdest in den Nether teleportiert!"));
        player.sendSystemMessage(Component.literal("§7Position: §f" + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()));

        return true;
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
