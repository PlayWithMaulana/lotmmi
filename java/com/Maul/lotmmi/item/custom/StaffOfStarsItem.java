package com.Maul.lotmmi.item.custom;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

public class StaffOfStarsItem extends SwordItem {

    private static final int WITNESS_TICK_INTERVAL = 20;

    private static final Random RANDOM = new Random();

    private static final double PASSIVE_TELEPORT_CHANCE = 0.001D;
    private static final int PANIC_RANDOM_ABILITY_MIN_AMPLIFIER = 3;
    private static final double PANIC_RANDOM_ABILITY_CHANCE = 0.0008D;

    private static final int MUTATION_DURATION_TICKS = 20 * 8;

    public StaffOfStarsItem(Properties properties) {
        super(Tiers.NETHERITE, properties.attributes(createStaffAttributes()));
    }

    private static ItemAttributeModifiers createStaffAttributes() {
        return SwordItem.createAttributes(Tiers.NETHERITE, 5.0F, -2.4F);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!target.level().isClientSide) {
            target.addEffect(new MobEffectInstance(de.jakob.lotm.effect.ModEffects.MUTATED, MUTATION_DURATION_TICKS, 0), attacker);
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        if (player.isShiftKeyDown()) {
            StaffTeleportUtil.openMenu(serverPlayer, stack, hand == InteractionHand.MAIN_HAND);
            return InteractionResultHolder.success(stack);
        }

        boolean cast = StaffAbilityUtil.castSelected((ServerLevel) level, serverPlayer, stack);
        return cast ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!isSelected) return;
        if (level.getGameTime() % WITNESS_TICK_INTERVAL != 0) return;

        rollPassiveDownsides((ServerLevel) level, player, stack);
    }

    private void rollPassiveDownsides(ServerLevel level, ServerPlayer player, ItemStack stack) {
        double teleportChance = PanicUtil.scale(PASSIVE_TELEPORT_CHANCE, player);
        if (RANDOM.nextDouble() < teleportChance) {
            List<TeleportSlotUtil.Slot> slots = TeleportSlotUtil.getSlots(stack);
            if (!slots.isEmpty() && RANDOM.nextBoolean()) {
                StaffTeleportUtil.teleportToSlot(player, slots.get(RANDOM.nextInt(slots.size())));
            } else {
                StaffTeleportUtil.teleportToRandomPlace(player);
            }
        }

        if (PanicUtil.getAmplifier(player) >= PANIC_RANDOM_ABILITY_MIN_AMPLIFIER
                && RANDOM.nextDouble() < PANIC_RANDOM_ABILITY_CHANCE) {
            List<String> wheel = StaffMemoryUtil.getWheel(stack);
            if (!wheel.isEmpty()) {
                String abilityId = wheel.get(RANDOM.nextInt(wheel.size()));
                Ability ability = LOTMCraft.abilityHandler.getById(abilityId);
                if (ability != null) {
                    player.sendSystemMessage(Component.literal(
                            "The stars flicker without your command - " + ability.getName().getString() + " tears loose."
                    ).withStyle(ChatFormatting.LIGHT_PURPLE));
                    StaffAbilityUtil.applyStaffScaling(player, ability);
                    try {
                        ability.useAbility(level, player, false, false, true, false);
                    } finally {
                        de.jakob.lotm.util.helper.AbilityUtil.removeArtifactScaling(player);
                    }
                }
            }
        }
    }
}
