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

@Mixin(AlchemiterBlockEntity.class)
public abstract class AlchemiterGristCostMixin
{
	@Shadow
	public abstract ItemStack getOutput();

	@Shadow
	protected ItemStack dowel;

	@ModifyReturnValue(method = "getGristCost", at = @At("RETURN"))
	private GristSet alchemancystuck$applyCostProperties(GristSet original, @Local(ordinal = 0) int count)
	{
		return AlchemiterCostHelper.applyCostProperties(original, getOutput(), this.dowel, count);
	}
}
