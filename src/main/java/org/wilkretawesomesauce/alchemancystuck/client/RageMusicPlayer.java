package org.wilkretawesomesauce.alchemancystuck.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonSoundEvents;

/**
 * Purely a local, per-player experience - the server has no say in what plays here, so this rides
 * {@link RageCameraHandler#isPossessing}'s own client-tick transition (same "possessing" definition,
 * tracked separately rather than shared - this class only cares about start/end, not the per-tick
 * camera state {@link RageCameraHandler} manages) rather than anything server-driven.
 */
@EventBusSubscriber(modid = Alchemancystuck.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class RageMusicPlayer {

    private static boolean wasPossessing = false;
    private static SoundInstance currentTrack = null;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft client = Minecraft.getInstance();
        boolean possessing = RageCameraHandler.isPossessing(client);

        if (possessing && !wasPossessing) {
            SoundEvent track = client.player.getRandom().nextBoolean()
                    ? AddonSoundEvents.RAGE_BAD_TO_THE_BONE.get()
                    : AddonSoundEvents.RAGE_THE_CARNIVAL.get();
            currentTrack = SimpleSoundInstance.forMusic(track);
            client.getSoundManager().play(currentTrack);
        } else if (!possessing && wasPossessing && currentTrack != null) {
            client.getSoundManager().stop(currentTrack);
            currentTrack = null;
        }

        wasPossessing = possessing;
    }
}
