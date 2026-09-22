package org.wilkretawesomesauce.alchemancystuck.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;
import org.wilkretawesomesauce.alchemancystuck.entity.RageCloneEntity;

public class AddonEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Alchemancystuck.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<RageCloneEntity>> RAGE_CLONE = ENTITIES.register("rage_clone",
            () -> EntityType.Builder.of(RageCloneEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "rage_clone").toString()));

    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(RAGE_CLONE.get(), RageCloneEntity.createAttributes().build());
    }
}
