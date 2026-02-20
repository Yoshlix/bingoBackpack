package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.BingoBackpack;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Instant Structure - Spawnt eine zufällige Vanilla-Struktur an deiner
 * Position.
 * Kann Häuser, Türme, Hütten und andere nützliche Strukturen spawnen.
 */
public class InstantStructure extends BingoItem {

    private final Random random = new Random();

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
            // Hole alle registrierten Strukturen
            var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            List<ResourceKey<Structure>> structures = new ArrayList<>();
            // listElements returns Stream<Holder.Reference<T>>, we extract the key
            registry.listElements().forEach(holder -> structures.add(holder.key()));

            if (structures.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cKeine Strukturen gefunden!"));
                return false;
            }

            // Wähle eine zufällige Struktur
            ResourceKey<Structure> randomStructure = structures.get(random.nextInt(structures.size()));
            // .identifier() instead of .location() based on project mappings
            String structureId = randomStructure.identifier().toString();

            // Erstelle CommandSourceStack mit Admin-Rechten für den Befehl (Server Console)
            CommandSourceStack source = level.getServer().createCommandSourceStack()
                    .withPosition(player.position())
                    .withRotation(player.getRotationVector())
                    .withSuppressedOutput();

            // Position (direkt beim Spieler)
            BlockPos pos = player.blockPosition();

            // Führe /place structure aus
            // Syntax: /place structure <structure> [pos]
            String command = String.format("place structure %s %d %d %d", 
                    structureId, pos.getX(), pos.getY(), pos.getZ());
            
            level.getServer().getCommands().performPrefixedCommand(source, command);

            // Feedback an den Spieler
            player.sendSystemMessage(Component.literal("§a§l✨ Struktur gespawnt! ✨"));
            player.sendSystemMessage(
                    Component.literal("§7Eine §e" + structureId + " §7wurde beschworen!"));
            
            return true;

        } catch (Exception e) {
            BingoBackpack.LOGGER.error("Failed to spawn structure via command", e);
            player.sendSystemMessage(Component.literal("§cFehler beim Spawnen der Struktur!"));
            return false;
        }
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Spawnt eine §czufällige §7Minecraft-Struktur"),
                Component.literal("§7Kann §eALLES §7sein:"),
                Component.literal("§7Dörfer, Festungen, Antike Städte..."),
                Component.literal("§c§l⚠ MAXIMALES CHAOS! ⚠"),
                Component.literal("§7Die ganze Struktur wird generiert!"));
    }
}
