package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.entity.RageCloneEntity;
import org.wilkretawesomesauce.alchemancystuck.entity.RageFakePlayerNetHandler;

/**
 * {@code PlayerList#placeNewPlayer} unconditionally does {@code new ServerGamePacketListenerImpl(...)}
 * for whatever {@code ServerPlayer} it's given - there's no factory hook or subclass-aware branch to
 * override, so the only way to have it construct {@link RageFakePlayerNetHandler} instead for our own
 * fake players is to wrap that exact constructor call. Real players are untouched
 * ({@code original.call(...)}).
 */
@Mixin(PlayerList.class)
public abstract class PlayerListNetHandlerMixin {

    @WrapOperation(method = "placeNewPlayer", at = @At(value = "NEW", target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"))
    private ServerGamePacketListenerImpl alchemancystuck$useFakeNetHandler(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie, Operation<ServerGamePacketListenerImpl> original) {
        if (player instanceof RageCloneEntity)
            return new RageFakePlayerNetHandler(server, connection, player, cookie);

        return original.call(server, connection, player, cookie);
    }

    /**
     * Suppresses the "X joined the game" broadcast specifically for rage clones - they aren't a real
     * player connecting, and the message is just confusing chat noise every time a rage triggers.
     */
    @WrapOperation(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void alchemancystuck$suppressCloneJoinMessage(PlayerList instance, Component message, boolean bypassHiddenChat, Operation<Void> original, @Local(argsOnly = true) ServerPlayer player) {
        if (!(player instanceof RageCloneEntity))
            original.call(instance, message, bypassHiddenChat);
    }
}
