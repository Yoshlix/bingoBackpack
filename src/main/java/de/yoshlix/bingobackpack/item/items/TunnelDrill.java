package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Tunnel-Bohrer - Instantly digs a 3x3 tunnel, 30 blocks in your look
 * direction.
 * Perfect for quick Nether travel or reaching structures!
 */
public class TunnelDrill extends BingoItem {

    private static final int TUNNEL_LENGTH = 30;

    @Override
    public String getId() {
        return "tunnel_drill";
    }

    @Override
    public String getName() {
        return "Tunnel-Bohrer";
    }

    @Override
    public String getDescription() {
        return "Gräbt instant einen 3x3 Tunnel, 30 Blöcke in Blickrichtung!";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        // Get player's look direction
        Direction facing = player.getDirection();

        // For horizontal tunnels, use horizontal direction
        // For looking up/down significantly, adjust
        float pitch = player.getXRot();
        boolean goingUp = pitch < -45;
        boolean goingDown = pitch > 45;

        BlockPos startPos = player.blockPosition().above(); // Start at eye level

        int blocksDestroyed = 0;
        int bedrockHit = 0;

        // Drill the tunnel
        for (int depth = 1; depth <= TUNNEL_LENGTH; depth++) {
            BlockPos centerPos;

            if (goingUp) {
                centerPos = startPos.above(depth);
            } else if (goingDown) {
                centerPos = startPos.below(depth);
            } else {
                centerPos = startPos.relative(facing, depth);
            }

            // Dig 3x3 area
            for (int w = -1; w <= 1; w++) {
                for (int h = -1; h <= 1; h++) {
                    BlockPos targetPos;

                    if (goingUp || goingDown) {
                        // Vertical tunnel - use horizontal spread
                        targetPos = centerPos.offset(w, 0, h);
                    } else {
                        // Horizontal tunnel
                        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                            targetPos = centerPos.offset(w, h, 0);
                        } else {
                            targetPos = centerPos.offset(0, h, w);
                        }
                    }

                    BlockState state = level.getBlockState(targetPos);

                    // Skip air and fluids
                    if (state.isAir())
                        continue;

                    // Can't destroy bedrock
                    if (state.is(Blocks.BEDROCK)) {
                        bedrockHit++;
                        continue;
                    }

                    // Skip certain protected blocks
                    if (state.is(Blocks.END_PORTAL_FRAME) ||
                            state.is(Blocks.END_PORTAL) ||
                            state.is(Blocks.NETHER_PORTAL)) {
                        continue;
                    }

                    // Destroy the block (no drops - it's instant mining)
                    level.destroyBlock(targetPos, false);
                    blocksDestroyed++;
                }
            }
        }

        // Play drilling sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.3f, 1.5f);

        // Show result
        String direction = goingUp ? "nach oben"
                : goingDown ? "nach unten" : "Richtung " + facing.getName().toUpperCase();

        player.sendSystemMessage(Component.literal("§6§l⛏ BRRRRRR! §r§6Tunnel gebohrt!"));
        player.sendSystemMessage(Component.literal("§7" + blocksDestroyed + " Blöcke zerstört " + direction));

        if (bedrockHit > 0) {
            player.sendSystemMessage(Component.literal("§8(Bedrock kann nicht durchbohrt werden)"));
        }

        // Teleport player slightly into the tunnel to prevent getting stuck
        if (!goingUp && !goingDown) {
            BlockPos safePos = startPos.relative(facing, 2);
            player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
        }

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§6⛏ 3x3 Tunnel"),
                Component.literal("§730 Blöcke tief"),
                Component.literal("§7Perfekt für Nether/Stronghold!"),
                Component.literal("§8(Kann kein Bedrock durchbohren)"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
