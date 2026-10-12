package org.wilkretawesomesauce.alchemancystuck.properties;

import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.cibernet.alchemancy.properties.Property;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonProperties;

public class DiscountProperty extends Property {

    private static final float PER_ITEM_RATE = 0.05F;
    private static final float MAX_DISCOUNT_RATE = 0.30F;

    public static float getRateFor(Player player) {
        Inventory inventory = player.getInventory();
        float rate = 0F;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!InfusedPropertiesHelper.hasProperty(stack, AddonProperties.DISCOUNT))
                continue;

            rate += InfusedPropertiesHelper.hasProperty(stack, AddonProperties.SBAHJ) ? -PER_ITEM_RATE : PER_ITEM_RATE;
        }

        return Math.min(rate, MAX_DISCOUNT_RATE);
    }

    @Override
    public int getColor(ItemStack stack) {
        return 0xFFD700;
    }
}
