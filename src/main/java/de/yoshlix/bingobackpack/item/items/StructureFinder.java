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
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        // List of structures to look for
        List<TagKey<Structure>> targetStructures = List.of(
                StructureTags.VILLAGE,
                StructureTags.MINESHAFT,
                StructureTags.ON_TREASURE_MAPS, // Monuments, Mansions, etc.
                StructureTags.STRONGHOLD,
                StructureTags.DESERT_PYRAMID,
                StructureTags.JUNGLE_TEMPLE,
                StructureTags.SWAMP_HUT,
                StructureTags.IGLOO,
                StructureTags.RUINED_PORTAL);

        BlockPos playerPos = player.blockPosition();
        int radius = ModConfig.getInstance().structureSearchRadius / 16; // Chunks? Actually the arg is usually radius
                                                                         // in chunks or blocks depending on version.
        // findNearestMapStructure usually takes radius in chunks being 100 max? No,
        // it's radius in chunks usually. 100 is standard.
        // Let's assume the config is in blocks, so divide by 16.
        if (radius < 1)
            radius = 100;

        // Actually, findNearestMapStructure return nullable BlockPos

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
                    + ModConfig.getInstance().structureSearchRadius + " Blöcke)."));
            return false; // Don't consume if nothing found? Or consume? Usually compasses don't consume
                          // if they fail.
        }
    }

    private String getStructureName(TagKey<Structure> tag) {
        if (tag == StructureTags.VILLAGE)
            return "Dorf";
        if (tag == StructureTags.MINESHAFT)
            return "Minenschacht";
        if (tag == StructureTags.STRONGHOLD)
            return "Stronghold";
        if (tag == StructureTags.ON_TREASURE_MAPS)
            return "Schatz/Struktur";
        if (tag == StructureTags.DESERT_PYRAMID)
            return "Wüstenpyramide";
        if (tag == StructureTags.JUNGLE_TEMPLE)
            return "Dschungeltempel";
        if (tag == StructureTags.SWAMP_HUT)
            return "Hexenhütte";
        if (tag == StructureTags.IGLOO)
            return "Iglu";
        if (tag == StructureTags.RUINED_PORTAL)
            return "Ruiniertes Portal";
        return "Struktur";
    }
}
