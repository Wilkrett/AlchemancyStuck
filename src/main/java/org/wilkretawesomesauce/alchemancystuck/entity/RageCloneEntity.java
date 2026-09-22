package org.wilkretawesomesauce.alchemancystuck.entity;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.jetbrains.annotations.Nullable;
import org.wilkretawesomesauce.alchemancystuck.effect.RageEffect;

import java.util.Optional;
import java.util.UUID;

/**
 * The "AI" that takes over during a rage: a throwaway body that copies the owning player's current
 * combat stats and gear and fights on their behalf while the real player watches from spectator mode.
 * This class only carries the clone's identity, stats, and combat AI - spawning it, granting it its
 * own copy of {@link RageEffect} (for the actual combat buffs), the countdown, and end-of-rage cleanup
 * all live in {@code RageEffect} itself. Does not persist across restarts by design - if the server
 * goes down mid-rage, {@code RageEffect}'s login safety net restores the player rather than trying to
 * resume the clone.
 */
public class RageCloneEntity extends PathfinderMob {

    /**
     * Synced to the client purely so {@code RageCloneRenderer} can look up and render the owner's
     * real skin - the plain {@link #ownerUUID} field below is server-only and not visible to the
     * client at all (regular Java fields on an Entity subclass don't sync themselves).
     */
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID = SynchedEntityData.defineId(RageCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    @Nullable
    private UUID ownerUUID;

    public RageCloneEntity(EntityType<? extends RageCloneEntity> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, Optional.empty());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.ATTACK_SPEED, 4.0) // vanilla's own player default - deliberately not
                // copied from the owner in possess(), unlike the attributes below, so the clone's own
                // copied weapon (setItemSlot further down) is the only thing that ever modifies it;
                // copying the owner's already-weapon-modified value here would double the penalty/bonus
                .add(Attributes.MOVEMENT_SPEED, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS)
                .add(Attributes.KNOCKBACK_RESISTANCE)
                .add(Attributes.SWEEPING_DAMAGE_RATIO); // so a copied Sweeping Edge sword's own
                // attribute modifier actually has something to attach to - see RageMeleeAttackGoal
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RageMeleeAttackGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new RageBreakBlockGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.2D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new RageTargetGoal(this));
    }

    /**
     * Copies the owner's current effective combat stats, equipment, and health onto this clone.
     * {@code RageEffect#startRage} grants this clone its own copy of {@link RageEffect} right after
     * calling this - that's where the +10% attack damage and +10% armor actually come from, not from
     * anything here.
     */
    public void possess(ServerPlayer owner) {
        this.ownerUUID = owner.getUUID();
        this.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));

        copyAttributeValue(owner, Attributes.MAX_HEALTH);
        copyAttributeValue(owner, Attributes.ATTACK_DAMAGE);
        copyAttributeValue(owner, Attributes.ARMOR);
        copyAttributeValue(owner, Attributes.ARMOR_TOUGHNESS);
        copyAttributeValue(owner, Attributes.MOVEMENT_SPEED);
        copyAttributeValue(owner, Attributes.KNOCKBACK_RESISTANCE);

        this.setHealth((float) Math.min(owner.getHealth(), this.getMaxHealth()));

        for (EquipmentSlot slot : EquipmentSlot.values())
            this.setItemSlot(slot, owner.getItemBySlot(slot).copy());

        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void copyAttributeValue(Player owner, Holder<Attribute> attribute) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null)
            instance.setBaseValue(owner.getAttributeValue(attribute));
    }

    public Optional<UUID> getSyncedOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID);
    }

    @Nullable
    public ServerPlayer getOwnerPlayer() {
        if (this.ownerUUID == null || !(this.level() instanceof ServerLevel serverLevel))
            return null;

        Player player = serverLevel.getServer().getPlayerList().getPlayer(this.ownerUUID);
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide)
            return;

        ServerPlayer owner = this.getOwnerPlayer();
        if (owner == null || !RageEffect.isRaging(owner, this))
            this.discard();
    }

    /**
     * The 10% resistance is a real {@link Attributes#ARMOR} modifier from {@link RageEffect}, so
     * vanilla's own armor formula (in the {@code super.hurt}/{@code actuallyHurt} chain) already
     * applies it before this ever sees zero health - no need to pre-guess post-armor damage the way
     * an earlier version of this class did. Once that formula actually brings the clone down, this
     * fires with the real, final damage already accounted for, and ends the rage instead of letting
     * the clone (and the fight) die outright - see {@link RageEffect#endRage}.
     */
    @Override
    public void die(DamageSource source) {
        ServerPlayer owner = this.getOwnerPlayer();
        super.die(source);

        if (!this.level().isClientSide && owner != null)
            RageEffect.endRage(owner, true);
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel serverLevel, DamageSource damageSource) {
        // Equipment is a visual copy of the owner's real inventory - never drop it.
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUUID != null)
            tag.putUUID("Owner", this.ownerUUID);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner"))
            this.ownerUUID = tag.getUUID("Owner");
    }
}
