package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mraof.minestuck.api.alchemy.GristSet;
import com.mraof.minestuck.client.gui.MiniAlchemiterScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wilkretawesomesauce.alchemancystuck.alchemy.AlchemiterCostHelper;
import org.wilkretawesomesauce.alchemancystuck.alchemy.CardPropertyTransfer;

/**
 * {@code MiniAlchemiterScreen#renderLabels} computes the grist-board quote it actually draws to the
 * player by calling the matched {@code GristCostRecipe}'s own {@code getGristCost} directly, client-side,
 * on the plain decoded card - a completely separate path from {@code contentsValid}/{@code
 * processContents} (patched by {@link MiniAlchemiterGristCostMixin}/{@link MiniAlchemiterMixin}), which
 * carry the dowel's stored properties onto the decoded stack before pricing it. Without carrying them
 * here too, the GUI quote would stay at the plain recipe cost even once the server-side fix makes the
 * real charge (and the affordability check) reflect the properties correctly.
 */
@Mixin(MiniAlchemiterScreen.class)
public abstract class MiniAlchemiterScreenGristCostMixin
{
	@ModifyReturnValue(method = "lambda$renderLabels$0", at = @At("RETURN"))
	private GristSet alchemancystuck$applyCostProperties(GristSet original, @Local(ordinal = 0) ItemStack item)
	{
		ItemStack card = ((AbstractContainerScreenAccessor) (Object) this).alchemancystuck$getMenu().getSlot(0).getItem();
		ItemStack carried = CardPropertyTransfer.carryProperties(item, false, card);
		return AlchemiterCostHelper.applyCostProperties(original, carried, 1);
	}
}
