package org.wilkretawesomesauce.alchemancystuck.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wilkretawesomesauce.alchemancystuck.properties.DiscountProperty;

/**
 * {@code updateSpecialPrices(Player)} is vanilla's own per-trade-window-open hook - it's how the Hero
 * of the Village discount gets applied fresh for whichever player is opening the trade GUI right now
 * (see the reputation and Hero of the Village blocks right before this injector's tail), and
 * {@code stopTrading} calls {@code resetSpecialPriceDiff} on close, so riding the same
 * {@code addToSpecialPriceDiff} mechanism here needs no separate reset of our own.
 *
 * <p>{@code getOffers()} is called via a plain cast rather than {@code @Shadow} - it's declared on
 * {@code AbstractVillager}, not {@code Villager} itself, and {@code @Shadow} only resolves methods
 * actually present in the exact {@code @Mixin} target class's own bytecode, not inherited ones (this
 * mixin crashed on launch with "was not located in the target class" until this was fixed). Since it's
 * public, a cast is all a plain method call needs anyway.
 *
 * <p>{@link DiscountProperty#getRateFor} folds Discount + SBAHJ's surcharge in already (and its own
 * 30%-discount/no-cap-on-surcharge asymmetry), so this just signs the same {@code addToSpecialPriceDiff}
 * adjustment by the rate and rolls the mocking "economy" line once per trade-window-open (matching this
 * hook's own granularity) whenever the net rate comes out negative.
 */
@Mixin(Villager.class)
public abstract class VillagerDiscountMixin {

    private static final float TAUNT_CHANCE = 0.2F;

    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void alchemancystuck$applyDiscount(Player player, CallbackInfo ci) {
        float rate = DiscountProperty.getRateFor(player);
        if (rate == 0F)
            return;

        Villager villager = (Villager) (Object) this;
        for (MerchantOffer offer : villager.getOffers()) {
            int magnitude = Math.max(1, Mth.floor(offer.getBaseCostA().getCount() * Math.abs(rate)));
            offer.addToSpecialPriceDiff(rate > 0F ? -magnitude : magnitude);
        }

        if (rate < 0F && player.getRandom().nextFloat() < TAUNT_CHANCE)
            player.sendSystemMessage(Component.translatable("message.alchemancystuck.sbahj.economy"));
    }
}
