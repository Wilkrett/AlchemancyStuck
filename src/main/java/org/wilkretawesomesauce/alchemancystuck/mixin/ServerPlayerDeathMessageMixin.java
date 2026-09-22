package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.entity.RageCloneEntity;

/**
 * Suppresses the "X was slain by Y"-style death broadcast specifically for rage clones -
 * {@code RageCloneEntity#die} already handles ending the rage itself; broadcasting the clone's death
 * to the whole server's chat every time a rage fight goes badly is just noise, not a real player death.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMessageMixin {

    @WrapOperation(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void alchemancystuck$suppressCloneDeathMessage(PlayerList instance, Component message, boolean bypassHiddenChat, Operation<Void> original) {
        if (!(((Object) this) instanceof RageCloneEntity))
            original.call(instance, message, bypassHiddenChat);
    }
}
