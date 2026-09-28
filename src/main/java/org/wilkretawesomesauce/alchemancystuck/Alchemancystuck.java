package org.wilkretawesomesauce.alchemancystuck;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.wilkretawesomesauce.alchemancystuck.command.PropertiesCommand;
import org.wilkretawesomesauce.alchemancystuck.effect.RageEffect;
import org.wilkretawesomesauce.alchemancystuck.properties.BadassProperty;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonEntities;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonMobEffects;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonProperties;

@Mod(Alchemancystuck.MODID)
public class Alchemancystuck {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "alchemancystuck";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    public Alchemancystuck(IEventBus modEventBus, ModContainer modContainer) {
        AddonProperties.PROPERTIES.register(modEventBus);
        AddonEntities.ENTITIES.register(modEventBus);
        AddonMobEffects.MOB_EFFECTS.register(modEventBus);
        modEventBus.addListener(AddonEntities::onEntityAttributeCreation);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(BadassProperty::onExplosionDetonate);
        NeoForge.EVENT_BUS.addListener(RageEffect::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(RageEffect::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(RageEffect::onEffectExpired);
        NeoForge.EVENT_BUS.addListener(RageEffect::onEffectRemoved);
        NeoForge.EVENT_BUS.addListener(RageEffect::onPlayerDeath);
        // HIGH, not the default NORMAL - Alchemancy's own PropertyEventHandler#onCriticalHit (the
        // listener that reads event.isCriticalHit() to fire e.g. Smiting's onCriticalAttack) registers
        // at NORMAL too, and same-priority listeners across two different mods run in an order neither
        // mod controls. Forcing the crit has to happen strictly before that read, or Alchemancy's own
        // crit-triggered property hooks silently never see it.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, RageEffect::onCriticalHit);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PropertiesCommand.register(event.getDispatcher());
    }
}
