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
