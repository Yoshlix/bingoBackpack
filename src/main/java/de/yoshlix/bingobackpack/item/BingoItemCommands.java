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
 * Handles commands like /bingobackpack select, /bingobackpack reroll, etc.
 */
public class BingoItemCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bingobackpack")
                // /bingobackpack select <id/number> - For CompleteChosenBingoField
                .then(Commands.literal("select")
                        .then(Commands.argument("selection", StringArgumentType.greedyString())
                                .executes(BingoItemCommands::handleSelect)))

                // /bingobackpack reroll <number> - For RerollChosenField
                .then(Commands.literal("reroll")
                        .then(Commands.argument("selection", StringArgumentType.string())
                                .executes(BingoItemCommands::handleReroll)))

                // /bingobackpack reset <number> - For ResetFieldProgress
                .then(Commands.literal("reset")
                        .then(Commands.argument("selection", StringArgumentType.string())
                                .executes(BingoItemCommands::handleReset)))

                // /bingobackpack biome <number> - For BiomeTeleportChoice
                .then(Commands.literal("biome")
                        .then(Commands.argument("selection", StringArgumentType.string())
                                .executes(BingoItemCommands::handleBiome)))

                // /bingobackpack swap <number> - For SwapLocationChoice
                .then(Commands.literal("swap")
                        .then(Commands.argument("selection", StringArgumentType.string())
                                .executes(BingoItemCommands::handleSwap)))

                // /bingobackpack timeout <number> - For TimeoutPlayer
                .then(Commands.literal("timeout")
                        .then(Commands.argument("selection", StringArgumentType.string())
                                .executes(BingoItemCommands::handleTimeout)))

                // /bingobackpack lockdown <number> - For Lockdown
                .then(Commands.literal("lockdown")
                        .then(Commands.argument("selection", StringArgumentType.string())
                                .executes(BingoItemCommands::handleLockdown)))

                // /bingobackpack wildcard <item_id> - For Wildcard
                .then(Commands.literal("wildcard")
                        .then(Commands.argument("selection", StringArgumentType.greedyString())
                                .executes(BingoItemCommands::handleWildcard))));
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
