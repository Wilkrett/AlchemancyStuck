package org.wilkretawesomesauce.alchemancystuck.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Two private {@code ServerPlayer} fields a rage clone needs to force to specific values right after
 * spawning, neither of which has a vanilla setter:
 * <ul>
 *   <li>{@code spawnInvulnerableTime} - every fresh {@code ServerPlayer} starts with 60 (3 seconds of
 *   spawn-protection meant for a real player who just joined) - {@code ServerPlayer#hurt} explicitly
 *   refuses all damage while this is {@code > 0}, but a rage clone is meant to be fightable the
 *   instant it exists.</li>
 *   <li>{@code isChangingDimension} - {@code ServerPlayer#isInvulnerableTo} unconditionally returns
 *   true while this is set, normally cleared once a real client acknowledges the dimension-change
 *   packet sequence. {@code RageCloneEntity#spawn}'s cross-dimension teleport (needed because
 *   {@code PlayerList#load} always resets a fresh/save-less player to the overworld) routes through
 *   {@code ServerPlayer#changeDimension}, which sets this - and since the clone's connection is fake
 *   and never acknowledges anything, it would otherwise stay stuck true forever, making the clone
 *   permanently invulnerable to everything.</li>
 * </ul>
 */
@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {

    @Accessor("spawnInvulnerableTime")
    void alchemancystuck$setSpawnInvulnerableTime(int ticks);

    @Accessor("isChangingDimension")
    void alchemancystuck$setChangingDimension(boolean changingDimension);
}
