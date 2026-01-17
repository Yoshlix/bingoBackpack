package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;

/**
 * Bingo Radar - Shows hints for incomplete objectives.
 * Scans nearby biomes and shows which ones contain resources for your tasks.
 */
public class BingoRadar extends BingoItem {

    @Override
    public String getId() {
        return "bingo_radar";
    }

    @Override
    public String getName() {
        return "Bingo Radar";
    }

    @Override
    public String getDescription() {
        return "Scannt die Umgebung und zeigt Hinweise für unabgeschlossene Felder.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var teams = BingoApi.getTeams();
        if (teams == null) {
            player.sendSystemMessage(Component.literal("§cKein Bingo-Spiel aktiv!"));
            return false;
        }

        var playerTeam = teams.getTeamForPlayer(player.getUUID());
        if (playerTeam == null) {
            player.sendSystemMessage(Component.literal("§cDu bist in keinem Team!"));
            return false;
        }

        var game = BingoApi.getGameExtended();
        if (game == null) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        var card = game.getActiveCard();
        if (card == null) {
            player.sendSystemMessage(Component.literal("§cKeine Bingo-Karte vorhanden!"));
            return false;
        }

        // Get incomplete objectives
        var incompleteObjectives = new ArrayList<String>();
        for (var objective : card.getObjectives()) {
            if (!objective.hasAchieved(playerTeam.getId())) {
                String name = objective.getDisplayName() != null ? objective.getDisplayName() : objective.getId();
                incompleteObjectives.add(name);
            }
        }

        if (incompleteObjectives.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Du hast alle Felder abgeschlossen!"));
            return false;
        }

        // Scan nearby biomes
        ServerLevel level = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();

        var nearbyBiomes = new ArrayList<String>();

        // Sample biomes in a grid pattern
        for (int x = -ModConfig.getInstance().bingoRadarScanRadius; x <= ModConfig
                .getInstance().bingoRadarScanRadius; x += 64) {
            for (int z = -ModConfig.getInstance().bingoRadarScanRadius; z <= ModConfig
                    .getInstance().bingoRadarScanRadius; z += 64) {
                BlockPos checkPos = playerPos.offset(x, 0, z);
                Holder<Biome> biome = level.getBiome(checkPos);
                String biomeName = biome.unwrapKey()
                        .map(k -> k.identifier().getPath())
                        .orElse("unknown");

                if (!nearbyBiomes.contains(biomeName)) {
                    nearbyBiomes.add(biomeName);
                }
            }
        }

        // Display radar results
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l═══════ 🔍 BINGO RADAR 🔍 ═══════"));
        player.sendSystemMessage(Component.literal(""));

        // Show incomplete objectives
        player.sendSystemMessage(Component.literal("§e§lUnabgeschlossene Felder: §f" + incompleteObjectives.size()));
        player.sendSystemMessage(Component.literal(""));

        int shown = 0;
        for (String objective : incompleteObjectives) {
            if (shown >= 5) {
                player.sendSystemMessage(
                        Component.literal("§7... und " + (incompleteObjectives.size() - 5) + " weitere"));
                break;
            }
            player.sendSystemMessage(Component.literal("  §7• §f" + objective));
            shown++;
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component
                .literal("§a§lBiome in der Nähe (" + ModConfig.getInstance().bingoRadarScanRadius + " Blöcke):"));

        shown = 0;
        for (String biome : nearbyBiomes) {
            if (shown >= 8) {
                player.sendSystemMessage(Component.literal("§7  ... und " + (nearbyBiomes.size() - 8) + " weitere"));
                break;
            }
            player.sendSystemMessage(Component.literal("  §7• §b" + formatBiomeName(biome)));
            shown++;
        }

        // Show player coordinates and facing direction
        player.sendSystemMessage(Component.literal(""));
        String direction = getCardinalDirection(player.getYRot());
        player.sendSystemMessage(Component.literal("§7Position: §f" +
                (int) player.getX() + ", " + (int) player.getY() + ", " + (int) player.getZ() +
                " §7| Richtung: §f" + direction));

        player.sendSystemMessage(Component.literal("§6§l══════════════════════════════"));

        return true;
    }

    private String formatBiomeName(String biomeName) {
        String[] parts = biomeName.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0)
                result.append(" ");
            result.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return result.toString();
    }

    private String getCardinalDirection(float yaw) {
        // Normalize yaw to 0-360
        float normalizedYaw = ((yaw % 360) + 360) % 360;

        if (normalizedYaw >= 315 || normalizedYaw < 45)
            return "Süden";
        if (normalizedYaw >= 45 && normalizedYaw < 135)
            return "Westen";
        if (normalizedYaw >= 135 && normalizedYaw < 225)
            return "Norden";
        return "Osten";
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Zeigt unabgeschlossene Felder"),
                Component.literal("§7und Biome in der Nähe"));
    }
}
