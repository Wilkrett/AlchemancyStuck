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

@Mixin(TotemLatheBlockEntity.class)
public abstract class TotemLatheMixin
{
	@Shadow
	public abstract ItemStack getCard1();

	@Shadow
	public abstract ItemStack getCard2();

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
