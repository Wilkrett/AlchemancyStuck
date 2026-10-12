package org.wilkretawesomesauce.alchemancystuck.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;
import org.wilkretawesomesauce.alchemancystuck.effect.RageEffect;
import org.wilkretawesomesauce.alchemancystuck.mixin.ServerPlayerAccessor;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonEntities;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonMobEffects;

import java.util.Set;
import java.util.UUID;

public class RageCloneEntity extends ServerPlayer {

    private static final int GUARANTEED_CRIT_INTERVAL = 5;

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private RagePathBrainEntity pathBrain;
    private int attacksLanded;

    private RageCloneEntity(MinecraftServer server, ServerLevel level, GameProfile profile) {
        super(server, level, profile, ClientInformation.createDefault());
    }

    public static RageCloneEntity spawn(ServerPlayer owner) {
        MinecraftServer server = owner.getServer();
        ServerLevel level = owner.serverLevel();

        GameProfile profile = new GameProfile(UUID.randomUUID(), owner.getGameProfile().getName() + "_Rage");
        profile.getProperties().putAll(owner.getGameProfile().getProperties());

        RageCloneEntity clone = new RageCloneEntity(server, level, profile);
        clone.ownerUUID = owner.getUUID();

        CommonListenerCookie cookie = new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), false);
        server.getPlayerList().placeNewPlayer(new FakeConnection(PacketFlow.SERVERBOUND), clone, cookie);

        clone.possess(owner);

        clone.teleportTo(level, owner.getX(), owner.getY(), owner.getZ(), Set.of(), owner.getYRot(), owner.getXRot());
        ((ServerPlayerAccessor) clone).alchemancystuck$setChangingDimension(false);

        clone.setHealth((float) Math.min(owner.getHealth(), clone.getMaxHealth()));
        clone.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);

        ((ServerPlayerAccessor) clone).alchemancystuck$setSpawnInvulnerableTime(0);

        clone.pathBrain = spawnPathBrain(clone, owner, level);

        return clone;
    }

    private static RagePathBrainEntity spawnPathBrain(RageCloneEntity clone, ServerPlayer owner, ServerLevel level) {
        RagePathBrainEntity brain = new RagePathBrainEntity(AddonEntities.RAGE_PATH_BRAIN.get(), level);
        brain.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        brain.setPuppet(clone, owner);
        brain.setItemSlot(EquipmentSlot.MAINHAND, owner.getItemBySlot(EquipmentSlot.MAINHAND).copy());

        AttributeInstance speed = brain.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null)
            speed.setBaseValue(owner.getAttributeValue(Attributes.MOVEMENT_SPEED));

        brain.addEffect(new MobEffectInstance(AddonMobEffects.RAGE, RageEffect.DURATION_TICKS, 0, false, false));
        level.addFreshEntity(brain);

        return brain;
    }

    private void possess(ServerPlayer owner) {
        this.getAttributes().assignAllValues(owner.getAttributes());

        for (MobEffectInstance effect : owner.getActiveEffects())
            if (!effect.getEffect().is(AddonMobEffects.RAGE.getKey()))
                this.addEffect(new MobEffectInstance(effect));

        for (EquipmentSlot slot : EquipmentSlot.values())
            this.setItemSlot(slot, owner.getItemBySlot(slot).copy());
    }

    @Nullable
    public ServerPlayer getOwnerPlayer() {
        if (this.ownerUUID == null || !(this.level() instanceof ServerLevel serverLevel))
            return null;

        Player player = serverLevel.getServer().getPlayerList().getPlayer(this.ownerUUID);
        return player instanceof ServerPlayer serverPlayer && serverPlayer != this ? serverPlayer : null;
    }

    @Override
    public void tick() {
        super.tick();

        try {
            this.doTick();
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("alchemancystuck").warn("RageCloneEntity.doTick() threw", t);
        }

        if (this.level().isClientSide)
            return;

        ServerPlayer owner = this.getOwnerPlayer();
        if (owner == null || !RageEffect.isRaging(owner, this)) {
            despawn();
            return;
        }

        if (this.pathBrain == null || !this.pathBrain.isAlive()) {
            despawn();
            return;
        }

        this.moveTo(this.pathBrain.getX(), this.pathBrain.getY(), this.pathBrain.getZ(), this.pathBrain.getYRot(), this.pathBrain.getXRot());
        this.setYHeadRot(this.pathBrain.getYHeadRot());

        this.setOnGround(this.pathBrain.onGround());
        this.setSpeed(this.pathBrain.getSpeed());
        this.fallDistance = this.pathBrain.fallDistance;

        tryAttack();
    }

    private void tryAttack() {
        if (this.pathBrain == null || this.getAttackStrengthScale(1.0F) < 1.0F)
            return;

        LivingEntity target = this.pathBrain.getTarget();
        if (target == null || !target.isAlive())
            return;

        if (!this.pathBrain.isWithinMeleeAttackRange(target) || !this.pathBrain.getSensing().hasLineOfSight(target))
            return;

        this.attack(target);
        this.swing(InteractionHand.MAIN_HAND);
    }

    public boolean rollGuaranteedCrit() {
        return ++this.attacksLanded % GUARANTEED_CRIT_INTERVAL == 0;
    }

    public void despawn() {
        if (this.pathBrain != null) {
            this.pathBrain.discard();
            this.pathBrain = null;
        }

        MinecraftServer server = this.getServer();
        if (server != null)
            server.getPlayerList().remove(this);
    }

    @Override
    public void die(DamageSource source) {
        ServerPlayer owner = this.getOwnerPlayer();
        this.getInventory().clearContent();
        super.die(source);

        if (owner != null)
            RageEffect.endRage(owner, true);
    }

    @Override
    public void giveExperiencePoints(int xpPoints) {
        ServerPlayer owner = getOwnerPlayer();
        if (owner != null)
            owner.giveExperiencePoints(xpPoints);
        else
            super.giveExperiencePoints(xpPoints);
    }

    @Override
    public boolean allowsListing() {
        return false;
    }

    @Override
    public String getIpAddress() {
        return "127.0.0.1";
    }
}
