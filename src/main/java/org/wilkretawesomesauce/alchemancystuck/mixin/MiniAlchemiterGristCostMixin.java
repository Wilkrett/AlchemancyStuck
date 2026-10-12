package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mraof.minestuck.api.alchemy.GristSet;
import com.mraof.minestuck.api.alchemy.GristType;
import com.mraof.minestuck.blockentity.machine.MiniAlchemiterBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.alchemy.AlchemiterCostHelper;

@Mixin(MiniAlchemiterBlockEntity.class)
public abstract class MiniAlchemiterGristCostMixin
{
	@WrapOperation(
			method = {"contentsValid", "processContents"},
			at = @At(value = "INVOKE", target = "Lcom/mraof/minestuck/api/alchemy/recipe/GristCostRecipe;findCostForItem(Lnet/minecraft/world/item/ItemStack;Lcom/mraof/minestuck/api/alchemy/GristType;ZLnet/minecraft/world/level/Level;)Lcom/mraof/minestuck/api/alchemy/GristSet;")
	)
	private GristSet alchemancystuck$applyCostProperties(ItemStack output, GristType wildcardGrist, boolean allowMissing, Level level, Operation<GristSet> original)
	{
		ItemStack card = ((MachineProcessBlockEntityAccessor) (Object) this).alchemancystuck$getItemHandler().getStackInSlot(0);
		return AlchemiterCostHelper.applyCostProperties(original.call(output, wildcardGrist, allowMissing, level), output, card, 1);
	}
}
