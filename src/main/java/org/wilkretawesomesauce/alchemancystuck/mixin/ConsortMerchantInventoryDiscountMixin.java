package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mraof.minestuck.inventory.ConsortMerchantInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wilkretawesomesauce.alchemancystuck.properties.DiscountProperty;

@Mixin(ConsortMerchantInventory.class)
public abstract class ConsortMerchantInventoryDiscountMixin {

    private static final float TAUNT_CHANCE = 0.2F;

    @Shadow
    @Final
    private int[] prices;

    @Inject(method = "handlePurchase", at = @At("HEAD"))
    private void alchemancystuck$applyDiscount(ServerPlayer player, boolean all, int index, CallbackInfo ci, @Share("originalPrice") LocalIntRef originalPrice) {
        if (index < 0 || index >= prices.length) {
            originalPrice.set(0);
            return;
        }

        originalPrice.set(prices[index]);

        if (prices[index] == 0)
            return;

        float rate = DiscountProperty.getRateFor(player);
        if (rate == 0F)
            return;

        prices[index] = Math.max(1, (int) Math.floor(prices[index] * (1.0F - rate)));

        if (rate < 0F && player.getRandom().nextFloat() < TAUNT_CHANCE)
            player.sendSystemMessage(Component.translatable("message.alchemancystuck.sbahj.economy"));
    }

    @Inject(method = "handlePurchase", at = @At("RETURN"))
    private void alchemancystuck$restorePrice(ServerPlayer player, boolean all, int index, CallbackInfo ci, @Share("originalPrice") LocalIntRef originalPrice) {
        if (index >= 0 && index < prices.length)
            prices[index] = originalPrice.get();
    }
}
