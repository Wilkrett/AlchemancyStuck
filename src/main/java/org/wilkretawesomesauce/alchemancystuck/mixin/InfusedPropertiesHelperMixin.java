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
