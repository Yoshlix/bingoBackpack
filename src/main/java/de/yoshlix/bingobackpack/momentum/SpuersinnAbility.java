package de.yoshlix.bingobackpack.momentum;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.items.StructureFinder;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bonus ability: repeats {@link StructureFinder#locateNearestStructure} for
 * every online team member every 60 seconds, for the ability's duration —
 * an automatic, ongoing version of the Struktur-Kompass item.
 */
public class SpuersinnAbility implements MomentumAbility {

    private static final long PING_INTERVAL_MS = 60_000L;

    private final Map<String, Long> expiryMillis = new HashMap<>();
    private final Map<String, Long> nextPingMillis = new HashMap<>();

    @Override
    public String getId() {
        return "spuersinn";
    }

    @Override
    public String getName() {
        return "Spürsinn";
    }

    @Override
    public String getDescription() {
        return "Bonus: Findet automatisch alle 60 Sekunden die nächste Struktur für jedes Teammitglied, "
                + ModConfig.getInstance().momentumSpuersinnDurationSeconds + " Sekunden lang.";
    }

    @Override
    public boolean isHarmful() {
        return false;
    }

    @Override
    public void apply(MinecraftServer server, ServerPlayer activator, IBingoTeam team) {
        long now = System.currentTimeMillis();
        long duration = ModConfig.getInstance().momentumSpuersinnDurationSeconds * 1000L;
        expiryMillis.put(team.getId(), now + duration);
        nextPingMillis.put(team.getId(), now);

        for (UUID memberId : team.getPlayers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                member.sendSystemMessage(Component.literal("§a§lSPÜRSINN AKTIV! §rIhr spürt nahe Strukturen auf."));
            }
        }
    }

    @Override
    public void tick(MinecraftServer server) {
        if (expiryMillis.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();

        for (var entry : expiryMillis.entrySet()) {
            String teamId = entry.getKey();
            if (now >= entry.getValue()) {
                expired.add(teamId);
                continue;
            }
            long nextPing = nextPingMillis.getOrDefault(teamId, 0L);
            if (now >= nextPing) {
                nextPingMillis.put(teamId, now + PING_INTERVAL_MS);
                pingTeam(server, teamId);
            }
        }

        for (String teamId : expired) {
            expiryMillis.remove(teamId);
            nextPingMillis.remove(teamId);
        }
    }

    private void pingTeam(MinecraftServer server, String teamId) {
        IBingoTeam team = BingoBridge.getTeamById(teamId);
        if (team == null) {
            return;
        }
        for (UUID memberId : team.getPlayers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                StructureFinder.locateNearestStructure(member);
            }
        }
    }
}
