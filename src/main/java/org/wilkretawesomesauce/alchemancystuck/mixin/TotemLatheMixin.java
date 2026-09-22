package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mraof.minestuck.blockentity.ItemStackBlockEntity;
import com.mraof.minestuck.blockentity.machine.TotemLatheBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wilkretawesomesauce.alchemancystuck.alchemy.CardPropertyTransfer;

/**
 * Carries Alchemancy properties from the two captcha cards being combined onto the carved dowel,
 * since the cards themselves are only ever read through {@code EncodedItemComponent.getEncodedOrBlank},
 * which reduces them to a plain default item instance and drops any infused/dormant properties otherwise.
 */
@Mixin(TotemLatheBlockEntity.class)
public abstract class TotemLatheMixin
{
	@Shadow
	public abstract ItemStack getCard1();

	@Shadow
	public abstract ItemStack getCard2();

	@Inject(method = "setCarvedItem", at = @At("TAIL"))
	private void alchemancystuck$carryProperties(ItemStack output, CallbackInfo ci, @Local ItemStackBlockEntity beItem)
	{
		ItemStack dowel = beItem.getStack();
		if(dowel.isEmpty())
			return;

		beItem.setStack(CardPropertyTransfer.carryProperties(dowel, getCard1(), getCard2()));
	}
}
