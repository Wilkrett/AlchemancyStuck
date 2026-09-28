package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.client.RageCameraHandler;

/**
 * The owner's real gamemode is genuinely {@code GameType.SPECTATOR} while raging (see {@code
 * RageEffect#startRage}) - that's what makes their own body safe/non-interactive while the clone fights.
 * The side effect vanilla's {@code Gui} was never meant to handle is that spectator mode also hides the
 * whole survival HUD (hotbar, hearts, hunger, XP bar) in favor of the spectator command-wheel overlay,
 * which reads as "nothing is happening" rather than "your clone is mid-fight."
 * <p>
 * {@code Gui} gates all of these on a handful of {@code MultiPlayerGameMode} queries -
 * {@code getPlayerMode() == SPECTATOR}, {@code canHurtPlayer()}, {@code hasExperience()} - so faking
 * their answers specifically while {@link RageCameraHandler#isPossessing} is true (camera-locked onto a
 * live rage clone, not just "gamemode happens to be spectator" for some unrelated reason) makes {@code
 * Gui} render the normal survival HUD again. Since {@code renderItemHotbar}/{@code renderPlayerHealth}
 * pull their data from {@code Gui#getCameraPlayer()} (the camera target, i.e. the clone itself) rather
 * than the local player, what's shown is the clone's own live health/hunger/hotbar, not a stale
 * snapshot of the owner's - actual combat feedback, not just an unhidden but frozen UI.
 * <p>
 * This only touches {@code Gui}'s rendering queries, not {@code MultiPlayerGameMode} itself - the owner's
 * actual interaction permissions (block breaking, attacking, placing) are untouched and still correctly
 * blocked server-side, this is purely cosmetic.
 */
@Mixin(Gui.class)
public abstract class RageHudMixin {

    @ModifyExpressionValue(
            method = {"renderHotbar", "maybeRenderSelectedItemName", "maybeRenderSpectatorTooltip"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;getPlayerMode()Lnet/minecraft/world/level/GameType;")
    )
    private GameType alchemancystuck$hidePossessedSpectatorMode(GameType mode) {
        return RageCameraHandler.isPossessing(Minecraft.getInstance()) ? GameType.SURVIVAL : mode;
    }

    @ModifyExpressionValue(
            method = "maybeRenderPlayerHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;canHurtPlayer()Z")
    )
    private boolean alchemancystuck$showPossessedHealth(boolean canHurtPlayer) {
        return canHurtPlayer || RageCameraHandler.isPossessing(Minecraft.getInstance());
    }

    @ModifyExpressionValue(
            method = "isExperienceBarVisible",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;hasExperience()Z")
    )
    private boolean alchemancystuck$showPossessedExperience(boolean hasExperience) {
        return hasExperience || RageCameraHandler.isPossessing(Minecraft.getInstance());
    }
}
