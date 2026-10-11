package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mraof.minestuck.blockentity.machine.MiniAlchemiterBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.alchemy.CardPropertyTransfer;

/**
 * Same treatment as {@link AlchemiterMixin}, but for the miniature Alchemiter, which reads the dowel and
 * produces its output stack inline within {@code processContents} AND independently again within
 * {@code contentsValid} (its own separate {@code getEncodedOrBlank} call, not shared with {@code
 * processContents}) rather than through one common getter. Both must carry properties identically - if
 * only {@code processContents} did, {@code contentsValid} would wave a craft through using the cheaper,
 * properties-blind cost, while {@code processContents} then tries to actually deduct the real,
 * properties-surcharged cost and silently fails once that's pricier than what was approved.
 */
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
