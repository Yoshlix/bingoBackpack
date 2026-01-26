package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.*;

/**
 * Paranoia - Jumpscares an enemy player with random creepy sounds for 60
 * seconds.
 */
public class Paranoia extends BingoItem {

    // Track active paranoia effects
    private static final Map<UUID, Long> activeParanoias = new HashMap<>();
    private static final int DURATION_SECONDS = 60;
    private static final int SOUND_INTERVAL_TICKS_MIN = 20; // 1 second
    private static final int SOUND_INTERVAL_TICKS_MAX = 100; // 5 seconds

    // Track next sound time per player
    private static final Map<UUID, Long> nextSoundTimes = new HashMap<>();

    @Override
    public String getId() {
        return "paranoia";
    }

    @Override
    public String getName() {
        return "Paranoia";
    }

    @Override
    public String getDescription() {
        return "Gibt einem Gegner für 60s Paranoia (Jumpscare Sounds!)";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        // Find enemy players
        List<ServerPlayer> targets = getTargetableEnemies(player);

        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKeine angreifbaren Gegner gefunden!"));
            return false;
        }

        // Pick random target
        Random random = new Random();
        ServerPlayer target = targets.get(random.nextInt(targets.size()));

        // Check if already active
        if (isActive(target.getUUID())) {
            player.sendSystemMessage(Component.literal("§cDieser Spieler leidet bereits unter Paranoia!"));
            return false;
        }

        // Activate
        long endTime = System.currentTimeMillis() + (DURATION_SECONDS * 1000L);
        activeParanoias.put(target.getUUID(), endTime);
        nextSoundTimes.put(target.getUUID(), System.currentTimeMillis());

        // Notify user
        player.sendSystemMessage(
                Component.literal("§5§l☠ Paranoia aktiviert auf §d" + target.getName().getString() + "§5!"));

        // Target gets a subtle hint (or maybe not?)
        // target.sendSystemMessage(Component.literal("§8Du fühlst dich
        // beobachtet..."));

        return true;
    }

    /**
     * Called every tick to play sounds.
     */
    public static void tickParanoiaEffects(net.minecraft.server.MinecraftServer server) {
        if (activeParanoias.isEmpty())
            return;

        long currentTime = System.currentTimeMillis();
        var expiredPlayers = new ArrayList<UUID>();
        Random random = new Random();

        for (var entry : activeParanoias.entrySet()) {
            UUID playerId = entry.getKey();
            long endTime = entry.getValue();

            // Check if expired
            if (currentTime >= endTime) {
                expiredPlayers.add(playerId);
                continue;
            }

            // Check if time for next sound
            Long nextSound = nextSoundTimes.get(playerId);
            if (nextSound == null || currentTime >= nextSound) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    playRandomCreepySound(player, random);
                }

                // Schedule next sound
                long delay = (SOUND_INTERVAL_TICKS_MIN
                        + random.nextInt(SOUND_INTERVAL_TICKS_MAX - SOUND_INTERVAL_TICKS_MIN)) * 50L;
                nextSoundTimes.put(playerId, currentTime + delay);
            }
        }

        // Cleanup
        for (UUID uuid : expiredPlayers) {
            activeParanoias.remove(uuid);
            nextSoundTimes.remove(uuid);
        }
    }

    private static void playRandomCreepySound(ServerPlayer player, Random random) {
        if (player == null || player.connection == null) {
            return;
        }

        // List of creepy sounds - using only safe, valid sound events
        // Removed potentially problematic sounds and ensured all are valid SoundEvent
        // instances
        List<net.minecraft.sounds.SoundEvent> sounds = Arrays.asList(
                new net.minecraft.sounds.SoundEvent[] {
                        SoundEvents.CREEPER_PRIMED,
                        SoundEvents.TNT_PRIMED,
                        SoundEvents.ENDERMAN_SCREAM,
                        SoundEvents.PHANTOM_SWOOP,
                        SoundEvents.WARDEN_HEARTBEAT,
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundEvents.WITCH_CELEBRATE,
                        SoundEvents.GHAST_SCREAM,
                        SoundEvents.WITHER_SPAWN,
                        SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundEvents.SCULK_SHRIEKER_SHRIEK,
                        SoundEvents.ELDER_GUARDIAN_CURSE
                });
        if (sounds.isEmpty()) {
            return;
        }

        net.minecraft.sounds.SoundEvent soundEvent = sounds.get(random.nextInt(sounds.size()));

        // Validate sound event is not null
        if (soundEvent == null) {
            return;
        }

        float volume = 0.5f + random.nextFloat() * 0.5f;
        float pitch = 0.8f + random.nextFloat() * 0.4f;

        // Play sound ONLY to that player (creating a packet)
        // We use player's position but randomize it slightly to sound directional
        double dx = (random.nextDouble() - 0.5) * 6.0;
        double dy = (random.nextDouble() - 0.5) * 2.0;
        double dz = (random.nextDouble() - 0.5) * 6.0;

        try {
            player.connection.send(new ClientboundSoundPacket(
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent),
                    SoundSource.MASTER,
                    player.getX() + dx,
                    player.getY() + dy,
                    player.getZ() + dz,
                    volume,
                    pitch,
                    random.nextLong()));
        } catch (Exception e) {
            // Silently fail if sound cannot be played (e.g., player disconnected)
            // Log only in debug mode to avoid spam
        }
    }

    private boolean isActive(UUID playerId) {
        Long endTime = activeParanoias.get(playerId);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    private List<ServerPlayer> getTargetableEnemies(ServerPlayer player) {
        var teams = me.jfenn.bingo.api.BingoApi.getTeams();
        if (teams == null)
            return Collections.emptyList();

        var playerTeam = teams.getTeamForPlayer(player.getUUID());
        if (playerTeam == null)
            return Collections.emptyList();

        var server = player.level().getServer();
        if (server == null)
            return Collections.emptyList();

        List<ServerPlayer> enemies = new ArrayList<>();

        for (var team : teams) {
            // Skip own team
            if (team.getId().equals(playerTeam.getId()))
                continue;

            // Skip shielded teams
            if (TeamShield.isTeamShielded(team.getId()))
                continue;

            for (UUID memberId : team.getPlayers()) {
                // Skip shielded players
                if (TeamShield.isPlayerShielded(memberId))
                    continue;

                ServerPlayer enemy = server.getPlayerList().getPlayer(memberId);
                if (enemy != null) {
                    enemies.add(enemy);
                }
            }
        }
        return enemies;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Spielt gruselige Sounds"),
                Component.literal("§7nur für den Gegner hörbar!"),
                Component.literal("§7Dauer: 60 Sekunden"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
