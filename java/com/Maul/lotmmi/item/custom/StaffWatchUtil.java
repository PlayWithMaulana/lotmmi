package com.Maul.lotmmi.item.custom;

import de.jakob.lotm.util.BeyonderData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StaffWatchUtil {
    private record Stare(UUID target, int seconds) {}

    private static final Map<UUID, Stare> STARES = new HashMap<>();

    private static final double RANGE = 24.0D;
    private static final double MIN_DOT = 0.985D;

    public static void tick(ServerLevel level, ServerPlayer player, ItemStack staff) {
        LivingEntity target = findLookedAt(level, player);
        if (target == null) {
            STARES.remove(player.getUUID());
            return;
        }

        Stare previous = STARES.get(player.getUUID());
        int seconds = previous != null && previous.target().equals(target.getUUID()) ? previous.seconds() + 1 : 1;
        STARES.put(player.getUUID(), new Stare(target.getUUID(), seconds));

        long staredTicks = Math.min(StaffMemoryUtil.STARE_MAX_TICKS, seconds * 20L);
        if (staredTicks < StaffMemoryUtil.STARE_UNLOCK_TICKS) return;

        List<StaffMemoryUtil.EntityWatch> watches = StaffMemoryUtil.getEntityWatches(staff);
        StaffMemoryUtil.EntityWatch existing = null;
        for (StaffMemoryUtil.EntityWatch watch : watches) {
            if (watch.sourceUUID().equals(target.getUUID())) {
                existing = watch;
                break;
            }
        }
        if (existing == null && watches.size() >= StaffMemoryUtil.MAX_WATCHES) return;
        if (existing != null && existing.watchedTicks() >= staredTicks) return;

        boolean beyonder = BeyonderData.isBeyonder(target);
        String pathway = beyonder ? BeyonderData.getPathway(target) : "none";
        int sequence = beyonder ? BeyonderData.getSequence(target) : 9;

        CompoundTag nbt = new CompoundTag();
        target.saveWithoutId(nbt);

        StaffMemoryUtil.putWatch(staff, new StaffMemoryUtil.EntityWatch(target.getUUID(),
                String.valueOf(EntityType.getKey(target.getType())), nbt,
                target.getName().getString(), pathway, sequence, staredTicks));

        if (existing == null) {
            player.displayClientMessage(Component.literal("The Staff records the likeness of " + target.getName().getString() + ".")
                    .withStyle(ChatFormatting.AQUA), true);
        }
    }

    private static LivingEntity findLookedAt(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB box = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(2.0);

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof Player))) {
            Vec3 offset = entity.getBoundingBox().getCenter().subtract(eye);
            double distance = offset.length();
            if (distance > RANGE || distance < 0.001) continue;
            if (offset.normalize().dot(look) < MIN_DOT) continue;
            if (!player.hasLineOfSight(entity)) continue;

            if (distance < bestDistance) {
                best = entity;
                bestDistance = distance;
            }
        }
        return best;
    }

}
