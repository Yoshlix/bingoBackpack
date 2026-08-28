package de.yoshlix.bingobackpack.item;

import de.yoshlix.bingobackpack.bingo.BingoBridge;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Abstract base class for all Bingo Items.
 * 
 * To create a new item, extend this class and implement:
 * - getId(): Unique identifier for this item
 * - getName(): Display name for this item
 * - getDescription(): Description shown in lore
 * - getRarity(): The rarity level
 * - onUse(): The action when right-clicked
 * 
 * Optional overrides:
 * - getDropChanceMultiplier(): Modify the drop chance from mobs
 * - canDropFromMob(): Whether this item can drop from mobs
 * - getExtraLore(): Additional lore lines
 */
public abstract class BingoItem {

    public static final String NBT_KEY = "BingoItemId";

    /** Shared RNG, so items don't each seed their own. */
    protected static final Random RANDOM = new Random();

    /**
     * All online enemy players that are not protected by a Team Shield.
     *
     * Shared by the targeted PvP items so the enemy-selection loop (skip own
     * team, skip shielded teams and players, online and alive only) lives in one
     * place.
     */
    protected static java.util.List<ServerPlayer> onlineEnemies(ServerPlayer player,
            IBingoTeam playerTeam) {
        var server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        var enemies = new java.util.ArrayList<ServerPlayer>();
        for (IBingoTeam team : BingoBridge.getEnemyTeams(playerTeam.getId())) {
            if (de.yoshlix.bingobackpack.item.items.TeamShield.isTeamShielded(team.getId())) {
                continue;
            }
            for (java.util.UUID memberId : team.getPlayers()) {
                ServerPlayer enemy = server.getPlayerList().getPlayer(memberId);
                if (enemy != null && enemy.isAlive()
                        && !de.yoshlix.bingobackpack.item.items.TeamShield.isPlayerShielded(memberId)) {
                    enemies.add(enemy);
                }
            }
        }
        return enemies;
    }

