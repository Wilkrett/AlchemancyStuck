package org.wilkretawesomesauce.alchemancystuck.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.common.ItemAbilities;

import java.util.EnumSet;

/**
 * Vanilla's own {@code MeleeAttackGoal} hardcodes a flat 20-tick (1s) cooldown between attacks
 * regardless of what's held - its cooldown field is {@code private}, not {@code protected}, so there's
 * no way to override just the duration by subclassing it (a subclass's own
 * {@code resetAttackCooldown()} override can't write to a field it can't see). This reimplements the
 * same chase-and-attack behavior (adapted from the vanilla source, trimmed of the
 * pathfinding-penalty bookkeeping that only matters when many mobs path at once), but times attacks
 * off the clone's real {@link Attributes#ATTACK_SPEED} - which now actually reflects whatever's in its
 * mainhand, since equipment attribute modifiers only apply once the base attribute is registered on
 * the entity (see {@code RageCloneEntity#createAttributes}). Fists (attack speed 4.0) throw roughly 4x
 * faster than vanilla's fixed pace; a slow weapon paces itself down accordingly.
 *
 * <p>{@link #trySweepAttack} additionally reimplements the sword sweep - {@code Mob#doHurtTarget}
 * (what actually lands the clone's hits) is a much simpler generic method than {@code Player#attack},
 * and skips every player-specific combat mechanic entirely, sweep included. The sweep-eligibility
 * check, hitbox, and enchant-scaling are copied from {@code Player#attack}'s own logic, adapted to a
 * generic {@code LivingEntity} attacker instead of a {@code Player} (a couple of the vanilla helpers
 * this leans on, like the sweep hitbox itself, are NeoForge extension methods that only accept a
 * {@code Player} - their actual default implementation doesn't use the player parameter for anything,
 * so the formula is inlined here rather than worked around).
 */
public class RageMeleeAttackGoal extends Goal {

    private final RageCloneEntity mob;
    private final double speedModifier;

    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int ticksUntilNextAttack;

    public RageMeleeAttackGoal(RageCloneEntity mob, double speedModifier) {
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
        this.ticksUntilNextAttack = 0;
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

        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        this.checkAndPerformAttack(target);
    }

    private void checkAndPerformAttack(LivingEntity target) {
        if (this.ticksUntilNextAttack <= 0 && this.mob.isWithinMeleeAttackRange(target) && this.mob.getSensing().hasLineOfSight(target)) {
            this.ticksUntilNextAttack = attackIntervalTicks();
            this.mob.swing(InteractionHand.MAIN_HAND);
            if (this.mob.doHurtTarget(target))
                trySweepAttack(target);
        }
    }

    private int attackIntervalTicks() {
        double attackSpeed = this.mob.getAttributeValue(Attributes.ATTACK_SPEED);
        return attackSpeed > 0.0 ? Math.max(1, Math.round(20.0F / (float) attackSpeed)) : 20;
    }

    /**
     * Mirrors {@code Player#attack}'s sweep branch: eligible only on ground with a sword-capable
     * mainhand item, hits every non-allied {@code LivingEntity} in a box around the primary target
     * (not just directly in front - matches vanilla's own hitbox, which is that loose), with
     * {@code 1 + sweepingRatio * attackDamage} damage run through the normal enchant-damage pipeline.
     */
    private void trySweepAttack(LivingEntity primaryTarget) {
        if (!this.mob.onGround() || !(this.mob.level() instanceof ServerLevel serverLevel))
            return;

        ItemStack weapon = this.mob.getMainHandItem();
        if (!weapon.canPerformAction(ItemAbilities.SWORD_SWEEP))
            return;

        float attackDamage = (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float sweepDamage = 1.0F + (float) this.mob.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * attackDamage;
        DamageSource damageSource = this.mob.damageSources().mobAttack(this.mob);

        double knockbackX = -Mth.sin(this.mob.getYRot() * 0.017453292F);
        double knockbackZ = Mth.cos(this.mob.getYRot() * 0.017453292F);

        for (LivingEntity nearby : this.mob.level().getEntitiesOfClass(LivingEntity.class, primaryTarget.getBoundingBox().inflate(1.0, 0.25, 1.0))) {
            if (nearby == this.mob || nearby == primaryTarget || this.mob.isAlliedTo(nearby))
                continue;
            if (nearby instanceof ArmorStand armorStand && armorStand.isMarker())
                continue;

            float enchantedSweepDamage = EnchantmentHelper.modifyDamage(serverLevel, weapon, nearby, damageSource, sweepDamage);
            nearby.knockback(0.4000000059604645, knockbackX, knockbackZ);
            if (nearby.hurt(damageSource, enchantedSweepDamage))
                EnchantmentHelper.doPostAttackEffects(serverLevel, nearby, damageSource);
        }

        this.mob.level().playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.mob.getSoundSource(), 1.0F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, this.mob.getX() + knockbackX, this.mob.getY(0.5), this.mob.getZ() + knockbackZ, 0, knockbackX, 0.0, knockbackZ, 0.0);
    }
}
