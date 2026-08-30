package de.yoshlix.bingobackpack.banish;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TaskArena implements BanishTask {

    private static final EntityType<?>[] MOB_TYPES = {
        EntityTypes.ZOMBIE,
        EntityTypes.SKELETON,
        EntityTypes.SPIDER,
        EntityTypes.HUSK,
        EntityTypes.STRAY,
        EntityTypes.CAVE_SPIDER,
    };

    private static final String[] MOB_NAMES = {
        "Zombies", "Skelette", "Spinnen", "Husks", "Strays", "Höhlenspinnen"
    };

    @Override
    public String getTaskDescription() {
        return "Besiege alle Monster in der Arena! Danach erscheint ein Flucht-Button.";
    }

    @Override
    public Vec3 generate(ServerLevel level, BlockPos origin) {
        Random rand = TaskUtils.randomFor(origin);

        if (rand.nextBoolean()) {
            generateMultiChamber(level, origin, rand);
        } else {
            generateOpenPit(level, origin, rand);
        }

        // Place a solid block under origin for the button later
        level.setBlock(origin, Blocks.BEDROCK.defaultBlockState(), 3);

        return getSpawnPos(origin);
    }

    private void generateOpenPit(ServerLevel level, BlockPos origin, Random rand) {
        int size = 8 + rand.nextInt(6); // 8-13, varies the arena size per instance

        TaskUtils.fill(level, origin.offset(-size - 5, -5, -size - 5), origin.offset(size + 5, 10, size + 5), Blocks.AIR);
        TaskUtils.hollowBox(level, origin.offset(-size, -1, -size), origin.offset(size, 6, size), Blocks.BEDROCK, Blocks.AIR);
        lightGrid(level, origin, size);

        // Occasional hazard: a lava strip across half the floor - always leaves
        // the other side clear so it can't accidentally wall the player in.
        if (rand.nextInt(3) == 0) {
            int lz = rand.nextInt(size * 2 - 4) - (size - 2);
            int startX = rand.nextBoolean() ? -size + 2 : 0;
            TaskUtils.fill(level, origin.offset(startX, 0, lz), origin.offset(startX + size - 3, 0, lz), Blocks.LAVA);
        }
    }

    private void generateMultiChamber(ServerLevel level, BlockPos origin, Random rand) {
        int chambers = 2 + rand.nextInt(2); // 2-3 chambers connected by corridors
        int chamberSize = 5 + rand.nextInt(3); // 5-7
        int spacing = chamberSize * 2 + 5;

        TaskUtils.fill(level, origin.offset(-chamberSize - 3, -5, -chamberSize - 3),
                origin.offset(chamberSize + spacing * (chambers - 1) + 3, 10, chamberSize + 3), Blocks.AIR);

        BlockPos[] centers = new BlockPos[chambers];
        for (int i = 0; i < chambers; i++) {
            centers[i] = origin.offset(spacing * i, 0, 0);
            TaskUtils.hollowBox(level, centers[i].offset(-chamberSize, -1, -chamberSize),
                    centers[i].offset(chamberSize, 6, chamberSize), Blocks.BEDROCK, Blocks.AIR);
            lightGrid(level, centers[i], chamberSize);
        }

        // Corridors carved after every chamber's walls exist, so they don't
        // get resealed by the next chamber's hollowBox call.
        for (int i = 0; i < chambers - 1; i++) {
            BlockPos a = centers[i];
            BlockPos b = centers[i + 1];
            TaskUtils.fill(level, a.offset(chamberSize, 0, -1), b.offset(-chamberSize, 3, 1), Blocks.AIR);
            TaskUtils.fill(level, a.offset(chamberSize, -1, -1), b.offset(-chamberSize, -1, 1), Blocks.BEDROCK);
        }
    }

    private void lightGrid(ServerLevel level, BlockPos center, int size) {
        int half = Math.max(1, size / 2);
        level.setBlock(center.offset(0, 4, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
        level.setBlock(center.offset(-half, 4, -half), Blocks.GLOWSTONE.defaultBlockState(), 3);
        level.setBlock(center.offset(half, 4, -half), Blocks.GLOWSTONE.defaultBlockState(), 3);
        level.setBlock(center.offset(-half, 4, half), Blocks.GLOWSTONE.defaultBlockState(), 3);
        level.setBlock(center.offset(half, 4, half), Blocks.GLOWSTONE.defaultBlockState(), 3);
    }

    @Override
    public boolean isWinCondition(ServerLevel level, BlockPos interactedBlock, BlockPos origin) {
        return level.getBlockState(interactedBlock).is(Blocks.POLISHED_BLACKSTONE_BUTTON) && interactedBlock.distSqr(origin) < 400;
    }

    /**
     * Deterministic from origin alone (no persisted state), same convention
     * as the original: {@code [primaryIndex, secondaryIndex or -1]}. About a
     * third of instances mix two mob types instead of one.
     */
    private int[] mobComposition(BlockPos origin) {
        Random r = new Random(origin.asLong());
        int primary = Math.abs(r.nextInt()) % MOB_TYPES.length;
        boolean mixed = r.nextInt(3) == 0;
        int secondary = mixed ? Math.abs(r.nextInt()) % MOB_TYPES.length : -1;
        return new int[]{primary, secondary};
    }

    @Override
    public void tick(ServerPlayer player, BlockPos origin) {
        BanishData data = BanishManager.getInstance().getBanishData(player);
        if (data == null) return;

        data.taskTime++; // incremented once per second
        ServerLevel level = (ServerLevel) player.level();

        // Hunger works naturally for banished players (HungerMixin exception)
        // However, fighting doesn't drain enough exhaustion in 3 mins.
        // We artificially give them exhaustion so they *have* to eat the steak!
        if (data.taskTime > 2 && data.taskTime % 2 == 0) { // every 2 seconds
            player.causeFoodExhaustion(1.0f);
        }

        int[] composition = mobComposition(origin);
        EntityType<?> primaryType = MOB_TYPES[composition[0]];
        EntityType<?> secondaryType = composition[1] >= 0 ? MOB_TYPES[composition[1]] : null;

        // Give gear and spawn mobs after 2 seconds
        if (data.taskTime == 2) {
            giveArenaGear(player);

            int primaryCount = (primaryType == EntityTypes.CAVE_SPIDER) ? 15 : 10;
            if (secondaryType != null) {
                primaryCount = primaryCount / 2 + 1;
            }
            spawnMobs(level, origin, primaryType, primaryCount);

            StringBuilder message = new StringBuilder(String.valueOf(primaryCount)).append(" ").append(MOB_NAMES[composition[0]]);
            if (secondaryType != null) {
                int secondaryCount = (secondaryType == EntityTypes.CAVE_SPIDER) ? 8 : 5;
                spawnMobs(level, origin, secondaryType, secondaryCount);
                message.append(" und ").append(secondaryCount).append(" ").append(MOB_NAMES[composition[1]]);
            }
            player.sendSystemMessage(Component.literal("§e" + message + " spawnen! Besiege sie alle!"));
        }

        // Check win condition: button appears after 180 seconds (3 min) OR all mobs dead
        boolean giveButton = false;
        if (data.taskTime > 180) {
            giveButton = true;
        } else if (data.taskTime > 5 && data.taskTime % 2 == 0) {
            long mobCount = level.getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(20)
            ).stream().filter(e -> e.getType() == primaryType || e.getType() == secondaryType).count();
            if (mobCount == 0) {
                giveButton = true;
            }
        }

        if (giveButton && !level.getBlockState(origin.above()).is(Blocks.POLISHED_BLACKSTONE_BUTTON)) {
            level.setBlock(origin.above(), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState(), 3);
            player.sendSystemMessage(Component.literal("§aGeschafft! Der Flucht-Button ist in der Mitte!"));
        }
    }

    private void spawnMobs(ServerLevel level, BlockPos origin, EntityType<?> mobType, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos spawnPos = new BlockPos(
                    (int) (origin.getX() - 5 + Math.random() * 10),
                    origin.getY() + 1,
                    (int) (origin.getZ() - 5 + Math.random() * 10)
            );
            mobType.spawn(level, spawnPos, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        }
    }

    @Override
    public void onRespawn(ServerPlayer player, BlockPos origin) {
        giveArenaGear(player);
    }

    private void giveArenaGear(ServerPlayer player) {
        player.getInventory().clearContent();
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        // Shield for extra defense
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        // Food since hunger is active for banished players
        player.getInventory().add(new ItemStack(Items.COOKED_BEEF, 16));
    }

    @Override
    public Vec3 getSpawnPos(BlockPos origin) {
        return new Vec3(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
    }
}
