package org.wilkretawesomesauce.alchemancystuck.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonAttachments;

@EventBusSubscriber(modid = Alchemancystuck.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class RageCameraHandler {

    private static boolean wasPossessing = false;

    public static boolean isPossessing(Minecraft client) {
        Entity camera = client.getCameraEntity();
        return client.player != null && client.player.isSpectator() && camera != null && camera.getData(AddonAttachments.IS_RAGE_CLONE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft client = Minecraft.getInstance();
        boolean possessing = isPossessing(client);

        if (possessing) {
            if (client.options.getCameraType().isFirstPerson())
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (wasPossessing) {
            client.options.setCameraType(CameraType.FIRST_PERSON);
        }

        wasPossessing = possessing;
    }
}
