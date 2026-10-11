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
 *     <li>properties already infused or stored onto the source (via
 *     {@link InfusedPropertiesHelper#getInfusedProperties}/{@link InfusedPropertiesHelper#getStoredProperties}),
 *     read both from the source itself (self-consistent with whatever this class previously wrote onto a card or
 *     dowel) and from the item it represents, if any</li>
 *     <li>properties the item the source represents has dormant potential for (via its {@code dormant_properties}
 *     item tags) - the same check the Alchemancy Forge performs when it "discovers" a property from an ingredient.
 *     Cards and dowels are never themselves tagged this way, so this only matters once resolved to the real item
 *     the card/dowel encodes</li>
 * </ul>
 * <p>
 * {@code target} itself is either a card/dowel container (Punch Designix, Totem Lathe) or the real decoded item
 * (Alchemiter). Only the Alchemiter's target should actually have these properties active while it's just sitting
 * in an inventory, so {@code asContainer} controls where the carried properties land: {@link
 * InfusedPropertiesHelper#storeProperties} (inert, ignored by {@code hasProperty}/{@code forEachProperty}) for a
 * container, versus {@link InfusedPropertiesHelper#addProperties} (live) for a real item.
 */
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

	/**
	 * Same property-carrying as {@link #carryProperties}, but for exactly two sources that are meant to
	 * contribute evenly rather than pooled - the Totem Lathe combining two cards with no matching
	 * Minestuck recipe should end up with half of card 1's properties and half of card 2's, not
	 * whichever properties happen to survive truncation after dumping both cards into one list (which,
	 * given {@link #carryProperties}'s combine order, would always favor card 1).
	 * <p>
	 * The 50/50 split only applies when both sources actually contribute something - if one slot is
	 * empty (e.g. carving a dowel from a single card with no second card present), the other source
	 * isn't artificially capped to half; it carries over in full, same as {@link #carryProperties}.
	 */
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
			toApply.addAll(capTo(collectFromSource(target, source1), half));
			toApply.addAll(capTo(collectFromSource(target, source2), half));
		}
		else
		{
			toApply.addAll(collectFromSource(target, source1));
			toApply.addAll(collectFromSource(target, source2));
		}

		return applyProperties(target, asContainer, toApply);
	}

	/**
	 * Like {@link #carryProperties}, but replaces {@code target}'s properties with exactly {@code
	 * source}'s existing ones instead of pooling them together - used by the Punch Designix family,
	 * where re-punching a card should leave it a clean copy of the newly-typed item's properties, not
	 * a growing combination of everything ever punched into it. Only properties {@code source} already
	 * carries (infused/stored) transfer; unlike {@link #carryProperties}, {@code source}'s dormant
	 * potential is never discovered/activated here, since this is a replacement of {@code target}, not
	 * a forge-style combination of it with something else.
	 */
	public static ItemStack overwriteWithProperties(ItemStack target, boolean asContainer, ItemStack source)
	{
		if(target.isEmpty())
			return target;

		return applyPropertiesOverwrite(target, asContainer, collectExistingProperties(target, source));
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

			// Alchemancy's own merge points (ForgeRecipeGrid, AlchemancyCatalystBlock) always truncate
			// immediately after adding, with no "staging above the cap, truncate later" exception - mirror
			// that here via the same Mutable#truncateProperties algorithm, rather than letting a card/dowel
			// accumulate an unbounded number of stored properties across repeated no-recipe combinations.
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
