package org.wilkretawesomesauce.alchemancystuck.properties;

import net.cibernet.alchemancy.properties.Property;
import net.cibernet.alchemancy.util.ColorUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.wilkretawesomesauce.alchemancystuck.effect.RageEffect;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonMobEffects;

/**
 * The only thing this property does is roll a rare chance on taking damage while worn in an armor
 * slot, and - if it hits - apply {@link RageEffect} to the wearer. Everything else (spawning the
 * fighting clone, the spectator/camera swap, the combat buffs, the hunger drain, ending the rage, and
 * login/logout edge cases) is owned entirely by the effect itself; see its class doc.
 */
public class RageInducingProperty extends Property {

    private static final float TRIGGER_CHANCE = 0.05F;

    @Override
    public void modifyAttackDamage(Entity user, ItemStack weapon, LivingDamageEvent.Pre event) {

        if (!(user instanceof ServerPlayer player) || player.hasEffect(AddonMobEffects.RAGE))
            return;

        if (player.getRandom().nextFloat() < TRIGGER_CHANCE)
            player.addEffect(new MobEffectInstance(AddonMobEffects.RAGE, RageEffect.DURATION_TICKS, 0, false, false));
    }

    @Override
    public void onDamageReceived(LivingEntity user, ItemStack weapon, EquipmentSlot slot, DamageSource damageSource) {
        if (!slot.isArmor())
            return;

        if (!(user instanceof ServerPlayer player) || player.hasEffect(AddonMobEffects.RAGE))
            return;

        if (player.getRandom().nextFloat() < TRIGGER_CHANCE)
            player.addEffect(new MobEffectInstance(AddonMobEffects.RAGE, RageEffect.DURATION_TICKS, 0, false, false));
    }

    @Override
    public int getColor(ItemStack stack) {
        return ColorUtils.interpolateColorsOverTime(0.6f, 0x8B0000, 0x8d0400, 0x4b1853, 0x631db4);
    }
}
