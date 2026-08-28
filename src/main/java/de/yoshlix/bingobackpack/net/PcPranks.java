package de.yoshlix.bingobackpack.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side helper for the PC-prank networking.
 *
 * All of this is common/server API — nothing here touches client classes, so it
 * loads fine on a dedicated server. The client half lives in the client source
 * set (BingoBackpackClient / PcPrankHandler).
 */
public final class PcPranks {

    private PcPranks() {
    }

    /**
     * Register the payload type. Must run on both sides, so it is called from the
     * common mod initializer.
     */
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(PcPrankPayload.TYPE, PcPrankPayload.CODEC);
    }

    /** Whether this player's client has the mod and can receive prank packets. */
    public static boolean canReach(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, PcPrankPayload.TYPE);
    }

    /** Send a prank to a reachable client. No-op (returns false) if unreachable. */
    public static boolean send(ServerPlayer target, String action, String arg, int durationSeconds) {
        if (!canReach(target)) {
            return false;
        }
        ServerPlayNetworking.send(target, new PcPrankPayload(action, arg, durationSeconds));
        return true;
    }
}
