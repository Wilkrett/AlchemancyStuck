package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mraof.minestuck.blockentity.machine.AlchemiterBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.alchemy.CardPropertyTransfer;

@Mixin(AlchemiterBlockEntity.class)
public abstract class AlchemiterMixin
{
	@Shadow
	protected ItemStack dowel;

	@ModifyReturnValue(method = "getOutput", at = @At("RETURN"))
	private ItemStack alchemancystuck$carryProperties(ItemStack original)
	{
		return CardPropertyTransfer.carryProperties(original, false, this.dowel);
	}
}
