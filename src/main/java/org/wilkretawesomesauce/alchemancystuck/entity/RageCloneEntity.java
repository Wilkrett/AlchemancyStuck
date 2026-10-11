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

/**
 * The "AI" that takes over during a rage - now a genuine (if headless) {@link ServerPlayer}, not a
 * {@code Mob}, because {@code Mob#doHurtTarget} structurally cannot reach real player-only combat
 * mechanics: sweep attacks, critical hits, attack-strength charging, and {@code ItemStack#hurtEnemy}
 * (the hook most custom weapon on-hit effects are actually built on - its second parameter is typed
 * {@code Player}, not {@code LivingEntity}, so a {@code Mob} attacker can never satisfy it no matter
 * what). Being a real {@code ServerPlayer} and calling the real {@code Player#attack}/mining methods
 * gets all of that back for free.
 *
 * <p>The cost: {@code Player} has no {@code GoalSelector}/{@code PathNavigation}/{@code Sensing} the
 * way {@code Mob} does, so all "AI" behavior here has to be hand-driven from {@link #tick()} rather
 * than composed from vanilla goals.
 *
 * <p><b>Known, deliberately accepted imperfections</b> (real ServerPlayer machinery not worth fighting
 * any further - the join/kill chat broadcasts were worth suppressing via mixin since they fired every
 * single rage and were genuinely confusing; this one is silent and easy to ignore):
 * <ul>
 *   <li>Despawning ({@link #despawn}) writes a real (tiny, throwaway-UUID) playerdata file to disk,
 *   the same as any other player disconnecting - these are never cleaned up automatically.</li>
 * </ul>
 *
 * <p>Getting a real {@code ServerPlayer} into the world without a real client requires two supporting
 * pieces that don't exist otherwise: {@link FakeConnection} (a {@code Connection} backed by an
 * in-memory netty channel instead of a socket) and {@code PlayerListNetHandlerMixin} (redirects
 * {@code PlayerList#placeNewPlayer}'s hardcoded {@code new ServerGamePacketListenerImpl(...)} to
 * {@link RageFakePlayerNetHandler} for instances of this class specifically).
 */
public class RageCloneEntity extends ServerPlayer {

    /**
     * Every Nth landed hit (counted per clone, so a fresh rage always starts the count over) is a
     * guaranteed critical hit - see {@link #rollGuaranteedCrit()} and {@code RageEffect#onCriticalHit}.
     */
    private static final int GUARANTEED_CRIT_INTERVAL = 5;

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private RagePathBrainEntity pathBrain;
    private int attacksLanded;

    private RageCloneEntity(MinecraftServer server, ServerLevel level, GameProfile profile) {
        super(server, level, profile, ClientInformation.createDefault());
    }