    /**
     * Shared plumbing for the PC-prank items: honour the config toggle, pick a
     * random reachable enemy, and send the packet. The caller supplies its own
     * flavour messages around the returned target.
     *
     * @return the target the prank was sent to, or null if it could not fire
     *         (a reason was already reported to the player)
     */
    protected static ServerPlayer firePcPrank(ServerPlayer player, String action, String arg,
            int durationSeconds) {
        if (!de.yoshlix.bingobackpack.ModConfig.getInstance().pcPranksEnabled) {
            player.sendSystemMessage(
                    Component.literal("§cPC-Effekte sind auf diesem Server deaktiviert."));
            return null;
        }
        IBingoTeam team = requireTeam(player);
        if (team == null) {
            return null;
        }
        var enemies = onlineEnemies(player, team);
        enemies.removeIf(e -> !de.yoshlix.bingobackpack.net.PcPranks.canReach(e));
        if (enemies.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("§6Kein erreichbarer Gegner mit installiertem Client-Mod!"));
            return null;
        }
        ServerPlayer target = enemies.get(RANDOM.nextInt(enemies.size()));
        de.yoshlix.bingobackpack.net.PcPranks.send(target, action, arg, durationSeconds);
        return target;
    }

    /**
     * Resolve the player's bingo team, sending the standard failure message if
     * bingo is not running or the player is not on a team.
     *
     * @return the team, or null if the item cannot be used right now
     */
    protected static IBingoTeam requireTeam(ServerPlayer player) {
        if (!BingoBridge.isAvailable()) {
            player.sendSystemMessage(Component.literal("§cKein Bingo-Spiel aktiv!"));
            return null;
        }

        IBingoTeam team = BingoBridge.getTeamForPlayer(player.getUUID());
        if (team == null) {
            player.sendSystemMessage(Component.literal("§cDu bist in keinem Team!"));
            return null;
        }
        return team;
    }

    /**
     * Unique identifier for this item type.
     * Used for NBT storage and registry lookup.
     */
    public abstract String getId();

    /**
     * Display name for this item (without color formatting).
     */
    public abstract String getName();

    /**
     * Short description of what this item does.
     */
    public abstract String getDescription();

    /**
     * The rarity level of this item.
     */
    public abstract ItemRarity getRarity();

    /**
     * Called when a player right-clicks with this item.
     * The item will be consumed after this is called (unless you return false).
     * 
     * @param player The player who used the item
     * @return true if the item should be consumed, false to keep it
     */
    public abstract boolean onUse(ServerPlayer player);

    /**
     * Multiplier for the drop chance from mobs.
     * Default is 1.0 (uses rarity base chance).
     * Override to make specific items more or less common.
     */
    public double getDropChanceMultiplier() {
        return 1.0;
    }

    /**
     * Whether this item can drop from killing mobs.
     * Override and return false for items that should only
     * be obtained through other means (like completing bingo rows).
     */
    public boolean canDropFromMob() {
        return true;
    }

    /**
     * Additional lore lines to display.
     * Override to add custom information.
     */
    public List<Component> getExtraLore() {
        return List.of();
    }

    /**
     * Calculate the actual drop chance for this item.
     */
    public double getDropChance() {
        return getRarity().getBaseDropChance() * getDropChanceMultiplier();
    }

    /**
     * Create an ItemStack representing this Bingo Item.
     */
    public ItemStack createItemStack() {
        return createItemStack(1);
    }

    /**
     * Create an ItemStack with a specific count.
     */
    public ItemStack createItemStack(int count) {
        ItemStack stack = new ItemStack(Items.PAPER, count);

        // Set custom name with rarity color
        Component displayName = Component.literal(getName())
                .withStyle(Style.EMPTY
                        .withColor(getRarity().getColor())
                        .withItalic(false));
        stack.set(DataComponents.CUSTOM_NAME, displayName);

        // Build lore
        List<Component> loreList = new ArrayList<>();

        // Rarity line
        loreList.add(Component.literal(getRarity().getDisplayName())
                .withStyle(Style.EMPTY
                        .withColor(getRarity().getColor())
                        .withItalic(true)));

        // Empty line
        loreList.add(Component.empty());

        // Description
        loreList.add(Component.literal(getDescription())
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GRAY)
                        .withItalic(false)));

        // Extra lore from subclass
        List<Component> extraLore = getExtraLore();
        if (!extraLore.isEmpty()) {
            loreList.add(Component.empty());
            loreList.addAll(extraLore);
        }

        // Usage hint
        loreList.add(Component.empty());
        loreList.add(Component.literal("Rechtsklick zum Einlösen")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.YELLOW)
                        .withItalic(true)));

        stack.set(DataComponents.LORE, new ItemLore(loreList));

        // Store item ID in NBT
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_KEY, getId());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        return stack;
    }

    /**
     * Check if an ItemStack is this specific Bingo Item.
     */
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.PAPER)) {
            return false;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        CompoundTag tag = customData.copyTag();
        return tag.contains(NBT_KEY) && tag.getString(NBT_KEY).orElse("").equals(getId());
    }

    /**
     * Remove a single copy of this item from the player's inventory.
     *
     * Items that defer their effect to a chat selection must call this
     * <em>before</em> applying the effect and abort if it returns false —
     * otherwise a player who stashes the item in the team backpack between
     * opening the menu and confirming gets the effect for free.
     *
     * @return true if a copy was found and removed
     */
    public boolean consumeOne(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (matches(stack)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** Whether the player is carrying at least one copy of this item. */
    public boolean isCarriedBy(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (matches(inventory.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Guard for the command handlers of deferred items: confirms the player is
     * still carrying the item before the effect runs.
     *
     * Handlers execute synchronously on the server thread, so a check here and
     * a {@link #consumeOrWarn} after the effect cannot be interleaved with an
     * inventory change — while still leaving the item intact if the effect
     * itself fails.
     *
     * @return true if the effect may proceed
     */
    public static boolean requireItemOrWarn(ServerPlayer player, String itemId) {
        boolean present = BingoItemRegistry.getById(itemId)
                .map(item -> item.isCarriedBy(player))
                .orElse(false);
        if (!present) {
            player.sendSystemMessage(
                    Component.literal("§cDu hast das Item nicht mehr im Inventar!"));
        }
        return present;
    }

    /**
     * Consume one copy of the item with the given ID, reporting failure to the
     * player. For use from the static command handlers of deferred items.
     *
     * @return true if the effect may proceed
     */
    public static boolean consumeOrWarn(ServerPlayer player, String itemId) {
        boolean consumed = BingoItemRegistry.getById(itemId)
                .map(item -> item.consumeOne(player))
                .orElse(false);
        if (!consumed) {
            player.sendSystemMessage(
                    Component.literal("§cDu hast das Item nicht mehr im Inventar!"));
        }
        return consumed;
    }

    /**
     * Utility method to get the effect strength based on rarity.
     */
    protected double getEffectStrength() {
        return getRarity().getEffectMultiplier();
    }

    /**
     * Utility method to get the duration multiplier based on rarity.
     */
    protected double getDurationMultiplier() {
        return getRarity().getDurationMultiplier();
    }

    /**
     * Utility method to calculate a duration in ticks.
     * 
     * @param baseSeconds Base duration in seconds
     * @return Duration in ticks, scaled by rarity
     */
    protected int getDurationTicks(int baseSeconds) {
        return (int) (baseSeconds * 20 * getDurationMultiplier());
    }
}
