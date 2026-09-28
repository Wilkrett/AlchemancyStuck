package org.wilkretawesomesauce.alchemancystuck.alchemy;

import com.mraof.minestuck.api.alchemy.GristAmount;
import com.mraof.minestuck.api.alchemy.GristSet;
import com.mraof.minestuck.api.alchemy.GristType;
import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.api.alchemy.MutableGristSet;
import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.minecraft.world.item.ItemStack;
import org.wilkretawesomesauce.alchemancystuck.properties.CruxiteProperty;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonProperties;

public final class AlchemiterCostHelper {

    private AlchemiterCostHelper() {
    }

    public static GristSet applyCostProperties(GristSet original, ItemStack output) {
        if (original == null)
            return null;

        GristSet cost = original;

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
}
