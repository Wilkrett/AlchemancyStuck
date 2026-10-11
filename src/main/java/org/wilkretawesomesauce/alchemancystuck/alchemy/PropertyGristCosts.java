package org.wilkretawesomesauce.alchemancystuck.alchemy;

import com.mraof.minestuck.api.alchemy.GristSet;
import com.mraof.minestuck.api.alchemy.GristType;
import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.api.alchemy.MutableGristSet;
import net.cibernet.alchemancy.properties.Property;
import net.cibernet.alchemancy.registries.AlchemancyProperties;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Per-property grist surcharge applied by {@link AlchemiterCostHelper} on top of an item's normal
 * Minestuck grist cost for every Alchemancy property active on it, covering every property
 * registered by either the base mod ({@link AlchemancyProperties}) or this addon
 * ({@link AddonProperties}).
 * <p>
 * The grist type(s) per property are hand-picked to loosely match the property's name/flavor - most
 * properties cost a single type, but ones that are either mechanically significant (Awakened,
 * Vampiric, Exploding, ...) or whose flavor naturally spans two materials (Gilded, Smiting, ...)
 * cost a blend of two or three. Amounts are a flat-ish surcharge per type with a small,
 * per-property-stable amount of variance (seeded off the property's own registry id, so it doesn't
 * reshuffle every launch) rather than a hand-tuned balance pass. {@link GristTypes#ZILLIUM} is
 * deliberately never used here, since it's reserved as an endgame-only grist type.
 */
public final class PropertyGristCosts
{
	private PropertyGristCosts()
	{
	}

	private static final int MIN_AMOUNT = 5;
	private static final int AMOUNT_VARIANCE = 8;

	private static final Map<ResourceLocation, GristSet> COSTS = new HashMap<>();

	public static GristSet get(Holder<Property> property)
	{
		ResourceLocation id = AlchemancyProperties.getKeyFor(property.value());
		return id != null ? COSTS.get(id) : null;
	}

	@SafeVarargs
	private static void cost(DeferredHolder<Property, ? extends Property> property, Supplier<GristType>... types)
	{
		ResourceLocation id = property.getId();
		Random random = new Random(id.hashCode());

		MutableGristSet set = MutableGristSet.newDefault();
		for (Supplier<GristType> type : types)
			set.add(type, MIN_AMOUNT + random.nextInt(AMOUNT_VARIANCE));

		COSTS.put(id, set.asImmutable());
	}

	static
	{
		//Elemental
		cost(AlchemancyProperties.BURNING, GristTypes.SULFUR, GristTypes.TAR);
		cost(AlchemancyProperties.WET, GristTypes.MERCURY);
		cost(AlchemancyProperties.FROSTED, GristTypes.QUARTZ, GristTypes.MERCURY);
		cost(AlchemancyProperties.SHOCKING, GristTypes.COBALT, GristTypes.TAR);
		cost(AlchemancyProperties.PHOTOSYNTHETIC, GristTypes.CHALK);

		//Materials
		cost(AlchemancyProperties.FLAMMABLE, GristTypes.SULFUR, GristTypes.AMBER);
		cost(AlchemancyProperties.CHARRED, GristTypes.TAR);
		cost(AlchemancyProperties.STURDY, GristTypes.SHALE);
		cost(AlchemancyProperties.BRITTLE, GristTypes.CHALK);
		cost(AlchemancyProperties.RUSTY, GristTypes.RUST);
		cost(AlchemancyProperties.FERROUS, GristTypes.COBALT);
		cost(AlchemancyProperties.GILDED, GristTypes.GOLD, GristTypes.DIAMOND);
		cost(AlchemancyProperties.LUSTROUS, GristTypes.DIAMOND, GristTypes.QUARTZ);
		cost(AlchemancyProperties.WEALTHY, GristTypes.GOLD, GristTypes.RUBY);
		cost(AlchemancyProperties.REINFORCED, GristTypes.SHALE);
		cost(AlchemancyProperties.PRISTINE, GristTypes.DIAMOND);
		cost(AlchemancyProperties.HELLBENT, GristTypes.SULFUR);
		cost(AlchemancyProperties.DEPTH_DWELLER, GristTypes.SHALE);
		cost(AlchemancyProperties.OVERGROWTH, GristTypes.AMBER);
		cost(AlchemancyProperties.WILDFIRE, GristTypes.SULFUR);
		cost(AlchemancyProperties.MALLEABLE, GristTypes.MERCURY);
		cost(AlchemancyProperties.CLAY_MOLD, GristTypes.CAULK);
		cost(AlchemancyProperties.HARDENED, GristTypes.DIAMOND);
		cost(AlchemancyProperties.CRACKED, GristTypes.CHALK);

		//Mobility
		cost(AlchemancyProperties.ENERGIZED, GristTypes.COBALT);
		cost(AlchemancyProperties.BOUNCY, GristTypes.BUILD);
		cost(AlchemancyProperties.STICKY, GristTypes.AMBER);
		cost(AlchemancyProperties.SLIPPERY, GristTypes.MERCURY);
		cost(AlchemancyProperties.BUOYANT, GristTypes.CAULK);
		cost(AlchemancyProperties.LIGHTWEIGHT, GristTypes.CHALK);
		cost(AlchemancyProperties.HEAVY, GristTypes.SHALE);
		cost(AlchemancyProperties.ANTIGRAV, GristTypes.QUARTZ);
		cost(AlchemancyProperties.DEXTEROUS, GristTypes.COBALT);
		cost(AlchemancyProperties.WAVE_RIDER, GristTypes.MERCURY);
		cost(AlchemancyProperties.AIR_WALKER, GristTypes.QUARTZ);
		cost(AlchemancyProperties.ATHLETIC, GristTypes.BUILD);
		cost(AlchemancyProperties.ANCHORED, GristTypes.SHALE);

		//Tools
		cost(AlchemancyProperties.MINING, GristTypes.SHALE);
		cost(AlchemancyProperties.CHOPPING, GristTypes.AMBER);
		cost(AlchemancyProperties.DIGGING, GristTypes.SHALE);
		cost(AlchemancyProperties.REAPING, GristTypes.RUST);
		cost(AlchemancyProperties.SHEARING, GristTypes.DIAMOND);
		cost(AlchemancyProperties.SLASHING, GristTypes.RUBY);
		cost(AlchemancyProperties.SHARPSHOOTING, GristTypes.COBALT);
		cost(AlchemancyProperties.SHIELDING, GristTypes.SHALE);
		cost(AlchemancyProperties.FIRESTARTING, GristTypes.SULFUR);
		cost(AlchemancyProperties.BRUSHING, GristTypes.CHALK);
		cost(AlchemancyProperties.SCOPING, GristTypes.QUARTZ);
		cost(AlchemancyProperties.WAYFINDING, GristTypes.GARNET);
		cost(AlchemancyProperties.HEADWEAR, GristTypes.GOLD);
		cost(AlchemancyProperties.SADDLED, GristTypes.AMBER);
		cost(AlchemancyProperties.GLIDER, GristTypes.QUARTZ);
		cost(AlchemancyProperties.DEATH_TRACKER, GristTypes.TAR);
		cost(AlchemancyProperties.CRAFTY, GristTypes.BUILD);
		cost(AlchemancyProperties.STONECUTTING, GristTypes.SHALE);
		cost(AlchemancyProperties.ENDER_POCKET, GristTypes.AMETHYST);
		cost(AlchemancyProperties.SMELTING, GristTypes.SULFUR);
		cost(AlchemancyProperties.ASSIMILATING, GristTypes.MERCURY);
		cost(AlchemancyProperties.ASSEMBLING, GristTypes.COBALT);
		cost(AlchemancyProperties.REPLICATING, GristTypes.QUARTZ);
		cost(AlchemancyProperties.FRAGMENTED, GristTypes.CHALK);

		//Storage
		cost(AlchemancyProperties.HOLLOW, GristTypes.CAULK);
		cost(AlchemancyProperties.BUCKETING, GristTypes.MERCURY);
		cost(AlchemancyProperties.ENCAPSULATING, GristTypes.QUARTZ);
		cost(AlchemancyProperties.CAPTURING, GristTypes.AMBER);
		cost(AlchemancyProperties.DRIPPING, GristTypes.MERCURY);
		cost(AlchemancyProperties.ABSORBING, GristTypes.CHALK);
		cost(AlchemancyProperties.RESTOCKER, GristTypes.BUILD);
		cost(AlchemancyProperties.NULLIFIER, GristTypes.TAR);

		//Triggering
		cost(AlchemancyProperties.EDIBLE, GristTypes.BUILD);
		cost(AlchemancyProperties.JAGGED, GristTypes.SHALE);
		cost(AlchemancyProperties.ROOTED, GristTypes.AMBER);
		cost(AlchemancyProperties.SENSITIVE, GristTypes.COBALT);
		cost(AlchemancyProperties.INTERACTABLE, GristTypes.BUILD);
		cost(AlchemancyProperties.MYCELLIC, GristTypes.RUST);
		cost(AlchemancyProperties.SPORADIC, GristTypes.TAR);
		cost(AlchemancyProperties.SHATTERING, GristTypes.CHALK);
		cost(AlchemancyProperties.THROWABLE, GristTypes.BUILD);
		cost(AlchemancyProperties.TOGGLEABLE, GristTypes.COBALT);
		cost(AlchemancyProperties.TICKING, GristTypes.COBALT);
		cost(AlchemancyProperties.HYDROPHOBIC, GristTypes.MERCURY);
		cost(AlchemancyProperties.ALLERGIC, GristTypes.IODINE);
		cost(AlchemancyProperties.ARMOR_PULSE, GristTypes.COBALT);
		cost(AlchemancyProperties.RUNNING_START, GristTypes.BUILD);
		cost(AlchemancyProperties.SCATTERSHOT, GristTypes.GARNET);
		cost(AlchemancyProperties.DISPENSING, GristTypes.BUILD);
		cost(AlchemancyProperties.DEATHRATTLE, GristTypes.TAR);
		cost(AlchemancyProperties.CHANCE_STRIKE, GristTypes.AMETHYST);

		//Mob Effects
		cost(AlchemancyProperties.LEVITATING, GristTypes.QUARTZ);
		cost(AlchemancyProperties.SWIFT, GristTypes.COBALT);
		cost(AlchemancyProperties.SLUGGISH, GristTypes.TAR);
		cost(AlchemancyProperties.POISONOUS, GristTypes.IODINE);
		cost(AlchemancyProperties.DECAYING, GristTypes.RUST);
		cost(AlchemancyProperties.TIPSY, GristTypes.AMBER);
		cost(AlchemancyProperties.BLINDING, GristTypes.DIAMOND);
		cost(AlchemancyProperties.NOCTURNAL, GristTypes.TAR);
		cost(AlchemancyProperties.AQUATIC, GristTypes.MERCURY);
		cost(AlchemancyProperties.LEAPING, GristTypes.BUILD);
		cost(AlchemancyProperties.GLOWING_AURA, GristTypes.AMETHYST);
		cost(AlchemancyProperties.OMINOUS, GristTypes.TAR);
		cost(AlchemancyProperties.HEARTY, GristTypes.RUBY);
		cost(AlchemancyProperties.INFERNAL, GristTypes.SULFUR, GristTypes.TAR);
		cost(AlchemancyProperties.IMBUED, GristTypes.AMETHYST);

		//Offensive
		cost(AlchemancyProperties.GRAPPLING, GristTypes.SHALE);
		cost(AlchemancyProperties.SPIKING, GristTypes.DIAMOND);
		cost(AlchemancyProperties.LAUNCHING, GristTypes.COBALT);
		cost(AlchemancyProperties.SHARP, GristTypes.DIAMOND);
		cost(AlchemancyProperties.WEAK, GristTypes.CHALK);
		cost(AlchemancyProperties.DENSE, GristTypes.MERCURY);
		cost(AlchemancyProperties.GAMBLING, GristTypes.RUBY);
		cost(AlchemancyProperties.ARCANE, GristTypes.AMETHYST);
		cost(AlchemancyProperties.RESIZED, GristTypes.QUARTZ);
		cost(AlchemancyProperties.FERAL, GristTypes.RUST);
		cost(AlchemancyProperties.CRACKLING, GristTypes.COBALT);
		cost(AlchemancyProperties.ECHOED, GristTypes.QUARTZ);
		cost(AlchemancyProperties.CORROSIVE, GristTypes.RUST, GristTypes.IODINE);

		//On Crit
		cost(AlchemancyProperties.EXPLODING, GristTypes.SULFUR, GristTypes.SHALE);
		cost(AlchemancyProperties.WIND_CHARGED, GristTypes.QUARTZ, GristTypes.COBALT);
		cost(AlchemancyProperties.SMITING, GristTypes.COBALT, GristTypes.GARNET);

		//Resistances
		cost(AlchemancyProperties.COZY, GristTypes.AMBER);
		cost(AlchemancyProperties.WAXED, GristTypes.CAULK);
		cost(AlchemancyProperties.FIRE_RESISTANT, GristTypes.SHALE);
		cost(AlchemancyProperties.BLAST_RESISTANT, GristTypes.DIAMOND);
		cost(AlchemancyProperties.INSULATED, GristTypes.CAULK);
		cost(AlchemancyProperties.WARDING, GristTypes.AMETHYST, GristTypes.SHALE);
		cost(AlchemancyProperties.ANCIENT, GristTypes.SHALE, GristTypes.GOLD);
		cost(AlchemancyProperties.ETERNAL, GristTypes.DIAMOND, GristTypes.ARTIFACT);
		cost(AlchemancyProperties.MUFFLED, GristTypes.CHALK);
		cost(AlchemancyProperties.MAGIC_RESISTANT, GristTypes.AMETHYST, GristTypes.SHALE);
		cost(AlchemancyProperties.SMOOTH, GristTypes.QUARTZ);

		//Soulbind
		cost(AlchemancyProperties.SOULBIND, GristTypes.MARBLE, GristTypes.AMETHYST);
		cost(AlchemancyProperties.LOYAL, GristTypes.RUBY);
		cost(AlchemancyProperties.VENGEFUL, GristTypes.GARNET);
		cost(AlchemancyProperties.VAMPIRIC, GristTypes.RUBY, GristTypes.MERCURY);
		cost(AlchemancyProperties.ENERGY_SAPPER, GristTypes.COBALT, GristTypes.TAR);
		cost(AlchemancyProperties.RELENTLESS, GristTypes.SHALE, GristTypes.RUBY);
		cost(AlchemancyProperties.SPIRIT_BOND, GristTypes.MARBLE, GristTypes.RUBY);
		cost(AlchemancyProperties.PHASING, GristTypes.MARBLE, GristTypes.QUARTZ);
		cost(AlchemancyProperties.HUNGERING, GristTypes.TAR, GristTypes.RUBY);
		cost(AlchemancyProperties.PARASITIC, GristTypes.RUST, GristTypes.TAR);
		cost(AlchemancyProperties.SOUL_HARVESTER, GristTypes.MARBLE, GristTypes.TAR);
		cost(AlchemancyProperties.FLEETING, GristTypes.QUARTZ);

		//Voidborn
		cost(AlchemancyProperties.VOIDBORN, GristTypes.TAR, GristTypes.AMETHYST);
		cost(AlchemancyProperties.CEASELESS_VOID, GristTypes.TAR, GristTypes.URANIUM);
		cost(AlchemancyProperties.VOIDTOUCH, GristTypes.TAR);
		cost(AlchemancyProperties.KINETIC_GRAB, GristTypes.COBALT, GristTypes.AMETHYST);
		cost(AlchemancyProperties.VACUUMING, GristTypes.TAR, GristTypes.COBALT);

		//Misc
		cost(AlchemancyProperties.NONLETHAL, GristTypes.BUILD);
		cost(AlchemancyProperties.MENDING, GristTypes.CAULK, GristTypes.DIAMOND);
		cost(AlchemancyProperties.FLOURISH, GristTypes.AMBER);
		cost(AlchemancyProperties.UNDEAD, GristTypes.RUST);
		cost(AlchemancyProperties.UNDYING, GristTypes.DIAMOND, GristTypes.RUBY);
		cost(AlchemancyProperties.INFECTED, GristTypes.RUST);
		cost(AlchemancyProperties.DEAD, GristTypes.TAR);
		cost(AlchemancyProperties.SANITIZED, GristTypes.CHALK);
		cost(AlchemancyProperties.SWEET, GristTypes.AMBER);
		cost(AlchemancyProperties.SCARY, GristTypes.TAR);
		cost(AlchemancyProperties.SEEDED, GristTypes.AMBER);
		cost(AlchemancyProperties.PUTRID, GristTypes.RUST);
		cost(AlchemancyProperties.CHARMING, GristTypes.AMETHYST, GristTypes.RUBY);
		cost(AlchemancyProperties.CONDUCTIVE, GristTypes.COBALT);
		cost(AlchemancyProperties.CLUELESS, GristTypes.BUILD);
		cost(AlchemancyProperties.ENCHANTING, GristTypes.AMETHYST, GristTypes.GOLD);
		cost(AlchemancyProperties.WISE, GristTypes.QUARTZ, GristTypes.AMETHYST);
		cost(AlchemancyProperties.EXPERIENCED, GristTypes.GOLD);
		cost(AlchemancyProperties.ENDER, GristTypes.AMETHYST, GristTypes.TAR);
		cost(AlchemancyProperties.SCULKING, GristTypes.TAR, GristTypes.RUST);
		cost(AlchemancyProperties.LOOSE, GristTypes.CHALK);
		cost(AlchemancyProperties.SPARKING, GristTypes.COBALT);
		cost(AlchemancyProperties.EXTENDED, GristTypes.BUILD);
		cost(AlchemancyProperties.CALCAREOUS, GristTypes.CHALK);
		cost(AlchemancyProperties.MUSICAL, GristTypes.QUARTZ);
		cost(AlchemancyProperties.TARGETED, GristTypes.GARNET, GristTypes.COBALT);
		cost(AlchemancyProperties.REPELLED, GristTypes.COBALT, GristTypes.TAR);
		cost(AlchemancyProperties.LIGHT_SEEKING, GristTypes.QUARTZ);
		cost(AlchemancyProperties.FLIMSY, GristTypes.CHALK);
		cost(AlchemancyProperties.COMPACT, GristTypes.SHALE);
		cost(AlchemancyProperties.MAGNETIC, GristTypes.COBALT);
		cost(AlchemancyProperties.KINETIC_RECHARGE, GristTypes.COBALT);
		cost(AlchemancyProperties.LAZY, GristTypes.TAR);
		cost(AlchemancyProperties.CONVEYED, GristTypes.COBALT);

		//Cosmetic
		cost(AlchemancyProperties.REVEALED, GristTypes.QUARTZ);
		cost(AlchemancyProperties.REVEALING, GristTypes.QUARTZ);
		cost(AlchemancyProperties.SCRAMBLED, GristTypes.TAR);
		cost(AlchemancyProperties.CONCEALED, GristTypes.TAR);
		cost(AlchemancyProperties.DISGUISED, GristTypes.AMETHYST);
		cost(AlchemancyProperties.SEETHROUGH, GristTypes.QUARTZ);
		cost(AlchemancyProperties.TINTED, GristTypes.GARNET);
		cost(AlchemancyProperties.FLATTENED, GristTypes.CHALK);
		cost(AlchemancyProperties.SPARKLING, GristTypes.DIAMOND, GristTypes.QUARTZ);

		//Special
		cost(AlchemancyProperties.AWAKENED, GristTypes.ARTIFACT, GristTypes.AMETHYST);
		cost(AlchemancyProperties.PARADOXICAL, GristTypes.ARTIFACT, GristTypes.AMETHYST);
		cost(AlchemancyProperties.LIMIT_BREAK, GristTypes.ARTIFACT, GristTypes.DIAMOND);
		cost(AlchemancyProperties.ENTANGLED, GristTypes.AMETHYST);
		cost(AlchemancyProperties.USE_ENTANGLED, GristTypes.AMETHYST);
		cost(AlchemancyProperties.CROUCH_ENTANGLED, GristTypes.AMETHYST);
		cost(AlchemancyProperties.JUMP_ENTANGLED, GristTypes.AMETHYST);
		cost(AlchemancyProperties.SPRINT_ENTANGLED, GristTypes.AMETHYST);
		cost(AlchemancyProperties.QUANTUM_SHIFT, GristTypes.AMETHYST, GristTypes.QUARTZ);
		cost(AlchemancyProperties.DIRTY, GristTypes.RUST);
		cost(AlchemancyProperties.AWKWARD, GristTypes.BUILD);
		cost(AlchemancyProperties.WARPED, GristTypes.AMETHYST);

		//Goof-around
		cost(AlchemancyProperties.RANDOM, GristTypes.BUILD);
		cost(AlchemancyProperties.WORLD_OBLITERATOR, GristTypes.URANIUM, GristTypes.ARTIFACT);
		cost(AlchemancyProperties.UNMOVABLE, GristTypes.SHALE);
		cost(AlchemancyProperties.ITEM_PULL, GristTypes.COBALT);
		cost(AlchemancyProperties.CHROMATIZE, GristTypes.DIAMOND, GristTypes.RUBY, GristTypes.AMETHYST);
		cost(AlchemancyProperties.ROTATING, GristTypes.COBALT);
		cost(AlchemancyProperties.BATTERY_POWERED, GristTypes.COBALT, GristTypes.URANIUM);
		cost(AlchemancyProperties.LIVING_BATTERY, GristTypes.COBALT, GristTypes.RUBY);
		cost(AlchemancyProperties.BADA_QUIP, GristTypes.GOLD);

		//Innate
		cost(AlchemancyProperties.INFUSION_CODEX, GristTypes.ARTIFACT, GristTypes.AMETHYST);
		cost(AlchemancyProperties.AUXILIARY, GristTypes.BUILD);
		cost(AlchemancyProperties.ETERNAL_GLOW, GristTypes.DIAMOND);
		cost(AlchemancyProperties.PHASE_STEP, GristTypes.MARBLE);
		cost(AlchemancyProperties.DEATH_WARD, GristTypes.AMETHYST);
		cost(AlchemancyProperties.FRIENDLY, GristTypes.RUBY);
		cost(AlchemancyProperties.WAYWARD_WARP, GristTypes.AMETHYST);
		cost(AlchemancyProperties.ROCKET_POWERED, GristTypes.SULFUR, GristTypes.COBALT);
		cost(AlchemancyProperties.BINDING, GristTypes.CAULK);
		cost(AlchemancyProperties.INFUSION_CLEANSE, GristTypes.CHALK);
		cost(AlchemancyProperties.DIVINE_CLEANSE, GristTypes.ARTIFACT, GristTypes.DIAMOND);
		cost(AlchemancyProperties.FLAME_STEP, GristTypes.SULFUR);
		cost(AlchemancyProperties.FLAME_EMPEROR, GristTypes.ARTIFACT, GristTypes.SULFUR);
		cost(AlchemancyProperties.BLINKING, GristTypes.AMETHYST);
		cost(AlchemancyProperties.CLOUD_DASH, GristTypes.QUARTZ);
		cost(AlchemancyProperties.CRYSTAL_DASH, GristTypes.DIAMOND);
		cost(AlchemancyProperties.HOME_RUN, GristTypes.GOLD);
		cost(AlchemancyProperties.VAULTPICKING, GristTypes.GOLD);
		cost(AlchemancyProperties.GUST_JET, GristTypes.QUARTZ);
		cost(AlchemancyProperties.TINTED_LENS, GristTypes.AMETHYST, GristTypes.QUARTZ);

		//Addon properties
		cost(AddonProperties.ALCHEMIZING, GristTypes.BUILD);
		cost(AddonProperties.CAPTCHALOGUING, GristTypes.AMBER);
		cost(AddonProperties.MAGICAL, GristTypes.AMETHYST);
		cost(AddonProperties.RAGE_INDUCING, GristTypes.RUBY, GristTypes.SULFUR);
		cost(AddonProperties.CRUXITE, GristTypes.GOLD, GristTypes.ARTIFACT);
		cost(AddonProperties.PERFECTLY_GENERIC, GristTypes.BUILD);
		cost(AddonProperties.BADASS, GristTypes.DIAMOND);
		cost(AddonProperties.GRISTIFYING, GristTypes.BUILD);
		cost(AddonProperties.SBAHJ, GristTypes.ARTIFACT, GristTypes.RUBY);
		cost(AddonProperties.AIMBOT, GristTypes.COBALT);
		cost(AddonProperties.DISCOUNT, GristTypes.GOLD);
	}
}
