package org.wilkretawesomesauce.alchemancystuck.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;
import org.wilkretawesomesauce.alchemancystuck.entity.RagePathBrainEntity;

public class AddonEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Alchemancystuck.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<RagePathBrainEntity>> RAGE_PATH_BRAIN = ENTITIES.register("rage_path_brain",
            () -> EntityType.Builder.of(RagePathBrainEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(0)
                    .build(ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "rage_path_brain").toString()));

    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(RAGE_PATH_BRAIN.get(), RagePathBrainEntity.createAttributes().build());
    }
}
