package com.Maul.lotmmi.entity.goals;

import com.Maul.lotmmi.entity.QilangosEntity;
import com.Maul.lotmmi.entity.QilangosSouls;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.util.helper.AbilityUtil;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class QilangosSoulAbilityGoal extends Goal {

    private static final int MIN_DELAY = 50;
    private static final int DELAY_SPREAD = 60;

    private final QilangosEntity mob;
    private final Random random = new Random();
    private int delay = MIN_DELAY;

    public QilangosSoulAbilityGoal(QilangosEntity mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        return this.mob.getTarget() != null && this.mob.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.delay-- > 0) return;

        LivingEntity target = this.mob.getTarget();
        if (target == null || !(this.mob.level() instanceof ServerLevel level)) return;

        this.delay = MIN_DELAY + this.random.nextInt(DELAY_SPREAD);

        List<QilangosSouls.SoulAbility> pool = new ArrayList<>(QilangosSouls.resolve());
        Collections.shuffle(pool, this.random);
        for (QilangosSouls.SoulAbility entry : pool) {
            if (this.tryCast(level, target, entry)) return;
        }
    }

    private boolean tryCast(ServerLevel level, LivingEntity target, QilangosSouls.SoulAbility entry) {
        Ability ability = entry.ability();

        if (!ability.canBeUsedByNPC) {
            return this.mob.castCustom(ability.getId(), target);
        }
        if (!ability.canUse(this.mob, false, false, false) || !ability.shouldUseAbility(this.mob)) {
            return false;
        }

        this.mob.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        AbilityUtil.setArtifactScaling(this.mob, entry.pathway(), entry.sequence());
        try {
            ability.useAbility(level, this.mob, false, false, false, false);
        } finally {
            AbilityUtil.removeArtifactScaling(this.mob);
        }
        return true;
    }
}
