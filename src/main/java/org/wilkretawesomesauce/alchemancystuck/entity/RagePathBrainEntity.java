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

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

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
