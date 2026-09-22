package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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
	@ModifyReturnValue(method = "createResult", at = @At("RETURN"))
	private ItemStack alchemancystuck$carryProperties(ItemStack original)
	{
		ItemStackHandler itemHandler = ((MachineProcessBlockEntityAccessor)(Object) this).alchemancystuck$getItemHandler();
		return CardPropertyTransfer.carryProperties(original, itemHandler.getStackInSlot(0), itemHandler.getStackInSlot(1));
	}
}
