package com.Maul.lotmmi.command;

import com.Maul.lotmmi.arrodes.ArrodesHandler;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class ArrodesCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arrodes")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reset")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            if (!(source.getEntity() instanceof ServerPlayer player)) {
                                source.sendFailure(Component.literal("Name a player: /arrodes reset <player>"));
                                return 0;
                            }
                            ArrodesHandler.resetCooldown(player);
                            source.sendSuccess(() -> Component.literal("Arrodes' cooldown has been reset for you."), true);
                            return 1;
                        })
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "players");
                                    for (ServerPlayer target : targets) ArrodesHandler.resetCooldown(target);
                                    int count = targets.size();
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Reset Arrodes' cooldown for " + count + " player" + (count == 1 ? "" : "s") + "."), true);
                                    return count;
                                }))));
    }
}
