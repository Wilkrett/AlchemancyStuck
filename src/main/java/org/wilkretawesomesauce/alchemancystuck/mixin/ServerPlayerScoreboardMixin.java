package org.wilkretawesomesauce.alchemancystuck.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wilkretawesomesauce.alchemancystuck.entity.RageCloneEntity;

/**
 * {@code ServerPlayer#doTick} (needed for real per-tick Player/LivingEntity behavior - see
 * {@code RageCloneEntity#tick}'s own doc comment) calls this every tick for health/food/air/armor/xp/
 * level, writing a score into whatever scoreboard objectives track those criteria. That's how a rage
 * clone ends up showing up in a leaderboard-style sidebar/tab-list objective mid-rage - it's a real,
 * tracked score holder as far as the scoreboard is concerned, same as any other player.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerScoreboardMixin {

    @Inject(method = "updateScoreForCriteria", at = @At("HEAD"), cancellable = true)
    private void alchemancystuck$suppressCloneScore(ObjectiveCriteria criteria, int points, CallbackInfo ci) {
        if (((Object) this) instanceof RageCloneEntity)
            ci.cancel();
    }
}
