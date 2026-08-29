package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Einarmiger Bandit - three reels, three symbols. Two matching gives a small
 * buff, three matching gives a real prize; anything else and the item was
 * just spent.
 */
public class SlotMachine extends BingoItem {

    private static final String[] SYMBOLS = {"§c🍒", "§e⭐", "§b🔔", "§d💎", "§67️⃣"};

    @Override
    public String getId() {
        return "slot_machine";
    }

    @Override
    public String getName() {
        return "Einarmiger Bandit";
    }

    @Override
    public String getDescription() {
        return "Zieh am Hebel! Zwei Symbole gleich = kleiner Gewinn, drei gleich = Jackpot.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        int a = RANDOM.nextInt(SYMBOLS.length);
        int b = RANDOM.nextInt(SYMBOLS.length);
        int c = RANDOM.nextInt(SYMBOLS.length);

        player.sendSystemMessage(Component.literal(
                "§7[ " + SYMBOLS[a] + " §7| " + SYMBOLS[b] + " §7| " + SYMBOLS[c] + " §7]"));

        boolean allMatch = a == b && b == c;
        boolean twoMatch = a == b || b == c || a == c;

        if (allMatch) {
            var pool = BingoItemRegistry.getItemsByRarity(ItemRarity.UNCOMMON);
            if (!pool.isEmpty()) {
                var prize = pool.get(RANDOM.nextInt(pool.size()));
                player.getInventory().add(prize.createItemStack());
            }
            player.sendSystemMessage(Component.literal("§6§l✦ JACKPOT! ✦"));
        } else if (twoMatch) {
            int ticks = ModConfig.getInstance().slotMachineBuffDurationSeconds * 20;
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks, 0, false, true, true));
            player.sendSystemMessage(Component.literal("§a2 gleich! Kleiner Gewinn."));
        } else {
            player.sendSystemMessage(Component.literal("§8Nichts. Pech gehabt."));
        }

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§72 gleich: kurzer Speed/Haste-Boost"),
                Component.literal("§73 gleich: garantiertes Uncommon-Item"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
