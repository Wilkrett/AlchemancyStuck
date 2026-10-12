package org.wilkretawesomesauce.alchemancystuck.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {

    @Accessor("spawnInvulnerableTime")
    void alchemancystuck$setSpawnInvulnerableTime(int ticks);

    @Accessor("isChangingDimension")
    void alchemancystuck$setChangingDimension(boolean changingDimension);
}
