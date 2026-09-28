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

/**
 * {@code prices} is a shared array on the consort's own stock (persisted, one entry per shop slot,
 * common to every player who trades with that consort) - it isn't safe to discount permanently.
 * Instead this temporarily overwrites the slot's entry for the duration of a single
 * {@code handlePurchase} call (server is single-threaded per level, and the method doesn't yield
 * mid-call, so there's no reentrancy to worry about) and restores it before returning, so the discount
 * only ever applies to whichever player is buying right now.
 *
 * <p>{@code handlePurchase} reads {@code prices[index]} directly for both the affordability check and
 * the actual boondollar deduction - unlike {@code calculatePrice}/{@code createPricesFor}, which only
 * affects what's *displayed* via consort reputation and never touches what's actually charged. So
 * patching the shared array here (rather than only where the UI price is computed) is what actually
 * makes the discount apply to the purchase itself, not just the shown price.
 *
 * <p>{@link DiscountProperty#getRateFor} folds Discount + SBAHJ's surcharge in already (and its own
 * 30%-discount/no-cap-on-surcharge asymmetry), so this just turns the rate into a multiplier and rolls
 * the mocking "economy" line once per actual purchase, matching this hook's own granularity (one
 * {@code handlePurchase} call per trade, unlike the villager mixin's once-per-window-open) whenever the
 * net rate comes out negative.
 */
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
