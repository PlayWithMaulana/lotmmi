package com.Maul.lotmmi.command;

import com.Maul.lotmmi.data.ModDataComponents;
import com.Maul.lotmmi.item.custom.AwakenedCreepingHungerItem;
import com.Maul.lotmmi.item.custom.CreepingHungerItem;
import com.Maul.lotmmi.network.packets.toClient.SyncItemIntrospectPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CreepingHungerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("creepinghunger")
                .then(Commands.literal("release")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    int slot = IntegerArgumentType.getInteger(context, "slot");

                                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                                        source.sendFailure(Component.literal("This command can only be used by a player."));
                                        return 0;
                                    }

                                    ItemStack held = findGlove(player);
                                    if (held.isEmpty()) {
                                        source.sendFailure(Component.literal("You must be holding Creeping Hunger in your main or off hand."));
                                        return 0;
                                    }

                                    List<CreepingHungerItem.SoulSlot> souls = CreepingHungerItem.getSouls(held);
                                    if (slot < 0 || slot >= souls.size()) {
                                        source.sendFailure(Component.literal(
                                                "Invalid slot. Creeping Hunger currently holds " + souls.size() + " soul(s) (valid: 0-" + (souls.size() - 1) + ")."
                                        ));
                                        return 0;
                                    }

                                    CreepingHungerItem.releaseSoul(player, held, slot);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("reserve")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    int slot = IntegerArgumentType.getInteger(context, "slot");

                                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                                        source.sendFailure(Component.literal("This command can only be used by a player."));
                                        return 0;
                                    }

                                    ItemStack held = findGlove(player);
                                    if (held.isEmpty()) {
                                        source.sendFailure(Component.literal("You must be holding Creeping Hunger in your main or off hand."));
                                        return 0;
                                    }

                                    List<CreepingHungerItem.SoulSlot> souls = CreepingHungerItem.getSouls(held);
                                    if (slot < 0 || slot >= souls.size()) {
                                        source.sendFailure(Component.literal(
                                                "Invalid slot. Creeping Hunger currently holds " + souls.size() + " active soul(s)."
                                        ));
                                        return 0;
                                    }

                                    CreepingHungerItem.moveToReserve(player, held, slot);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("list")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            if (!(source.getEntity() instanceof ServerPlayer player)) {
                                source.sendFailure(Component.literal("This command can only be used by a player."));
                                return 0;
                            }

                            ItemStack held = findGlove(player);
                            if (held.isEmpty()) {
                                source.sendFailure(Component.literal("You must be holding Creeping Hunger in your main or off hand."));
                                return 0;
                            }

                            List<CreepingHungerItem.SoulSlot> souls = CreepingHungerItem.getSouls(held);
                            CreepingHungerItem.SoulSlot food = CreepingHungerItem.getFoodSoul(held);

                            if (souls.isEmpty() && food == null) {
                                player.sendSystemMessage(Component.literal("Creeping Hunger holds no souls.")
                                        .withStyle(ChatFormatting.GRAY));
                                return 1;
                            }

                            player.sendSystemMessage(Component.literal("Creeping Hunger's held souls:")
                                    .withStyle(ChatFormatting.DARK_PURPLE));
                            for (int i = 0; i < souls.size(); i++) {
                                CreepingHungerItem.SoulSlot s = souls.get(i);
                                player.sendSystemMessage(Component.literal(
                                        "[" + i + "] " + s.ownerName() + " - " + s.pathway() + " Seq " + s.sequence()
                                                + " (" + s.abilityIds().size() + " abilities)"
                                ).withStyle(ChatFormatting.LIGHT_PURPLE));
                            }
                            if (food != null) {
                                player.sendSystemMessage(Component.literal(
                                        "Reserve (food): " + food.ownerName() + " - " + food.pathway()
                                                + " Seq " + food.sequence()
                                ).withStyle(ChatFormatting.DARK_GREEN));
                            } else {
                                player.sendSystemMessage(Component.literal(
                                        "Reserve (food): empty"
                                ).withStyle(ChatFormatting.DARK_RED));
                            }
                            return 1;
                        })
                )
                .then(timerCommand())
        );
    }

    private enum TimerKind {
        FEED("Feeding", CreepingHungerItem.FEED_INTERVAL_TICKS, ModDataComponents.LAST_FED_TICK.get()),
        PRAYER("Prayer", AwakenedCreepingHungerItem.PRAYER_INTERVAL_TICKS, ModDataComponents.LAST_PRAYER_TICK.get());

        private final String label;
        private final long interval;
        private final DataComponentType<Long> component;

        TimerKind(String label, long interval, DataComponentType<Long> component) {
            this.label = label;
            this.interval = interval;
            this.component = component;
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> timerCommand() {
        return Commands.literal("timer")
                .requires(source -> source.hasPermission(2))
                .then(timerNode("feed", TimerKind.FEED))
                .then(timerNode("prayer", TimerKind.PRAYER));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> timerNode(String name, TimerKind kind) {
        return Commands.literal(name)
                .then(Commands.literal("set")
                        .then(Commands.argument("time", TimeArgument.time(0))
                                .executes(context -> applyTimer(context, kind, false))))
                .then(Commands.literal("add")
                        .then(Commands.argument("time", TimeArgument.time(Integer.MIN_VALUE))
                                .executes(context -> applyTimer(context, kind, true))));
    }

    private static int applyTimer(CommandContext<CommandSourceStack> context, TimerKind kind, boolean add) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        ItemStack glove = findGlove(player);
        if (glove.isEmpty()) {
            source.sendFailure(Component.literal("You must be holding Creeping Hunger in your main or off hand."));
            return 0;
        }

        if (kind == TimerKind.PRAYER && !(glove.getItem() instanceof AwakenedCreepingHungerItem)) {
            source.sendFailure(Component.literal("Only the Awakened Creeping Hunger has a prayer timer."));
            return 0;
        }

        int time = IntegerArgumentType.getInteger(context, "time");
        long now = player.level().getGameTime();
        long last = glove.getOrDefault(kind.component, now);
        long updated = add ? last + time : now - (kind.interval - time);
        glove.set(kind.component, updated);
        SyncItemIntrospectPacket.sendTo(player, glove);

        long remaining = Math.max(0L, updated + kind.interval - now);
        source.sendSuccess(() -> Component.literal(kind.label + " timer " + (add ? "adjusted" : "set")
                + ": " + formatTicks(remaining) + " remaining.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static String formatTicks(long ticks) {
        long totalSeconds = ticks / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private static ItemStack findGlove(ServerPlayer player) {
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.getItem() instanceof CreepingHungerItem) return main;
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (off.getItem() instanceof CreepingHungerItem) return off;
        return ItemStack.EMPTY;
    }
}
