package de.yoshlix.bingobackpack.momentum;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.bingo.BingoBridge;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Per-team Momentum meter (0-100), charged by earned actions (kills,
 * objective/row completions, banish escapes — never by standing/rubber
 * banding). At 100% a random ability from
 * {@link MomentumAbilityRegistry} is drawn and held as "ready" — announced
 * immediately, not applied automatically — until any online team member
 * activates it.
 */
public class MomentumManager {
    private static MomentumManager instance;

    private final Random random = new Random();
    private MinecraftServer server;

    private final Map<String, Double> charge = new HashMap<>();
    private final Map<String, MomentumAbility> ready = new HashMap<>();

    public static MomentumManager getInstance() {
        if (instance == null) {
            instance = new MomentumManager();
        }
        return instance;
    }

    private MomentumManager() {
    }

    public void init(MinecraftServer server) {
        this.server = server;
    }

    public void reset() {
        charge.clear();
        ready.clear();
    }

    public void tick(MinecraftServer server) {
        this.server = server;
        MomentumAbilityRegistry.tickAll(server);
    }

    public double getCharge(String teamId) {
        return charge.getOrDefault(teamId, 0.0);
    }

    public MomentumAbility getReady(String teamId) {
        return ready.get(teamId);
    }

    /**
     * Adds earned charge for a team. Ignored once a team already has a ready
     * ability waiting, so charge can't stack up "for free" while unused.
     */
    public void addCharge(String teamId, double amount) {
        if (teamId == null || amount <= 0 || ready.containsKey(teamId)) {
            return;
        }

        double newValue = charge.merge(teamId, amount, Double::sum);
        if (newValue >= 100.0) {
            charge.put(teamId, 100.0);
            List<MomentumAbility> pool = MomentumAbilityRegistry.getAll();
            if (pool.isEmpty()) {
                return;
            }
            MomentumAbility chosen = pool.get(random.nextInt(pool.size()));
            ready.put(teamId, chosen);
            broadcastReady(teamId, chosen);
        }
    }

    private void broadcastReady(String teamId, MomentumAbility ability) {
        if (server == null) {
            return;
        }
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§d✦§l MOMENTUM BEREIT! §r§eTeam " + teamId + " §dkann jetzt §e"
                        + ability.getName() + " §deinsetzen! §7(" + ability.getDescription() + ")"),
                false);
    }

    /** Called from {@code MobDeathMixin} on every player kill. */
    public void onMobKilled(LivingEntity killed, Player killer) {
        if (killed instanceof Player) {
            return;
        }
        if (!(killer instanceof ServerPlayer serverPlayer)) {
            return;
        }
        IBingoTeam team = BingoBridge.getTeamForPlayer(serverPlayer.getUUID());
        if (team == null) {
            return;
        }
        addCharge(team.getId(), ModConfig.getInstance().momentumChargePerKill);
    }

    /** Any online member of a team with a ready ability can trigger it. */
    public boolean tryActivate(ServerPlayer player) {
        IBingoTeam team = BingoBridge.getTeamForPlayer(player.getUUID());
        if (team == null || server == null) {
            return false;
        }

        MomentumAbility ability = ready.remove(team.getId());
        if (ability == null) {
            return false;
        }
        charge.put(team.getId(), 0.0);

        ability.apply(server, player, team);
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§d✦ §eTeam " + team.getId() + " §dhat §e" + ability.getName()
                        + " §daktiviert!"),
                false);
        return true;
    }

    /** Admin/testing override — force a team's meter to 100% with a specific ability. */
    public boolean forceReady(String teamId, String abilityId) {
        var abilityOpt = MomentumAbilityRegistry.getById(abilityId);
        if (abilityOpt.isEmpty()) {
            return false;
        }
        charge.put(teamId, 100.0);
        ready.put(teamId, abilityOpt.get());
        broadcastReady(teamId, abilityOpt.get());
        return true;
    }
}
