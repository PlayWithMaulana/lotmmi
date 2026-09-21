package com.Maul.lotmmi.item.custom;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.beyonders.artifacts.NegativeEffect;
import de.jakob.lotm.beyonders.artifacts.SealedArtifactData;
import de.jakob.lotm.data.ModDataComponents;
import de.jakob.lotm.util.helper.AbilityUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class StaffAbilityUtil {

    private static final Random RANDOM = new Random();
    private static final float BACKFIRE_DAMAGE = 4.0F;
    private static final double BACKFIRE_CHANCE_ON_FAIL = 0.2D;
    private static final int POTENCY_SEQUENCE_PENALTY = 1;

    public static void rebuildSealedArtifactData(ItemStack stack) {
        List<String> wheel = StaffMemoryUtil.getWheel(stack);
        List<Ability> abilities = new ArrayList<>();
        for (String id : wheel) {
            Ability ability = LOTMCraft.abilityHandler.getById(id);
            if (ability != null) abilities.add(ability);
        }
        SealedArtifactData data = new SealedArtifactData("hanged_man", 5, abilities, NegativeEffect.createDefault());
        stack.set(ModDataComponents.SEALED_ARTIFACT_DATA.get(), data);

        int selected = stack.getOrDefault(ModDataComponents.SEALED_ARTIFACT_SELECTED.get(), 0);
        if (selected >= abilities.size()) {
            stack.set(ModDataComponents.SEALED_ARTIFACT_SELECTED.get(), Math.max(0, abilities.size() - 1));
        }
    }

    public static void selectAbility(ItemStack stack, String abilityId) {
        SealedArtifactData data = stack.get(ModDataComponents.SEALED_ARTIFACT_DATA.get());
        if (data == null) return;
        for (int i = 0; i < data.abilities().size(); i++) {
            if (data.abilities().get(i).getId().equals(abilityId)) {
                stack.set(ModDataComponents.SEALED_ARTIFACT_SELECTED.get(), i);
                return;
            }
        }
    }

    public static void applyStaffScaling(ServerPlayer player, Ability ability) {
        Map<String, Integer> requirements = ability.getRequirements();
        if (requirements.isEmpty()) return;
        Map.Entry<String, Integer> requirement = requirements.entrySet().iterator().next();
        AbilityUtil.setArtifactScaling(player, requirement.getKey(),
                Math.min(9, requirement.getValue() + POTENCY_SEQUENCE_PENALTY));
    }

    public static boolean castSelected(ServerLevel level, ServerPlayer player, ItemStack stack) {
        SealedArtifactData data = stack.get(ModDataComponents.SEALED_ARTIFACT_DATA.get());
        if (data == null || data.abilities().isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "The staff has no reenactable abilities placed in its rotation yet."));
            return false;
        }

        int selectedIndex = stack.getOrDefault(ModDataComponents.SEALED_ARTIFACT_SELECTED.get(), 0);
        if (selectedIndex >= data.abilities().size()) selectedIndex = 0;
        Ability ability = data.abilities().get(selectedIndex);

        Map<String, Integer> requirements = ability.getRequirements();
        if (requirements.isEmpty()) return false;
        Map.Entry<String, Integer> requirement = requirements.entrySet().iterator().next();
        String pathway = requirement.getKey();
        int sequence = requirement.getValue();

        double accuracy = StaffMemoryUtil.getAccuracy(stack, ability.getId(), sequence);
        double failChance = PanicUtil.scale(1.0D - accuracy, player);

        AbilityUtil.setArtifactScaling(player, pathway, Math.min(9, sequence + POTENCY_SEQUENCE_PENALTY));

        if (RANDOM.nextDouble() >= failChance) {
            ability.useAbility(level, player, true, false, true, false);
        } else {
            PanicUtil.escalate(player);
            if (RANDOM.nextDouble() < BACKFIRE_CHANCE_ON_FAIL) {
                player.hurt(player.damageSources().magic(), BACKFIRE_DAMAGE);
            }
        }
        return true;
    }
}
