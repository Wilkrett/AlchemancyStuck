package org.wilkretawesomesauce.alchemancystuck.registry;

import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;

import java.util.function.Supplier;

public class AddonAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Alchemancystuck.MODID);

    /**
     * The client never sees a rage clone as {@code RageCloneEntity} - that class only exists
     * server-side; any player-type entity that isn't the client's own local player gets mirrored
     * client-side as a generic {@code RemotePlayer}, so an {@code instanceof RageCloneEntity} check
     * (what this used to be) is always false there. This attachment is a synced marker the server
     * sets on the clone at spawn so the client can actually tell "is the entity my camera is locked
     * onto my own rage clone" without needing to see its real Java type.
     */
    public static final Supplier<AttachmentType<Boolean>> IS_RAGE_CLONE = ATTACHMENT_TYPES.register("is_rage_clone",
            () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());
}
