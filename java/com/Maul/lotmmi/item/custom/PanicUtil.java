package com.Maul.lotmmi.item.custom;

import com.Maul.lotmmi.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class PanicUtil {
    private static final int DURATION_TICKS = 20 * 60;
    private static final int MAX_AMPLIFIER = 4;

    private static boolean isNegated(LivingEntity entity) {
        return entity.getMainHandItem().getItem() instanceof SeaGodScepterItem
                || entity.getOffhandItem().getItem() instanceof SeaGodScepterItem;
    }

    public static void escalate(LivingEntity entity) {
        if (isNegated(entity)) return;

        int currentAmplifier = -1;
        MobEffectInstance current = entity.getEffect(ModEffects.PANIC);
        if (current != null) currentAmplifier = current.getAmplifier();

        int nextAmplifier = Math.min(MAX_AMPLIFIER, currentAmplifier + 1);
        entity.addEffect(new MobEffectInstance(ModEffects.PANIC, DURATION_TICKS, nextAmplifier, false, true, true));
    }

    public static void calm(LivingEntity entity) {
        MobEffectInstance current = entity.getEffect(ModEffects.PANIC);
        if (current == null) return;

        int next = current.getAmplifier() - 1;
        entity.removeEffect(ModEffects.PANIC);
        if (next >= 0) {
            entity.addEffect(new MobEffectInstance(ModEffects.PANIC, DURATION_TICKS, next, false, true, true));
        }
    }

    public static int getAmplifier(LivingEntity entity) {
        if (isNegated(entity)) return -1;

        MobEffectInstance current = entity.getEffect(ModEffects.PANIC);
        return current == null ? -1 : current.getAmplifier();
    }

    public static double getMultiplier(LivingEntity entity) {
        if (isNegated(entity)) return 1.0D;

        MobEffectInstance current = entity.getEffect(ModEffects.PANIC);
        if (current == null) return 1.0D;
        return Math.pow(2, current.getAmplifier() + 1);
    }

    public static double scale(double baseChance, LivingEntity entity) {
        return Math.min(1.0D, baseChance * getMultiplier(entity));
    }
}
