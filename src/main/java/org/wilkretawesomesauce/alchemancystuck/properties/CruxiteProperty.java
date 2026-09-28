package org.wilkretawesomesauce.alchemancystuck.properties;

import com.mraof.minestuck.player.IdentifierHandler;
import com.mraof.minestuck.util.ColorHandler;
import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.cibernet.alchemancy.properties.Property;
import net.cibernet.alchemancy.registries.AlchemancyProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.wilkretawesomesauce.alchemancystuck.Alchemancystuck;

public class CruxiteProperty extends Property {

    public static final float GRIST_COST_SCALE = 0.65F;
    private static final float DAMAGE_BUFF = 0.05F;
    private static final ResourceLocation DAMAGE_MOD_KEY = ResourceLocation.fromNamespaceAndPath(Alchemancystuck.MODID, "cruxite_color_match");

    @Override
    public void onEquippedTick(LivingEntity entity, EquipmentSlot slot, ItemStack itemStack) {
        if (!(entity instanceof Player player))
            return;

        if (!(player.level() instanceof ServerLevel))
            return;

        AttributeInstance attackDamage = player.getAttributes().getInstance(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null)
            return;

        int itemColor = FastColor.ARGB32.opaque(ColorHandler.getColorFromStack(itemStack));
        int playerColor = FastColor.ARGB32.opaque(ColorHandler.getColorForPlayer(IdentifierHandler.encode(player), player.level()));
        boolean matches = itemColor == playerColor;

        if (!matches && InfusedPropertiesHelper.hasProperty(itemStack, AlchemancyProperties.TINTED)) {
            int tintedColor = FastColor.ARGB32.opaque(AlchemancyProperties.TINTED.value().getColor(itemStack));
            matches = tintedColor == playerColor;
        }

        boolean hasModifier = attackDamage.hasModifier(DAMAGE_MOD_KEY);

        if (matches && !hasModifier) {
            attackDamage.addPermanentModifier(new AttributeModifier(DAMAGE_MOD_KEY, DAMAGE_BUFF, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!matches && hasModifier) {
            attackDamage.removeModifier(DAMAGE_MOD_KEY);
        }
    }

    @Override
    public int getColor(ItemStack itemStack) {
        return 0xCDEAFC;
    }
}
