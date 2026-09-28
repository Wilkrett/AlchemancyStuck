package org.wilkretawesomesauce.alchemancystuck.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wilkretawesomesauce.alchemancystuck.effect.RageEffect;
import org.wilkretawesomesauce.alchemancystuck.entity.RageCloneEntity;

/**
 * {@code setCamera} is how a spectating owner's camera gets reassigned - including by things this addon
 * never intended as an "exit rage-spectating early" button: sneaking while spectating triggers
 * {@code ServerPlayer#tick}'s own {@code wantsToStopRiding() -> setCamera(this)} reversion, and clicking
 * another player in the spectator player-list UI sends {@code ServerboundTeleportToEntityPacket} - which
 * only repositions the owner's own entity, but since a normal spectator's camera never moved off their
 * own body in the first place, that reposition is all it takes there. Once camera is detached, {@code
 * ServerPlayer#tick}'s per-tick "follow the camera target" {@code absMoveTo} stops running entirely, so
 * the owner is suddenly a completely free-flying spectator mid-rage instead of locked to watching their
 * own clone fight.
 * <p>
 * Cancelling any reassignment away from the current rage clone while raging blocks both paths at once -
 * without a successful {@code setCamera} call, the follow logic never lets go, so even a one-off
 * teleport-to-entity reposition gets overwritten the very next tick regardless. {@link RageEffect}'s own
 * legitimate calls are unaffected: {@code startRage} targets the clone itself (allowed), and {@code
 * cleanupRage} always removes the owner from the active-rage tracking before it calls {@code
 * setCamera(owner)} to hand control back, so {@link RageEffect#isRaging(ServerPlayer)} already reads
 * false by the time that runs.
 */
@Mixin(ServerPlayer.class)
public abstract class RageCameraLockMixin {

    @Inject(method = "setCamera", at = @At("HEAD"), cancellable = true)
    private void alchemancystuck$lockCameraDuringRage(Entity entityToSpectate, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (RageEffect.isRaging(self) && !(entityToSpectate instanceof RageCloneEntity clone && RageEffect.isRaging(self, clone)))
            ci.cancel();
    }
}
