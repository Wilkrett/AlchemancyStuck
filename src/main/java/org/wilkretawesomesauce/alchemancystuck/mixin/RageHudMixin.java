package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.client.RageCameraHandler;

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
