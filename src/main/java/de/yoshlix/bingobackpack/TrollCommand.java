package de.yoshlix.bingobackpack;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TrollCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trolls")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("enable").executes(context -> setEnabled(context.getSource(), true)))
                .then(Commands.literal("disable").executes(context -> setEnabled(context.getSource(), false)))
                .then(Commands.literal("toggle").executes(context -> {
                    boolean enabled = TrollManager.getInstance().toggle();
                    context.getSource().sendSuccess(() -> status(enabled), true);
                    return enabled ? 1 : 0;
                }))
                .then(Commands.literal("status").executes(context -> {
                    context.getSource().sendSuccess(() -> status(TrollManager.getInstance().isEnabled()), false);
                    return 1;
                })));
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) {
        TrollManager.getInstance().setEnabled(enabled);
        source.sendSuccess(() -> status(enabled), true);
        return enabled ? 1 : 0;
    }

    private static Component status(boolean enabled) {
        String state = enabled ? "§aaktiviert" : "§cdeaktiviert";
        return Component.literal("Server-Trolls sind jetzt " + state + ".");
    }
}
