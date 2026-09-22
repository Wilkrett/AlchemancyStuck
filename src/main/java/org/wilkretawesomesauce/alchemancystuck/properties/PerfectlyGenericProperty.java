package org.wilkretawesomesauce.alchemancystuck.properties;

import net.cibernet.alchemancy.properties.Property;
import net.minecraft.world.item.ItemStack;

public class PerfectlyGenericProperty extends Property {
/** dont worry if this looks off, this is a {@link org.wilkretawesomesauce.alchemancystuck.mixin.InfusedPropertiesHelperMixin} only property :d
 */
    @Override
    public int getColor(ItemStack stack) {
        return 0x9A9A9A;
    }
}
