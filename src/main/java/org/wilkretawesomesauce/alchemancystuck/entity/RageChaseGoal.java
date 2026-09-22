package org.wilkretawesomesauce.alchemancystuck.entity;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Vanilla's own {@code MeleeAttackGoal} hardcodes a flat 20-tick (1s) cooldown between attacks
 * regardless of what's held, and bundles the chase movement together with landing the actual hit.
 * This keeps only the chase/pathing/look-at half (adapted from vanilla's source, trimmed of the
 * pathfinding-penalty bookkeeping that only matters when many mobs path at once) - landing the hit is
 * {@link RageCloneEntity}'s job now, since only a real {@code Player} can call the real
 * {@code Player#attack} (sweep, crits, custom on-hit weapon effects and all) that this addon's whole
 * FakePlayer rewrite exists to reach. This goal just gets the brain's body next to the target and
 * facing it; {@code RageCloneEntity#tick} checks {@link RagePathBrainEntity#isWithinMeleeAttackRange}
 * itself each tick to decide when to actually swing.
 */
public class RageChaseGoal extends Goal {

    private final RagePathBrainEntity mob;
    private final double speedModifier;

    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;

    public RageChaseGoal(RagePathBrainEntity mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive())
            return false;

        this.path = this.mob.getNavigation().createPath(target, 0);
        return this.path != null || this.mob.isWithinMeleeAttackRange(target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive() || !this.mob.isWithinRestriction(target.blockPosition()))
            return false;

        return !(target instanceof Player player) || !player.isSpectator() && !player.isCreative();
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.path, this.speedModifier);
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
    }

    @Override
    public void stop() {
        LivingEntity target = this.mob.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target))
            this.mob.setTarget(null);

        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null)
            return;

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);

        if (this.mob.getSensing().hasLineOfSight(target) && this.ticksUntilNextPathRecalculation <= 0
                && (this.pathedTargetX == 0.0 && this.pathedTargetY == 0.0 && this.pathedTargetZ == 0.0
                    || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0
                    || this.mob.getRandom().nextFloat() < 0.05F)) {
            this.pathedTargetX = target.getX();
            this.pathedTargetY = target.getY();
            this.pathedTargetZ = target.getZ();
            this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);

            if (!this.mob.getNavigation().moveTo(target, this.speedModifier))
                this.ticksUntilNextPathRecalculation += 15;
        }
    }
}
