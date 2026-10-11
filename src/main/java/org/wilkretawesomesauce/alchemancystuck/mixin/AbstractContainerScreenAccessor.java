package org.wilkretawesomesauce.alchemancystuck.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the protected {@code menu} field declared on {@link AbstractContainerScreen} itself - {@code
 * @Shadow} can't reach it directly from a mixin targeting a subclass like {@code MiniAlchemiterScreen},
 * since it's only declared on this superclass.
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor
{
	@Accessor("menu")
	AbstractContainerMenu alchemancystuck$getMenu();
}
