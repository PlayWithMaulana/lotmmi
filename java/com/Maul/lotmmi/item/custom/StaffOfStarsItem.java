package com.Maul.lotmmi.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

public class StaffOfStarsItem extends SwordItem {

    private static final double WITNESS_RADIUS = 10.0D;
    private static final int WITNESS_TICK_INTERVAL = 20;
    private static final double STARE_CONE_COSINE = 0.85D;

    private static final Random RANDOM = new Random();

    private static final double PASSIVE_TELEPORT_CHANCE = 0.001D;
    private static final double PASSIVE_SUMMON_CHANCE = 0.0005D;

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

        ServerLevel serverLevel = (ServerLevel) level;

        AABB range = player.getBoundingBox().inflate(WITNESS_RADIUS);
        Vec3 look = player.getLookAngle();
        List<LivingEntity> nearbyEntities = serverLevel.getEntitiesOfClass(LivingEntity.class, range,
                e -> e != player && e.isAlive());

        for (LivingEntity seen : nearbyEntities) {
            Vec3 toEntity = seen.position().subtract(player.getEyePosition()).normalize();
            if (look.dot(toEntity) < STARE_CONE_COSINE) continue;
            if (!player.hasLineOfSight(seen)) continue;
            StaffMemoryUtil.addStareTicks(stack, seen, WITNESS_TICK_INTERVAL);
        }

        rollPassiveDownsides(serverLevel, player, stack);
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

        double summonChance = PanicUtil.scale(PASSIVE_SUMMON_CHANCE, player);
        if (RANDOM.nextDouble() < summonChance && StaffEntitySummonUtil.canSummonMore(stack)) {
            List<StaffMemoryUtil.EntityWatch> eligible = StaffMemoryUtil.getEntityWatches(stack).stream()
                    .filter(w -> w.watchedTicks() >= StaffMemoryUtil.STARE_UNLOCK_TICKS)
                    .toList();
            if (!eligible.isEmpty()) {
                StaffMemoryUtil.EntityWatch chosen = eligible.get(RANDOM.nextInt(eligible.size()));
                StaffEntitySummonUtil.summon(level, player, stack, chosen);
            }
        }
    }
}
