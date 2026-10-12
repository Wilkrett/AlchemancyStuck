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
