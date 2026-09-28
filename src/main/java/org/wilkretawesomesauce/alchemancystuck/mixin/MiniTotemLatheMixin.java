package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mraof.minestuck.blockentity.machine.MiniTotemLatheBlockEntity;
import com.mraof.minestuck.item.components.EncodedItemComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.alchemy.CardPropertyTransfer;

/**
 * Same treatment as {@link TotemLatheMixin}, but for the miniature Totem Lathe, which has its own
 * independent {@code createResult} implementation.
 */
@Mixin(MiniTotemLatheBlockEntity.class)
public abstract class MiniTotemLatheMixin
{
	/**
	 * Same reasoning as {@link TotemLatheMixin#alchemancystuck$fallbackToFirstCard} - falls back to
	 * slot 0's own represented item when {@code CombinationRecipe.findResult} finds no matching &&
	 * recipe for the two slotted cards, instead of {@code createResult} discarding the attempt
	 * entirely.
	 */
	@ModifyExpressionValue(method = "createResult", at = @At(value = "INVOKE", target = "Lcom/mraof/minestuck/api/alchemy/recipe/combination/CombinationRecipe;findResult(Lcom/mraof/minestuck/api/alchemy/recipe/combination/CombinationInput;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack alchemancystuck$fallbackToFirstCard(ItemStack result)
	{
		if (!result.isEmpty())
			return result;

		ItemStackHandler itemHandler = ((MachineProcessBlockEntityAccessor)(Object) this).alchemancystuck$getItemHandler();
		return EncodedItemComponent.getEncodedOrBlank(itemHandler.getStackInSlot(0));
	}

	@ModifyReturnValue(method = "createResult", at = @At("RETURN"))
	private ItemStack alchemancystuck$carryProperties(ItemStack original)
	{
		ItemStackHandler itemHandler = ((MachineProcessBlockEntityAccessor)(Object) this).alchemancystuck$getItemHandler();
		return CardPropertyTransfer.carrySplitProperties(original, true, itemHandler.getStackInSlot(0), itemHandler.getStackInSlot(1));
	}
}
