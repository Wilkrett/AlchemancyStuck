package org.wilkretawesomesauce.alchemancystuck.entity;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * A real {@code ServerGamePacketListenerImpl} (not a hand-rolled stand-in) so every other vanilla
 * system that expects {@code ServerPlayer#connection} to behave like the genuine article keeps
 * working - the only thing overridden is {@link #send}, since there's no real client on the other end
 * of {@link FakeConnection} to receive anything.
 */
public class RageFakePlayerNetHandler extends ServerGamePacketListenerImpl {

    public RageFakePlayerNetHandler(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        super(server, connection, player, cookie);
    }

    @Override
    public void send(Packet<?> packet) {
    }
}
