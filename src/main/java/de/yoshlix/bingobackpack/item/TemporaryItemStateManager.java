package de.yoshlix.bingobackpack.item;

import de.yoshlix.bingobackpack.banish.BanishManager;
import de.yoshlix.bingobackpack.item.items.BiomeTeleportChoice;
import de.yoshlix.bingobackpack.item.items.CompleteChosenBingoField;
import de.yoshlix.bingobackpack.item.items.Flight15Min;
import de.yoshlix.bingobackpack.item.items.Flight1Min;
import de.yoshlix.bingobackpack.item.items.Flight5Min;
import de.yoshlix.bingobackpack.item.items.Lockdown;
import de.yoshlix.bingobackpack.item.items.MobPheromone;
import de.yoshlix.bingobackpack.item.items.Paranoia;
import de.yoshlix.bingobackpack.item.items.RerollChosenField;
import de.yoshlix.bingobackpack.item.items.ResetFieldProgress;
import de.yoshlix.bingobackpack.item.items.SwapLocationChoice;
import de.yoshlix.bingobackpack.item.items.TeamShield;
import de.yoshlix.bingobackpack.item.items.TimeoutPlayer;
import de.yoshlix.bingobackpack.item.items.Wildcard;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Abilities;

public class TemporaryItemStateManager {

    private TemporaryItemStateManager() {
    }

    public static void clearForRoundReset(MinecraftServer server) {
        clearGlobalState();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            clearPlayerState(player);
            BanishManager.getInstance().clearBanish(player);
        }
    }

    private static void clearGlobalState() {
        TeamShield.clearAllShields();
        MobPheromone.clearAllPheromones();
        Paranoia.clearAllParanoias();
        Lockdown.clearAllLockdowns();
        TimeoutPlayer.clearAllTimeouts();
        Flight1Min.clearAllFlightTimes();
        Flight5Min.clearAllFlightTimes();
        Flight15Min.clearAllFlightTimes();
        BiomeTeleportChoice.clearPendingSelections();
        CompleteChosenBingoField.clearPendingSelections();
        RerollChosenField.clearPendingRerolls();
        ResetFieldProgress.clearPendingResets();
        SwapLocationChoice.clearPendingSwaps();
        Wildcard.clearPendingSelections();
    }

    private static void clearPlayerState(ServerPlayer player) {
        Flight1Min.clearFlightTime(player.getUUID());
        Flight5Min.clearFlightTime(player.getUUID());
        Flight15Min.clearFlightTime(player.getUUID());

        if (!player.isCreative() && !player.isSpectator()) {
            Abilities abilities = player.getAbilities();
            if (abilities.mayfly) {
                abilities.mayfly = false;
                abilities.flying = false;
                player.onUpdateAbilities();
            }
        }

        player.removeEffect(MobEffects.SPEED);
        player.removeEffect(MobEffects.HASTE);
        player.removeEffect(MobEffects.SLOWNESS);
        player.removeEffect(MobEffects.MINING_FATIGUE);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.JUMP_BOOST);
        player.sendSystemMessage(Component.literal("§7Temporäre Backpack-Item-Effekte wurden zurückgesetzt."));
    }
}
