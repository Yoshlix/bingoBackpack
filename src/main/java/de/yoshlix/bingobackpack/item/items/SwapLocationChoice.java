package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Allows the player to choose a player to swap locations with.
 */
public class SwapLocationChoice extends BingoItem {

    private static final Map<UUID, List<ServerPlayer>> pendingSwaps = new HashMap<>();

    @Override
    public String getId() {
        return "swap_location_choice";
    }

    @Override
    public String getName() {
        return "Gezielter Positions-Tausch";
    }

    @Override
    public String getDescription() {
        return "Wähle einen Spieler für den Positions-Tausch.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
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

        // Find all online enemy players (excluding shielded ones)
        var server = ((ServerLevel) player.level()).getServer();
        var enemyPlayers = new ArrayList<ServerPlayer>();

        for (var team : teams) {
            if (team.getId().equals(playerTeam.getId()))
                continue;

            // Skip if the entire team is shielded
            if (TeamShield.isTeamShielded(team.getId())) {
                continue;
            }

            for (UUID memberId : team.getPlayers()) {
                ServerPlayer enemy = server.getPlayerList().getPlayer(memberId);
                if (enemy != null && !TeamShield.isPlayerShielded(memberId)) {
                    enemyPlayers.add(enemy);
                }
            }
        }

        if (enemyPlayers.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Keine gegnerischen Spieler online! (Oder alle geschützt)"));
            return false;
        }

        // Store pending swap
        pendingSwaps.put(player.getUUID(), enemyPlayers);

        // Show selection menu
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l═══════ Wähle einen Spieler ═══════"));
        player.sendSystemMessage(Component.literal(""));

        int index = 1;
        for (var enemy : enemyPlayers) {
            String biome = ((ServerLevel) enemy.level()).getBiome(enemy.blockPosition())
                    .unwrapKey().map(k -> k.identifier().getPath()).orElse("unknown");

            Component message = Component.literal("  §e[" + index + "] ")
                    .append(Component.literal(enemy.getName().getString()).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.RED)
                            .withClickEvent(new ClickEvent.RunCommand("/bingobackpack swap " + index))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("§7Biom: " + formatBiomeName(biome) +
                                            "\n§aKlicke zum Tauschen")))));

            player.sendSystemMessage(message);
            index++;
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Klicke oder schreibe §f/bingobackpack swap <nummer>"));
        player.sendSystemMessage(Component.literal("§6§l══════════════════════════════"));

        return false;
    }

    public static boolean processSwap(ServerPlayer player, String selection) {
        List<ServerPlayer> enemies = pendingSwaps.remove(player.getUUID());
        if (enemies == null) {
            player.sendSystemMessage(Component.literal("§cKeine ausstehende Auswahl!"));
            return false;
        }

        int index;
        try {
            index = Integer.parseInt(selection) - 1;
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        if (index < 0 || index >= enemies.size()) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        ServerPlayer target = enemies.get(index);

        if (!target.isAlive() || target.isRemoved()) {
            player.sendSystemMessage(Component.literal("§cSpieler nicht mehr verfügbar!"));
            return false;
        }

        // Store positions
        ServerLevel playerLevel = (ServerLevel) player.level();
        ServerLevel targetLevel = (ServerLevel) target.level();

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        float playerYaw = player.getYRot();
        float playerPitch = player.getXRot();

        double targetX = target.getX();
        double targetY = target.getY();
        double targetZ = target.getZ();
        float targetYaw = target.getYRot();
        float targetPitch = target.getXRot();

        // Swap positions
        player.teleportTo(targetLevel, targetX, targetY, targetZ,
                java.util.Set.of(), targetYaw, targetPitch, true);
        target.teleportTo(playerLevel, playerX, playerY, playerZ,
                java.util.Set.of(), playerYaw, playerPitch, true);

        player.sendSystemMessage(Component.literal("§a§lSWAP! §rDu hast mit §e" +
                target.getName().getString() + " §rgetauscht!"));
        target.sendSystemMessage(Component.literal("§c§lSWAP! §r" + player.getName().getString() +
                " §rhat dich teleportiert!"));

        ((ServerLevel) player.level()).getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§6✦ §e" + player.getName().getString() + " §6und §e" +
                        target.getName().getString() + " §6haben die Positionen getauscht!"),
                false);

        consumeItem(player);
        return true;
    }

    private static void consumeItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            var itemOpt = de.yoshlix.bingobackpack.item.BingoItemRegistry.fromItemStack(stack);
            if (itemOpt.isPresent() && itemOpt.get().getId().equals("swap_location_choice")) {
                stack.shrink(1);
                return;
            }
        }
    }

    public static boolean hasPendingSwap(UUID playerId) {
        return pendingSwaps.containsKey(playerId);
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
                Component.literal("§7Du wählst das Ziel!"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
