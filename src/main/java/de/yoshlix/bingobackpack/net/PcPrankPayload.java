package de.yoshlix.bingobackpack.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client message telling one player's client to run a PC prank.
 *
 * The action is a fixed string constant; {@code arg} carries the URL for
 * {@link #ACTION_OPEN_URL} and is empty otherwise. {@code durationSeconds} is
 * how long a time-limited effect (the monitor flip) should last before the
 * client reverts it on its own.
 */
public record PcPrankPayload(String action, String arg, int durationSeconds) implements CustomPacketPayload {

    /** Show the dismissible fake "Windows is shutting down" overlay. */
    public static final String ACTION_SHUTDOWN_SCREEN = "shutdown_screen";
    /** Open {@code arg} (an http/https URL) in the default browser. */
    public static final String ACTION_OPEN_URL = "open_url";
    /** Rotate all displays 180° for {@code durationSeconds}, then revert. */
    public static final String ACTION_FLIP_MONITOR = "flip_monitor";

    public static final Type<PcPrankPayload> TYPE =
            new Type<>(Identifier.parse("bingobackpack:pc_prank"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PcPrankPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, PcPrankPayload::action,
                    ByteBufCodecs.STRING_UTF8, PcPrankPayload::arg,
                    ByteBufCodecs.VAR_INT, PcPrankPayload::durationSeconds,
                    PcPrankPayload::new);

    @Override
    public Type<PcPrankPayload> type() {
        return TYPE;
    }
}
