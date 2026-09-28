package org.wilkretawesomesauce.alchemancystuck.mixin;

import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlotGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.wilkretawesomesauce.alchemancystuck.client.SbahjTextTransformer;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonProperties;

/**
 * Both private {@code drawInternal} overloads (one per {@code String}, one per
 * {@link FormattedCharSequence}) are the two chokepoints every public {@code Font.drawInBatch} overload
 * ultimately funnels through - the {@code Component} overload converts to a
 * {@code FormattedCharSequence} and calls the second one - so together they cover essentially all
 * on-screen text (tooltips, buttons, chat, signs, books, HUD) without needing to also hook every public
 * overload individually.
 *
 * <p>Client-only mixin (see {@code alchemancystuck.mixins.json}'s {@code "client"} array, not
 * {@code "mixins"}) - {@code Font} doesn't exist on a dedicated server, and this addon's other mixins
 * already learned the hard way (see {@code VillagerDiscountMixin}'s history) that Mixin failures on
 * launch are not gentle.
 */
@Mixin(Font.class)
public abstract class FontSbahjMixin {

    @ModifyVariable(
            method = "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I",
            at = @At("HEAD"),
            argsOnly = true
    )
    private String alchemancystuck$sbahjifyString(String text) {
        return isSbahjWorn() ? SbahjTextTransformer.transform(text) : text;
    }

    @ModifyVariable(
            method = "drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
            at = @At("HEAD"),
            argsOnly = true
    )
    private FormattedCharSequence alchemancystuck$sbahjifySequence(FormattedCharSequence sequence) {
        return isSbahjWorn() ? SbahjTextTransformer.transform(sequence) : sequence;
    }

    private static boolean isSbahjWorn() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && InfusedPropertiesHelper.hasItemWithProperty(player, AddonProperties.SBAHJ, false, EquipmentSlotGroup.ARMOR);
    }
}
