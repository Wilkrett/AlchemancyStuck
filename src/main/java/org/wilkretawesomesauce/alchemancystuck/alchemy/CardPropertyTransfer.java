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

public final class CardPropertyTransfer
{
	private CardPropertyTransfer()
	{
	}

	public static ItemStack carryProperties(ItemStack target, boolean asContainer, ItemStack... sources)
	{
		if(target.isEmpty())
			return target;

		List<Holder<Property>> toApply = new ArrayList<>();
		for(ItemStack source : sources)
			toApply.addAll(collectFromSource(target, source));

		return applyProperties(target, asContainer, toApply);
	}

	public static ItemStack carrySplitProperties(ItemStack target, boolean asContainer, ItemStack source1, ItemStack source2)
	{
		if(target.isEmpty())
			return target;

		boolean hasSource1 = source1 != null && !source1.isEmpty();
		boolean hasSource2 = source2 != null && !source2.isEmpty();

		List<Holder<Property>> toApply = new ArrayList<>();
		if(hasSource1 && hasSource2)
		{
			int half = InfusedPropertiesHelper.getInfusionSlots(target) / 2;
			toApply.addAll(capTo(collectExistingProperties(target, source1), half));
			toApply.addAll(capTo(collectExistingProperties(target, source2), half));
		}
		else
		{
			toApply.addAll(collectExistingProperties(target, source1));
			toApply.addAll(collectExistingProperties(target, source2));
		}

		return applyProperties(target, asContainer, toApply);
	}

	public static ItemStack overwriteWithProperties(ItemStack target, boolean asContainer, ItemStack source)
	{
		if(target.isEmpty())
			return target;

		return applyPropertiesOverwrite(target, asContainer, collectExistingProperties(target, source));
	}

	public static List<Holder<Property>> getExistingProperties(ItemStack source)
	{
		if(source == null || source.isEmpty())
			return List.of();

		List<Holder<Property>> collected = new ArrayList<>();
		collected.addAll(InfusedPropertiesHelper.getInfusedProperties(source));
		collected.addAll(InfusedPropertiesHelper.getStoredProperties(source));

		ItemStack represented = resolveRepresentedItem(source);
		if(represented != source)
		{
			collected.addAll(InfusedPropertiesHelper.getInfusedProperties(represented));
			collected.addAll(InfusedPropertiesHelper.getStoredProperties(represented));
		}

		return collected;
	}

	private static List<Holder<Property>> collectExistingProperties(ItemStack target, ItemStack source)
	{
		if(source == null || source.isEmpty())
			return List.of();

		List<Holder<Property>> collected = new ArrayList<>();
		ItemStack represented = resolveRepresentedItem(source);

		collected.addAll(InfusedPropertiesHelper.getInfusedProperties(source));
		collected.addAll(InfusedPropertiesHelper.getStoredProperties(source));
		PropertyDataComponent.mergeData(target, source);

		if(represented != source)
		{
			collected.addAll(InfusedPropertiesHelper.getInfusedProperties(represented));
			collected.addAll(InfusedPropertiesHelper.getStoredProperties(represented));
			PropertyDataComponent.mergeData(target, represented);
		}

		return collected;
	}

	private static ItemStack applyPropertiesOverwrite(ItemStack target, boolean asContainer, List<Holder<Property>> properties)
	{
		if(asContainer)
		{
			InfusedPropertiesComponent.Mutable mutable = new InfusedPropertiesComponent.Mutable(new InfusedPropertiesComponent(new ArrayList<>(properties)));
			mutable.truncateProperties(InfusedPropertiesHelper.getInfusionSlots(target));

			return InfusedPropertiesHelper.storeProperties(target, mutable.toImutable().properties());
		}
		else
		{
			target.remove(AlchemancyItems.Components.INFUSED_PROPERTIES);
			target = InfusedPropertiesHelper.addProperties(target, properties);
			return InfusedPropertiesHelper.truncateProperties(target);
		}
	}

	private static List<Holder<Property>> capTo(List<Holder<Property>> properties, int limit)
	{
		return properties.size() <= limit ? properties : new ArrayList<>(properties.subList(0, limit));
	}

	private static List<Holder<Property>> collectFromSource(ItemStack target, ItemStack source)
	{
		if(source == null || source.isEmpty())
			return List.of();

		List<Holder<Property>> collected = new ArrayList<>();
		ItemStack represented = resolveRepresentedItem(source);

		collected.addAll(InfusedPropertiesHelper.getInfusedProperties(source));
		collected.addAll(InfusedPropertiesHelper.getStoredProperties(source));
		PropertyDataComponent.mergeData(target, source);

		if(represented != source)
		{
			collected.addAll(InfusedPropertiesHelper.getInfusedProperties(represented));
			collected.addAll(InfusedPropertiesHelper.getStoredProperties(represented));
			PropertyDataComponent.mergeData(target, represented);
		}

		collected.addAll(collectApplicableDormantProperties(represented, target));
		return collected;
	}

	private static ItemStack applyProperties(ItemStack target, boolean asContainer, List<Holder<Property>> toApply)
	{
		if(toApply.isEmpty())
			return target;

		if(asContainer)
		{
			List<Holder<Property>> combined = new ArrayList<>(InfusedPropertiesHelper.getStoredProperties(target));
			for(Holder<Property> holder : toApply)
				if(!combined.contains(holder))
					combined.add(holder);

			InfusedPropertiesComponent.Mutable mutable = new InfusedPropertiesComponent.Mutable(new InfusedPropertiesComponent(combined));
			mutable.truncateProperties(InfusedPropertiesHelper.getInfusionSlots(target));

			return InfusedPropertiesHelper.storeProperties(target, mutable.toImutable().properties());
		}
		else
		{
			target = InfusedPropertiesHelper.addProperties(target, toApply);
			return InfusedPropertiesHelper.truncateProperties(target);
		}
	}

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
