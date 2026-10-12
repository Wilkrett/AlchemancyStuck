package org.wilkretawesomesauce.alchemancystuck.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wilkretawesomesauce.alchemancystuck.entity.RageCloneEntity;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerScoreboardMixin {

    @Inject(method = "updateScoreForCriteria", at = @At("HEAD"), cancellable = true)
    private void alchemancystuck$suppressCloneScore(ObjectiveCriteria criteria, int points, CallbackInfo ci) {
        if (((Object) this) instanceof RageCloneEntity)
            ci.cancel();
    }
}
