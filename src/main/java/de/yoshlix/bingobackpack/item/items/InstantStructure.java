package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.BingoBackpack;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Instant Structure - Spawnt eine zufällige Vanilla-Struktur an deiner Position.
 * Kann Häuser, Türme, Hütten und andere nützliche Strukturen spawnen.
 */
public class InstantStructure extends BingoItem {

    private final Random random = new Random();

    // Liste von ALLEN Vanilla-Strukturen, die gespawnt werden können
    // MAXIMALES CHAOS! Alle Strukturen können gespawnt werden!
    private static final List<String> STRUCTURE_IDS = List.of(
            // ========== VILLAGE STRUCTURES ==========
            // Plains villages
            "village/plains/houses/plains_small_house_1", "village/plains/houses/plains_small_house_2",
            "village/plains/houses/plains_small_house_3", "village/plains/houses/plains_small_house_4",
            "village/plains/houses/plains_small_house_5", "village/plains/houses/plains_small_house_6",
            "village/plains/houses/plains_small_house_7", "village/plains/houses/plains_small_house_8",
            "village/plains/houses/plains_medium_house_1", "village/plains/houses/plains_medium_house_2",
            "village/plains/houses/plains_medium_house_3", "village/plains/houses/plains_medium_house_4",
            "village/plains/houses/plains_big_house_1", "village/plains/houses/plains_big_house_2",
            "village/plains/houses/plains_butcher_shop_1", "village/plains/houses/plains_butcher_shop_2",
            "village/plains/houses/plains_cartographer_house_1", "village/plains/houses/plains_fletcher_house_1",
            "village/plains/houses/plains_library_1", "village/plains/houses/plains_library_2",
            "village/plains/houses/plains_mason_house_1", "village/plains/houses/plains_shepherd_house_1",
            "village/plains/houses/plains_tannery_1", "village/plains/houses/plains_temple_1",
            "village/plains/houses/plains_temple_2", "village/plains/houses/plains_tool_smith_1",
            "village/plains/houses/plains_weaponsmith_1", "village/plains/houses/plains_armorer_house_1",
            "village/plains/houses/plains_stable_1", "village/plains/houses/plains_stable_2",
            "village/plains/houses/plains_fisher_cottage_1",
            // Desert villages
            "village/desert/houses/desert_small_house_1", "village/desert/houses/desert_small_house_2",
            "village/desert/houses/desert_small_house_3", "village/desert/houses/desert_small_house_4",
            "village/desert/houses/desert_small_house_5", "village/desert/houses/desert_small_house_6",
            "village/desert/houses/desert_small_house_7", "village/desert/houses/desert_small_house_8",
            "village/desert/houses/desert_medium_house_1", "village/desert/houses/desert_medium_house_2",
            "village/desert/houses/desert_medium_house_3", "village/desert/houses/desert_big_house_1",
            "village/desert/houses/desert_butcher_shop_1", "village/desert/houses/desert_cartographer_house_1",
            "village/desert/houses/desert_fletcher_house_1", "village/desert/houses/desert_mason_house_1",
            "village/desert/houses/desert_shepherd_house_1", "village/desert/houses/desert_tannery_1",
            "village/desert/houses/desert_temple_1", "village/desert/houses/desert_temple_2",
            "village/desert/houses/desert_tool_smith_1", "village/desert/houses/desert_weaponsmith_house_1",
            "village/desert/houses/desert_armorer_house_1",
            // Savanna villages
            "village/savanna/houses/savanna_small_house_1", "village/savanna/houses/savanna_small_house_2",
            "village/savanna/houses/savanna_small_house_3", "village/savanna/houses/savanna_small_house_4",
            "village/savanna/houses/savanna_small_house_5", "village/savanna/houses/savanna_small_house_6",
            "village/savanna/houses/savanna_small_house_7", "village/savanna/houses/savanna_small_house_8",
            "village/savanna/houses/savanna_medium_house_1", "village/savanna/houses/savanna_medium_house_2",
            "village/savanna/houses/savanna_big_house_1", "village/savanna/houses/savanna_butcher_shop_1",
            "village/savanna/houses/savanna_cartographer_house_1", "village/savanna/houses/savanna_fletcher_house_1",
            "village/savanna/houses/savanna_mason_house_1", "village/savanna/houses/savanna_shepherd_house_1",
            "village/savanna/houses/savanna_tannery_1", "village/savanna/houses/savanna_temple_1",
            "village/savanna/houses/savanna_temple_2", "village/savanna/houses/savanna_tool_smith_1",
            "village/savanna/houses/savanna_weaponsmith_house_1", "village/savanna/houses/savanna_armorer_house_1",
            "village/savanna/houses/savanna_fisher_cottage_1",
            // Taiga villages
            "village/taiga/houses/taiga_small_house_1", "village/taiga/houses/taiga_small_house_2",
            "village/taiga/houses/taiga_small_house_3", "village/taiga/houses/taiga_small_house_4",
            "village/taiga/houses/taiga_small_house_5", "village/taiga/houses/taiga_medium_house_1",
            "village/taiga/houses/taiga_medium_house_2", "village/taiga/houses/taiga_medium_house_3",
            "village/taiga/houses/taiga_big_house_1", "village/taiga/houses/taiga_butcher_shop_1",
            "village/taiga/houses/taiga_cartographer_house_1", "village/taiga/houses/taiga_fletcher_house_1",
            "village/taiga/houses/taiga_mason_house_1", "village/taiga/houses/taiga_shepherd_house_1",
            "village/taiga/houses/taiga_tannery_1", "village/taiga/houses/taiga_temple_1",
            "village/taiga/houses/taiga_tool_smith_1", "village/taiga/houses/taiga_weaponsmith_house_1",
            "village/taiga/houses/taiga_armorer_house_1", "village/taiga/houses/taiga_fisher_cottage_1",
            // Snowy villages
            "village/snowy/houses/snowy_small_house_1", "village/snowy/houses/snowy_small_house_2",
            "village/snowy/houses/snowy_small_house_3", "village/snowy/houses/snowy_small_house_4",
            "village/snowy/houses/snowy_small_house_5", "village/snowy/houses/snowy_small_house_6",
            "village/snowy/houses/snowy_small_house_7", "village/snowy/houses/snowy_small_house_8",
            "village/snowy/houses/snowy_medium_house_1", "village/snowy/houses/snowy_medium_house_2",
            "village/snowy/houses/snowy_medium_house_3", "village/snowy/houses/snowy_big_house_1",
            "village/snowy/houses/snowy_butcher_shop_1", "village/snowy/houses/snowy_cartographer_house_1",
            "village/snowy/houses/snowy_fletcher_house_1", "village/snowy/houses/snowy_mason_house_1",
            "village/snowy/houses/snowy_shepherd_house_1", "village/snowy/houses/snowy_tannery_1",
            "village/snowy/houses/snowy_temple_1", "village/snowy/houses/snowy_temple_2",
            "village/snowy/houses/snowy_tool_smith_1", "village/snowy/houses/snowy_weaponsmith_house_1",
            "village/snowy/houses/snowy_armorer_house_1", "village/snowy/houses/snowy_fisher_cottage_1",
            // Village decorations
            "village/plains/decoration", "village/desert/decoration", "village/savanna/decoration",
            "village/taiga/decoration", "village/snowy/decoration",
            // ========== RUINED PORTALS ==========
            "ruined_portal/desert_1", "ruined_portal/desert_2", "ruined_portal/desert_3", "ruined_portal/desert_4",
            "ruined_portal/standard_1", "ruined_portal/standard_2", "ruined_portal/standard_3", "ruined_portal/standard_4",
            "ruined_portal/jungle_1", "ruined_portal/jungle_2", "ruined_portal/jungle_3", "ruined_portal/jungle_4",
            "ruined_portal/mountain_1", "ruined_portal/mountain_2", "ruined_portal/mountain_3", "ruined_portal/mountain_4",
            "ruined_portal/ocean_1", "ruined_portal/ocean_2", "ruined_portal/ocean_3", "ruined_portal/ocean_4",
            "ruined_portal/nether_1", "ruined_portal/nether_2", "ruined_portal/nether_3", "ruined_portal/nether_4",
            "ruined_portal/swamp_1", "ruined_portal/swamp_2", "ruined_portal/swamp_3", "ruined_portal/swamp_4",
            // ========== PILLAGER OUTPOSTS ==========
            "pillager_outpost/watchtower_1", "pillager_outpost/watchtower_2", "pillager_outpost/watchtower_3",
            "pillager_outpost/watchtower_4", "pillager_outpost/watchtower_5", "pillager_outpost/watchtower_6",
            "pillager_outpost/watchtower_7", "pillager_outpost/watchtower_8", "pillager_outpost/watchtower_9",
            "pillager_outpost/watchtower_10", "pillager_outpost/watchtower_11", "pillager_outpost/watchtower_12",
            "pillager_outpost/watchtower_13", "pillager_outpost/watchtower_14", "pillager_outpost/watchtower_15",
            "pillager_outpost/watchtower_16", "pillager_outpost/watchtower_17", "pillager_outpost/watchtower_18",
            "pillager_outpost/watchtower_19", "pillager_outpost/watchtower_20", "pillager_outpost/watchtower_21",
            "pillager_outpost/watchtower_22", "pillager_outpost/watchtower_23", "pillager_outpost/watchtower_24",
            "pillager_outpost/watchtower_25", "pillager_outpost/watchtower_26", "pillager_outpost/watchtower_27",
            "pillager_outpost/watchtower_28", "pillager_outpost/watchtower_29", "pillager_outpost/watchtower_30",
            "pillager_outpost/watchtower_31", "pillager_outpost/watchtower_32", "pillager_outpost/watchtower_33",
            "pillager_outpost/watchtower_34", "pillager_outpost/watchtower_35", "pillager_outpost/watchtower_36",
            "pillager_outpost/watchtower_37", "pillager_outpost/watchtower_38", "pillager_outpost/watchtower_39",
            "pillager_outpost/watchtower_40", "pillager_outpost/watchtower_41", "pillager_outpost/watchtower_42",
            "pillager_outpost/watchtower_43", "pillager_outpost/watchtower_44", "pillager_outpost/watchtower_45",
            "pillager_outpost/watchtower_46", "pillager_outpost/watchtower_47", "pillager_outpost/watchtower_48",
            "pillager_outpost/watchtower_49", "pillager_outpost/watchtower_50", "pillager_outpost/watchtower_51",
            "pillager_outpost/watchtower_52", "pillager_outpost/watchtower_53", "pillager_outpost/watchtower_54",
            "pillager_outpost/watchtower_55", "pillager_outpost/watchtower_56", "pillager_outpost/watchtower_57",
            "pillager_outpost/watchtower_58", "pillager_outpost/watchtower_59", "pillager_outpost/watchtower_60",
            "pillager_outpost/watchtower_61", "pillager_outpost/watchtower_62", "pillager_outpost/watchtower_63",
            "pillager_outpost/watchtower_64", "pillager_outpost/watchtower_65", "pillager_outpost/watchtower_66",
            "pillager_outpost/watchtower_67", "pillager_outpost/watchtower_68", "pillager_outpost/watchtower_69",
            "pillager_outpost/watchtower_70", "pillager_outpost/watchtower_71", "pillager_outpost/watchtower_72",
            "pillager_outpost/watchtower_73", "pillager_outpost/watchtower_74", "pillager_outpost/watchtower_75",
            "pillager_outpost/watchtower_76", "pillager_outpost/watchtower_77", "pillager_outpost/watchtower_78",
            "pillager_outpost/watchtower_79", "pillager_outpost/watchtower_80", "pillager_outpost/watchtower_81",
            "pillager_outpost/watchtower_82", "pillager_outpost/watchtower_83", "pillager_outpost/watchtower_84",
            "pillager_outpost/watchtower_85", "pillager_outpost/watchtower_86", "pillager_outpost/watchtower_87",
            "pillager_outpost/watchtower_88", "pillager_outpost/watchtower_89", "pillager_outpost/watchtower_90",
            "pillager_outpost/watchtower_91", "pillager_outpost/watchtower_92", "pillager_outpost/watchtower_93",
            "pillager_outpost/watchtower_94", "pillager_outpost/watchtower_95", "pillager_outpost/watchtower_96",
            "pillager_outpost/watchtower_97", "pillager_outpost/watchtower_98", "pillager_outpost/watchtower_99",
            "pillager_outpost/watchtower_100",
            // ========== OTHER STRUCTURES ==========
            "igloo/igloo_bottom", "igloo/igloo_middle", "igloo/igloo_top",
            "swamp_hut/swamp_hut",
            "desert_well",
            "ocean_ruin/warm_ruin_1", "ocean_ruin/warm_ruin_2", "ocean_ruin/warm_ruin_3", "ocean_ruin/warm_ruin_4",
            "ocean_ruin/warm_ruin_5", "ocean_ruin/warm_ruin_6", "ocean_ruin/warm_ruin_7", "ocean_ruin/warm_ruin_8",
            "ocean_ruin/cold_ruin_1", "ocean_ruin/cold_ruin_2", "ocean_ruin/cold_ruin_3", "ocean_ruin/cold_ruin_4",
            "ocean_ruin/cold_ruin_5", "ocean_ruin/cold_ruin_6", "ocean_ruin/cold_ruin_7", "ocean_ruin/cold_ruin_8",
            "trail_ruins/trail_ruins_1", "trail_ruins/trail_ruins_2", "trail_ruins/trail_ruins_3", "trail_ruins/trail_ruins_4",
            "trail_ruins/trail_ruins_5", "trail_ruins/trail_ruins_6", "trail_ruins/trail_ruins_7", "trail_ruins/trail_ruins_8",
            "trial_chambers/corridor", "trial_chambers/entrance", "trial_chambers/hall", "trial_chambers/inter_chamber",
            "trial_chambers/jail", "trial_chambers/rampart", "trial_chambers/spawner", "trial_chambers/storage",
            "trial_chambers/supply", "trial_chambers/trap", "trial_chambers/water",
            // ========== LARGE STRUCTURES (CHAOS MODE!) ==========
            // These are huge and will cause maximum chaos!
            "ancient_city/city_center", "ancient_city/city_entrance", "ancient_city/walls",
            "mineshaft/mineshaft", "mineshaft/mineshaft_mesa",
            "jungle_pyramid",
            "desert_pyramid",
            "woodland_mansion",
            "ocean_monument",
            "bastion/units/air_base", "bastion/units/center_piece", "bastion/units/rampart_units",
            "bastion/units/rampart_plates", "bastion/units/stable_front", "bastion/units/stable_middle",
            "bastion/units/stable_back", "bastion/units/treasure_base", "bastion/units/treasure_center",
            "bastion/units/treasure_entrance", "bastion/units/treasure_room", "bastion/units/wall_base",
            "bastion/units/wall_units", "bastion/bridge/bridge", "bastion/bridge/bridge_end",
            "bastion/bridge/bridge_piece", "bastion/bridge/legs", "bastion/bridge/wall_base",
            "bastion/bridge/wall_connector", "bastion/bridge/wall_extension", "bastion/bridge/wall_leg",
            "bastion/bridge/wall_side", "bastion/hoglin_stable/air_base", "bastion/hoglin_stable/connector",
            "bastion/hoglin_stable/entrance", "bastion/hoglin_stable/extension_front", "bastion/hoglin_stable/extension_left",
            "bastion/hoglin_stable/extension_right", "bastion/hoglin_stable/inside", "bastion/hoglin_stable/large_stable",
            "bastion/hoglin_stable/large_stable_1", "bastion/hoglin_stable/large_stable_2", "bastion/hoglin_stable/large_stable_3",
            "bastion/hoglin_stable/rampart_connector", "bastion/hoglin_stable/rampart_extension", "bastion/hoglin_stable/rampart_plates",
            "bastion/hoglin_stable/ramparts", "bastion/hoglin_stable/small_stable_1", "bastion/hoglin_stable/small_stable_2",
            "bastion/hoglin_stable/small_stable_3", "bastion/hoglin_stable/small_stable_4", "bastion/hoglin_stable/stable_connector",
            "bastion/hoglin_stable/wall_base", "bastion/hoglin_stable/wall_connector", "bastion/hoglin_stable/wall_extension",
            "bastion/hoglin_stable/wall_leg", "bastion/hoglin_stable/wall_side", "bastion/hoglin_stable/wall_straight",
            "bastion/stable/air_base", "bastion/stable/connector", "bastion/stable/entrance", "bastion/stable/extension_front",
            "bastion/stable/extension_left", "bastion/stable/extension_right", "bastion/stable/inside", "bastion/stable/large_stable",
            "bastion/stable/large_stable_1", "bastion/stable/large_stable_2", "bastion/stable/large_stable_3", "bastion/stable/rampart_connector",
            "bastion/stable/rampart_extension", "bastion/stable/rampart_plates", "bastion/stable/ramparts", "bastion/stable/small_stable_1",
            "bastion/stable/small_stable_2", "bastion/stable/small_stable_3", "bastion/stable/small_stable_4", "bastion/stable/stable_connector",
            "bastion/stable/wall_base", "bastion/stable/wall_connector", "bastion/stable/wall_extension", "bastion/stable/wall_leg",
            "bastion/stable/wall_side", "bastion/stable/wall_straight", "bastion/treasure/big_air_full", "bastion/treasure/big_air_full_2",
            "bastion/treasure/big_air_half", "bastion/treasure/big_air_half_2", "bastion/treasure/brains_1", "bastion/treasure/brains_2",
            "bastion/treasure/brains_3", "bastion/treasure/brains_4", "bastion/treasure/connector", "bastion/treasure/entrance",
            "bastion/treasure/entrance_2", "bastion/treasure/rampart_plates", "bastion/treasure/ramparts", "bastion/treasure/roof",
            "bastion/treasure/roof_2", "bastion/treasure/roof_3", "bastion/treasure/roof_4", "bastion/treasure/roof_5",
            "bastion/treasure/roof_6", "bastion/treasure/roof_7", "bastion/treasure/roof_8", "bastion/treasure/roof_9",
            "bastion/treasure/roof_10", "bastion/treasure/roof_11", "bastion/treasure/roof_12", "bastion/treasure/roof_13",
            "bastion/treasure/roof_14", "bastion/treasure/roof_15", "bastion/treasure/roof_16", "bastion/treasure/roof_17",
            "bastion/treasure/roof_18", "bastion/treasure/roof_19", "bastion/treasure/roof_20", "bastion/treasure/roof_21",
            "bastion/treasure/roof_22", "bastion/treasure/roof_23", "bastion/treasure/roof_24", "bastion/treasure/roof_25",
            "bastion/treasure/roof_26", "bastion/treasure/roof_27", "bastion/treasure/roof_28", "bastion/treasure/roof_29",
            "bastion/treasure/roof_30", "bastion/treasure/roof_31", "bastion/treasure/roof_32", "bastion/treasure/roof_33",
            "bastion/treasure/roof_34", "bastion/treasure/roof_35", "bastion/treasure/roof_36", "bastion/treasure/roof_37",
            "bastion/treasure/roof_38", "bastion/treasure/roof_39", "bastion/treasure/roof_40", "bastion/treasure/roof_41",
            "bastion/treasure/roof_42", "bastion/treasure/roof_43", "bastion/treasure/roof_44", "bastion/treasure/roof_45",
            "bastion/treasure/roof_46", "bastion/treasure/roof_47", "bastion/treasure/roof_48", "bastion/treasure/roof_49",
            "bastion/treasure/roof_50", "bastion/treasure/roof_51", "bastion/treasure/roof_52", "bastion/treasure/roof_53",
            "bastion/treasure/roof_54", "bastion/treasure/roof_55", "bastion/treasure/roof_56", "bastion/treasure/roof_57",
            "bastion/treasure/roof_58", "bastion/treasure/roof_59", "bastion/treasure/roof_60", "bastion/treasure/roof_61",
            "bastion/treasure/roof_62", "bastion/treasure/roof_63", "bastion/treasure/roof_64", "bastion/treasure/roof_65",
            "bastion/treasure/roof_66", "bastion/treasure/roof_67", "bastion/treasure/roof_68", "bastion/treasure/roof_69",
            "bastion/treasure/roof_70", "bastion/treasure/roof_71", "bastion/treasure/roof_72", "bastion/treasure/roof_73",
            "bastion/treasure/roof_74", "bastion/treasure/roof_75", "bastion/treasure/roof_76", "bastion/treasure/roof_77",
            "bastion/treasure/roof_78", "bastion/treasure/roof_79", "bastion/treasure/roof_80", "bastion/treasure/roof_81",
            "bastion/treasure/roof_82", "bastion/treasure/roof_83", "bastion/treasure/roof_84", "bastion/treasure/roof_85",
            "bastion/treasure/roof_86", "bastion/treasure/roof_87", "bastion/treasure/roof_88", "bastion/treasure/roof_89",
            "bastion/treasure/roof_90", "bastion/treasure/roof_91", "bastion/treasure/roof_92", "bastion/treasure/roof_93",
            "bastion/treasure/roof_94", "bastion/treasure/roof_95", "bastion/treasure/roof_96", "bastion/treasure/roof_97",
            "bastion/treasure/roof_98", "bastion/treasure/roof_99", "bastion/treasure/roof_100",
            "nether_fortress/bridge", "nether_fortress/bridge_end", "nether_fortress/bridge_gate", "nether_fortress/bridge_stairs",
            "nether_fortress/corridor_floor", "nether_fortress/corridor_nether_wart_room", "nether_fortress/corridor_roof",
            "nether_fortress/corridor_stairs", "nether_fortress/crossing", "nether_fortress/entrance", "nether_fortress/hall",
            "nether_fortress/hall_crossing", "nether_fortress/hall_entrance", "nether_fortress/hall_room", "nether_fortress/hall_stairs",
            "nether_fortress/room", "nether_fortress/room_crossing", "nether_fortress/room_entrance", "nether_fortress/room_exit",
            "nether_fortress/room_lava", "nether_fortress/room_stairs", "nether_fortress/small_corridor", "nether_fortress/small_corridor_crossing",
            "nether_fortress/small_corridor_left_turn", "nether_fortress/small_corridor_right_turn", "nether_fortress/small_hall",
            "nether_fortress/small_room", "nether_fortress/small_room_crossing", "nether_fortress/small_room_entrance",
            "nether_fortress/small_room_exit", "nether_fortress/small_room_stairs", "nether_fortress/staircase",
            "nether_fortress/staircase_crossing", "nether_fortress/staircase_entrance", "nether_fortress/staircase_left_turn",
            "nether_fortress/staircase_right_turn", "nether_fortress/staircase_stairs", "nether_fortress/start",
            "nether_fortress/start_entrance", "nether_fortress/start_piece", "nether_fortress/start_stairs",
            "end_city/base_floor", "end_city/base_roof", "end_city/bridge_end", "end_city/bridge_piece",
            "end_city/fat_tower_base", "end_city/fat_tower_middle", "end_city/fat_tower_top", "end_city/portal",
            "end_city/ship", "end_city/tower_base", "end_city/tower_floor", "end_city/tower_middle",
            "end_city/tower_piece", "end_city/tower_top"
    );

