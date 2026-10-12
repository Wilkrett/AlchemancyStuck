package org.wilkretawesomesauce.alchemancystuck.command;

import com.mojang.brigadier.CommandDispatcher;
import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.cibernet.alchemancy.properties.Property;
import net.cibernet.alchemancy.registries.AlchemancyProperties;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class PropertiesCommand
{
	private PropertiesCommand()
	{
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("alchemancystuck")
				.then(Commands.literal("properties")
						.executes(context -> report(context.getSource()))));
	}

	private static int report(CommandSourceStack source)
	{
		if(!(source.getEntity() instanceof Player player))
		{
			source.sendFailure(Component.literal("Must be run by a player."));
			return 0;
		}

		ItemStack held = player.getMainHandItem();
		if(held.isEmpty())
		{
			source.sendFailure(Component.literal("You aren't holding anything."));
			return 0;
		}

		List<Holder<Property>> infused = InfusedPropertiesHelper.getInfusedProperties(held);
		List<Holder<Property>> stored = InfusedPropertiesHelper.getStoredProperties(held);
		List<Holder<Property>> dormant = AlchemancyProperties.getDormantProperties(held);

		List<Holder<Property>> active = new java.util.ArrayList<>();
		InfusedPropertiesHelper.forEachProperty(held, active::add);

		source.sendSuccess(() -> Component.literal(held.getHoverName().getString()
				+ " - infused: " + names(infused) + " | stored: " + names(stored)
				+ " | dormant: " + names(dormant) + " | active: " + names(active)), false);
		return 1;
	}

	private static String names(List<Holder<Property>> properties)
	{
		if(properties.isEmpty())
			return "(none)";

		StringBuilder builder = new StringBuilder();
		for(Holder<Property> holder : properties)
		{
			if(!builder.isEmpty())
				builder.append(", ");
			builder.append(holder.value().getRawName());
		}
		return builder.toString();
	}
}
