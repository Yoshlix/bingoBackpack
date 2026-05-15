package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.BingoBackpack;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.item.TeleportSafety;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Instant Structure - Spawnt eine zufällige Vanilla-Struktur an deiner
 * Position.
 * Kann Häuser, Türme, Hütten und andere nützliche Strukturen spawnen.
 */
public class InstantStructure extends BingoItem {

    private final Random random = new Random();
    private static final List<String> OVERWORLD_STRUCTURES = List.of(
            "minecraft:desert_pyramid",
            "minecraft:jungle_pyramid",
            "minecraft:igloo",
            "minecraft:swamp_hut",
            "minecraft:pillager_outpost",
            "minecraft:shipwreck_beached",
            "minecraft:ruined_portal",
            "minecraft:ruined_portal_desert",
            "minecraft:ruined_portal_jungle",
            "minecraft:ruined_portal_swamp",
            "minecraft:ruined_portal_mountain",
            "minecraft:village_desert",
            "minecraft:village_plains",
            "minecraft:village_savanna",
            "minecraft:village_taiga",
            "minecraft:mansion");
    private static final List<String> NETHER_STRUCTURES = List.of(
            "minecraft:fortress",
            "minecraft:bastion_remnant",
            "minecraft:ruined_portal_nether",
            "minecraft:nether_fossil");
    private static final List<String> END_STRUCTURES = List.of(
            "minecraft:end_city");

    @Override
    public String getId() {
        return "instant_structure";
    }

    @Override
    public String getName() {
        return "Sofort-Struktur";
    }

    @Override
    public String getDescription() {
        return "Spawnt eine zufällige Vanilla-Struktur an deiner Position!";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        try {
            List<String> candidates = getStructureCandidates(level);

            if (candidates.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cKeine Strukturen gefunden!"));
                return false;
            }

            // Erstelle CommandSourceStack mit Admin-Rechten für den Befehl (Server Console)
            CommandSourceStack source = level.getServer().createCommandSourceStack()
                    .withPosition(player.position())
                    .withRotation(player.getRotationVector())
                    .withSuppressedOutput();

            Optional<BlockPos> safePos = TeleportSafety.findSafeSurface(level, player.blockPosition().getX(), player.blockPosition().getZ());
            if (safePos.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cKein sicherer Platz für eine Struktur gefunden."));
                return false;
            }

            BlockPos pos = safePos.get().offset(0, -1, 0);
            String structureId = null;
            boolean placed = false;

            for (int i = 0; i < Math.min(8, candidates.size()); i++) {
                structureId = candidates.remove(random.nextInt(candidates.size()));
                if (!isRegisteredStructure(level, structureId)) {
                    continue;
                }

                // Syntax: /place structure <structure> [pos]
                String command = String.format("place structure %s %d %d %d",
                        structureId, pos.getX(), pos.getY(), pos.getZ());

                level.getServer().getCommands().performPrefixedCommand(source, command);
                placed = true;
                break;
            }

            if (!placed) {
                player.sendSystemMessage(Component.literal("§cKonnte keine vollständige Struktur platzieren. Item wurde nicht verbraucht."));
                return false;
            }

            // Feedback an den Spieler
            player.sendSystemMessage(Component.literal("§a§l✨ Struktur gespawnt! ✨"));
            player.sendSystemMessage(
                    Component.literal("§7Eine vollständige §e" + structureId + " §7wurde beschworen!"));
            
            return true;

        } catch (Exception e) {
            BingoBackpack.LOGGER.error("Failed to spawn structure via command", e);
            player.sendSystemMessage(Component.literal("§cFehler beim Spawnen der Struktur!"));
            return false;
        }
    }

    private List<String> getStructureCandidates(ServerLevel level) {
        if (level.dimension() == Level.NETHER) {
            return new java.util.ArrayList<>(NETHER_STRUCTURES);
        }
        if (level.dimension() == Level.END) {
            return new java.util.ArrayList<>(END_STRUCTURES);
        }
        return new java.util.ArrayList<>(OVERWORLD_STRUCTURES);
    }

    private boolean isRegisteredStructure(ServerLevel level, String structureId) {
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, Identifier.parse(structureId));
        return registry.get(key).isPresent();
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Spawnt eine §czufällige §7große Vanilla-Struktur"),
                Component.literal("§7z.B. Wüstentempel, Dorf, Outpost"),
                Component.literal("§c§l⚠ MAXIMALES CHAOS! ⚠"));
    }
}