    @Override
    public String getId() {
        return "instant_structure";
    }

    @Override
    public String getName() {
        return "Sofort-Struktur";
    }

    @Override
    public String getDescription() {
        return "Spawnt eine zufällige Vanilla-Struktur an deiner Position!";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        try {
            // Versuche, eine Vanilla-Struktur zu spawnen
            if (trySpawnVanillaStructure(level, player)) {
                return true;
            }
        } catch (Exception e) {
            BingoBackpack.LOGGER.debug("Failed to spawn vanilla structure, falling back to custom structure", e);
        }

        // Fallback: Spawne eine einfache, nützliche Struktur
        spawnCustomStructure(level, player);
        return true;
    }

    /**
     * Versucht, eine Vanilla-Struktur aus dem StructureManager zu spawnen.
     */
    private boolean trySpawnVanillaStructure(ServerLevel level, ServerPlayer player) {
        StructureTemplateManager structureManager = level.getServer().getStructureManager();
        if (structureManager == null) {
            return false;
        }

        // Wähle eine zufällige Struktur
        String structureId = STRUCTURE_IDS.get(random.nextInt(STRUCTURE_IDS.size()));
        ResourceLocation structureLocation = ResourceLocation.fromNamespaceAndPath("minecraft", structureId);

        Optional<StructureTemplate> template = structureManager.get(structureLocation);
        if (template.isEmpty()) {
            return false;
        }

        StructureTemplate structure = template.get();
        BlockPos structureSize = structure.getSize();
        
        // Finde eine geeignete Position (vor dem Spieler)
        BlockPos spawnPos = findSpawnPosition(level, player, structureSize);
        if (spawnPos == null) {
            player.sendSystemMessage(Component.literal("§cKein Platz für die Struktur gefunden!"));
            return false;
        }

        // Platziere die Struktur
        structure.placeInWorld(
                level,
                spawnPos,
                spawnPos,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
                        .setRandom(level.getRandom())
                        .setMirror(net.minecraft.world.level.block.Mirror.NONE)
                        .setRotation(net.minecraft.world.level.block.Rotation.NONE)
                        .setIgnoreEntities(false)
                        .setChunkBounds(null),
                level.getRandom(),
                2 // Block update flags
        );

        // Sound und Nachricht
        level.playSound(null, spawnPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal("§a§l✨ Struktur gespawnt! ✨"));
        player.sendSystemMessage(Component.literal("§7Eine §e" + getStructureDisplayName(structureId) + " §7wurde bei §f" +
                spawnPos.getX() + ", " + spawnPos.getY() + ", " + spawnPos.getZ() + " §7erstellt!"));

        return true;
    }

