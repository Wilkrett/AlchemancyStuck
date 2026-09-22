package org.wilkretawesomesauce.alchemancystuck.alchemy;

import com.mraof.minestuck.item.components.CardStoredItemComponent;
import com.mraof.minestuck.item.components.EncodedItemComponent;
import com.mraof.minestuck.item.components.MSItemComponents;
import net.cibernet.alchemancy.crafting.ForgeRecipeGrid;
import net.cibernet.alchemancy.item.components.InfusedPropertiesComponent;
import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.cibernet.alchemancy.item.components.PropertyDataComponent;
import net.cibernet.alchemancy.properties.Property;
import net.cibernet.alchemancy.registries.AlchemancyItems;
import net.cibernet.alchemancy.registries.AlchemancyProperties;
import net.cibernet.alchemancy.registries.AlchemancyTags;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Carries Alchemancy properties onto items produced by Minestuck's alchemy machines (Punch Designix / Totem Lathe /
 * Alchemiter, including the miniature variants), which would otherwise reduce every result down to a plain
 * {@code Item} with none of its ingredients' properties.
 * <p>
 * Each source passed in may be a captcha card, a cruxite dowel, or a real item stack. Regardless of which, two
 * kinds of properties are carried over:
 * <ul>
 *     <li>properties already infused onto the source (via {@link InfusedPropertiesHelper#getInfusedProperties}),
 *     read both from the source itself (self-consistent with whatever this class previously wrote onto a card or
 *     dowel) and from the item it represents, if any</li>
 *     <li>properties the item the source represents has dormant potential for (via its {@code dormant_properties}
 *     item tags) - the same check the Alchemancy Forge performs when it "discovers" a property from an ingredient.
 *     Cards and dowels are never themselves tagged this way, so this only matters once resolved to the real item
 *     the card/dowel encodes</li>
 * </ul>
 */
public final class CardPropertyTransfer
{
	private CardPropertyTransfer()
	{
	}

	public static ItemStack carryProperties(ItemStack target, ItemStack... sources)
	{
		if(target.isEmpty())
			return target;

		List<Holder<Property>> toApply = new ArrayList<>();

		for(ItemStack source : sources)
		{
			if(source == null || source.isEmpty())
				continue;

			ItemStack represented = resolveRepresentedItem(source);

			toApply.addAll(InfusedPropertiesHelper.getInfusedProperties(source));
			PropertyDataComponent.mergeData(target, source);

			if(represented != source)
			{
				toApply.addAll(InfusedPropertiesHelper.getInfusedProperties(represented));
				PropertyDataComponent.mergeData(target, represented);
			}

			toApply.addAll(collectApplicableDormantProperties(represented, target));
		}

		if(!toApply.isEmpty())
			target = InfusedPropertiesHelper.addProperties(target, toApply);

		return target;
	}

	/**
	 * Resolves a captcha card or cruxite dowel to the (possibly blank) stack of the item it actually represents,
	 * since {@code dormant_properties} tags and any pre-existing infusions live on that item, not on the
	 * card/dowel container itself. Returns the input unchanged if it isn't a card/dowel wrapping anything.
	 */
	private static ItemStack resolveRepresentedItem(ItemStack source)
	{
		EncodedItemComponent encoded = source.get(MSItemComponents.ENCODED_ITEM);
		if(encoded != null)
			return encoded.asItemStack();

		CardStoredItemComponent stored = source.get(MSItemComponents.CARD_STORED_ITEM);
		if(stored != null)
			return stored.storedStack();

		return source;
	}

	private static List<Holder<Property>> collectApplicableDormantProperties(ItemStack source, ItemStack target)
	{
		if(source.is(AlchemancyTags.Items.REMOVES_INFUSIONS))
			return List.of();

		List<Holder<Property>> dormant = new ArrayList<>(AlchemancyProperties.getDormantProperties(source));
		dormant.addAll(source.getOrDefault(AlchemancyItems.Components.STORED_PROPERTIES, InfusedPropertiesComponent.EMPTY).properties());
		if(dormant.isEmpty())
			return List.of();

		ForgeRecipeGrid grid = new ForgeRecipeGrid(target);
		// Property#onInfusedByDormantProperty may mutate its "result" argument as a side effect even when just
		// checking applicability (apply=false) - Alchemancy's own ForgeRecipeGrid#handleDormantRecipes always
		// hands it a disposable copy in that case, never the real stack. Mirror that here, or a property's dormant
		// hook can silently corrupt target's other components (e.g. Minestuck's own EncodedItemComponent).
		ItemStack dryRunTarget = target.copy();
		List<Holder<Property>> applicable = new ArrayList<>();
		for(Holder<Property> holder : dormant)
		{
			if(InfusedPropertiesHelper.canInfuseWithProperty(target, holder)
					&& holder.value().onInfusedByDormantProperty(dryRunTarget, source, grid, dormant, false))
				applicable.add(holder);
		}
		return applicable;
	}
}
