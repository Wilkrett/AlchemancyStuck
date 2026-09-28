package org.wilkretawesomesauce.alchemancystuck.properties;

import com.mraof.minestuck.alchemy.GristHelper;
import com.mraof.minestuck.api.alchemy.GristSet;
import com.mraof.minestuck.api.alchemy.MutableGristSet;
import com.mraof.minestuck.api.alchemy.recipe.GristCostRecipe;
import com.mraof.minestuck.entity.item.GristEntity;
import net.cibernet.alchemancy.properties.Property;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Collection;

/**
 * Mirrors Minestuck's own {@code mine_and_grist}/{@code GristLootModifier} pattern (grist-cost lookup
 * + {@link GristEntity#spawnGristEntities}), just applied to a killed entity's drops instead of a
 * mined block's - same {@link #GRIST_YIELD} discount the pickaxe applies, for the same reason (a 1:1
 * trade would make grinding for grist strictly better than actually using the loot).
 *
 * <p>A dropped item with no known grist-cost recipe still gets converted, just to a random grist
 * type instead of a priced amount - {@link GristHelper#getPrimaryGrist}, the same weighted picker
 * Minestuck uses to align underlings, which already excludes types with no underling color set
 * (Zillium included - see {@code GristTypes.ZILLIUM}'s missing {@code .underlingType(...)} call).
 */
public class GristifyingProperty extends Property {

    private static final float GRIST_YIELD = 0.5F;
    private static final int UNPRICED_GRIST_PER_ITEM = 2;

    @Override
    public void modifyLivingDrops(LivingEntity dropsSource, ItemStack weapon, LivingEntity user, Collection<ItemEntity> drops, LivingDropsEvent event) {
        Level level = dropsSource.level();
        MutableGristSet gristSet = MutableGristSet.newDefault();

        for (ItemEntity drop : drops) {
            GristSet cost = GristCostRecipe.findCostForItem(drop.getItem(), null, true, level);
            if (cost != null && !cost.isEmpty())
                gristSet.add(cost.mutableCopy().scale(GRIST_YIELD, true));
            else
                gristSet.add(GristHelper.getPrimaryGrist(dropsSource.getRandom()), (long) UNPRICED_GRIST_PER_ITEM * drop.getItem().getCount());
        }

        drops.clear();

        if (!gristSet.isEmpty())
            GristEntity.spawnGristEntities(gristSet, level, dropsSource.getX(), dropsSource.getY(), dropsSource.getZ(), dropsSource.getRandom(), grist -> {
            });
    }

    @Override
    public int getColor(ItemStack stack) {
        return 0x00C853;
    }
}
