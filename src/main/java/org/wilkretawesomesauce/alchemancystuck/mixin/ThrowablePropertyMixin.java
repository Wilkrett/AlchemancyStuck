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

/**
 * Makes an infused, hand-thrown item ({@code ThrowableProperty#throwItem(Level, LivingEntity, ItemStack)},
 * the {@code onRightClickItem} path) fly exactly like Minestuck's own {@code sbahjarang}
 * ({@code ConsumableProjectileWeaponItem}'s {@code velocity}/{@code accuracy} of {@code 0.5F}/{@code 20.0F})
 * whenever SBAHJ is also on the item, overriding Throwable's own velocity/inaccuracy (and Sharpshooting's
 * 1.5x boost to it) entirely rather than combining with them.
 *
 * <p>The SBAHJ check has to happen at {@code HEAD}, before anything else in the method runs, and be
 * carried to the {@code shootFromRotation} call via a {@code @Share}d local rather than re-checked
 * there directly: for the common non-{@code ProjectileItem} branch, the method calls
 * {@code stack.split(1)} on the held stack to build the thrown {@code InfusedItemProjectile} *before*
 * {@code shootFromRotation} is invoked, and for a normal count-1 infused item that split empties the
 * original stack in place. {@code InfusedPropertiesHelper.hasProperty} short-circuits to {@code false}
 * on an empty stack, so checking the same local right at the {@code shootFromRotation} call site (which
 * is what a plain {@code @Local} capture there would see) always missed - the property was still on the
 * stack, just no longer on a stack {@code hasProperty} would even look at.
 */
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
