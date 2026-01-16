package de.yoshlix.bingobackpack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import me.jfenn.bingo.api.BingoApi;
import me.jfenn.bingo.api.data.BingoGameStatus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TeleportToSpawnCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn").executes(TeleportToSpawnCommand::teleportToSpawn));
    }

    private static int teleportToSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var game = BingoApi.getGame();
        if (game == null || game.getStatus() != BingoGameStatus.PLAYING)
            return 0;

        ServerPlayer player = context.getSource().getPlayerOrException();

        boolean success = SpawnManager.getInstance().teleportToSpawn(player);

        if (success) {
            player.sendSystemMessage(Component.literal("§aTeleported to your spawn!"));
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("§cYou don't have a spawn set!"));
            return 0;
        }
    }
}
