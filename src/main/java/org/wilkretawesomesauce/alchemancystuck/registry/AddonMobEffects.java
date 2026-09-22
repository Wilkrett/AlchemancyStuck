package org.wilkretawesomesauce.alchemancystuck.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;
import org.wilkretawesomesauce.alchemancystuck.effect.RageEffect;

public class AddonMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Alchemancystuck.MODID);

    public static final DeferredHolder<MobEffect, RageEffect> RAGE = MOB_EFFECTS.register("rage", RageEffect::new);
}
