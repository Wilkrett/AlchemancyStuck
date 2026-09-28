package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mraof.minestuck.blockentity.machine.MiniAlchemiterBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.alchemy.CardPropertyTransfer;

/**
 * Same treatment as {@link AlchemiterMixin}, but for the miniature Alchemiter, which reads the dowel and
 * produces its output stack inline within {@code processContents} rather than through a shared getter.
 */
@Mixin(MiniAlchemiterBlockEntity.class)
public abstract class MiniAlchemiterMixin
{
	@ModifyExpressionValue(method = "processContents", at = @At(value = "INVOKE", target = "Lcom/mraof/minestuck/item/components/EncodedItemComponent;getEncodedOrBlank(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack alchemancystuck$carryProperties(ItemStack original, @Local ItemStack card)
	{
		return CardPropertyTransfer.carryProperties(original, false, card);
	}
}
