package org.wilkretawesomesauce.alchemancystuck.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonEntities;

/**
 * {@code value = Dist.CLIENT} keeps FML from ever loading this class on a dedicated server, which
 * matters here specifically because {@link EntityRenderersEvent} and the renderer types below live
 * in client-only packages that simply aren't present on a server classpath.
 */
@EventBusSubscriber(modid = Alchemancystuck.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AddonEntities.RAGE_CLONE.get(), RageCloneRenderer::new);
    }
}
