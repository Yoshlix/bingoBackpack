package de.yoshlix.bingobackpack.momentum;

import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.items.TeamShield;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Harmful ability: deletes exactly one random item from a random reachable
 * enemy's inventory. A single-item variant of
 * {@link de.yoshlix.bingobackpack.item.items.DeleteEnemyItems}, whose
 * enemy-selection and unbreakable-item handling it mirrors.
 */
public class KonfiszierungAbility implements MomentumAbility {

    private final Random random = new Random();

    @Override
    public String getId() {
        return "konfiszierung";
    }

    @Override
    public String getName() {
        return "Konfiszierung";
    }

    @Override
    public String getDescription() {
        return "Schaden: Zerstört ein zufälliges Item eines zufälligen Gegners.";
    }

    @Override
    public boolean isHarmful() {
        return true;
    }

    @Override
    public void apply(MinecraftServer server, ServerPlayer activator, IBingoTeam team) {
        List<ServerPlayer> enemyPlayers = new ArrayList<>();
        for (IBingoTeam candidate : BingoBridge.getEnemyTeams(team.getId())) {
            if (TeamShield.isTeamShielded(candidate.getId())) {
                continue;
            }
            for (UUID memberId : candidate.getPlayers()) {
                ServerPlayer enemy = server.getPlayerList().getPlayer(memberId);
                if (enemy != null && !TeamShield.isPlayerShielded(memberId)) {
                    enemyPlayers.add(enemy);
                }
            }
        }

        if (enemyPlayers.isEmpty()) {
            activator.sendSystemMessage(Component.literal("§6Kein Gegner erreichbar (oder alle geschützt)!"));
            return;
        }

        ServerPlayer target = enemyPlayers.get(random.nextInt(enemyPlayers.size()));

        List<Integer> nonEmptySlots = new ArrayList<>();
        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            ItemStack stack = target.getInventory().getItem(i);
            if (!stack.isEmpty() && !stack.has(DataComponents.UNBREAKABLE)) {
                nonEmptySlots.add(i);
            }
        }

        if (nonEmptySlots.isEmpty()) {
            activator.sendSystemMessage(Component.literal("§6Gegner hat keine Items!"));
            return;
        }

        int slot = nonEmptySlots.get(random.nextInt(nonEmptySlots.size()));
        String deletedName = target.getInventory().getItem(slot).getHoverName().getString();
        target.getInventory().setItem(slot, ItemStack.EMPTY);

        activator.sendSystemMessage(Component.literal("§c§l✘ §r" + deletedName + " §rvon §e"
                + target.getName().getString() + " §rkonfisziert!"));
        target.sendSystemMessage(Component.literal("§4§l✘ §c" + activator.getName().getString()
                + " §chat dein §e" + deletedName + " §rkonfisziert!"));

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§4✘ §e" + activator.getName().getString() + " §chat §e" + deletedName
                        + " §cvon §e" + target.getName().getString() + " §ckonfisziert!"),
                false);
    }
}
