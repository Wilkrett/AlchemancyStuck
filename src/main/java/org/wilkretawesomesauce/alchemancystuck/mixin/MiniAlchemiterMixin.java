package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mraof.minestuck.blockentity.machine.MiniAlchemiterBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.alchemy.CardPropertyTransfer;

@Mixin(MiniAlchemiterBlockEntity.class)
public abstract class MiniAlchemiterMixin
{
	@ModifyExpressionValue(
			method = {"contentsValid", "processContents"},
			at = @At(value = "INVOKE", target = "Lcom/mraof/minestuck/item/components/EncodedItemComponent;getEncodedOrBlank(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack alchemancystuck$carryProperties(ItemStack original)
	{
		ItemStack card = ((MachineProcessBlockEntityAccessor) (Object) this).alchemancystuck$getItemHandler().getStackInSlot(0);
		return CardPropertyTransfer.carryProperties(original, false, card);
	}
}
