package com.Maul.lotmmi.entity.goals;

import com.Maul.lotmmi.entity.QilangosEntity;
import de.jakob.lotm.util.helper.AbilityUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class AttackEverythingGoal extends TargetGoal {

    private static final int SCAN_INTERVAL = 10;

    private final TargetingConditions conditions;
    private int scanDelay;

    public AttackEverythingGoal(Mob mob) {
        super(mob, false);
        this.conditions = TargetingConditions.forCombat()
                .range(mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE))
                .selector(this::isValidTarget);
    }

    private boolean isValidTarget(LivingEntity entity) {
        return !(entity instanceof QilangosEntity)
                && !(entity instanceof ArmorStand)
                && entity.getType().getCategory() != MobCategory.MISC
                && entity.isAttackable()
                && AbilityUtil.mayTarget(this.mob, entity);
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() != null) return false;
        if (this.scanDelay > 0) {
            this.scanDelay--;
            return false;
        }
        this.scanDelay = SCAN_INTERVAL + this.mob.getRandom().nextInt(SCAN_INTERVAL);

        double range = this.getFollowDistance();
        AABB box = this.mob.getBoundingBox().inflate(range, Math.min(range, 8.0D), range);
        List<LivingEntity> nearby = this.mob.level().getEntitiesOfClass(LivingEntity.class, box);
        this.targetMob = this.mob.level().getNearestEntity(nearby, this.conditions, this.mob,
                this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
        return this.targetMob != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
