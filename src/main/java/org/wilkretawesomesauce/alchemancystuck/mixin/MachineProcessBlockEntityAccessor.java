package org.wilkretawesomesauce.alchemancystuck.mixin;

import com.mraof.minestuck.blockentity.machine.MachineProcessBlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MachineProcessBlockEntity.class)
public interface MachineProcessBlockEntityAccessor
{
	@Accessor("itemHandler")
	ItemStackHandler alchemancystuck$getItemHandler();
}
