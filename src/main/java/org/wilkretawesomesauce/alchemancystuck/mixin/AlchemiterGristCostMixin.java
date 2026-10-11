package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mraof.minestuck.api.alchemy.GristSet;
import com.mraof.minestuck.blockentity.machine.AlchemiterBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.alchemy.AlchemiterCostHelper;

/**
 * {@code getGristCost} prices the item from {@code getOutput()}, which by then already carries any Alchemancy
 * properties transferred from the dowel (see {@link AlchemiterMixin}). Cruxite and SBAHJ both change what
 * the alchemizer is actually charged, so they're applied here, before payment, rather than as a refund after.
 */
@Mixin(AlchemiterBlockEntity.class)
public abstract class AlchemiterGristCostMixin
{
	@Shadow
	public abstract ItemStack getOutput();

	@ModifyReturnValue(method = "getGristCost", at = @At("RETURN"))
	private GristSet alchemancystuck$applyCostProperties(GristSet original, @Local(ordinal = 0) int count)
	{
		return AlchemiterCostHelper.applyCostProperties(original, getOutput(), count);
	}
}