    /**
     * Spawns and fully registers the clone as a live player of the server (via
     * {@code PlayerList#placeNewPlayer}, the same entry point a real client's login uses) - not just
     * constructing the entity, which on its own wouldn't be a real, tickable, damageable player yet.
     */
    public static RageCloneEntity spawn(ServerPlayer owner) {
        MinecraftServer server = owner.getServer();
        ServerLevel level = owner.serverLevel();

        // A random UUID - can't reuse the owner's own, that's still in use by the owner's real entity.
        // The name deliberately does NOT match the owner's: on a singleplayer world,
        // IntegratedServer#isSingleplayerOwner matches by name only (not UUID), and PlayerList#load
        // treats a name match as "the singleplayer host reconnecting" - force-loading the world's
        // cached player tag (equipment included) onto whatever ServerPlayer it's given, clobbering
        // possess()'s copy with a frozen, session-long-stale snapshot of whatever the owner happened to
        // be holding back when the world was first loaded. Copying the owner's GameProfile *properties*
        // (not just id/name) still carries their real skin texture data over regardless of the name
        // mismatch, so this still renders with the owner's actual skin - no custom renderer needed.
        // An underscore, not an apostrophe/space - a whitespace-containing name needs to be quoted for
        // every selector/command argument (and broke plain `/kill <name>` entirely), which matters a lot
        // more for something meant to be poked at with debug commands than a slightly nicer-looking name.
        GameProfile profile = new GameProfile(UUID.randomUUID(), owner.getGameProfile().getName() + "_Rage");
        profile.getProperties().putAll(owner.getGameProfile().getProperties());

        RageCloneEntity clone = new RageCloneEntity(server, level, profile);
        clone.ownerUUID = owner.getUUID();

        // possess() runs after placeNewPlayer so it always has the final say over the clone's
        // equipment/attributes, regardless of whatever placeNewPlayer's own internal load() step
        // populated it with first (now just "nothing," per the name-mismatch fix above, but there's no
        // reason to depend on that).
        CommonListenerCookie cookie = new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), false);
        server.getPlayerList().placeNewPlayer(new FakeConnection(PacketFlow.SERVERBOUND), clone, cookie);

        clone.possess(owner);

        // placeNewPlayer (just above) always resets a fresh/save-less player's dimension to the
        // overworld (PlayerList#load finds no file for a random UUID, defaults to Level.OVERWORLD) -
        // moveTo alone only sets position *within* whatever level the entity is already on, it can't
        // cross dimensions. teleportTo(ServerLevel, ...) is the actual cross-dimension move, needed
        // any time rage triggers somewhere other than the overworld (Nether, End, a modded dimension).
        // When it IS a cross-dimension move, ServerPlayer#teleportTo internally routes through
        // changeDimension, which sets isChangingDimension = true - normally cleared once a real client
        // acknowledges the dimension-change packets, which this fake connection never does, so it has
        // to be forced back off explicitly or ServerPlayer#isInvulnerableTo stays permanently true (see
        // ServerPlayerAccessor's own doc comment - this was the actual "can't be damaged at all" bug).
        clone.teleportTo(level, owner.getX(), owner.getY(), owner.getZ(), Set.of(), owner.getYRot(), owner.getXRot());
        ((ServerPlayerAccessor) clone).alchemancystuck$setChangingDimension(false);

        clone.setHealth((float) Math.min(owner.getHealth(), clone.getMaxHealth()));
        clone.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);

        // Every fresh ServerPlayer starts with 3 seconds of spawn-protection (ServerPlayer#hurt
        // explicitly refuses all damage while this is > 0) - meant for a real player who just joined,
        // not a clone that's supposed to be fightable the instant it exists.
        ((ServerPlayerAccessor) clone).alchemancystuck$setSpawnInvulnerableTime(0);

        clone.pathBrain = spawnPathBrain(clone, owner, level);

        return clone;
    }

    /**
     * The "legs" half of the split described in this class's doc comment - see
     * {@link RagePathBrainEntity} for why a real {@code Mob} has to exist alongside the real
     * {@code Player} at all. Carries a copy of the owner's mainhand item too, purely so
     * {@link RageBreakBlockGoal}'s mining-speed math (tool speed vs. block hardness) matches whatever
     * the clone is actually "holding" - the brain is never visible, so the rest of its equipment
     * doesn't matter.
     */
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

    /**
     * Copies the owner's current effective combat stats, active effects, equipment, and health onto
     * this clone. {@code RageEffect#startRage} grants this clone its own copy of {@link RageEffect}
     * right after calling this - that's where the attack damage/armor/speed buffs actually come from,
     * not from anything here.
     * <p>
     * {@code AttributeMap#assignAllValues} copies every attribute the owner's map has an instance for -
     * base value <em>and</em> modifiers - not just a hand-picked few. A hardcoded list here previously
     * missed {@code ATTACK_SPEED} (relying entirely on {@code detectEquipmentUpdates} independently
     * reconstructing the weapon's own penalty from the copied equipment) and silently missed anything
     * else outside that list too - {@code ATTACK_KNOCKBACK}, {@code LUCK}, and any attribute any other
     * mod (Alchemancy included) might add. Active potion/mob effects are a completely separate system
     * from attributes and were never copied at all - a real {@code Strength} or similar buff the owner
     * had running would silently not carry over to the clone, while still affecting the owner's own
     * (now-spectating, not fighting) body. Rage itself is excluded here since {@code startRage} grants
     * it separately right after this returns - the owner already has their own "marker" copy active by
     * the time this runs (that's what triggered {@code onEffectAdded} in the first place), so without
     * this exclusion the clone would end up with two independently-tracked Rage instances.
     */
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

        // ServerPlayer#tick() (just called via super, above) is only HALF of what a real player gets
        // every server tick - the other half, ServerPlayer#doTick(), normally only runs because
        // ServerGamePacketListenerImpl#tick() calls it, which itself only runs because a real network
        // connection got registered into the server's own connection-tick loop during the TCP accept
        // handshake. FakeConnection is constructed directly in Java and never goes through that, so
        // without this explicit call doTick() - and everything nested inside it, including
        // Player#tick()/LivingEntity#tick() (attackStrengthTicker++, setSpeed() needed for sweep
        // attacks, detectEquipmentUpdates() which attaches weapon attribute modifiers like attack
        // speed) - would never run for this entity at all. doTick() wraps its entire body in its own
        // try/catch(Throwable) that re-throws as a fatal ReportedException on the way out, so this is
        // caught broadly rather than just NullPointerException - something in there not tolerating a
        // fake connection should skip this tick, not crash the server.
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

        // The brain does the actual walking/pathing/obstacle-breaking (see RagePathBrainEntity) -
        // this just wears its result every tick, the same way a puppet follows its legs.
        this.moveTo(this.pathBrain.getX(), this.pathBrain.getY(), this.pathBrain.getZ(), this.pathBrain.getYRot(), this.pathBrain.getXRot());
        this.setYHeadRot(this.pathBrain.getYHeadRot());

        // moveTo (above) is a raw teleport - it never runs the normal Entity#move collision/physics
        // pass, so none of the fields that pass would normally derive (onGround, speed, fallDistance)
        // ever get set for this entity on their own; each is stuck at its Java default forever unless
        // mirrored here explicitly. The path brain is a real Mob that already maintains all three
        // correctly through normal physics, so just copy them over alongside position/rotation:
        //  - onGround: normally set for a ServerPlayer from the client's own movement packets
        //    (ServerGamePacketListenerImpl calling setOnGroundWithMovement with whatever the client
        //    reported), never computed server-side the way a Mob's is - FakeConnection never sends any
        //    such packets. Player#attack's sweep condition requires this to be true.
        //  - speed: LivingEntity#speed is a plain field only ever written by MoveControl#tick(), a Mob
        //    concept this Player-typed entity has none of. Player#attack's sweep condition also requires
        //    walked-distance-this-tick < getSpeed(), which can never hold against a permanently-0 speed.
        //  - fallDistance: a public field normally accumulated by Entity#move's own fall tracking.
        //    Player#attack's crit condition requires fallDistance > 0.0F - without this, a natural
        //    hit-while-falling crit (as opposed to this addon's own guaranteed-every-5th-hit crit) could
        //    never trigger no matter how far this was actually falling a moment ago.
        this.setOnGround(this.pathBrain.onGround());
        this.setSpeed(this.pathBrain.getSpeed());
        this.fallDistance = this.pathBrain.fallDistance;

        tryAttack();
    }

    /**
     * The one piece of combat this class handles itself rather than delegating to the brain - calling
     * the real {@link Player#attack(net.minecraft.world.entity.Entity)} (not {@code Mob#doHurtTarget})
     * is the entire reason this class is a {@code ServerPlayer} now: it's what makes sweep, crits, and
     * (the original motivation for this whole rewrite) custom on-hit weapon effects via
     * {@code ItemStack#hurtEnemy} actually fire, none of which {@code Mob} combat can ever reach.
     *
     * <p>Gates on {@link #getAttackStrengthScale} directly - the same value {@code attack()} itself
     * uses to decide crits/sweep/damage scaling - rather than a separately hand-rolled tick counter.
     * A parallel counter has to independently reconstruct vanilla's {@code 20 / attackSpeed} timing and
     * stay in lockstep with {@code attackStrengthTicker} (which only increments inside {@code doTick()})
     * every tick forever; reading the real value directly can't drift out of sync with it, and every
     * swing that lands is by construction the same "fully charged" swing a real player would get from
     * waiting for their crosshair to fill before clicking.
     *
     * <p>{@link #attack} has to run <em>before</em> {@link #swing} - not the more natural-looking
     * "swing, then land the hit" order - because {@code ServerPlayer} overrides {@code swing} to call
     * {@code resetAttackStrengthTicker()} as a side effect (harmless for a real client, whose melee
     * attacks never actually go through {@code ServerPlayer#swing} - the animation other players see
     * comes from a separate path entirely). Calling it ourselves right before {@code attack()} was
     * wiping out the exact charge just confirmed to be full a moment earlier, so every hit was computed
     * at near-zero charge no matter how long this waited. {@code attack()} resets the ticker itself once
     * it's done with it, so swinging after is redundant but harmless.
     */
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

    /**
     * Called once per {@code CriticalHitEvent} this clone is the attacker for (i.e. once per landed
     * hit, since that event only fires from inside the {@code f > 0.0F || f1 > 0.0F} branch of {@code
     * Player#attack}) - increments the counter and reports whether this specific hit is the guaranteed
     * Nth one. Instance state, not static, so it's scoped to this one clone/rage and always starts fresh.
     */
    public boolean rollGuaranteedCrit() {
        return ++this.attacksLanded % GUARANTEED_CRIT_INTERVAL == 0;
    }

    /**
     * The proper counterpart to how {@link #spawn} used {@code PlayerList#placeNewPlayer} - removes
     * this from the server the same way a real disconnect would ({@code PlayerList#remove}), not
     * {@code discard()} (which is for {@code Mob}s and would leave this still registered as a "logged
     * in" player everywhere else in the server that doesn't know it never had a real connection).
     * Also discards the path brain - it has no lifecycle of its own outside of this one.
     */
    public void despawn() {
        if (this.pathBrain != null) {
            this.pathBrain.discard();
            this.pathBrain = null;
        }

        MinecraftServer server = this.getServer();
        if (server != null)
            server.getPlayerList().remove(this);
    }

    /**
     * Equipment is a copy of the owner's real inventory - clearing it before the real
     * {@code ServerPlayer#die} runs is what stops it from actually dropping (a real player's death
     * drops their inventory by default), which would otherwise duplicate the owner's own items.
     */
    @Override
    public void die(DamageSource source) {
        ServerPlayer owner = this.getOwnerPlayer();
        this.getInventory().clearContent();
        super.die(source);

        if (owner != null)
            RageEffect.endRage(owner, true);
    }

    /**
     * {@code ExperienceOrb.playerTouch} calls this on whichever player physically walks over the orb -
     * since this clone is the one standing on the corpse it just made, that's always this clone itself,
     * never the (likely elsewhere, spectating) owner. Redirecting here is simpler than trying to keep
     * the clone away from its own kills' orbs.
     */
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
