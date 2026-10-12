package org.wilkretawesomesauce.alchemancystuck.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wilkretawesomesauce.alchemancystuck.effect.RageEffect;
import org.wilkretawesomesauce.alchemancystuck.entity.RageCloneEntity;

@Mixin(ServerPlayer.class)
public abstract class RageCameraLockMixin {

    @Inject(method = "setCamera", at = @At("HEAD"), cancellable = true)
    private void alchemancystuck$lockCameraDuringRage(Entity entityToSpectate, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (RageEffect.isRaging(self) && !(entityToSpectate instanceof RageCloneEntity clone && RageEffect.isRaging(self, clone)))
            ci.cancel();
    }
}
