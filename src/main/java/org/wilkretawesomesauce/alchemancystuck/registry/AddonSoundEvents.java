package org.wilkretawesomesauce.alchemancystuck.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;

public class AddonSoundEvents {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Alchemancystuck.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> RAGE_BAD_TO_THE_BONE = SOUND_EVENTS.register("rage.bad_to_the_bone",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "rage.bad_to_the_bone")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RAGE_THE_CARNIVAL = SOUND_EVENTS.register("rage.the_carnival",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "rage.the_carnival")));
}
