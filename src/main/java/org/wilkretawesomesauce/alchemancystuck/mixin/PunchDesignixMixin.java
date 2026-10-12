package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mraof.minestuck.blockentity.machine.PunchDesignixBlockEntity;
import com.mraof.minestuck.item.components.EncodedItemComponent;
import com.mraof.minestuck.item.components.MSItemComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wilkretawesomesauce.alchemancystuck.alchemy.CardPropertyTransfer;

@Mixin(PunchDesignixBlockEntity.class)
public abstract class PunchDesignixMixin
{
	@Shadow
	public abstract ItemStack getCard();

	@Shadow
	public abstract void setCard(ItemStack card);

	@Unique
	private ItemStack alchemancystuck$previousCard = ItemStack.EMPTY;

	@Inject(method = "punchCard", at = @At("HEAD"))
	private void alchemancystuck$snapshotPreviousCard(ServerPlayer player, CallbackInfo ci)
	{
		this.alchemancystuck$previousCard = getCard();
	}

	@ModifyExpressionValue(method = "punchCard", at = @At(value = "INVOKE", target = "Lcom/mraof/minestuck/api/alchemy/recipe/combination/CombinationRecipe;findResult(Lcom/mraof/minestuck/api/alchemy/recipe/combination/CombinationInput;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack alchemancystuck$fallbackToSlottedCard(ItemStack result)
	{
		if (!result.isEmpty())
			return result;

		EncodedItemComponent encoded = getCard().get(MSItemComponents.ENCODED_ITEM);
		return encoded != null ? encoded.asItemStack() : result;
	}

	@Inject(method = "punchCard", at = @At(value = "INVOKE", target = "Lcom/mraof/minestuck/blockentity/machine/PunchDesignixBlockEntity;setCard(Lnet/minecraft/world/item/ItemStack;)V", shift = At.Shift.AFTER))
	private void alchemancystuck$applyProperties(ServerPlayer player, CallbackInfo ci)
	{
		setCard(CardPropertyTransfer.overwriteWithProperties(getCard(), true, this.alchemancystuck$previousCard));
		this.alchemancystuck$previousCard = ItemStack.EMPTY;
	}
}
