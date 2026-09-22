package org.wilkretawesomesauce.alchemancystuck.properties;

import net.cibernet.alchemancy.properties.Property;
import net.cibernet.alchemancy.registries.AlchemancyTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;

//this is lesser arcane dude.
public class MagicalProperty extends Property {

    static ResourceKey<DamageType> MELEE_DAMAGE_KEY = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "magical"));
    static ResourceKey<DamageType> PROJECTILE_DAMAGE_KEY = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "magical_projectile"));


    @Override
    public void onIncomingAttack(Entity user, ItemStack weapon, LivingEntity target, LivingIncomingDamageEvent event) {
        if (!event.getSource().is(AlchemancyTags.DamageTypes.ARCANE_DAMAGE)) {
            damageItem(user, weapon, EquipmentSlot.MAINHAND, 1);

            float magicAmount = event.getAmount() * 0.15F;
            event.setAmount(event.getAmount() - magicAmount);

            target.hurt(new DamageSource(
                    target.damageSources().damageTypes.getHolderOrThrow(user.equals(event.getSource().getDirectEntity()) ? MELEE_DAMAGE_KEY : PROJECTILE_DAMAGE_KEY),
                    event.getSource().getDirectEntity(), event.getSource().getEntity(), event.getSource().getSourcePosition()), magicAmount);
        }
    }

    @Override
    public int modifyEnchantmentValue(int originalValue, int result) {
        return result + 3;
    }

    @Override
    public int getColor(ItemStack itemStack) {
        return 0x7A1165;
    }

}