    /**
     * Spawnt eine einfache, nützliche Struktur als Fallback.
     */
    private void spawnCustomStructure(ServerLevel level, ServerPlayer player) {
        BlockPos spawnPos = findSpawnPosition(level, player, new BlockPos(7, 5, 7));
        if (spawnPos == null) {
            spawnPos = player.blockPosition().relative(player.getDirection(), 3);
        }

        // Spawne eine zufällige einfache Struktur
        int structureType = random.nextInt(3);
        
        switch (structureType) {
            case 0 -> spawnSmallHouse(level, spawnPos);
            case 1 -> spawnTower(level, spawnPos);
            case 2 -> spawnLootHut(level, spawnPos);
        }

        level.playSound(null, spawnPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal("§a§l✨ Struktur gespawnt! ✨"));
        player.sendSystemMessage(Component.literal("§7Eine Struktur wurde bei §f" +
                spawnPos.getX() + ", " + spawnPos.getY() + ", " + spawnPos.getZ() + " §7erstellt!"));
    }

    /**
     * Spawnt ein kleines Haus.
     */
    private void spawnSmallHouse(ServerLevel level, BlockPos pos) {
        // Fundament
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                level.setBlock(pos.offset(x, -1, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            }
        }

        // Wände
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 5; x++) {
                level.setBlock(pos.offset(x, y, 0), Blocks.OAK_LOG.defaultBlockState(), 3);
                level.setBlock(pos.offset(x, y, 4), Blocks.OAK_LOG.defaultBlockState(), 3);
            }
            for (int z = 1; z < 4; z++) {
                level.setBlock(pos.offset(0, y, z), Blocks.OAK_LOG.defaultBlockState(), 3);
                level.setBlock(pos.offset(4, y, z), Blocks.OAK_LOG.defaultBlockState(), 3);
            }
        }

        // Dach
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                level.setBlock(pos.offset(x, 3, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            }
        }

        // Tür
        level.setBlock(pos.offset(2, 0, 0), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.offset(2, 1, 0), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.offset(2, 0, -1), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(pos.offset(2, 1, -1), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.SOUTH)
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.DoorBlock.DoorHalf.UPPER), 3);

        // Fenster
        level.setBlock(pos.offset(0, 1, 2), Blocks.GLASS.defaultBlockState(), 3);
        level.setBlock(pos.offset(4, 1, 2), Blocks.GLASS.defaultBlockState(), 3);

        // Kiste mit Loot
        BlockPos chestPos = pos.offset(2, 0, 2);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.IRON_INGOT, 4 + random.nextInt(8)));
            chest.setItem(1, new ItemStack(Items.BREAD, 4 + random.nextInt(8)));
            chest.setItem(2, new ItemStack(Items.COAL, 8 + random.nextInt(16)));
        }
    }

    /**
     * Spawnt einen Turm.
     */
    private void spawnTower(ServerLevel level, BlockPos pos) {
        int height = 8 + random.nextInt(5);
        
        // Fundament
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                level.setBlock(pos.offset(x, -1, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
        }

        // Turm
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    if (x == 0 || x == 2 || z == 0 || z == 2) {
                        level.setBlock(pos.offset(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    } else {
                        level.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            
            // Fenster alle 2 Ebenen
            if (y > 0 && y < height - 1 && y % 2 == 0) {
                level.setBlock(pos.offset(1, y, 0), Blocks.GLASS.defaultBlockState(), 3);
            }
        }

        // Dach
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                level.setBlock(pos.offset(x, height, z), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Spawnt eine kleine Hütte mit Loot.
     */
    private void spawnLootHut(ServerLevel level, BlockPos pos) {
        // Fundament
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                level.setBlock(pos.offset(x, -1, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        }

        // Wände
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 4; x++) {
                level.setBlock(pos.offset(x, y, 0), Blocks.COBBLESTONE.defaultBlockState(), 3);
                level.setBlock(pos.offset(x, y, 3), Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
            for (int z = 1; z < 3; z++) {
                level.setBlock(pos.offset(0, y, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
                level.setBlock(pos.offset(3, y, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        }

        // Dach
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                level.setBlock(pos.offset(x, 2, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            }
        }

        // Tür
        level.setBlock(pos.offset(1, 0, 0), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.offset(1, 1, 0), Blocks.AIR.defaultBlockState(), 3);

        // Kiste mit gutem Loot
        BlockPos chestPos = pos.offset(2, 0, 2);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.DIAMOND, 2 + random.nextInt(4)));
            chest.setItem(1, new ItemStack(Items.GOLD_INGOT, 4 + random.nextInt(8)));
            chest.setItem(2, new ItemStack(Items.IRON_INGOT, 8 + random.nextInt(16)));
            chest.setItem(3, new ItemStack(Items.EMERALD, 2 + random.nextInt(6)));
        }
    }

    /**
     * Findet eine geeignete Spawn-Position für die Struktur.
     * Erweitert für große Strukturen - sucht weiter weg!
     */
    private BlockPos findSpawnPosition(ServerLevel level, ServerPlayer player, BlockPos structureSize) {
        BlockPos playerPos = player.blockPosition();
        Direction facing = player.getDirection();
        
        // Für große Strukturen: suche weiter weg
        int maxSearchDistance = Math.max(50, Math.max(structureSize.getX(), structureSize.getZ()) + 20);
        
        // Versuche Position vor dem Spieler
        BlockPos tryPos = playerPos.relative(facing, 5).relative(facing.getClockWise(), structureSize.getX() / 2);
        
        // Finde passende Y-Position
        int y = findGroundLevel(level, tryPos);
        if (y == -1) {
            // Fallback: direkt beim Spieler
            y = playerPos.getY();
        }
        
        tryPos = new BlockPos(tryPos.getX(), y, tryPos.getZ());
        
        // Prüfe, ob genug Platz ist (lockerer für große Strukturen)
        if (hasEnoughSpace(level, tryPos, structureSize, true)) {
            return tryPos;
        }
        
        // Versuche andere Positionen in größerem Radius
        for (int offset = 5; offset <= maxSearchDistance; offset += 5) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos testPos = playerPos.relative(dir, offset);
                int testY = findGroundLevel(level, testPos);
                if (testY != -1) {
                    testPos = new BlockPos(testPos.getX(), testY, testPos.getZ());
                    if (hasEnoughSpace(level, testPos, structureSize, true)) {
                        return testPos;
                    }
                }
            }
        }
        
        // Letzter Versuch: Spawne einfach in der Luft wenn nötig (für maximale Chaos!)
        return new BlockPos(playerPos.getX(), playerPos.getY() + 10, playerPos.getZ());
    }

    /**
     * Findet das Bodenniveau an einer Position.
     */
    private int findGroundLevel(ServerLevel level, BlockPos pos) {
        for (int y = pos.getY() + 5; y >= pos.getY() - 10; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (!level.getBlockState(checkPos).isAir() && 
                level.getBlockState(checkPos.above()).isAir() &&
                level.getBlockState(checkPos.above(2)).isAir()) {
                return y + 1;
            }
        }
        return -1;
    }

    /**
     * Prüft, ob genug Platz für die Struktur vorhanden ist.
     * @param allowOverwrite Wenn true, erlaubt Überschreiben von natürlichen Blöcken (für Chaos!)
     */
    private boolean hasEnoughSpace(ServerLevel level, BlockPos pos, BlockPos size, boolean allowOverwrite) {
        // Für sehr große Strukturen: lockere Prüfung
        if (size.getX() * size.getY() * size.getZ() > 100000) {
            // Nur prüfen ob Chunks geladen sind
            for (int x = 0; x < size.getX(); x += 16) {
                for (int z = 0; z < size.getZ(); z += 16) {
                    BlockPos checkPos = pos.offset(x, 0, z);
                    if (!level.isLoaded(checkPos)) {
                        return false;
                    }
                }
            }
            return true; // Große Strukturen spawnen immer - CHAOS!
        }
        
        // Normale Prüfung für kleinere Strukturen
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    if (!level.isLoaded(checkPos)) {
                        return false;
                    }
                    if (!allowOverwrite) {
                        // Prüfe nur Luft oder nicht-solide Blöcke
                        BlockState state = level.getBlockState(checkPos);
                        if (state.isSolid() && !state.is(Blocks.GRASS) && !state.is(Blocks.DIRT) && 
                            !state.is(Blocks.STONE) && !state.is(Blocks.SAND)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Gibt einen Anzeigenamen für die Struktur zurück.
     */
    private String getStructureDisplayName(String structureId) {
        if (structureId.contains("village")) {
            if (structureId.contains("plains")) return "Dorfhaus (Ebenen)";
            if (structureId.contains("desert")) return "Dorfhaus (Wüste)";
            if (structureId.contains("savanna")) return "Dorfhaus (Savanne)";
            if (structureId.contains("taiga")) return "Dorfhaus (Taiga)";
            if (structureId.contains("snowy")) return "Dorfhaus (Schnee)";
            return "Dorfhaus";
        }
        if (structureId.contains("ruined_portal")) return "Ruiniertes Portal";
        if (structureId.contains("pillager")) return "Wachturm";
        if (structureId.contains("igloo")) return "Iglu";
        if (structureId.contains("swamp_hut")) return "Hexenhütte";
        if (structureId.contains("desert_well")) return "Wüstenbrunnen";
        if (structureId.contains("ocean_ruin")) return "Ozeanruine";
        if (structureId.contains("trail_ruins")) return "Pfadruine";
        if (structureId.contains("trial_chambers")) return "Probenkammer";
        if (structureId.contains("ancient_city")) return "§c§lANTIKE STADT §r§7(CHAOS!)";
        if (structureId.contains("mineshaft")) return "Minenschacht";
        if (structureId.contains("jungle_pyramid")) return "§e§lDschungeltempel";
        if (structureId.contains("desert_pyramid")) return "§e§lWüstentempel";
        if (structureId.contains("woodland_mansion")) return "§5§lWaldanwesen §r§7(RIESIG!)";
        if (structureId.contains("ocean_monument")) return "§b§lOzeanmonument §r§7(RIESIG!)";
        if (structureId.contains("bastion")) return "§cBastion §7(Nether)";
        if (structureId.contains("nether_fortress")) return "§c§lNetherfestung §r§7(RIESIG!)";
        if (structureId.contains("end_city")) return "§d§lEndstadt §r§7(RIESIG!)";
        return "Struktur";
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Spawnt eine §czufällige §7Vanilla-Struktur"),
                Component.literal("§7Kann §eALLES §7sein:"),
                Component.literal("§7Häuser, Türme, Tempel, Festungen..."),
                Component.literal("§c§l⚠ MAXIMALES CHAOS! ⚠"),
                Component.literal("§7Sogar §5riesige Strukturen §7können spawnen!"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
