package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mraof.minestuck.blockentity.ItemStackBlockEntity;
import com.mraof.minestuck.blockentity.machine.TotemLatheBlockEntity;
import com.mraof.minestuck.item.components.EncodedItemComponent;
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

	/**
	 * {@code processContents} only calls {@link ItemStackBlockEntity#setStack} (via {@code
	 * setCarvedItem}, below) when both cards are present AND {@code CombinationRecipe.findResult}
	 * actually found a matching && recipe - no matching recipe means the whole attempt is silently
	 * dropped, cards and all. Falling back to card 1's own represented item here means a
	 * property-only, no-Minestuck-recipe combination still carves the dowel (as card 1, sans any
	 * Minestuck item transformation) instead of doing nothing - and since {@link #alchemancystuck$carryProperties}
	 * always reads both cards regardless of how the vanilla item was decided, card 2's properties still
	 * make it onto the result the normal way.
	 */
	@ModifyExpressionValue(method = "processContents", at = @At(value = "INVOKE", target = "Lcom/mraof/minestuck/api/alchemy/recipe/combination/CombinationRecipe;findResult(Lcom/mraof/minestuck/api/alchemy/recipe/combination/CombinationInput;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack alchemancystuck$fallbackToFirstCard(ItemStack result)
	{
		return result.isEmpty() ? EncodedItemComponent.getEncodedOrBlank(getCard1()) : result;
	}

	@Inject(method = "setCarvedItem", at = @At("TAIL"))
	private void alchemancystuck$carryProperties(ItemStack output, CallbackInfo ci, @Local ItemStackBlockEntity beItem)
	{
		ItemStack dowel = beItem.getStack();
		if(dowel.isEmpty())
			return;

		beItem.setStack(CardPropertyTransfer.carrySplitProperties(dowel, true, getCard1(), getCard2()));
	}
}
