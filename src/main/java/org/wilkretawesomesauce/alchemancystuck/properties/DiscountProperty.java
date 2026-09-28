package org.wilkretawesomesauce.alchemancystuck.properties;

import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.cibernet.alchemancy.properties.Property;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonProperties;

/**
 * Purely a carry-in-inventory flag - no {@link Property} hooks of its own, since neither vanilla
 * villagers nor Minestuck's consort shops fire anything through the base mod's own event hooks.
 * {@link #getRateFor} is checked directly by {@code VillagerDiscountMixin} and
 * {@code ConsortMerchantInventoryDiscountMixin} instead.
 *
 * <p>Deliberately doesn't reuse {@link InfusedPropertiesHelper#hasItemWithProperty} - that method's
 * "checkAuxiliary" search of the full inventory only counts a stack sitting outside an equipment slot
 * if it's *also* tagged {@code AlchemancyProperties.AUXILIARY}, which isn't what "carried anywhere in
 * your inventory" is meant to require here.
 *
 * <p>Each carried Discount stack contributes {@link #PER_ITEM_RATE} - positive (cheaper) normally,
 * negative (pricier) if that same stack is also SBAHJ-infused, same as every other "SBAHJ + property"
 * interaction elsewhere in this addon. Contributions are summed (a normal item and a cursed one cancel
 * out), and only the positive side - the actual discount - is capped, at {@link #MAX_DISCOUNT_RATE};
 * stacking cursed items into an ever-bigger surcharge has no such ceiling.
 */
public class DiscountProperty extends Property {

    private static final float PER_ITEM_RATE = 0.05F;
    private static final float MAX_DISCOUNT_RATE = 0.30F;

    /**
     * @return the net rate to apply: positive means a discount (cheaper), negative a surcharge
     * (pricier), 0 means no effect. Positive values are capped at {@link #MAX_DISCOUNT_RATE}; negative
     * ones aren't capped at all.
     */
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
