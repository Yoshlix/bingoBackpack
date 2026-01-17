package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.ChatFormatting;

import java.util.*;

/**
 * Allows the player to choose and teleport to a specific biome.
 */
public class BiomeTeleportChoice extends BingoItem {

    private static final Map<UUID, List<Holder<Biome>>> pendingBiomeSelections = new HashMap<>();

    // Common biomes to show in the selection
    private static final List<String> COMMON_BIOMES = List.of(
            "plains", "forest", "desert", "taiga", "snowy_plains",
            "jungle", "swamp", "badlands", "beach", "ocean",
            "deep_ocean", "river", "mountain", "windswept_hills", "savanna",
            "dark_forest", "birch_forest", "cherry_grove", "meadow", "frozen_ocean");

    @Override
    public String getId() {
        return "biome_teleport_choice";
    }

    @Override
    public String getName() {
        return "Biom-Teleport Auswahl";
    }

    @Override
    public String getDescription() {
        return "Wähle ein Biom für den Teleport.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        // Get biomes from registry
        Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);

        // Filter to common/interesting biomes
        List<Holder<Biome>> availableBiomes = new ArrayList<>();
        for (String biomeName : COMMON_BIOMES) {
            var key = net.minecraft.resources.ResourceKey.create(Registries.BIOME,
                    net.minecraft.resources.Identifier.parse("minecraft:" + biomeName));
            biomeRegistry.get(key).ifPresent(holder -> availableBiomes.add(holder));
        }

        if (availableBiomes.isEmpty()) {
            // Fallback to all biomes
            availableBiomes.addAll(biomeRegistry.listElements().toList());
        }

        // Store for selection
        pendingBiomeSelections.put(player.getUUID(), availableBiomes);

        // Show selection menu
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l═══════ Wähle ein Biom ═══════"));
        player.sendSystemMessage(Component.literal(""));

        int index = 1;
        for (var biome : availableBiomes) {
            String biomeName = biome.unwrapKey().map(k -> k.identifier().getPath()).orElse("unknown");
            String displayName = formatBiomeName(biomeName);

            Component message = Component.literal("  §e[" + index + "] ")
                    .append(Component.literal(displayName).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent.RunCommand("/bingobackpack biome " + index))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("Klicke um zu " + displayName + " zu teleportieren")))));

            player.sendSystemMessage(message);
            index++;

            // Limit display
            if (index > 20) {
                player.sendSystemMessage(Component.literal("  §7... und mehr"));
                break;
            }
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Klicke oder schreibe §f/bingobackpack biome <nummer>"));
        player.sendSystemMessage(Component.literal("§6§l════════════════════════════"));

        return false; // Don't consume until selection
    }

    public static boolean processBiomeSelection(ServerPlayer player, String selection) {
        List<Holder<Biome>> biomes = pendingBiomeSelections.remove(player.getUUID());
        if (biomes == null) {
            player.sendSystemMessage(Component.literal("§cKeine ausstehende Biom-Auswahl!"));
            return false;
        }

        int index;
        try {
            index = Integer.parseInt(selection) - 1;
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        if (index < 0 || index >= biomes.size()) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        Holder<Biome> targetBiome = biomes.get(index);
        String biomeName = targetBiome.unwrapKey().map(k -> k.identifier().getPath()).orElse("unknown");

        ServerLevel level = (ServerLevel) player.level();
        player.sendSystemMessage(Component.literal("§6Suche " + formatBiomeName(biomeName) + "..."));

        // Find the biome
        var found = level.findClosestBiome3d(
                b -> targetBiome.unwrapKey().map(b::is).orElse(false),
                player.blockPosition(),
                ModConfig.getInstance().biomeTeleportSearchRadius,
                32,
                64);

        if (found == null) {
            player.sendSystemMessage(Component.literal("§cBiom nicht in der Nähe gefunden!"));
            return false;
        }

        BlockPos targetPos = found.getFirst();
        int safeY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, targetPos.getX(), targetPos.getZ()) + 1;

        player.teleportTo(level, targetPos.getX() + 0.5, safeY, targetPos.getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);

        player.sendSystemMessage(Component.literal("§a§lWOOSH! §rDu bist jetzt in: §e" + formatBiomeName(biomeName)));

        consumeItem(player);
        return true;
    }

    private static void consumeItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            var itemOpt = de.yoshlix.bingobackpack.item.BingoItemRegistry.fromItemStack(stack);
            if (itemOpt.isPresent() && itemOpt.get().getId().equals("biome_teleport_choice")) {
                stack.shrink(1);
                return;
            }
        }
    }

    public static boolean hasPendingSelection(UUID playerId) {
        return pendingBiomeSelections.containsKey(playerId);
    }

    private static String formatBiomeName(String biomeName) {
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
                Component.literal("§7Du wählst das Ziel-Biom!"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
