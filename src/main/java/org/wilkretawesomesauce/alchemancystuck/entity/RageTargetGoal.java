package org.wilkretawesomesauce.alchemancystuck.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * Vanilla's {@code NearestAttackableTargetGoal} only re-picks a target when the current one becomes
 * invalid (dead, out of range, out of sight too long) - it never switches away from a live, in-range
 * target just because something else got closer. That reads wrong for mindless rage: getting knocked
 * back into a different mob mid-horde-fight should make the clone turn on whatever's actually next to
 * it, not keep chasing the original target clear across the group.
 *
 * <p>An earlier version of this forced that by overriding {@link #canContinueToUse} to return false
 * whenever a closer target existed - which caused the goal to stop and immediately restart, and
 * {@code TargetGoal#stop} unconditionally does {@code mob.setTarget(null)} before
 * {@code start()} sets the new one. In a horde, where something is almost always marginally closer
 * after a knockback, that null was happening constantly, and {@code RageChaseGoal} skips its
 * move/attack for any tick where {@code mob.getTarget()} reads null - which looked like the clone
 * freezing in place right after every hit. {@link #tick} swaps the target directly instead, with no
 * stop/restart and no null in between.
 */
public class RageTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    /**
     * The new target has to be at least this much closer before switching - otherwise two roughly
     * equidistant mobs would flicker the target back and forth every tick.
     */
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
