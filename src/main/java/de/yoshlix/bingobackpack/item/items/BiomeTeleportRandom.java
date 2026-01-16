package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Random;

/**
 * Teleports the player to a random biome.
 */
public class BiomeTeleportRandom extends BingoItem {

    private final Random random = new Random();
    private static final int SEARCH_RADIUS = 10000;

    @Override
    public String getId() {
        return "biome_teleport_random";
    }

    @Override
    public String getName() {
        return "Zufälliger Biom-Teleport";
    }

    @Override
    public String getDescription() {
        return "Teleportiert dich in ein zufälliges Biom.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        // Get all biomes in the registry
        Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        var biomes = biomeRegistry.listElements().toList();

        if (biomes.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKeine Biome gefunden!"));
            return false;
        }

        // Select random biome
        final Holder<Biome> initialTargetBiome = biomes.get(random.nextInt(biomes.size()));
        Holder<Biome> targetBiome = initialTargetBiome;
        String biomeName = targetBiome.unwrapKey().orElseThrow().identifier().getPath();

        player.sendSystemMessage(Component.literal("§6Suche Biom: §e" + formatBiomeName(biomeName) + "§6..."));

        // Get the ResourceKey for searching
        var targetKey = initialTargetBiome.unwrapKey().orElseThrow();

        // Find the biome
        BlockPos playerPos = player.blockPosition();
        var found = level.findClosestBiome3d(
                b -> b.is(targetKey),
                playerPos,
                SEARCH_RADIUS,
                32,
                64);

        if (found == null) {
            // Try to find ANY different biome
            var currentBiome = level.getBiome(playerPos);
            found = level.findClosestBiome3d(
                    b -> !b.equals(currentBiome),
                    playerPos,
                    SEARCH_RADIUS,
                    32,
                    64);

            if (found != null) {
                targetBiome = level.getBiome(found.getFirst());
                biomeName = targetBiome.unwrapKey()
                        .map(k -> k.identifier().getPath())
                        .orElse("Unbekannt");
            }
        }

        if (found == null) {
            player.sendSystemMessage(Component.literal("§cKein passendes Biom in der Nähe gefunden!"));
            return false;
        }

        BlockPos targetPos = found.getFirst();
        int safeY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, targetPos.getX(), targetPos.getZ()) + 1;

        player.teleportTo(level, targetPos.getX() + 0.5, safeY, targetPos.getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);

        player.sendSystemMessage(Component.literal("§a§lWOOSH! §rDu bist jetzt in: §e" + formatBiomeName(biomeName)));

        return true;
    }

    private String formatBiomeName(String biomeName) {
        // Convert snake_case to Title Case
        String[] parts = biomeName.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Sucht im Umkreis von " + SEARCH_RADIUS + " Blöcken"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
