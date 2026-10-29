package com.Maul.lotmmi.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public enum CharmType {
    SEA_AFFINITY("sea_affinity", "Sea Affinity",
            "Swim faster and breathe underwater for a few minutes."),
    ELECTRIC_SHOCK("electric_shock", "Electric Shock",
            "Shocks the creature you are facing. Stronger if it is wet."),
    GALE("gale", "Gale",
            "Pushes everything in front of you away."),
    SEA_WHISPER("sea_whisper", "Sea Whisper",
            "Nearby sea creatures glow and swim to you. Hostile ones stop attacking.");

    private final String id;
    private final String displayName;
    private final String description;

    CharmType(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public int modelIndex() {
        return ordinal() + 1;
    }

    public static CharmType byId(String id) {
        if (id == null) return null;
        for (CharmType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return null;
    }

    public boolean apply(ServerLevel level, ServerPlayer player, float scale) {
        return switch (this) {
            case SEA_AFFINITY -> applySeaAffinity(level, player, scale);
            case ELECTRIC_SHOCK -> applyElectricShock(level, player, scale);
            case GALE -> applyGale(level, player, scale);
            case SEA_WHISPER -> applySeaWhisper(level, player, scale);
        };
    }

    private static void say(ServerPlayer player, String text, ChatFormatting color) {
        player.displayClientMessage(Component.literal(text).withStyle(color), true);
    }

    private static boolean applySeaAffinity(ServerLevel level, ServerPlayer player, float scale) {
        int duration = Math.max(200, Math.round(20 * 180 * scale));
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, duration, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, duration, 0, false, true, true));
        level.sendParticles(ParticleTypes.BUBBLE, player.getX(), player.getY() + 1.0, player.getZ(),
                20, 0.4, 0.6, 0.4, 0.02);
        say(player, "You swim faster and can breathe underwater.", ChatFormatting.AQUA);
        return true;
    }

    private static boolean applyElectricShock(ServerLevel level, ServerPlayer player, float scale) {
        LivingEntity target = findTargetInFront(level, player, 8.0, 0.85);
        if (target == null) {
            say(player, "There is nothing to shock.", ChatFormatting.GRAY);
            return false;
        }

        float damage = 4.0F * scale;
        if (target.isInWaterOrRain()) damage *= 1.5F;

        target.hurt(level.damageSources().lightningBolt(), damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true, true));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(),
                24, 0.3, 0.5, 0.3, 0.1);
        say(player, "Lightning leaps from the charm.", ChatFormatting.YELLOW);
        return true;
    }

    private static boolean applyGale(ServerLevel level, ServerPlayer player, float scale) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        Vec3 direction = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : look.normalize();

        double range = 10.0;
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(3.0);
        double strength = 1.4 * scale;

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            Vec3 offset = entity.position().subtract(player.position());
            double distance = offset.length();
            if (distance > range || distance < 0.001) continue;
            if (offset.normalize().dot(look.normalize()) < 0.5) continue;

            entity.setDeltaMovement(entity.getDeltaMovement().add(direction.x * strength, 0.35, direction.z * strength));
            entity.hurtMarked = true;
        }

        Vec3 eye = player.getEyePosition();
        for (int i = 1; i <= 8; i++) {
            Vec3 point = eye.add(look.scale(i));
            level.sendParticles(ParticleTypes.CLOUD, point.x, point.y, point.z, 3, 0.3, 0.3, 0.3, 0.05);
        }
        say(player, "You unleash a gale.", ChatFormatting.WHITE);
        return true;
    }

    private static boolean applySeaWhisper(ServerLevel level, ServerPlayer player, float scale) {
        AABB area = player.getBoundingBox().inflate(24.0);
        int glowTicks = Math.max(100, Math.round(20 * 20 * scale));
        int heard = 0;

        for (Mob mob : level.getEntitiesOfClass(Mob.class, area, CharmType::isSeaCreature)) {
            heard++;
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, glowTicks, 0, false, false, false));

            if (mob instanceof Enemy) {
                if (mob.getTarget() == player) mob.setTarget(null);
            } else {
                mob.getNavigation().moveTo(player, 1.0D);
            }
        }

        if (heard == 0) {
            say(player, "No sea creatures nearby.", ChatFormatting.GRAY);
            return false;
        }

        say(player, "The sea creatures answer your call.", ChatFormatting.AQUA);
        return true;
    }

    private static boolean isSeaCreature(Mob mob) {
        MobCategory category = mob.getType().getCategory();
        return category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE
                || category == MobCategory.AXOLOTLS
                || mob instanceof Guardian
                || mob instanceof Drowned;
    }

    private static LivingEntity findTargetInFront(ServerLevel level, ServerPlayer player, double range, double minDot) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0);

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            Vec3 offset = entity.getBoundingBox().getCenter().subtract(eye);
            double distance = offset.length();
            if (distance > range || distance < 0.001) continue;
            if (offset.normalize().dot(look) < minDot) continue;

            if (distance < bestDistance) {
                best = entity;
                bestDistance = distance;
            }
        }
        return best;
    }
}
