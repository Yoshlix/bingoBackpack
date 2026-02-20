package de.yoshlix.bingobackpack.item;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.yoshlix.bingobackpack.item.items.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command handler for Bingo Item selection menus.
 * Handles commands like /backpack perks select, /backpack perks reroll, etc.
 */
public class BingoItemCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("backpack")
                .then(Commands.literal("perks")
                        // /backpack perks select <id/number>
                        .then(Commands.literal("select")
                                .then(Commands.argument("selection", StringArgumentType.greedyString())
                                        .executes(BingoItemCommands::handleSelect)))

                        // /backpack perks reroll <number>
                        .then(Commands.literal("reroll")
                                .then(Commands.argument("selection", StringArgumentType.string())
                                        .executes(BingoItemCommands::handleReroll)))

                        // /backpack perks reset <number>
                        .then(Commands.literal("reset")
                                .then(Commands.argument("selection", StringArgumentType.string())
                                        .executes(BingoItemCommands::handleReset)))

                        // /backpack perks biome <number>
                        .then(Commands.literal("biome")
                                .then(Commands.argument("selection", StringArgumentType.string())
                                        .executes(BingoItemCommands::handleBiome)))

                        // /backpack perks swap <number>
                        .then(Commands.literal("swap")
                                .then(Commands.argument("selection", StringArgumentType.string())
                                        .executes(BingoItemCommands::handleSwap)))

                        // /backpack perks timeout <number>
                        .then(Commands.literal("timeout")
                                .then(Commands.argument("selection", StringArgumentType.string())
                                        .executes(BingoItemCommands::handleTimeout)))

                        // /backpack perks lockdown <number>
                        .then(Commands.literal("lockdown")
                                .then(Commands.argument("selection", StringArgumentType.string())
                                        .executes(BingoItemCommands::handleLockdown)))

                        // /backpack perks wildcard <item_id>
                        .then(Commands.literal("wildcard")
                                .then(Commands.argument("selection", StringArgumentType.greedyString())
                                        .executes(BingoItemCommands::handleWildcard)))
                        
                        // /backpack perks pheromone <self|enemy>
                        .then(Commands.literal("pheromone")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .executes(BingoItemCommands::handlePheromone)))));
    }

    private static int handlePheromone(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String target = StringArgumentType.getString(context, "target");
        boolean targetSelf;

        if (target.equalsIgnoreCase("self")) {
            targetSelf = true;
        } else if (target.equalsIgnoreCase("enemy")) {
            targetSelf = false;
        } else {
            player.sendSystemMessage(Component.literal("§cUngültiges Ziel! Nutze 'self' oder 'enemy'."));
            return 0;
        }

        // Verify player has the item
        boolean hasItem = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            var itemOpt = BingoItemRegistry.fromItemStack(stack);
            if (itemOpt.isPresent() && itemOpt.get() instanceof MobPheromone) {
                hasItem = true;
                break;
            }
        }

        if (!hasItem) {
            player.sendSystemMessage(Component.literal("§cDu hast keine Mob-Pheromone!"));
            return 0;
        }

        MobPheromone.activatePheromones(player, targetSelf);
        return 1;
    }

    private static int handleSelect(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String selection = StringArgumentType.getString(context, "selection");

        if (CompleteChosenBingoField.hasPendingSelection(player.getUUID())) {
            CompleteChosenBingoField.processSelection(player, selection);
            return 1;
        }

        player.sendSystemMessage(Component.literal("§cKeine ausstehende Auswahl!"));
        return 0;
    }

    private static int handleReroll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String selection = StringArgumentType.getString(context, "selection");

        if (RerollChosenField.hasPendingReroll(player.getUUID())) {
            RerollChosenField.processReroll(player, selection);
            return 1;
        }

        player.sendSystemMessage(Component.literal("§cKeine ausstehende Reroll-Auswahl!"));
        return 0;
    }

    private static int handleReset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String selection = StringArgumentType.getString(context, "selection");

        if (ResetFieldProgress.hasPendingReset(player.getUUID())) {
            ResetFieldProgress.processReset(player, selection);
            return 1;
        }

        player.sendSystemMessage(Component.literal("§cKeine ausstehende Reset-Auswahl!"));
        return 0;
    }

    private static int handleBiome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String selection = StringArgumentType.getString(context, "selection");

        if (BiomeTeleportChoice.hasPendingSelection(player.getUUID())) {
            BiomeTeleportChoice.processBiomeSelection(player, selection);
            return 1;
        }

        player.sendSystemMessage(Component.literal("§cKeine ausstehende Biom-Auswahl!"));
        return 0;
    }

    private static int handleSwap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String selection = StringArgumentType.getString(context, "selection");

        if (SwapLocationChoice.hasPendingSwap(player.getUUID())) {
            SwapLocationChoice.processSwap(player, selection);
            return 1;
        }

        player.sendSystemMessage(Component.literal("§cKeine ausstehende Swap-Auswahl!"));
        return 0;
    }

    private static int handleTimeout(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String selection = StringArgumentType.getString(context, "selection");

        if (TimeoutPlayer.hasPendingTimeout(player.getUUID())) {
            TimeoutPlayer.processTimeout(player, selection);
            return 1;
        }

        player.sendSystemMessage(Component.literal("§cKeine ausstehende Timeout-Auswahl!"));
        return 0;
    }

    private static int handleLockdown(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String selection = StringArgumentType.getString(context, "selection");

        if (Lockdown.hasPendingLockdown(player.getUUID())) {
            Lockdown.processLockdown(player, selection);
            return 1;
        }

        player.sendSystemMessage(Component.literal("§cKeine ausstehende Lockdown-Auswahl!"));
        return 0;
    }

    private static int handleWildcard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String selection = StringArgumentType.getString(context, "selection");

        if (Wildcard.hasPendingSelection(player.getUUID())) {
            Wildcard.selectItem(player, selection);
            return 1;
        }

        player.sendSystemMessage(Component.literal("§cKeine ausstehende Wildcard-Auswahl!"));
        return 0;
    }
}
