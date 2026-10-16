package org.wilkretawesomesauce.alchemancystuck.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonSoundEvents;

@EventBusSubscriber(modid = Alchemancystuck.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class RageMusicPlayer {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean wasPossessing = false;
    private static SoundInstance currentTrack = null;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft client = Minecraft.getInstance();
        boolean possessing = RageCameraHandler.isPossessing(client);

        if (possessing != wasPossessing)
            LOGGER.info("alchemancystuck debug [RageMusicPlayer]: possessing changed to {}", possessing);

        if (possessing && !wasPossessing) {
            SoundEvent track = AddonSoundEvents.RAGE_THE_CARNIVAL.get();
            currentTrack = SimpleSoundInstance.forMusic(track);
            LOGGER.info("alchemancystuck debug [RageMusicPlayer]: playing track={}, instance location={}",
                    track.getLocation(), currentTrack.getLocation());
            client.getSoundManager().play(currentTrack);
            LOGGER.info("alchemancystuck debug [RageMusicPlayer]: isActive after play={}", client.getSoundManager().isActive(currentTrack));
        } else if (!possessing && wasPossessing && currentTrack != null) {
            client.getSoundManager().stop(currentTrack);
            currentTrack = null;
        }

        wasPossessing = possessing;
    }
}
