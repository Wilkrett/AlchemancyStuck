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

public class RageInducingProperty extends Property {

    private static final float TRIGGER_CHANCE = 0.05F;
    private static final int EXTRA_DURABILITY_DAMAGE = 1;

    public static final float COLOR_CYCLE_SPEED = 0.6F;
    public static final int[] COLOR_CYCLE = {0x8B0000, 0x8d0400, 0x4b1853, 0x631db4};

    @Override
    public void modifyAttackDamage(Entity user, ItemStack weapon, LivingDamageEvent.Pre event) {

        damageItem(user, weapon, EquipmentSlot.MAINHAND, EXTRA_DURABILITY_DAMAGE);

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
        return ColorUtils.interpolateColorsOverTime(COLOR_CYCLE_SPEED, COLOR_CYCLE);
    }
}
