package org.wilkretawesomesauce.alchemancystuck.mixin;

import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.cibernet.alchemancy.properties.Property;
import net.cibernet.alchemancy.registries.AlchemancyProperties;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonProperties;

import java.util.function.Consumer;

/**
 * Unlike {@code DeadProperty}, whose "wipes the item" reputation is really just its own overrides
 * (clears attributes, cancels right-click), Perfectly Generic is meant to genuinely blank out every
 * other property on the item - the way alchemizing with nothing but a Perfectly Generic dowel yields
 * an item with no properties at all. That can't be done from a {@code Property} subclass: nothing in
 * the base mod's own property-suppression logic (see {@code InfusedPropertiesHelper#hasProperty}) is
 * generic - it's a hardcoded identity check against the {@code DEAD} holder, and it only ever
 * suppresses innate properties, not infused ones. So this mixin extends the same choke points
 * ({@code hasProperty} and {@code forEachProperty}) to treat Perfectly Generic as exclusive of
 * infused, innate, and dormant properties alike.
 *
 * <p>{@link #hasPerfectlyGeneric} deliberately never calls the (mixed) {@code hasProperty} methods -
 * an earlier version routed through them and stack-overflowed almost immediately. {@code hasProperty}'s
 * own body calls itself for {@code AlchemancyProperties.DEAD} as part of its innate-property check;
 * since {@code DEAD != PERFECTLY_GENERIC}, that call re-entered this mixin's suppression check, which
 * called back into {@code hasProperty(stack, PERFECTLY_GENERIC)}, which ran the original body again,
 * which checked {@code DEAD} again - an unbounded PERFECTLY_GENERIC&harr;DEAD cycle, not the one-level
 * recursion assumed at the time. Building the check from the non-recursive
 * {@code hasInfusedProperty}/{@code hasInnateProperty}/{@code hasDormantProperty} primitives instead
 * (none of which call back into {@code hasProperty}) avoids re-entering this mixin at all.
 */
@Mixin(InfusedPropertiesHelper.class)
public abstract class InfusedPropertiesHelperMixin {

    @Inject(method = "hasProperty(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;)Z", at = @At("HEAD"), cancellable = true)
    private static void alchemancystuck$suppressOtherProperties(ItemStack stack, Holder<Property> property, CallbackInfoReturnable<Boolean> cir) {
        if (property != AddonProperties.PERFECTLY_GENERIC && hasPerfectlyGeneric(stack))
            cir.setReturnValue(false);
    }

    @Inject(method = "hasProperty(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private static void alchemancystuck$suppressOtherPropertyTags(ItemStack stack, TagKey<Property> propertyTag, CallbackInfoReturnable<Boolean> cir) {
        if (!AddonProperties.PERFECTLY_GENERIC.is(propertyTag) && hasPerfectlyGeneric(stack))
            cir.setReturnValue(false);
    }

    @Inject(method = "forEachProperty(Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;Z)V", at = @At("HEAD"), cancellable = true)
    private static void alchemancystuck$onlyEmitPerfectlyGeneric(ItemStack stack, Consumer<Holder<Property>> consumer, boolean checkToggle, CallbackInfo ci) {
        if (hasPerfectlyGeneric(stack)) {
            consumer.accept(AddonProperties.PERFECTLY_GENERIC);
            ci.cancel();
        }
    }

    private static boolean hasPerfectlyGeneric(ItemStack stack) {
        Holder<Property> perfectlyGeneric = AddonProperties.PERFECTLY_GENERIC;

        if (InfusedPropertiesHelper.hasInfusedProperty(stack, perfectlyGeneric) || InfusedPropertiesHelper.hasInnateProperty(stack, perfectlyGeneric))
            return true;

        boolean awakened = InfusedPropertiesHelper.hasInfusedProperty(stack, AlchemancyProperties.AWAKENED) || InfusedPropertiesHelper.hasInnateProperty(stack, AlchemancyProperties.AWAKENED);
        return awakened && InfusedPropertiesHelper.hasDormantProperty(stack, perfectlyGeneric);
    }
}
