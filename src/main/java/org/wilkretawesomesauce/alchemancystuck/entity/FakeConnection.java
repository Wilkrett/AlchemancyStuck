package org.wilkretawesomesauce.alchemancystuck.entity;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;
import org.wilkretawesomesauce.alchemancystuck.mixin.ConnectionChannelAccessor;

/**
 * A {@link Connection} with nothing behind it but an in-memory {@link EmbeddedChannel} - no socket,
 * no real client. {@code PlayerList#placeNewPlayer} requires a {@code Connection} to construct the
 * real {@code ServerGamePacketListenerImpl} it hands back as {@code ServerPlayer#connection}; this is
 * what lets that happen without a real network connection existing at all. Plain
 * {@code new Connection(PacketFlow)} leaves its channel field null, which a lot of vanilla code
 * unconditionally dereferences - {@link ConnectionChannelAccessor} (a mixin, since that field is
 * private with no vanilla setter) is what lets this give itself one.
 */
public class FakeConnection extends Connection {

    public FakeConnection(PacketFlow receiving) {
        super(receiving);
        ((ConnectionChannelAccessor) this).alchemancystuck$setChannel(new EmbeddedChannel());
    }

    @Override
    public void setReadOnly() {
    }

    @Override
    public void handleDisconnection() {
    }

    @Override
    public void setListenerForServerboundHandshake(PacketListener packetListener) {
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetListener) {
    }
}
