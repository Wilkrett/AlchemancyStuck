package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mraof.minestuck.blockentity.machine.MiniPunchDesignixBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.alchemy.CardPropertyTransfer;

/**
 * Same treatment as {@link PunchDesignixMixin}, but for the miniature Punch Designix, which has its own
 * independent {@code createResult} implementation.
 */
@Mixin(MiniPunchDesignixBlockEntity.class)
public abstract class MiniPunchDesignixMixin
{
	/**
	 * Same reasoning as {@link PunchDesignixMixin#alchemancystuck$fallbackToSlottedCard} - falls back
	 * to slot 0's own (already-computed) {@code output} when {@code CombinationRecipe.findResult}
	 * finds no matching || recipe for the two slots, instead of {@code createResult} discarding the
	 * attempt entirely. Captured via {@code @Local} rather than recomputed, since slot 0's contribution
	 * to {@code output} depends on a three-way branch ({@code EncodedItemComponent} vs {@code
	 * CardStoredItemComponent} vs plain item, the middle one with its own captcha-code-missing early
	 * return) not worth duplicating here.
	 */
	@ModifyExpressionValue(method = "createResult", at = @At(value = "INVOKE", target = "Lcom/mraof/minestuck/api/alchemy/recipe/combination/CombinationRecipe;findResult(Lcom/mraof/minestuck/api/alchemy/recipe/combination/CombinationInput;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack alchemancystuck$fallbackToFirstSlot(ItemStack result, @Local(ordinal = 0) ItemStack output)
	{
		return result.isEmpty() ? output : result;
	}

	@ModifyReturnValue(method = "createResult", at = @At("RETURN"))
	private ItemStack alchemancystuck$carryProperties(ItemStack original)
	{
		ItemStackHandler itemHandler = ((MachineProcessBlockEntityAccessor)(Object) this).alchemancystuck$getItemHandler();
		return CardPropertyTransfer.carryProperties(original, true, itemHandler.getStackInSlot(0), itemHandler.getStackInSlot(1));
	}
}
