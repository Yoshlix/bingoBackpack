package de.yoshlix.bingobackpack.bounty;

import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The full bounty catalog, 3 entries per rarity tier. Registration mirrors
 * {@link de.yoshlix.bingobackpack.item.BingoItemRegistry}: a static list
 * built once and read-only afterwards.
 */
public final class BountyRegistry {

    private static final List<BountyDefinition> BOUNTIES = List.of(
            // COMMON — reward: 1 COMMON item to the completing player
            BountyDefinition.kill("zombie_plague", "Zombie-Plage",
                    "Töte 20x Zombie", ItemRarity.COMMON, EntityTypes.ZOMBIE, 20),
            BountyDefinition.location("hell_trip", "Höllentrip",
                    "Betrete den Nether", ItemRarity.COMMON,
                    player -> player.level().dimension().equals(Level.NETHER)),
            BountyDefinition.collect("collect_coal", "Kohle sammeln",
                    "Besitze 16x Kohle", ItemRarity.COMMON, Items.COAL, 16),

            // UNCOMMON — reward: 1 UNCOMMON item to the completing player
            BountyDefinition.kill("enderhunter", "Enderjäger",
                    "Töte 3x Enderman", ItemRarity.UNCOMMON, EntityTypes.ENDERMAN, 3),
            BountyDefinition.location("into_the_unknown", "Ins Unbekannte",
                    "Betrete das End", ItemRarity.UNCOMMON,
                    player -> player.level().dimension().equals(Level.END)),
            BountyDefinition.collect("collect_iron", "Eisenvorrat",
                    "Besitze 24x Eisenbarren", ItemRarity.UNCOMMON, Items.IRON_INGOT, 24),

            // RARE — reward: 1 RARE item to the whole team
            BountyDefinition.kill("skeleton_commander", "Skelett-Kommandant",
                    "Töte 5x Wither Skeleton", ItemRarity.RARE, EntityTypes.WITHER_SKELETON, 5),
            BountyDefinition.location("into_the_depths", "In die Tiefe",
                    "Erreiche Y ≤ -50", ItemRarity.RARE,
                    player -> player.getY() <= -50),
            BountyDefinition.collect("collect_diamond", "Diamantenschatz",
                    "Besitze 8x Diamant", ItemRarity.RARE, Items.DIAMOND, 8),

            // EPIC — reward: 1 EPIC item to the whole team
            BountyDefinition.kill("ravager_slayer", "Verwüster bezwingen",
                    "Töte 1x Ravager", ItemRarity.EPIC, EntityTypes.RAVAGER, 1),
            BountyDefinition.location("far_traveled", "Weit gereist",
                    "Entferne dich ≥5000 Blöcke von (0,0)", ItemRarity.EPIC,
                    player -> (player.getX() * player.getX() + player.getZ() * player.getZ()) >= 5000.0 * 5000.0),
            BountyDefinition.collect("collect_netherite_scrap", "Netherit-Suche",
                    "Besitze 4x Netherite Scrap", ItemRarity.EPIC, Items.NETHERITE_SCRAP, 4),

            // LEGENDARY — reward: 1 LEGENDARY item to the whole team
            BountyDefinition.kill("wither_slayer", "Wither bezwingen",
                    "Töte 1x Wither", ItemRarity.LEGENDARY, EntityTypes.WITHER, 1),
            BountyDefinition.kill("dragon_slayer", "Drachentöter",
                    "Töte 1x Ender Dragon", ItemRarity.LEGENDARY, EntityTypes.ENDER_DRAGON, 1),
            BountyDefinition.kill("warden_slayer", "Wächter bezwingen",
                    "Töte 1x Warden", ItemRarity.LEGENDARY, EntityTypes.WARDEN, 1));

    private BountyRegistry() {
    }

    public static List<BountyDefinition> getAll() {
        return BOUNTIES;
    }

    public static List<BountyDefinition> getByTier(ItemRarity tier) {
        return BOUNTIES.stream().filter(b -> b.getTier() == tier).toList();
    }

    public static java.util.Optional<BountyDefinition> getById(String id) {
        return BOUNTIES.stream().filter(b -> b.getId().equals(id)).findFirst();
    }
}
