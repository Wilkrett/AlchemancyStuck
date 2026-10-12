package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mraof.minestuck.api.alchemy.GristSet;
import com.mraof.minestuck.api.alchemy.GristType;
import com.mraof.minestuck.blockentity.machine.GristWidgetBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.alchemy.AlchemiterCostHelper;
import org.wilkretawesomesauce.alchemancystuck.alchemy.CardPropertyTransfer;

@Mixin(GristWidgetBlockEntity.class)
public abstract class GristWidgetGristCostMixin
{
	@WrapOperation(
			method = "getGristWidgetResult",
			at = @At(value = "INVOKE", target = "Lcom/mraof/minestuck/api/alchemy/recipe/GristCostRecipe;findCostForItem(Lnet/minecraft/world/item/ItemStack;Lcom/mraof/minestuck/api/alchemy/GristType;ZLnet/minecraft/world/level/Level;)Lcom/mraof/minestuck/api/alchemy/GristSet;"))
	private static GristSet alchemancystuck$applyCostProperties(ItemStack realItem, GristType wildcardGrist, boolean allowMissing, Level level, Operation<GristSet> original, @Local(ordinal = 0) ItemStack card)
	{
		GristSet cost = original.call(realItem, wildcardGrist, allowMissing, level);
		if (cost == null)
			return null;

		ItemStack carried = CardPropertyTransfer.carryProperties(realItem.copy(), false, card);
		return AlchemiterCostHelper.applyCostProperties(cost, carried, card, 1);
	}
}
