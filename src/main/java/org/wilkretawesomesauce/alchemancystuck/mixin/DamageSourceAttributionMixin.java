package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.entity.RageCloneEntity;

/**
 * Every kill-attribution consumer - vanilla's own {@code LivingEntity#lastHurtByPlayer}/{@code
 * getKillCredit} (loot tables' player-only drops, {@code Player#awardKillScore}'s stats/scoreboard/
 * advancement trigger), and third-party mods reading a death's killer off {@code LivingDeathEvent}
 * (e.g. Minestuck's Echeladder exp-from-kills, which checks {@code event.getSource().getEntity()
 * instanceof ServerPlayer}) - ultimately reads the killer through one of these two getters. Since
 * {@link RageCloneEntity} really is a {@code ServerPlayer}, none of that code has any reason to treat it
 * as anything other than the actual killer, so without this every reward from a rage kill (XP-from-kill
 * mods, stats, advancements, loot conditions) lands on the clone's own throwaway profile instead of the
 * owner's. Redirecting at this one choke point fixes all of them at once, rather than chasing down each
 * consumer individually.
 *
 * <p>Vanilla already does exactly this substitution for a tamed wolf's owner (see {@code
 * LivingEntity#hurt}'s {@code TamableAnimal} branch) - this just extends the same idea to a "pet" that
 * happens to itself be a real {@code Player}, which is why it isn't caught by that existing branch.
 */
@Mixin(DamageSource.class)
public abstract class DamageSourceAttributionMixin {

    @ModifyReturnValue(method = "getEntity", at = @At("RETURN"))
    private Entity alchemancystuck$redirectCausingEntity(Entity original) {
        return redirectToOwner(original);
    }

    @ModifyReturnValue(method = "getDirectEntity", at = @At("RETURN"))
    private Entity alchemancystuck$redirectDirectEntity(Entity original) {
        return redirectToOwner(original);
    }

    private static Entity redirectToOwner(@Nullable Entity original) {
        if (original instanceof RageCloneEntity clone) {
            ServerPlayer owner = clone.getOwnerPlayer();
            if (owner != null)
                return owner;
        }
        return original;
    }
}
