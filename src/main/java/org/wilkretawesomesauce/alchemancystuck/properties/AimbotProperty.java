package org.wilkretawesomesauce.alchemancystuck.properties;

import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.cibernet.alchemancy.properties.Property;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.wilkretawesomesauce.alchemancystuck.registry.AddonProperties;

public class AimbotProperty extends Property {

    private static final int TARGET_RANGE = 11;
    private static final TargetingConditions TARGETING_CONDITIONS = TargetingConditions.forCombat();

    private static final int RANDOM_LOOK_SPREAD = 5;

    @Override
    public void onAttack(Entity user, ItemStack weapon, DamageSource damageSource, LivingEntity target) {
        aimOrLookRandom(user, weapon);
    }

    @Override
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        aimOrLookRandom(event.getEntity(), event.getItemStack());
    }

    @Override
    public void onEquippedTick(LivingEntity user, EquipmentSlot slot, ItemStack stack) {
        if (slot.isArmor())
            aimOrLookRandom(user, stack);
    }

    private static void aimOrLookRandom(Entity user, ItemStack stack) {
        if (InfusedPropertiesHelper.hasProperty(stack, AddonProperties.SBAHJ))
            lookSomewhereRandom(user);
        else
            aimAtNearestTarget(user);
    }

    private static void lookSomewhereRandom(Entity user) {
        if (!(user instanceof ServerPlayer player))
            return;

        Vec3 randomFacingPos = new Vec3(
                player.getX() + player.getRandom().nextInt(2 * RANDOM_LOOK_SPREAD) - RANDOM_LOOK_SPREAD,
                player.getY() + player.getRandom().nextInt(2 * RANDOM_LOOK_SPREAD) - RANDOM_LOOK_SPREAD,
                player.getZ() + player.getRandom().nextInt(2 * RANDOM_LOOK_SPREAD) - RANDOM_LOOK_SPREAD
        );

        player.lookAt(player.createCommandSourceStack().getAnchor(), randomFacingPos);
    }

    private static void aimAtNearestTarget(Entity user) {
        if (!(user instanceof ServerPlayer player))
            return;

        BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        LivingEntity target = player.level().getNearestEntity(LivingEntity.class, TARGETING_CONDITIONS, player,
                player.getX(), player.getEyeY(), player.getZ(), new AABB(eyePos).inflate(TARGET_RANGE));

        if (target != null)
            player.lookAt(EntityAnchorArgument.Anchor.EYES, target, EntityAnchorArgument.Anchor.EYES);
    }

    @Override
    public int getColor(ItemStack stack) {
        return 0xFF0000;
    }
}
