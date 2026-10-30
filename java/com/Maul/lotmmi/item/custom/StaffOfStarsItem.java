package com.Maul.lotmmi.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.Maul.lotmmi.config.LotmmiConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Random;

public class StaffOfStarsItem extends SwordItem {
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    private static final int WITNESS_TICK_INTERVAL = 20;
    private static final int OFFHAND_SLOT = 40;
    private static final int SLIP_INTERVAL_TICKS = 1200;

    private static final Random RANDOM = new Random();

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
            applyStrangeEffect(target, attacker);
        }
        return result;
    }

    private static void applyStrangeEffect(LivingEntity target, LivingEntity attacker) {
        int roll = RANDOM.nextInt(100);

        if (roll < 40) {
            target.addEffect(new MobEffectInstance(de.jakob.lotm.effect.ModEffects.MUTATED, MUTATION_DURATION_TICKS, 0), attacker);
        } else if (roll < 52) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, 1), attacker);
        } else if (roll < 62) {
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20 * 3, 0), attacker);
        } else if (roll < 72) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 6, 0), attacker);
        } else if (roll < 80) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 8, 0), attacker);
        } else if (roll < 87) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 10, 0), attacker);
        } else if (roll < 93) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 3, 0), attacker);
        } else {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double distance = 4.0 + RANDOM.nextDouble() * 6.0;
            target.randomTeleport(target.getX() + Math.cos(angle) * distance, target.getY(),
                    target.getZ() + Math.sin(angle) * distance, true);
        }
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

        boolean sealed = StaffSealUtil.isSealed(level);
        boolean held = isSelected || slotId == OFFHAND_SLOT;

        if (held) {
            if (level.getGameTime() % 4 == 0) {
                ArtifactEffects.staffAura((ServerLevel) level, player, PanicUtil.getAmplifier(player), sealed, level.getGameTime());
            }
            if (level.getGameTime() % WITNESS_TICK_INTERVAL == 0) {
                StaffWatchUtil.tick((ServerLevel) level, player, stack);
            }
        }

        if (sealed) return;

        if (!held) {
            rollSlip((ServerLevel) level, player, stack, slotId);
            return;
        }

        if (level.getGameTime() % WITNESS_TICK_INTERVAL != 0) return;
        StaffAnomalyUtil.rollIntrusive((ServerLevel) level, player, stack);
    }

    private static void rollSlip(ServerLevel level, ServerPlayer player, ItemStack stack, int slotId) {
        if (level.getGameTime() % SLIP_INTERVAL_TICKS != 0) return;
        if (RANDOM.nextDouble() >= LotmmiConfig.STAFF_SLIP_CHANCE_STOWED.get()) return;
        if (level.dimensionType().hasCeiling()) return;

        ItemStack copy = stack.copy();
        net.minecraft.world.entity.item.ItemEntity dropped =
                new net.minecraft.world.entity.item.ItemEntity(level, player.getX(), player.getY() + 1.0, player.getZ(), copy);
        level.addFreshEntity(dropped);

        if (StaffSealUtil.slipAway(level, dropped)) {
            player.getInventory().setItem(slotId, ItemStack.EMPTY);
            player.displayClientMessage(Component.literal("The Staff of the Stars slipped out of your pack.")
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
        } else {
            dropped.discard();
        }
    }
}
