package org.wilkretawesomesauce.alchemancystuck.entity;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;
import org.wilkretawesomesauce.alchemancystuck.mixin.ConnectionChannelAccessor;

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
