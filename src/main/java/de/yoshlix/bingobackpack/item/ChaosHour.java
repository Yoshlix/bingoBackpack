package de.yoshlix.bingobackpack.item;

import de.yoshlix.bingobackpack.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.Random;

/**
 * Chaos-Stunde - once per match, at a random point, all mob-drop chances
 * double for a while. Purely automatic and server-wide; no item triggers it,
 * so it can't be hoarded by whoever finds it first.
 */
public final class ChaosHour {

    private ChaosHour() {
    }

    private static final Random RANDOM = new Random();

    private static volatile long triggerAtMillis = 0; // 0 = not scheduled
    private static volatile long endAtMillis = 0; // 0 = not currently active

    /** Call once when a bingo round starts, to schedule this round's Chaos-Stunde. */
    public static void onRoundStarted() {
        reset();
        if (!ModConfig.getInstance().chaosHourEnabled) {
            return;
        }
        int earliest = ModConfig.getInstance().chaosHourEarliestMinute;
        int latest = Math.max(earliest, ModConfig.getInstance().chaosHourLatestMinute);
        int span = latest - earliest;
        int delayMinutes = earliest + (span > 0 ? RANDOM.nextInt(span + 1) : 0);
        triggerAtMillis = System.currentTimeMillis() + delayMinutes * 60_000L;
    }

    /** Clear all state — call on round end/reset so nothing bleeds into the next match. */
    public static void reset() {
        triggerAtMillis = 0;
        endAtMillis = 0;
    }

    public static boolean isActive() {
        return endAtMillis > System.currentTimeMillis();
    }

    /** The drop-chance multiplier to apply right now — 1.0 outside the Chaos-Stunde window. */
    public static double dropMultiplier() {
        return isActive() ? ModConfig.getInstance().chaosHourDropMultiplier : 1.0;
    }

    /** Call every server tick to fire/expire the window at the right time. */
    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();

        if (endAtMillis > 0 && now >= endAtMillis) {
            endAtMillis = 0;
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§7§lDie Chaos-Stunde ist vorbei — Drop-Chancen sind wieder normal."),
                    false);
            return;
        }

        if (triggerAtMillis > 0 && now >= triggerAtMillis) {
            triggerAtMillis = 0;
            endAtMillis = now + ModConfig.getInstance().chaosHourDurationMinutes * 60_000L;
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§d§l⚡⚡⚡ CHAOS-STUNDE! ⚡⚡⚡ §r§eAlle Item-Drop-Chancen sind für "
                            + ModConfig.getInstance().chaosHourDurationMinutes + " Minuten verdoppelt!"),
                    false);
        }
    }
}
