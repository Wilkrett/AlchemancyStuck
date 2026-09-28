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

/**
 * Both {@code contentsValid} (the afford-check gating whether crafting starts) and {@code processContents}
 * (the actual deduction) call {@code GristCostRecipe.findCostForItem} independently on the same
 * property-carrying decoded stack (see {@link MiniAlchemiterMixin}), so both need the Cruxite/SBAHJ
 * cost adjustment applied identically, or the machine can stall refusing to start a craft it can actually afford.
 */
@Mixin(MiniAlchemiterBlockEntity.class)
public abstract class MiniAlchemiterGristCostMixin
{
	@WrapOperation(
			method = {"contentsValid", "processContents"},
			at = @At(value = "INVOKE", target = "Lcom/mraof/minestuck/api/alchemy/recipe/GristCostRecipe;findCostForItem(Lnet/minecraft/world/item/ItemStack;Lcom/mraof/minestuck/api/alchemy/GristType;ZLnet/minecraft/world/level/Level;)Lcom/mraof/minestuck/api/alchemy/GristSet;")
	)
	private GristSet alchemancystuck$applyCostProperties(ItemStack output, GristType wildcardGrist, boolean allowMissing, Level level, Operation<GristSet> original)
	{
		return AlchemiterCostHelper.applyCostProperties(original.call(output, wildcardGrist, allowMissing, level), output);
	}
}
