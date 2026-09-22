package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mraof.minestuck.blockentity.machine.PunchDesignixBlockEntity;
import com.mraof.minestuck.item.components.CardStoredItemComponent;
import com.mraof.minestuck.item.components.MSItemComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wilkretawesomesauce.alchemancystuck.alchemy.CardPropertyTransfer;

/**
 * Carries Alchemancy properties from the item being captchalogued/combined onto the resulting punched card,
 * since {@link com.mraof.minestuck.item.components.EncodedItemComponent} only ever stores the plain {@code Item}
 * and would otherwise silently discard any infused properties.
 */
@Mixin(PunchDesignixBlockEntity.class)
public abstract class PunchDesignixMixin
{
	@Shadow
	public abstract ItemStack getCard();

	@Shadow
	public abstract void setCard(ItemStack card);

	@Unique
	private ItemStack alchemancystuck$propertySource = ItemStack.EMPTY;

	@Unique
	private ItemStack alchemancystuck$previousCard = ItemStack.EMPTY;

	@Inject(method = "handleKeyboardClick", at = @At(value = "INVOKE", target = "Lcom/mraof/minestuck/blockentity/machine/PunchDesignixBlockEntity;setCaptcha(Ljava/lang/String;)V"))
	private void alchemancystuck$captureSource(Player player, CallbackInfo ci, @Local ItemStack heldStack)
	{
		CardStoredItemComponent stored = heldStack.get(MSItemComponents.CARD_STORED_ITEM);
		this.alchemancystuck$propertySource = stored != null ? stored.storedStack().copy() : ItemStack.EMPTY;
	}

	@Inject(method = "punchCard", at = @At("HEAD"))
	private void alchemancystuck$snapshotPreviousCard(ServerPlayer player, CallbackInfo ci)
	{
		this.alchemancystuck$previousCard = getCard();
	}

	@Inject(method = "punchCard", at = @At(value = "INVOKE", target = "Lcom/mraof/minestuck/blockentity/machine/PunchDesignixBlockEntity;setCard(Lnet/minecraft/world/item/ItemStack;)V", shift = At.Shift.AFTER))
	private void alchemancystuck$applyProperties(ServerPlayer player, CallbackInfo ci,
			@Local(ordinal = 0) ItemStack captchaItemStack, @Local(ordinal = 1) ItemStack storedStackInCard)
	{
		ItemStack propertySource = this.alchemancystuck$propertySource.getItem() == captchaItemStack.getItem()
				? this.alchemancystuck$propertySource : ItemStack.EMPTY;

		// captchaItemStack/storedStackInCard are always correctly-identified (even if blank), so pass them
		// alongside the sidecar-carrying card/stack snapshots to make sure dormant-property discovery never
		// silently depends on having physically held the source card when the code was read.
		setCard(CardPropertyTransfer.carryProperties(getCard(),
				this.alchemancystuck$previousCard, propertySource, captchaItemStack, storedStackInCard));

		this.alchemancystuck$previousCard = ItemStack.EMPTY;
	}
}
