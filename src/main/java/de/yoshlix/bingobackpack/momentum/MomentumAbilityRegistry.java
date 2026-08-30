package de.yoshlix.bingobackpack.momentum;

import java.util.List;
import java.util.Optional;

/**
 * The full Momentum ability pool. Mirrors
 * {@link de.yoshlix.bingobackpack.item.BingoItemRegistry}: a static list
 * built once, read-only afterwards. The whole pool is meant to be
 * discoverable in advance (via {@code /backpack momentum list}) so no
 * ability is a surprise.
 */
public final class MomentumAbilityRegistry {

    private static final List<MomentumAbility> ABILITIES = List.of(
            new AnsturmAbility(),
            new BollwerkAbility(),
            new SpuersinnAbility(),
            new VerdunkelungAbility(),
            new KonfiszierungAbility(),
            new StillstandAbility());

    private MomentumAbilityRegistry() {
    }

    public static List<MomentumAbility> getAll() {
        return ABILITIES;
    }

    public static Optional<MomentumAbility> getById(String id) {
        return ABILITIES.stream().filter(a -> a.getId().equals(id)).findFirst();
    }

    /** Abilities implement {@link MomentumAbility#tick} for ongoing effects (e.g. Spürsinn's repeat pings). */
    public static void tickAll(net.minecraft.server.MinecraftServer server) {
        for (MomentumAbility ability : ABILITIES) {
            ability.tick(server);
        }
    }
}
