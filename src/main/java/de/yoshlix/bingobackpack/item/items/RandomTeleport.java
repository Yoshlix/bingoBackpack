package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.item.TeleportSafety;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
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

        Optional<BlockPos> safePos = TeleportSafety.findSafeSurface(level, newX, newZ);

        if (safePos.isEmpty()) {
            // Try a few more times with different positions
            for (int i = 0; i < 10; i++) {
                angle = random.nextDouble() * 2 * Math.PI;
                distance = ModConfig.getInstance().randomTeleportMinDistance
                        + random.nextInt(ModConfig.getInstance().randomTeleportMaxDistance
                                - ModConfig.getInstance().randomTeleportMinDistance);
                newX = (int) (player.getX() + Math.cos(angle) * distance);
                newZ = (int) (player.getZ() + Math.sin(angle) * distance);

                safePos = TeleportSafety.findSafeSurface(level, newX, newZ);
                if (safePos.isPresent())
                    break;
            }
        }

        if (safePos.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKein sicherer Teleport-Ort gefunden. Item wurde nicht verbraucht."));
            return false;
        }

        BlockPos targetPos = safePos.get();

        // Teleport player
        player.teleportTo(level, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);

        player.sendSystemMessage(Component.literal("§a§lWOOSH! §rDu wurdest teleportiert!"));
        player.sendSystemMessage(Component.literal("§7Neue Position: §f" + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()));

        return true;
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
