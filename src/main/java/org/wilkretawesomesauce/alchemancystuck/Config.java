package org.wilkretawesomesauce.alchemancystuck;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

	public static class Server {

		private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

		private static final ModConfigSpec.BooleanValue PROPERTY_GRIST_COSTS = BUILDER
				.comment("Whether default Alchemancy properties add a grist surcharge to Alchemiter costs. Does not affect Cruxite or SBAHJ.")
				.define("gameplay.propertyGristCosts", true);

		static final ModConfigSpec SPEC = BUILDER.build();

		public static boolean propertyGristCosts() {
			return PROPERTY_GRIST_COSTS.get();
		}
	}
}
