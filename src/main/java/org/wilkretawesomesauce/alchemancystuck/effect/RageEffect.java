package org.wilkretawesomesauce.alchemancystuck.effect;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;
import org.wilkretawesomesauce.alchemancystuck.entity.RageCloneEntity;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonMobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the entire rage lifecycle - {@code RageInducingProperty} does nothing but roll the rare
 * chance and call {@code player.addEffect(...)}; everything downstream of "the player now has this
 * effect" (spawning the clone, the spectator/camera swap, ending the rage, and login/logout edge
 * cases) lives here.
 *
 * <p>Applied twice per rage: once to the owning player (the "marker" copy - its {@link #onEffectAdded}
 * is what kicks the whole thing off, and its duration is what naturally ends it via
 * {@link #onEffectExpired}), and once to the {@link RageCloneEntity} itself (granted in
 * {@link #startRage}, purely so the clone gets this class's attribute modifiers - the ones that
 * actually matter for combat, since the player doesn't fight during a rage).
 *
 * <p>Vanilla's own {@code MobEffect} has no "onEffectRemoved" hook (that's specific to Tensura's own
 * {@code TensuraMobEffect} subclass in the mysticism reference this addon doesn't have access to).
 * NeoForge's equivalent is a pair of global events instead, {@link MobEffectEvent.Expired} (natural
 * duration end) and {@link MobEffectEvent.Remove} (anything else, e.g. a debug {@code /effect clear})
 * - {@link #onEffectExpired}/{@link #onEffectRemoved} below do what an override would have.
 */
public class RageEffect extends MobEffect {

    public static final int DURATION_TICKS = 30 * 20;

    private static final ResourceLocation DAMAGE_BONUS = ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "rage_damage_bonus");
    private static final ResourceLocation ARMOR_BONUS = ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "rage_armor_bonus");
    private static final ResourceLocation SPEED_BONUS = ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "rage_speed_bonus");
    private static final String TAG_PRIOR_GAMEMODE = "alchemancystuck:rage_prior_gamemode";
    private static final int ACTIVATE_MESSAGE_COUNT = 3;

    /**
     * Per-level increments for the attribute bonuses below - amplifier 0 (the effect's default,
     * "level 1") gets one increment, amplifier 1 ("level 2") gets two, and so on. Nothing in this
     * addon currently applies Rage above amplifier 0, but anything that does (a debug
     * {@code /effect give ... 1}, or a future mechanic) gets these scaled automatically - the effect
     * itself doesn't care what decided the level.
     */
    private static final double DAMAGE_BONUS_PER_LEVEL = 0.20D;
    private static final double ARMOR_BONUS_PER_LEVEL = 0.20D;
    private static final double SPEED_BONUS_PER_LEVEL = 0.8D;

    private static final Map<UUID, UUID> ACTIVE_RAGES = new HashMap<>();

    public RageEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B0000);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, DAMAGE_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> DAMAGE_BONUS_PER_LEVEL * (amplifier + 1));
        this.addAttributeModifier(Attributes.ARMOR, ARMOR_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> ARMOR_BONUS_PER_LEVEL * (amplifier + 1));
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> SPEED_BONUS_PER_LEVEL * (amplifier + 1));
    }

    @Override
    public void onEffectAdded(LivingEntity entity, int amplifier) {
        super.onEffectAdded(entity, amplifier);

        // RageCloneEntity is itself a ServerPlayer now (see its own doc comment for why) - without this
        // exclusion, startRage's own `clone.addEffect(RAGE)` call (granting the clone the attack
        // damage/armor modifiers) would re-enter this exact method, see "a ServerPlayer just got Rage",
        // and spawn a second clone possessing the first, which grants *that* clone an effect too, and
        // so on forever - a runaway chain of clones each broadcasting their own "joined the game".
        if (entity instanceof ServerPlayer owner && !(entity instanceof RageCloneEntity) && entity.level() instanceof ServerLevel level && !isRaging(owner))
            startRage(owner, level);
    }

    public static boolean isRaging(ServerPlayer player) {
        return ACTIVE_RAGES.containsKey(player.getUUID());
    }

    public static boolean isRaging(ServerPlayer player, RageCloneEntity clone) {
        return clone.getUUID().equals(ACTIVE_RAGES.get(player.getUUID()));
    }

    private static void startRage(ServerPlayer owner, ServerLevel level) {
        RageCloneEntity clone = RageCloneEntity.spawn(owner);
        clone.addEffect(new MobEffectInstance(AddonMobEffects.RAGE, DURATION_TICKS, 0, false, false));

        owner.getPersistentData().putInt(TAG_PRIOR_GAMEMODE, owner.gameMode.getGameModeForPlayer().getId());
        ACTIVE_RAGES.put(owner.getUUID(), clone.getUUID());

        owner.setGameMode(GameType.SPECTATOR);
        owner.setCamera(clone);

        int variant = owner.getRandom().nextInt(ACTIVATE_MESSAGE_COUNT) + 1;
        owner.displayClientMessage(Component.translatable("message.alchemancystuck.rage_inducing.activate" + variant).withStyle(ChatFormatting.DARK_RED), true);
    }

    /**
     * For the one caller ({@code RageCloneEntity#die}) that fires while the owner's own effect is
     * still genuinely active - dying doesn't naturally remove it, so it has to be stripped here or
     * it'd keep buffing the player (back in survival) for the rest of its original duration.
     *
     * @param overwhelmed true if the clone took what would've been a killing blow - the owner is
     *                     dumped back into their own body at 1 HP instead of copying the clone's
     *                     (near-zero) health, so the rage reads as "shaken, not killed."
     */
    public static void endRage(ServerPlayer owner, boolean overwhelmed) {
        if (!cleanupRage(owner, overwhelmed))
            return;

        if (owner.hasEffect(AddonMobEffects.RAGE))
            owner.removeEffect(AddonMobEffects.RAGE);
    }

    /**
     * Everything about ending a rage except touching the owner's own effect instance. Used directly
     * by {@link #onEffectExpired}/{@link #onEffectRemoved}, which fire <em>while the owner's effect
     * is already being removed</em> - vanilla's {@code LivingEntity#tickEffects} posts
     * {@code MobEffectEvent.Expired} from inside an active iterator over its effects map, and
     * silently swallows any {@code ConcurrentModificationException} that escapes it. Calling
     * {@code removeEffect} from within that handler mutates the same map mid-iteration, throws, and
     * the exception gets eaten - which was silently aborting the rest of that tick's effect
     * processing (including, in practice, everything this method does) before this was split out.
     *
     * @return false if there was no active rage to end (so the caller shouldn't touch the effect either)
     */
    private static boolean cleanupRage(ServerPlayer owner, boolean overwhelmed) {
        if (!isRaging(owner))
            return false;

        UUID cloneUUID = ACTIVE_RAGES.remove(owner.getUUID());
        restoreGameMode(owner);

        // Searches every dimension, not just the owner's own level - the clone can wander through a
        // portal mid-rage (its AI doesn't avoid them), and the owner's own level never changes (they
        // never physically move, only their camera does), so looking it up in just owner.level() would
        // silently fail to find it whenever that happens.
        RageCloneEntity clone = null;
        if (cloneUUID != null && owner.getServer() != null)
            for (ServerLevel candidate : owner.getServer().getAllLevels())
                if (candidate.getEntity(cloneUUID) instanceof RageCloneEntity found) {
                    clone = found;
                    break;
                }

        if (clone != null && clone.level() instanceof ServerLevel cloneLevel) {
            owner.teleportTo(cloneLevel, clone.getX(), clone.getY(), clone.getZ(), Set.of(), clone.getYRot(), clone.getXRot());
            owner.setHealth(overwhelmed ? 1.0F : (float) Math.min(clone.getHealth(), owner.getMaxHealth()));
            clone.despawn();
        } else if (overwhelmed) {
            owner.setHealth(1.0F);
        }

        return true;
    }

    private static void restoreGameMode(ServerPlayer owner) {
        owner.setCamera(owner);

        CompoundTag persisted = owner.getPersistentData();
        GameType priorGameMode = persisted.contains(TAG_PRIOR_GAMEMODE) ? GameType.byId(persisted.getInt(TAG_PRIOR_GAMEMODE)) : GameType.SURVIVAL;
        persisted.remove(TAG_PRIOR_GAMEMODE);
        owner.setGameMode(priorGameMode);
    }

    /**
     * Compares via {@code Holder#is(ResourceKey)} rather than {@code ==} - identity comparison
     * already bit this addon once (see {@code InfusedPropertiesHelperMixin}'s doc comment): a Holder
     * sourced from somewhere other than the exact {@code AddonMobEffects.RAGE} constant (e.g. one
     * resolved fresh from the registry, which can happen depending on exactly how/where an effect
     * instance got constructed or deserialized) can represent the same registry entry without being
     * the same object. {@code is(ResourceKey)} compares correctly either way - unlike the
     * {@code is(Holder)} overload, which does the same thing but is deprecated in favor of this one.
     */
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect().is(AddonMobEffects.RAGE.getKey()) && event.getEntity() instanceof ServerPlayer player)
            cleanupRage(player, false);
    }

    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect().is(AddonMobEffects.RAGE.getKey()) && event.getEntity() instanceof ServerPlayer player)
            cleanupRage(player, false);
    }

    /**
     * The owner's real body can still die while raging (falling, drowning, a second attacker while
     * their idle spectator body just stands there, etc.) - a rage doesn't make the real player
     * immortal, only the clone absorbs actual combat risk. What it does need to do is clean up the
     * dangling rage state, since nothing else will: without this, the clone would keep fighting
     * indefinitely with no owner left to eventually restore into, and {@link #ACTIVE_RAGES} would keep
     * a stale entry forever. Reuses {@link #cleanupRage} - the same camera/gamemode-restoring,
     * clone-despawning cleanup {@link #onEffectExpired}/{@link #onEffectRemoved} already use - rather
     * than canceling the death event or forcing survival, which is what this used to do.
     */
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isRaging(player))
            cleanupRage(player, false);
    }

    /**
     * Unlike {@link #onEffectExpired}/{@link #onEffectRemoved}, this doesn't fire from inside
     * {@code LivingEntity#tickEffects}'s iteration, so calling {@link #endRage} (which strips the
     * owner's own effect instance) is safe here - and necessary, or the effect survives into the
     * player's save data and comes back on relog with none of the rest of the rage (no clone, no
     * spectating) attached to it anymore.
     */
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer owner)
            endRage(owner, false);
    }

    /**
     * Safety net for the rare case a rage was active when the server went down: the in-memory
     * {@link #ACTIVE_RAGES} map doesn't survive a restart, but the "prior gamemode" tag does (it's
     * on the player's persisted data), so use it to un-stick anyone who reconnects still spectating.
     */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer owner && !isRaging(owner) && owner.getPersistentData().contains(TAG_PRIOR_GAMEMODE))
            restoreGameMode(owner);
    }
}
