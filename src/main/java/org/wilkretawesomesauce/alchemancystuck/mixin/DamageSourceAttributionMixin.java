package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.entity.RageCloneEntity;

@Mixin(DamageSource.class)
public abstract class DamageSourceAttributionMixin {

    @ModifyReturnValue(method = "getEntity", at = @At("RETURN"))
    private Entity alchemancystuck$redirectCausingEntity(Entity original) {
        return redirectToOwner(original);
    }

    @ModifyReturnValue(method = "getDirectEntity", at = @At("RETURN"))
    private Entity alchemancystuck$redirectDirectEntity(Entity original) {
        return redirectToOwner(original);
    }

    private static Entity redirectToOwner(@Nullable Entity original) {
        if (original instanceof RageCloneEntity clone) {
            ServerPlayer owner = clone.getOwnerPlayer();
            if (owner != null)
                return owner;
        }
        return original;
    }
}
