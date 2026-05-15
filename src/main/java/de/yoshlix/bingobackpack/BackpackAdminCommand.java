package de.yoshlix.bingobackpack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.yoshlix.bingobackpack.banish.BanishManager;
import de.yoshlix.bingobackpack.item.items.Lockdown;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BackpackAdminCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("backpack")
                .then(Commands.literal("admin")
                        .requires(source -> Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source))
                        .then(Commands.literal("clearbanish")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(BackpackAdminCommand::clearBanish)))
                        .then(Commands.literal("clearlockdown")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(BackpackAdminCommand::clearLockdown)))));
    }

    private static int clearBanish(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        if (BanishManager.getInstance().clearBanish(target)) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "§aBanish-Status von " + target.getName().getString() + " entfernt."), true);
            return 1;
        }

        context.getSource().sendFailure(Component.literal(
                "§c" + target.getName().getString() + " ist aktuell nicht in einer Banish-Challenge."));
        return 0;
    }

    private static int clearLockdown(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        if (Lockdown.clearLockdown(target.getUUID())) {
            target.sendSystemMessage(Component.literal("§aDein Lockdown wurde von einem Admin entfernt."));
            context.getSource().sendSuccess(() -> Component.literal(
                    "§aLockdown von " + target.getName().getString() + " entfernt."), true);
            return 1;
        }

        context.getSource().sendFailure(Component.literal(
                "§c" + target.getName().getString() + " hat aktuell keinen aktiven Lockdown."));
        return 0;
    }
}
