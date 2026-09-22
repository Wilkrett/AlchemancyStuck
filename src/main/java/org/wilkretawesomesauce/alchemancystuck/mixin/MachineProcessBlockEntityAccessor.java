package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.mraof.minestuck.blockentity.machine.MachineProcessBlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the protected {@code itemHandler} field declared on {@link MachineProcessBlockEntity} so the miniature
 * machine mixins can read its input slots regardless of which subclass they actually target.
 */
@Mixin(MachineProcessBlockEntity.class)
public interface MachineProcessBlockEntityAccessor
{
	@Accessor("itemHandler")
	ItemStackHandler alchemancystuck$getItemHandler();
}
