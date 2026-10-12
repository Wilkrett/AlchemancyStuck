package org.wilkretawesomesauce.alchemancystuck.registry;

import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;

import java.util.function.Supplier;

public class AddonAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Alchemancystuck.MODID);

    public static final Supplier<AttachmentType<Boolean>> IS_RAGE_CLONE = ATTACHMENT_TYPES.register("is_rage_clone",
            () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());
}
