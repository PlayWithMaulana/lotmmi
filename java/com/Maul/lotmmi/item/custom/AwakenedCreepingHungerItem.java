package com.Maul.lotmmi.item.custom;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AwakenedCreepingHungerItem extends CreepingHungerItem {

    public static final int AWAKENED_MAX_SLOTS = 7;

    public static final Map<UUID, PendingGraze> PENDING = new HashMap<>();

    public record PendingGraze(String targetName, String targetUUID, String pathway, int sequence,
                                List<Ability> candidates) {}

    public AwakenedCreepingHungerItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxSlots() {
        return AWAKENED_MAX_SLOTS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static void grazeSoul(ServerPlayer player, LivingEntity target, ItemStack glove) {
        if (!BeyonderData.isBeyonder(target)) {
            player.sendSystemMessage(Component.literal("This target has no soul worth grazing.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        List<CreepingHungerItem.SoulSlot> souls = prepareGrazeSlots(player, glove, target, AWAKENED_MAX_SLOTS);
        if (souls == null) return;

        String targetPathway = BeyonderData.getPathway(target);
        int targetSequence = BeyonderData.getSequence(target);

        List<Ability> candidates = LOTMCraft.abilityHandler.getByPathwayAndSequence(targetPathway, targetSequence)
                .stream()
                .filter(a -> a.canBeCopied)
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            player.sendSystemMessage(Component.literal("This soul's power resists being grazed.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        PENDING.put(player.getUUID(), new PendingGraze(
                target.getName().getString(), target.getUUID().toString(),
                targetPathway, targetSequence, candidates));

        player.sendSystemMessage(Component.literal(
                "Grazed over " + target.getName().getString() + "'s soul."
        ).withStyle(ChatFormatting.LIGHT_PURPLE));

        List<String> candidateIds = candidates.stream().map(Ability::getId).toList();
        String targetName = target.getName().getString();
        player.openMenu(new com.Maul.lotmmi.gui.GrazeChoiceMenuProvider(targetName, targetPathway, targetSequence, candidateIds),
                buf -> {
                    buf.writeUtf(targetName);
                    buf.writeUtf(targetPathway);
                    buf.writeVarInt(targetSequence);
                    buf.writeVarInt(candidateIds.size());
                    for (String id : candidateIds) buf.writeUtf(id);
                });
    }

    public static void confirmGrazeChoice(ServerPlayer player, ItemStack glove, PendingGraze pending, Ability chosen) {
        List<CreepingHungerItem.SoulSlot> souls = getSouls(glove);
        if (souls.size() >= AWAKENED_MAX_SLOTS) {
            player.sendSystemMessage(Component.literal("No open slot remains for that soul.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        List<Ability> granted = new ArrayList<>();
        granted.add(chosen);

        List<Ability> excluded = new ArrayList<>();
        excluded.add(chosen);
        granted.addAll(rollAbilities(pending.pathway(), pending.sequence(), ABILITIES_PER_SOUL - 1, excluded));

        finalizeGraze(player, glove, pending.targetName(), pending.targetUUID(),
                pending.pathway(), pending.sequence(), souls, granted);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Awakened - 7 soul slots, and grazing lets you choose")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("one ability yourself; two more are still rolled at random")
                .withStyle(ChatFormatting.GOLD));
    }
}
