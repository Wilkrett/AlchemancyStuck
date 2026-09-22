package org.wilkretawesomesauce.alchemancystuck.mixin;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes a setter for {@code Connection}'s private {@code channel} field - needed by
 * {@code FakeConnection} to give itself a real (embedded, in-memory) netty channel, since a plain
 * {@code new Connection(PacketFlow)} otherwise leaves it null and a great many vanilla code paths
 * (packet flushing, disconnection handling) unconditionally touch {@code channel()}.
 */
@Mixin(Connection.class)
public interface ConnectionChannelAccessor {
    @Accessor("channel")
    void alchemancystuck$setChannel(Channel channel);
}
