package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StructureFinder extends BingoItem {

    @Override
    public String getId() {
        return "structure_finder";
    }

    @Override
    public String getName() {
        return "Struktur-Kompass";
    }

    @Override
    public String getDescription() {
        return "Findet die nächste Struktur in der Nähe.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        return locateNearestStructure(player);
    }

    /**
     * Finds and messages the player about the nearest known structure.
     * Extracted from {@link #onUse} so it can be reused by the Momentum
     * "Spürsinn" ability, which repeats this same search automatically for a
     * team over time instead of a one-shot item use.
     *
     * @return true if a structure was found (and, when called as the item, should be consumed)
     */
    public static boolean locateNearestStructure(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        // List of structures to look for (MC 1.21.x compatible tags)
        List<TagKey<Structure>> targetStructures = List.of(
                StructureTags.VILLAGE,
                StructureTags.MINESHAFT,
                StructureTags.ON_TREASURE_MAPS, // Monuments, Mansions, etc.
                StructureTags.RUINED_PORTAL,
                StructureTags.ON_WOODLAND_EXPLORER_MAPS,
                StructureTags.ON_OCEAN_EXPLORER_MAPS);

        BlockPos playerPos = player.blockPosition();
        // findNearestMapStructure()'s radius parameter is in chunks (verified
        // against ServerLevel's signature); structureSearchRadius is configured
        // in blocks, hence the /16. searchedBlocks tracks whatever radius we
        // actually end up using, in blocks, for the failure message below.
        int radius = ModConfig.getInstance().structureSearchRadius / 16;
        if (radius < 1) {
            radius = 100;
        }
        int searchedBlocks = radius * 16;

        BlockPos nearestPos = null;
        String nearestName = "Unbekannt";
        double nearestDistanceSq = Double.MAX_VALUE;

        for (TagKey<Structure> tag : targetStructures) {
            BlockPos foundPos = level.findNearestMapStructure(tag, playerPos, radius, false);
            if (foundPos != null) {
                double distSq = foundPos.distSqr(playerPos);
                if (distSq < nearestDistanceSq) {
                    nearestDistanceSq = distSq;
                    nearestPos = foundPos;
                    nearestName = getStructureName(tag);
                }
            }
        }

        if (nearestPos != null) {
            player.sendSystemMessage(Component.literal("§aStruktur gefunden: §e" + nearestName));
            player.sendSystemMessage(
                    Component.literal("§7Koordinaten: §f" + nearestPos.getX() + ", (~), " + nearestPos.getZ()));
            player.sendSystemMessage(
                    Component.literal("§7Distanz: §f" + (int) Math.sqrt(nearestDistanceSq) + " Blöcke"));
            return true;
        } else {
            player.sendSystemMessage(Component.literal("§cKeine Strukturen in der Nähe gefunden ("
                    + searchedBlocks + " Blöcke)."));
            return false; // Don't consume if nothing found? Or consume? Usually compasses don't consume
                          // if they fail.
        }
    }

    private static String getStructureName(TagKey<Structure> tag) {
        if (tag == StructureTags.VILLAGE)
            return "Dorf";
        if (tag == StructureTags.MINESHAFT)
            return "Minenschacht";
        if (tag == StructureTags.ON_TREASURE_MAPS)
            return "Schatz/Struktur";
        if (tag == StructureTags.RUINED_PORTAL)
            return "Ruiniertes Portal";
        if (tag == StructureTags.ON_WOODLAND_EXPLORER_MAPS)
            return "Waldanwesen";
        if (tag == StructureTags.ON_OCEAN_EXPLORER_MAPS)
            return "Ozeanmonument";
        return "Struktur";
    }
}
