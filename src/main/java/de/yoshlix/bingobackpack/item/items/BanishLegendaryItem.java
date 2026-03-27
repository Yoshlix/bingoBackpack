package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.TeamManager;
import de.yoshlix.bingobackpack.banish.BanishManager;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BanishLegendaryItem extends BingoItem {
    @Override
    public String getId() {
        return "banish_legendary";
    }

    @Override
    public String getName() {
        return "Banish (Legendary)";
    }

    @Override
    public String getDescription() {
        return "Verbannt alle Gegner ins End.\nSie müssen eine Aufgabe lösen, um zurückzukehren.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        List<ServerPlayer> enemies = new ArrayList<>();
        String myTeam = TeamManager.getInstance().getPlayerTeam(player.getUUID());
        
        for (ServerPlayer p : player.level().getServer().getPlayerList().getPlayers()) {
            if (p.getUUID().equals(player.getUUID())) continue;
            
            String theirTeam = TeamManager.getInstance().getPlayerTeam(p.getUUID());
            if (myTeam == null || theirTeam == null || !myTeam.equals(theirTeam)) {
                enemies.add(p);
            }
        }
        
        if (enemies.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cEs gibt keine Gegner, die verbannt werden können."));
            return false;
        }
        
        for (ServerPlayer target : enemies) {
            BanishManager.getInstance().banish(target);
        }
        
        player.level().getServer().getPlayerList().broadcastSystemMessage(Component.literal("§d§l" + player.getName().getString() + " §ehat das gesamte gegnerische Team verbannt!"), false);
        
        consumeItem(player);
        return true;
    }

    private void consumeItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            var itemOpt = BingoItemRegistry.fromItemStack(stack);
            if (itemOpt.isPresent() && itemOpt.get().getId().equals(getId())) {
                stack.shrink(1);
                return;
            }
        }
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
