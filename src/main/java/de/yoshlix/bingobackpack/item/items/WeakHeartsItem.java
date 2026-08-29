package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Caps a random enemy's max health at a few hearts for a couple of minutes.
 * Purely in-game (a temporary MAX_HEALTH modifier), so it is not gated by the
 * pcPranksEnabled toggle and needs no client mod.
 */
public class WeakHeartsItem extends BingoItem {

    private static final Identifier MODIFIER_ID = Identifier.parse("bingobackpack:weak_hearts");

    private record ActiveDebuff(int hearts, long endTimeMillis) {
    }

    // Target UUID -> active debuff state.
    private static final Map<UUID, ActiveDebuff> affected = new HashMap<>();

    @Override
    public String getId() {
        return "weak_hearts";
    }

    @Override
    public String getName() {
        return "Glasherz";
    }

    @Override
    public String getDescription() {
        return "Senkt die Herzen eines zufälligen Gegners für 2 Minuten auf 3.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var playerTeam = requireTeam(player);
        if (playerTeam == null) {
            return false;
        }

        var enemies = onlineEnemies(player, playerTeam);
        if (enemies.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("§6Keine gegnerischen Spieler online! (Oder alle geschützt)"));
            return false;
        }

        ServerPlayer target = enemies.get(RANDOM.nextInt(enemies.size()));
        int hearts = Math.max(1, ModConfig.getInstance().weakHeartsCount);
        int seconds = ModConfig.getInstance().weakHeartsDurationSeconds;
        applyWeakHearts(target, hearts, seconds);

        player.sendSystemMessage(Component.literal("§c§l💔 §r§7" + target.getName().getString()
                + " §7hat jetzt nur noch §c" + hearts + " Herzen§7!"));

        ((ServerLevel) player.level()).getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§c§l💔 §e" + player.getName().getString()
                        + " §7hat §e" + target.getName().getString()
                        + " §7auf " + hearts + " Herzen geschwächt!"),
                false);

        return true;
    }

    private static void applyWeakHearts(ServerPlayer target, int hearts, int seconds) {
        AttributeInstance attr = target.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        // Remove any previous instance first so re-application measures the real base.
        attr.removeModifier(MODIFIER_ID);

        double cap = hearts * 2.0;
        double delta = cap - attr.getValue();
        // A transient modifier is not saved to disk, so a crash/restart can't leave
        // the debuff stuck on the player.
        attr.addTransientModifier(new AttributeModifier(MODIFIER_ID, delta, AttributeModifier.Operation.ADD_VALUE));

        if (target.getHealth() > (float) cap) {
            target.setHealth((float) cap);
        }

        affected.put(target.getUUID(), new ActiveDebuff(hearts, System.currentTimeMillis() + seconds * 1000L));
    }

    /**
     * Re-adds the modifier if it's missing from the current attribute instance.
     * Being transient means it lives on the entity object, not on disk — a
     * disconnect/reconnect within the debuff window hands the player a fresh
     * entity without it, which would otherwise lift the cap early for free.
     */
    private static void reapplyIfMissing(ServerPlayer target, int hearts) {
        AttributeInstance attr = target.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null || attr.hasModifier(MODIFIER_ID)) {
            return;
        }
        double cap = hearts * 2.0;
        double delta = cap - attr.getValue();
        attr.addTransientModifier(new AttributeModifier(MODIFIER_ID, delta, AttributeModifier.Operation.ADD_VALUE));
        if (target.getHealth() > (float) cap) {
            target.setHealth((float) cap);
        }
    }

    private static void restore(ServerPlayer target) {
        if (target == null) {
            return;
        }
        AttributeInstance attr = target.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.removeModifier(MODIFIER_ID);
        }
    }

    /** Lift the debuff from anyone whose timer has run out. Call each server tick. */
    public static void tickExpiry(MinecraftServer server) {
        if (affected.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        affected.entrySet().removeIf(entry -> {
            ActiveDebuff debuff = entry.getValue();
            if (now < debuff.endTimeMillis()) {
                ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
                if (target != null) {
                    reapplyIfMissing(target, debuff.hearts());
                }
                return false;
            }
            restore(server.getPlayerList().getPlayer(entry.getKey()));
            return true;
        });
    }

    /** Clear all debuffs (e.g. on a new round). */
    public static void clearAll(MinecraftServer server) {
        for (UUID id : affected.keySet()) {
            restore(server.getPlayerList().getPlayer(id));
        }
        affected.clear();
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§cZufälliger gegnerischer Spieler"),
                Component.literal("§7Wirkt sofort, hält 2 Minuten."));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
