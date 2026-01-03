package de.yoshlix.bingobackpack;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TrollManager {
    private static final int MIN_COOLDOWN_TICKS = 120; // ~6 seconds
    private static final int MAX_COOLDOWN_TICKS = 260; // ~13 seconds
    private static final double CHICKEN_HAT_HEIGHT_OFFSET = 1.2;
    private static final int HOTBAR_SIZE = 9;
    private static final int MAX_DROP_ATTEMPTS = 12;

    private static final TrollManager INSTANCE = new TrollManager();
    private boolean enabled = false;
    private int cooldown = MAX_COOLDOWN_TICKS;

    public void init(MinecraftServer server) {
        this.cooldown = MAX_COOLDOWN_TICKS;
        BingoBackpack.LOGGER.info("TrollManager initialized. Enabled: {}", enabled);
    }

    public void tick(MinecraftServer server) {
        if (!enabled)
            return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            resetCooldown();
            return;
        }

        ServerPlayer target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        performRandomTroll(target);
        resetCooldown();
    }

    private void performRandomTroll(ServerPlayer player) {
        int choice = ThreadLocalRandom.current().nextInt(7);
        switch (choice) {
            case 0 -> rocketJump(player);
            case 1 -> creepyHiss(player);
            case 2 -> chickenHat(player);
            case 3 -> shuffleHotbar(player);
            case 4 -> dropRandomItem(player);
            case 5 -> dizzySpell(player);
            default -> surpriseSnack(player);
        }
    }

    private void rocketJump(ServerPlayer player) {
        var random = ThreadLocalRandom.current();
        double x = (random.nextDouble() - 0.5) * 0.4;
        double y = 1.0 + random.nextDouble() * 0.5;
        double z = (random.nextDouble() - 0.5) * 0.4;
        player.push(x, y, z);
        player.hurtMarked = true;
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY(),
                player.getZ(), 12, 0.4, 0.2, 0.4, 0.02);
        player.sendSystemMessage(Component.literal("§6Wooosh! Raketenrucksack aktiviert."));
    }

    private void creepyHiss(ServerPlayer player) {
        var random = ThreadLocalRandom.current();
        player.level().playSound(null, player.blockPosition(), SoundEvents.CREEPER_PRIMED, SoundSource.PLAYERS, 1.6F,
                0.8F + random.nextFloat() * 0.4F);
        player.sendSystemMessage(Component.literal("§2Pssst... hast du das gehört?"));
    }

    private void chickenHat(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Chicken chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.COMMAND);
        if (chicken != null) {
            chicken.setPos(player.getX(), player.getY() + CHICKEN_HAT_HEIGHT_OFFSET, player.getZ());
            chicken.setCustomName(Component.literal("§eHut-Huhn"));
            chicken.setCustomNameVisible(true);
            level.addFreshEntity(chicken);
            boolean mounted = chicken.startRiding(player, true, true);
            if (!mounted) {
                mounted = player.startRiding(chicken, true, true);
            }
            if (mounted) {
                player.sendSystemMessage(Component.literal("§eDein neuer Hut ist da!"));
            } else {
                chicken.discard();
            }
        }
    }

    private void shuffleHotbar(ServerPlayer player) {
        var inventory = player.getInventory();
        ItemStack first = inventory.getItem(0);
        for (int i = 0; i < HOTBAR_SIZE - 1; i++) {
            inventory.setItem(i, inventory.getItem(i + 1));
        }
        inventory.setItem(HOTBAR_SIZE - 1, first);
        player.sendSystemMessage(Component.literal("§dHotbar durchgeschüttelt!"));
    }

    private void dropRandomItem(ServerPlayer player) {
        var inventory = player.getInventory();
        int size = inventory.getContainerSize();
        var random = ThreadLocalRandom.current();
        for (int i = 0; i < Math.min(size, MAX_DROP_ATTEMPTS); i++) {
            int slot = random.nextInt(size);
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                ItemStack drop = stack.split(1);
                player.drop(drop, false);
                player.sendSystemMessage(Component.literal("§cUps! Dir ist etwas heruntergefallen."));
                break;
            }
        }
    }

    private void dizzySpell(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 140, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
        player.sendSystemMessage(Component.literal("§5Dir wird ganz schwindelig..."));
    }

    private void surpriseSnack(ServerPlayer player) {
        ItemStack snack = new ItemStack(Items.POTATO, 3);
        snack.set(DataComponents.CUSTOM_NAME, Component.literal("§aMysteriöse Kartoffel"));
        if (!player.getInventory().add(snack)) {
            player.drop(snack, false);
        }
        var random = ThreadLocalRandom.current();
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F,
                0.6F + random.nextFloat() * 0.2F);
        player.sendSystemMessage(Component.literal("§aSnack-Pause! Wer hat die Kartoffeln bestellt?"));
    }

    private void resetCooldown() {
        cooldown = MIN_COOLDOWN_TICKS
                + ThreadLocalRandom.current().nextInt(MAX_COOLDOWN_TICKS - MIN_COOLDOWN_TICKS + 1);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean toggle() {
        this.enabled = !this.enabled;
        return this.enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Singleton
    public static TrollManager getInstance() {
        return INSTANCE;
    }

    private TrollManager() {
    }
}
