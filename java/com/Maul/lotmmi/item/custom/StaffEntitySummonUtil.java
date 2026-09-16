package com.Maul.lotmmi.item.custom;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.util.helper.subordinates.SubordinateUtils;
import de.jakob.lotm.util.scheduling.ServerScheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class StaffEntitySummonUtil {

    private static final String HISTORICAL_VOID_SUMMONING_ID = "historical_void_summoning_ability";

    public static long baseDurationTicks(int sequence) {
        int clamped = Math.max(1, Math.min(9, sequence));
        int seconds = 60 + (clamped - 1) * 30;
        return seconds * 20L;
    }

    public static double watchTimeMultiplier(long watchedTicks) {
        long unlock = StaffMemoryUtil.STARE_UNLOCK_TICKS;
        long max = StaffMemoryUtil.STARE_MAX_TICKS;
        double progress = Math.max(0, Math.min(1, (watchedTicks - unlock) / (double) (max - unlock)));
        return 0.6D + 0.4D * progress;
    }

    public static long actualDurationTicks(int sequence, long watchedTicks) {
        return Math.round(baseDurationTicks(sequence) * watchTimeMultiplier(watchedTicks));
    }

    public static boolean canSummonMore(ItemStack stack) {
        return StaffMemoryUtil.getActiveEntitySummons(stack).size() < StaffMemoryUtil.MAX_ACTIVE_ENTITY_SUMMONS;
    }

    public static void chargeCost(ServerLevel level, ServerPlayer player) {
        Ability historicalVoid = LOTMCraft.abilityHandler.getById(HISTORICAL_VOID_SUMMONING_ID);
        if (historicalVoid == null) return;
        float cost = historicalVoid.getInflatedSpiritualityCost(player, level);
        de.jakob.lotm.util.BeyonderData.reduceSpirituality(player, cost);
    }

    public static void summon(ServerLevel level, ServerPlayer player, ItemStack stack, StaffMemoryUtil.EntityWatch watch) {
        if (watch.watchedTicks() < StaffMemoryUtil.STARE_UNLOCK_TICKS) return;
        if (!canSummonMore(stack)) return;

        Optional<EntityType<?>> optionalType = EntityType.byString(watch.entityType());
        if (optionalType.isEmpty()) return;

        Entity entity = optionalType.get().create(level);
        if (entity == null) return;

        entity.load(watch.entityNbt());

        Vec3 pos = player.position().add(player.getLookAngle().scale(2));
        entity.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0);
        entity.setUUID(UUID.randomUUID());

        level.addFreshEntity(entity);
        entity.getPersistentData().putBoolean("VoidSummoned", true);

        if (entity instanceof LivingEntity livingEntity) {
            SubordinateUtils.turnEntityIntoSubordinate(livingEntity, player, false);
        }

        chargeCost(level, player);

        long durationTicks = actualDurationTicks(watch.sequence(), watch.watchedTicks());
        long expiryTick = level.getGameTime() + durationTicks;
        UUID summonedUUID = entity.getUUID();

        StaffMemoryUtil.addActiveEntitySummon(stack, new StaffMemoryUtil.ActiveEntitySummon(
                summonedUUID, watch.displayName(), watch.pathway(), watch.sequence(), expiryTick));

        ServerScheduler.scheduleDelayed((int) durationTicks, () -> {
            Entity toRemove = level.getEntity(summonedUUID);
            if (toRemove != null) toRemove.discard();
            StaffMemoryUtil.removeActiveEntitySummon(stack, summonedUUID);
        }, level);
    }

    public static void withdraw(ServerLevel level, ItemStack stack, UUID summonedUUID) {
        Entity entity = level.getEntity(summonedUUID);
        if (entity != null) entity.discard();
        StaffMemoryUtil.removeActiveEntitySummon(stack, summonedUUID);
    }
}
