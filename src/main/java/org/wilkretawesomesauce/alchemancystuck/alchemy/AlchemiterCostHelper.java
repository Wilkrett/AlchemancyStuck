package org.wilkretawesomesauce.alchemancystuck.alchemy;

import com.mraof.minestuck.api.alchemy.GristAmount;
import com.mraof.minestuck.api.alchemy.GristSet;
import com.mraof.minestuck.api.alchemy.GristType;
import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.api.alchemy.MutableGristSet;
import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.minecraft.world.item.ItemStack;
import org.wilkretawesomesauce.alchemancystuck.Config;
import org.wilkretawesomesauce.alchemancystuck.properties.CruxiteProperty;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonProperties;

public final class AlchemiterCostHelper {

    private AlchemiterCostHelper() {
    }

    public static GristSet applyCostProperties(GristSet original, ItemStack output, int count) {
        if (original == null)
            return null;

        GristSet cost = addPropertyCosts(original, output, count);

        if (InfusedPropertiesHelper.hasProperty(output, AddonProperties.CRUXITE)) {
            MutableGristSet reduced = MutableGristSet.newDefault();
            for (GristAmount amount : cost.asAmounts())
                reduced.set(amount.type(), (long) Math.ceil(amount.amount() * CruxiteProperty.GRIST_COST_SCALE));
            cost = reduced;
        }

        if (InfusedPropertiesHelper.hasProperty(output, AddonProperties.SBAHJ)) {
            GristType artifact = GristTypes.ARTIFACT.get();
            long totalAmount = 0;
            for (GristAmount amount : cost.asAmounts())
                totalAmount += amount.amount();
            cost = new GristAmount(artifact, totalAmount);
        }

        return cost;
    }

    /**
     * Adds {@link PropertyGristCosts}' per-property surcharge for every property active on
     * {@code output}, before Cruxite/SBAHJ get a chance to scale or collapse the total - so both
     * still apply to the property surcharges too, not just the item's own base cost. {@code original}
     * already reflects {@code count} (the Alchemiter scales the base recipe cost by the requested
     * amount before this runs), so only the per-property surcharge - priced per single item - needs
     * scaling here, or it'd stay flat while the base cost rises as the player adjusts the amount.
     * <p>
     * Gated by {@link Config.Server#propertyGristCosts()} - Cruxite/SBAHJ are applied separately by
     * the caller and are unaffected by this setting.
     */
    private static GristSet addPropertyCosts(GristSet original, ItemStack output, int count) {
        if (!Config.Server.propertyGristCosts())
            return original;

        MutableGristSet withPropertyCosts = original.mutableCopy();
        InfusedPropertiesHelper.forEachProperty(output, holder -> {
            GristSet propertyCost = PropertyGristCosts.get(holder);
            if (propertyCost != null)
                withPropertyCosts.add(propertyCost.mutableCopy().scale(count));
        });
        return withPropertyCosts;
    }
}
