package org.wilkretawesomesauce.alchemancystuck.properties;

import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.cibernet.alchemancy.properties.Property;
import net.cibernet.alchemancy.util.ColorUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonProperties;

import java.util.List;

/**
 * Nothing here fires from a {@link Property} hook - the base mod has no "an explosion happened
 * nearby" event, so this is driven by {@link #onExplosionDetonate}, a plain NeoForge listener
 * registered on {@code NeoForge.EVENT_BUS} in {@code Alchemancystuck}'s constructor.
 */
public class BadassProperty extends Property {

    private static final int EFFECT_DURATION_TICKS = 200;

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide())
            return;

        Vec3 explosionCenter = event.getExplosion().center();

        for (Entity entity : event.getAffectedEntities()) {
            if (!(entity instanceof Player player))
                continue;

            if (!InfusedPropertiesHelper.hasItemWithProperty(player, AddonProperties.BADASS, false, EquipmentSlotGroup.ARMOR))
                continue;

            if (isLookingAwayFrom(player, explosionCenter))
                grantRandomPositiveEffect(player);
        }
    }

    private static boolean isLookingAwayFrom(Player player, Vec3 point) {
        Vec3 towardPoint = point.subtract(player.getEyePosition()).normalize();
        return player.getLookAngle().dot(towardPoint) < 0;
    }

    private static void grantRandomPositiveEffect(Player player) {
        List<Holder.Reference<MobEffect>> positiveEffects = BuiltInRegistries.MOB_EFFECT.holders()
                .filter(holder -> holder.value().isBeneficial())
                .toList();

        if (positiveEffects.isEmpty())
            return;

        Holder<MobEffect> effect = positiveEffects.get(player.getRandom().nextInt(positiveEffects.size()));
        player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION_TICKS));
    }

    @Override
    public int getColor(ItemStack itemStack) {
        return ColorUtils.sineColorsOverTime(2f,0xE25822, 0x000000);
    }
}
