package de.yoshlix.bingobackpack.bounty;

import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.function.Predicate;

/**
 * One entry in the bounty catalog. Immutable — built once via the static
 * factories and held in {@link BountyRegistry}.
 */
public final class BountyDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final ItemRarity tier;
    private final BountyType type;

    // KILL
    private final EntityType<?> mobType;
    private final int killCount;

    // LOCATION
    private final Predicate<ServerPlayer> locationCheck;

    // COLLECT
    private final Item collectItem;
    private final int collectCount;

    private BountyDefinition(String id, String name, String description, ItemRarity tier, BountyType type,
            EntityType<?> mobType, int killCount, Predicate<ServerPlayer> locationCheck,
            Item collectItem, int collectCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tier = tier;
        this.type = type;
        this.mobType = mobType;
        this.killCount = killCount;
        this.locationCheck = locationCheck;
        this.collectItem = collectItem;
        this.collectCount = collectCount;
    }

    public static BountyDefinition kill(String id, String name, String description, ItemRarity tier,
            EntityType<?> mobType, int count) {
        return new BountyDefinition(id, name, description, tier, BountyType.KILL, mobType, count, null, null, 0);
    }

    public static BountyDefinition location(String id, String name, String description, ItemRarity tier,
            Predicate<ServerPlayer> locationCheck) {
        return new BountyDefinition(id, name, description, tier, BountyType.LOCATION, null, 0, locationCheck, null,
                0);
    }

    /**
     * Collect targets must always be plain vanilla items, never a
     * {@code BingoItem} — consuming a bingo item to satisfy a bounty would
     * block the team's actual bingo progress with that item.
     * {@link BountyManager} additionally guards this defensively at scan
     * time, but callers should never pass one here either.
     */
    public static BountyDefinition collect(String id, String name, String description, ItemRarity tier,
            Item item, int count) {
        return new BountyDefinition(id, name, description, tier, BountyType.COLLECT, null, 0, null, item, count);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ItemRarity getTier() {
        return tier;
    }

    public BountyType getType() {
        return type;
    }

    public EntityType<?> getMobType() {
        return mobType;
    }

    public int getKillCount() {
        return killCount;
    }

    public Predicate<ServerPlayer> getLocationCheck() {
        return locationCheck;
    }

    public Item getCollectItem() {
        return collectItem;
    }

    public int getCollectCount() {
        return collectCount;
    }
}
