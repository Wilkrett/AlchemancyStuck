package org.wilkretawesomesauce.alchemancystuck.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public class RageTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    private static final double RETARGET_MARGIN = 2.0D;

    private final RagePathBrainEntity brain;

    public RageTargetGoal(RagePathBrainEntity mob) {
        super(mob, LivingEntity.class, 1, true, false, mob::isValidTarget);
        this.brain = mob;
    }

    @Override
    public void tick() {
        LivingEntity current = this.brain.getTarget();
        if (current == null || !current.isAlive())
            return;

        this.findTarget();
        if (this.target != null && this.target != current) {
            double currentDist = Math.sqrt(this.brain.distanceToSqr(current));
            double candidateDist = Math.sqrt(this.brain.distanceToSqr(this.target));
            if (candidateDist + RETARGET_MARGIN < currentDist)
                this.brain.setTarget(this.target);
        }
    }
}
