package org.wilkretawesomesauce.alchemancystuck.registry;

import net.cibernet.alchemancy.properties.Property;
import net.cibernet.alchemancy.registries.AlchemancyProperties;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;
import org.wilkretawesomesauce.alchemancystuck.properties.AimbotProperty;
import org.wilkretawesomesauce.alchemancystuck.properties.AlchemizingProperty;
import org.wilkretawesomesauce.alchemancystuck.properties.BadassProperty;
import org.wilkretawesomesauce.alchemancystuck.properties.CaptchaloguingProperty;
import org.wilkretawesomesauce.alchemancystuck.properties.CruxiteProperty;
import org.wilkretawesomesauce.alchemancystuck.properties.DiscountProperty;
import org.wilkretawesomesauce.alchemancystuck.properties.GristifyingProperty;
import org.wilkretawesomesauce.alchemancystuck.properties.MagicalProperty;
import org.wilkretawesomesauce.alchemancystuck.properties.PerfectlyGenericProperty;
import org.wilkretawesomesauce.alchemancystuck.properties.RageInducingProperty;
import org.wilkretawesomesauce.alchemancystuck.properties.SBAHJProperty;

public class AddonProperties {

    public static final DeferredRegister<Property> PROPERTIES = DeferredRegister.create(
            AlchemancyProperties.REGISTRY_KEY,
            Alchemancystuck.MODID
    );

    public static final DeferredHolder<Property, Property> ALCHEMIZING = PROPERTIES.register("alchemizing", AlchemizingProperty::new);
    public static final DeferredHolder<Property, Property> CAPTCHALOGUING = PROPERTIES.register("captchaloguing", CaptchaloguingProperty::new);
    public static final DeferredHolder<Property, Property> MAGICAL = PROPERTIES.register("magical", MagicalProperty::new);
    public static final DeferredHolder<Property, Property> RAGE_INDUCING = PROPERTIES.register("rage_inducing", RageInducingProperty::new);
    public static final DeferredHolder<Property, Property> CRUXITE = PROPERTIES.register("cruxite", CruxiteProperty::new);
    public static final DeferredHolder<Property, Property> PERFECTLY_GENERIC = PROPERTIES.register("perfectly_generic", PerfectlyGenericProperty::new);
    public static final DeferredHolder<Property, Property> BADASS = PROPERTIES.register("badass", BadassProperty::new);
    public static final DeferredHolder<Property, Property> GRISTIFYING = PROPERTIES.register("gristifying", GristifyingProperty::new);
    public static final DeferredHolder<Property, Property> SBAHJ = PROPERTIES.register("sbahj", SBAHJProperty::new);
    public static final DeferredHolder<Property, Property> AIMBOT = PROPERTIES.register("aimbot", AimbotProperty::new);
    public static final DeferredHolder<Property, Property> DISCOUNT = PROPERTIES.register("discount", DiscountProperty::new);

}
