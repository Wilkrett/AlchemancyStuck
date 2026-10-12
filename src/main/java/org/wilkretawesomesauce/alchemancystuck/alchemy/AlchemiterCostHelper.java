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

    public static GristSet applyCostProperties(GristSet original, ItemStack output, ItemStack source, int count) {
        if (original == null)
            return null;

        GristSet cost = addPropertyCosts(original, source, count);

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

    private static GristSet addPropertyCosts(GristSet original, ItemStack source, int count) {
        if (!Config.Server.propertyGristCosts())
            return original;

        MutableGristSet withPropertyCosts = original.mutableCopy();
        for (var holder : CardPropertyTransfer.getExistingProperties(source)) {
            GristSet propertyCost = PropertyGristCosts.get(holder);
            if (propertyCost != null)
                withPropertyCosts.add(propertyCost.mutableCopy().scale(count));
        }
        return withPropertyCosts;
    }
}
