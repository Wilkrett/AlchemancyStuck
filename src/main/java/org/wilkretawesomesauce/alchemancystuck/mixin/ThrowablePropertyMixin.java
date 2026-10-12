package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.cibernet.alchemancy.properties.ThrowableProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonProperties;

@Mixin(ThrowableProperty.class)
public abstract class ThrowablePropertyMixin {

    private static final float SBAHJARANG_VELOCITY = 0.5F;
    private static final float SBAHJARANG_INACCURACY = 20.0F;

    @Inject(
            method = "throwItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD")
    )
    private void alchemancystuck$captureSbahj(Level level, LivingEntity user, ItemStack stack, CallbackInfo ci, @Share("sbahj") LocalBooleanRef sbahj) {
        sbahj.set(InfusedPropertiesHelper.hasProperty(stack, AddonProperties.SBAHJ));
    }

    @ModifyArgs(
            method = "throwItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V")
    )
    private void alchemancystuck$sbahjThrow(Args args, @Share("sbahj") LocalBooleanRef sbahj) {
        if (sbahj.get()) {
            args.set(4, SBAHJARANG_VELOCITY);
            args.set(5, SBAHJARANG_INACCURACY);
        }
    }
}
