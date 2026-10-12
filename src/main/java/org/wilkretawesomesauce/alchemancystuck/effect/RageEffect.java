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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;
import org.wilkretawesomesauce.alchemancystuck.entity.RageCloneEntity;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonAttachments;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonMobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RageEffect extends MobEffect {

    public static final int DURATION_TICKS = 30 * 20;

    private static final ResourceLocation DAMAGE_BONUS = ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "rage_damage_bonus");
    private static final ResourceLocation ARMOR_BONUS = ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "rage_armor_bonus");
    private static final ResourceLocation SPEED_BONUS = ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "rage_speed_bonus");
    private static final String TAG_PRIOR_GAMEMODE = "alchemancystuck:rage_prior_gamemode";
    private static final int ACTIVATE_MESSAGE_COUNT = 3;

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

        if (entity instanceof ServerPlayer owner && !(entity instanceof RageCloneEntity) && entity.level() instanceof ServerLevel level && !isRaging(owner))
            startRage(owner, level, true);
    }

    public static boolean isRaging(ServerPlayer player) {
        return ACTIVE_RAGES.containsKey(player.getUUID());
    }

    public static boolean isRaging(ServerPlayer player, RageCloneEntity clone) {
        return clone.getUUID().equals(ACTIVE_RAGES.get(player.getUUID()));
    }

    private static void startRage(ServerPlayer owner, ServerLevel level, boolean announce) {
        RageCloneEntity clone = RageCloneEntity.spawn(owner);
        clone.setData(AddonAttachments.IS_RAGE_CLONE, true);
        clone.addEffect(new MobEffectInstance(AddonMobEffects.RAGE, DURATION_TICKS, 0, false, false));

        owner.getPersistentData().putInt(TAG_PRIOR_GAMEMODE, owner.gameMode.getGameModeForPlayer().getId());
        ACTIVE_RAGES.put(owner.getUUID(), clone.getUUID());

        owner.setGameMode(GameType.SPECTATOR);
        owner.setCamera(clone);

        if (announce) {
            int variant = owner.getRandom().nextInt(ACTIVATE_MESSAGE_COUNT) + 1;
            owner.displayClientMessage(Component.translatable("message.alchemancystuck.rage_inducing.activate" + variant).withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    public static void endRage(ServerPlayer owner, boolean overwhelmed) {
        if (!cleanupRage(owner, overwhelmed))
            return;

        if (owner.hasEffect(AddonMobEffects.RAGE))
            owner.removeEffect(AddonMobEffects.RAGE);
    }

    private static boolean cleanupRage(ServerPlayer owner, boolean overwhelmed) {
        if (!isRaging(owner))
            return false;

        UUID cloneUUID = ACTIVE_RAGES.remove(owner.getUUID());
        restoreGameMode(owner);

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

            for (EquipmentSlot slot : EquipmentSlot.values())
                owner.setItemSlot(slot, clone.getItemBySlot(slot).copy());

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

    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect().is(AddonMobEffects.RAGE.getKey()) && event.getEntity() instanceof ServerPlayer player)
            cleanupRage(player, false);
    }

    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect().is(AddonMobEffects.RAGE.getKey()) && event.getEntity() instanceof ServerPlayer player)
            cleanupRage(player, false);
    }

    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isRaging(player)) {
            event.setCanceled(true);
            cleanupRage(player, false);
        }
    }

    public static void onCriticalHit(CriticalHitEvent event) {
        if (event.getEntity() instanceof RageCloneEntity clone && clone.rollGuaranteedCrit()) {
            event.setCriticalHit(true);
            event.setDamageMultiplier(1.5F);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer owner)
            cleanupRage(owner, false);
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer owner) || isRaging(owner))
            return;

        if (owner.getPersistentData().contains(TAG_PRIOR_GAMEMODE))
            restoreGameMode(owner);

        if (owner.hasEffect(AddonMobEffects.RAGE) && owner.level() instanceof ServerLevel level)
            startRage(owner, level, false);
    }
}
