package org.wilkretawesomesauce.alchemancystuck.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Purely a "pathfinding brain" for {@link RageCloneEntity} - registered with
 * {@code clientTrackingRange(0)} (see {@code AddonEntities}), so no client ever even learns this
 * exists, and {@link #canBeSeenByAnyone} is hard-overridden false so no other mob's target selector
 * can lock onto it server-side either. It never fights; {@link RageCloneEntity#tick} mirrors this
 * entity's position/rotation onto itself every tick and does the actual attacking there instead.
 *
 * <p>The split exists because neither half of "real combat, real pathfinding" can be gotten from a
 * single entity: {@code Player} (what {@link RageCloneEntity} now is, so that
 * {@code ItemStack#hurtEnemy} - and by extension custom on-hit weapon effects - actually fires) has no
 * {@code PathNavigation}/{@code GoalSelector}, while {@code Mob} (what this is) can never reach that
 * same Player-only combat hook no matter what. This mob does the legs, the clone does the fists.
 */
public class RagePathBrainEntity extends PathfinderMob {

    @Nullable
    private UUID puppetUUID;
    @Nullable
    private UUID ownerUUID;

    public RagePathBrainEntity(EntityType<? extends RagePathBrainEntity> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
        this.setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new RageChaseGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new RageBreakBlockGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.2D));
        this.targetSelector.addGoal(1, new RageTargetGoal(this));
    }

    /**
     * Default {@code Mob.createNavigation} returns {@code GroundPathNavigation}, whose {@code
     * hasValidPathType} hardcodes {@code PathType.WATER -> false} - an absolute veto, not just a heavy
     * malus, so a chase across/through water was previously impossible to path at all (the goal would
     * just fail to find a path and give up at the shore). {@code AmphibiousPathNavigation} is vanilla's
     * own navigator for mobs meant to path over both land and water. No {@code FloatGoal} alongside
     * this - its constant surface-seeking jump impulse would fight a chase that actually needs to dive
     * after a submerged target, and this entity is already invulnerable, so it was never protecting
     * against drowning damage in the first place.
     */
    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    /**
     * Tells this brain which entities are off-limits as targets - its own puppet body and the
     * spectating owner, both of which are otherwise perfectly ordinary, alive {@code LivingEntity}s
     * sitting right next to it and would get picked up by a plain nearest-target search otherwise.
     */
    public void setPuppet(RageCloneEntity puppet, ServerPlayer owner) {
        this.puppetUUID = puppet.getUUID();
        this.ownerUUID = owner.getUUID();
    }

    boolean isValidTarget(LivingEntity entity) {
        return !entity.getUUID().equals(this.puppetUUID) && !entity.getUUID().equals(this.ownerUUID);
    }

    @Override
    public boolean canBeSeenByAnyone() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }
}
