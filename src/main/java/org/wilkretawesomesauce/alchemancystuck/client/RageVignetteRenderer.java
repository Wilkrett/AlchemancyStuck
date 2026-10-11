package org.wilkretawesomesauce.alchemancystuck.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.cibernet.alchemancy.util.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;
import org.wilkretawesomesauce.alchemancystuck.properties.RageInducingProperty;

/**
 * Reuses vanilla's own night-vignette trick ({@code Gui#renderVignette}) rather than inventing a new
 * one: {@code textures/misc/vignette.png} is a grayscale gradient (black at the center, light gray at
 * the edges) blended with {@code blendFuncSeparate(ZERO, ONE_MINUS_SRC_COLOR, ONE, ZERO)}, so
 * {@code outColor = dstColor * (1 - texel * tint)}. A black center means the center channel is
 * multiplied by {@code (1 - 0)} - always untouched - while the lighter edges get scaled down by
 * however much of that channel the tint asks to crush.
 *
 * <p>That means the tint is "how much to remove," not "what color to add" - to make the edges read as
 * a given color the tint fed in has to be {@code 1 - color}, the same inversion vanilla's own
 * world-border-warning vignette uses (it sets red to {@code 0} to *keep* red, not remove it, letting
 * blue/green get crushed instead so the edges read red). {@link RageInducingProperty#COLOR_CYCLE} is
 * reused as-is here (inverted below) so the vignette cycles in step with the item's own tint.
 */
@EventBusSubscriber(modid = Alchemancystuck.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class RageVignetteRenderer {

    private static final ResourceLocation VIGNETTE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");

    @SubscribeEvent
    public static void onRenderCameraOverlays(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.CAMERA_OVERLAYS.equals(event.getName()))
            return;

        if (!RageCameraHandler.isPossessing(Minecraft.getInstance()))
            return;

        int color = ColorUtils.interpolateColorsOverTime(RageInducingProperty.COLOR_CYCLE_SPEED, RageInducingProperty.COLOR_CYCLE);
        float r = 1.0F - FastColor.ARGB32.red(color) / 255.0F;
        float g = 1.0F - FastColor.ARGB32.green(color) / 255.0F;
        float b = 1.0F - FastColor.ARGB32.blue(color) / 255.0F;

        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        graphics.setColor(r, g, b, 1.0F);
        graphics.blit(VIGNETTE_TEXTURE, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
