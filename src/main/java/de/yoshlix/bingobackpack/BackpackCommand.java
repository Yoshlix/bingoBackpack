package de.yoshlix.bingobackpack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BackpackCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("backpack")
                // /backpack - Open backpack (for players in a team)
                .executes(BackpackCommand::openBackpack)

                // /backpack get - Get the backpack bundle item
                .then(Commands.literal("get")
                        .executes(BackpackCommand::getBackpackItem))

                .then(Commands.literal("config")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("hunger")
                                .then(Commands.literal("on")
                                        .executes(ctx -> toggleConfig(ctx, "hunger", true)))
                                .then(Commands.literal("off")
                                        .executes(ctx -> toggleConfig(ctx, "hunger", false)))))
                .then(Commands.literal("spawn")
                        .then(Commands.literal("on")
                                .executes(ctx -> toggleConfig(ctx, "spawn", true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> toggleConfig(ctx, "spawn", false))))
                .then(Commands.literal("status")
                        .executes(BackpackCommand::mixinStatus))

                // Team management commands (OP only)
                .then(Commands.literal("team")
                        .requires(source -> source.hasPermission(2)) // OP level 2

                        // /backpack team create <name> [player1] [player2] ...
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(BackpackCommand::createTeam)
                                        .then(Commands.argument("player1", EntityArgument.player())
                                                .executes(BackpackCommand::createTeamWithPlayers)
                                                .then(Commands.argument("player2", EntityArgument.player())
                                                        .executes(BackpackCommand::createTeamWithPlayers)
                                                        .then(Commands.argument("player3", EntityArgument.player())
                                                                .executes(BackpackCommand::createTeamWithPlayers)
                                                                .then(Commands
                                                                        .argument("player4", EntityArgument.player())
                                                                        .executes(
                                                                                BackpackCommand::createTeamWithPlayers)
                                                                        .then(Commands
                                                                                .argument("player5",
                                                                                        EntityArgument.player())
                                                                                .executes(
                                                                                        BackpackCommand::createTeamWithPlayers))))))))

                        // /backpack team delete <name>
                        .then(Commands.literal("delete")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests(BackpackCommand::suggestTeams)
                                        .executes(BackpackCommand::deleteTeam)))

                        // /backpack team add <team> <player>
                        .then(Commands.literal("add")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .suggests(BackpackCommand::suggestTeams)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(BackpackCommand::addPlayerToTeam))))

                        // /backpack team remove <team> <player>
                        .then(Commands.literal("remove")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .suggests(BackpackCommand::suggestTeams)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(BackpackCommand::removePlayerFromTeam))))

                        // /backpack team list
                        .then(Commands.literal("list")
                                .executes(BackpackCommand::listTeams))

                        // /backpack team info <team>
                        .then(Commands.literal("info")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .suggests(BackpackCommand::suggestTeams)
                                        .executes(BackpackCommand::teamInfo)))

                        // /backpack team reset - Delete all teams and backpacks
                        .then(Commands.literal("reset")
                                .executes(BackpackCommand::resetAllTeams))

                        // /backpack team sync - Manual sync with scoreboard teams (Bingo integration)
                        .then(Commands.literal("sync")
                                .executes(BackpackCommand::syncTeams))

                        // /backpack team autosync <on|off> - Toggle automatic sync
                        .then(Commands.literal("autosync")
                                .then(Commands.literal("on")
                                        .executes(ctx -> setAutoSync(ctx, true)))
                                .then(Commands.literal("off")
                                        .executes(ctx -> setAutoSync(ctx, false))))));

    }

    private static CompletableFuture<Suggestions> suggestTeams(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        Set<String> teams = TeamManager.getInstance().getAllTeams();
        for (String team : teams) {
            if (team.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(team);
            }
        }
        return builder.buildFuture();
    }

    private static int openBackpack(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerUUID = player.getUUID();

        String teamName = TeamManager.getInstance().getPlayerTeam(playerUUID);
        if (teamName == null) {
            context.getSource()
                    .sendFailure(Component.literal("You are not in a team! Ask an admin to add you to a team."));
            return 0;
        }

        BackpackManager.getInstance().openBackpack(player, teamName);
        return 1;
    }

    private static int getBackpackItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerUUID = player.getUUID();

        String teamName = TeamManager.getInstance().getPlayerTeam(playerUUID);
        if (teamName == null) {
            context.getSource()
                    .sendFailure(Component.literal("You are not in a team! Ask an admin to add you to a team."));
            return 0;
        }

        // Create a special bundle item
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.CUSTOM_NAME, Component.literal("§6Team Backpack: §e" + teamName));

        // Add custom NBT to identify it as a backpack item
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putBoolean("bingobackpack_item", true);
        tag.putString("team", teamName);
        bundle.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        // Give item to player
        if (!player.getInventory().add(bundle)) {
            player.drop(bundle, false);
        }

        context.getSource().sendSuccess(() -> Component.literal("You received the team backpack item!"), false);
        return 1;
    }

    private static int createTeam(CommandContext<CommandSourceStack> context) {
        String teamName = StringArgumentType.getString(context, "name");

        if (TeamManager.getInstance().createTeam(teamName)) {
            context.getSource().sendSuccess(() -> Component.literal("Team '" + teamName + "' created!"), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Team '" + teamName + "' already exists!"));
            return 0;
        }
    }

    private static int deleteTeam(CommandContext<CommandSourceStack> context) {
        String teamName = StringArgumentType.getString(context, "name");

        if (TeamManager.getInstance().deleteTeam(teamName)) {
            context.getSource().sendSuccess(() -> Component.literal("Team '" + teamName + "' deleted!"), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Team '" + teamName + "' does not exist!"));
            return 0;
        }
    }

    private static int addPlayerToTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String teamName = StringArgumentType.getString(context, "team");
        ServerPlayer player = EntityArgument.getPlayer(context, "player");

        if (!TeamManager.getInstance().teamExists(teamName)) {
            context.getSource().sendFailure(Component.literal("Team '" + teamName + "' does not exist!"));
            return 0;
        }

        if (TeamManager.getInstance().addPlayerToTeam(teamName, player.getUUID())) {
            context.getSource().sendSuccess(
                    () -> Component.literal("Added " + player.getName().getString() + " to team '" + teamName + "'!"),
                    true);
            player.sendSystemMessage(Component.literal("You have been added to team '" + teamName + "'!"));
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to add player to team!"));
            return 0;
        }
    }

    private static int removePlayerFromTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String teamName = StringArgumentType.getString(context, "team");
        ServerPlayer player = EntityArgument.getPlayer(context, "player");

        if (TeamManager.getInstance().removePlayerFromTeam(teamName, player.getUUID())) {
            context.getSource().sendSuccess(
                    () -> Component
                            .literal("Removed " + player.getName().getString() + " from team '" + teamName + "'!"),
                    true);
            player.sendSystemMessage(Component.literal("You have been removed from team '" + teamName + "'!"));
            return 1;
        } else {
            context.getSource()
                    .sendFailure(Component.literal("Player is not in team '" + teamName + "' or team does not exist!"));
            return 0;
        }
    }

    private static int listTeams(CommandContext<CommandSourceStack> context) {
        Set<String> teams = TeamManager.getInstance().getAllTeams();

        if (teams.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No teams exist yet."), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder("§6Teams:\n");
        for (String team : teams) {
            int memberCount = TeamManager.getInstance().getTeamMembers(team).size();
            sb.append("§e- ").append(team).append(" §7(").append(memberCount).append(" members)\n");
        }

        context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int teamInfo(CommandContext<CommandSourceStack> context) {
        String teamName = StringArgumentType.getString(context, "team");

        if (!TeamManager.getInstance().teamExists(teamName)) {
            context.getSource().sendFailure(Component.literal("Team '" + teamName + "' does not exist!"));
            return 0;
        }

        Set<UUID> members = TeamManager.getInstance().getTeamMembers(teamName);
        StringBuilder sb = new StringBuilder("§6Team: §e" + teamName + "\n§6Members:\n");

        if (members.isEmpty()) {
            sb.append("§7(No members)");
        } else {
            for (UUID uuid : members) {
                ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(uuid);
                String playerName = player != null ? player.getName().getString() : uuid.toString();
                String status = player != null ? "§a(online)" : "§c(offline)";
                sb.append("§e- ").append(playerName).append(" ").append(status).append("\n");
            }
        }

        context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int resetAllTeams(CommandContext<CommandSourceStack> context) {
        int teamCount = TeamManager.getInstance().getAllTeams().size();
        TeamManager.getInstance().resetAll();
        BackpackManager.getInstance().clearAllBackpacks();

        context.getSource().sendSuccess(
                () -> Component.literal("All " + teamCount + " teams and their backpacks have been deleted!"), true);
        return 1;
    }

    private static int createTeamWithPlayers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String teamName = StringArgumentType.getString(context, "name");

        if (!TeamManager.getInstance().createTeam(teamName)) {
            context.getSource().sendFailure(Component.literal("Team '" + teamName + "' already exists!"));
            return 0;
        }

        // Add all specified players to the team
        ServerPlayer player1 = EntityArgument.getPlayer(context, "player1");
        TeamManager.getInstance().addPlayerToTeam(teamName, player1.getUUID());

        // Try to add optional players if they were specified
        try {
            ServerPlayer player2 = EntityArgument.getPlayer(context, "player2");
            TeamManager.getInstance().addPlayerToTeam(teamName, player2.getUUID());
        } catch (IllegalArgumentException e) {
            // player2 not specified, continue
        }

        try {
            ServerPlayer player3 = EntityArgument.getPlayer(context, "player3");
            TeamManager.getInstance().addPlayerToTeam(teamName, player3.getUUID());
        } catch (IllegalArgumentException e) {
            // player3 not specified, continue
        }

        try {
            ServerPlayer player4 = EntityArgument.getPlayer(context, "player4");
            TeamManager.getInstance().addPlayerToTeam(teamName, player4.getUUID());
        } catch (IllegalArgumentException e) {
            // player4 not specified, continue
        }

        try {
            ServerPlayer player5 = EntityArgument.getPlayer(context, "player5");
            TeamManager.getInstance().addPlayerToTeam(teamName, player5.getUUID());
        } catch (IllegalArgumentException e) {
            // player5 not specified, continue
        }

        Set<UUID> members = TeamManager.getInstance().getTeamMembers(teamName);
        context.getSource().sendSuccess(
                () -> Component.literal("Team '" + teamName + "' created with " + members.size() + " member(s)!"),
                true);
        return 1;
    }

    private static int syncTeams(CommandContext<CommandSourceStack> context) {
        int teamCount = BingoIntegration.getInstance().manualSync();
        context.getSource().sendSuccess(
                () -> Component.literal("§aSynced with scoreboard teams! Found " + teamCount + " teams."), true);
        return 1;
    }

    private static int setAutoSync(CommandContext<CommandSourceStack> context, boolean enabled) {
        BingoIntegration.getInstance().setEnabled(enabled);
        if (enabled) {
            context.getSource().sendSuccess(
                    () -> Component.literal("§aAutomatic team sync enabled! Teams will sync with scoreboard."), true);
        } else {
            context.getSource().sendSuccess(
                    () -> Component.literal("§cAutomatic team sync disabled."), true);
        }
        return 1;
    }

    private static int toggleConfig(CommandContext<CommandSourceStack> ctx, String configName, boolean value) {
        ModConfig config = ModConfig.getInstance();

        switch (configName.toLowerCase()) {
            case "hunger":
                config.hungerMixinEnabled = value;
                break;
            case "spawn":
                config.spawnTeleportEnabled = value;
                break;
            default:
                ctx.getSource().sendFailure(Component.literal("Unknown config flag: " + configName));
                return 0;
        }

        ModConfig.save(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a" + configName + " mixin " + (value ? "enabled" : "disabled")), true);

        return 1;
    }

    private static int mixinStatus(CommandContext<CommandSourceStack> ctx) {
        ModConfig config = ModConfig.getInstance();

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§6Config Status:\n" +
                        "§7- Hunger: " + (config.hungerMixinEnabled ? "§aEnabled" : "§cDisabled") + "\n" +
                        "§7- Spawn Teleport: " + (config.spawnTeleportEnabled ? "§aEnabled" : "§cDisabled")),
                false);

        return 1;
    }
}
