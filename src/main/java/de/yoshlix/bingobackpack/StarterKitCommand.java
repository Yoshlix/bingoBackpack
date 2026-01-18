package de.yoshlix.bingobackpack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command for players to receive the starter kit.
 * Can be used at any time to get/refresh the starter kit.
 * Old starter kit items are removed before giving new ones.
 */
public class StarterKitCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("starterkit")
                .executes(StarterKitCommand::giveStarterKit));
    }

    private static int giveStarterKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        StarterKitManager.getInstance().giveStarterKit(player);

        return 1;
    }
}
