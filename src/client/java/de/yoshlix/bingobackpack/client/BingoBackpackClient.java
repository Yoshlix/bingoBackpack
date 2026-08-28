package de.yoshlix.bingobackpack.client;

import de.yoshlix.bingobackpack.net.PcPrankPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client entrypoint. Receives PC-prank packets from the server and hands them
 * to {@link PcPrankHandler} on the client thread. Also reverts a flipped
 * monitor if the player disconnects while it is still upside down.
 */
public class BingoBackpackClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PcPrankPayload.TYPE, (payload, context) ->
                context.client().execute(() -> PcPrankHandler.handle(payload)));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                PcPrankHandler.revertMonitorIfActive());
    }
}
