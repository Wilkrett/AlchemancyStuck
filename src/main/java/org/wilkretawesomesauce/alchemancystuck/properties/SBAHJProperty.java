package org.wilkretawesomesauce.alchemancystuck.properties;

import net.cibernet.alchemancy.item.components.InfusedPropertiesHelper;
import net.cibernet.alchemancy.properties.Property;
import net.cibernet.alchemancy.properties.data.IDataHolder;
import net.cibernet.alchemancy.registries.AlchemancyProperties;
import net.cibernet.alchemancy.util.ColorUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Same mechanic as Minestuck's own SBAHJ-tier weapons ({@code sord}, {@code nife}, {@code batleacks},
 * {@code lanec}, {@code ow_the_edge} - all built on {@code OnHitEffect.SORD_DROP}): a flat 25% chance
 * per hit to slip out of the attacker's hand and land on the ground as a real, pickup-delayed item
 * entity, plus a chat line. Unlike {@code SORD_DROP} (one fixed message baked into the item's own
 * description id), this picks randomly from a small pool of its own, since it's meant to be infused
 * onto whatever weapon rather than tied to one specific item.
 *
 * <p>Also absorbs the former {@code ArtifactProperty}'s grist-cost behavior (collapsing the
 * alchemizing cost of an infused item into a single Artifact-grist total) - see
 * {@link org.wilkretawesomesauce.alchemancystuck.alchemy.AlchemiterCostHelper}.
 *
 * <p>Worn armor also gets an 80% chance to actually stay equipped, rolled once per equip via
 * {@link #onEquippedTick} rather than every tick - {@code onEquippedTick} alone has no "you just put
 * this on" moment, so a per-stack {@code IDataHolder<Boolean>} flag (persisted via the base mod's own
 * {@code PropertyDataComponent} mechanism, same as {@code HollowProperty}'s stored item) remembers
 * whether this particular equip has already been rolled, and {@link #onInventoryTick} clears it back
 * to unrolled the moment the item isn't worn, so taking it off and putting it back on rolls fresh.
 *
 * <p>Also breaks {@code AntigravProperty} when both are on the same item: instead of
 * {@code Antigrav}'s own "zero gravity, drift wherever" ({@code Attributes.GRAVITY} zeroed when worn,
 * {@code setNoGravity(true)} when dropped or thrown), SBAHJ layers a constant upward push on top -
 * dropped ({@link #onEntityItemTick}) and thrown/shot ({@link #onProjectileTick}) just get a forced
 * upward velocity every tick (nothing to announce it to), while worn *or held*
 * ({@link #onEquippedTick}, unlike Antigrav's own armor-only version) grants the wearer vanilla
 * Levitation instead, refreshed every tick same as {@code GlowingProperty}'s effect-refresh idiom.
 * The "LIFDOFF" line only fires for the player case, gated on {@code !user.hasEffect(LEVITATION)}
 * checked *before* re-applying it that tick - since the effect is refreshed continuously while
 * equipped, that condition is only ever true on the first tick of a given liftoff, not every tick.
 *
 * <p>SBAHJ + {@code EdibleProperty}: {@link #onFinishUsingItem} just piggybacks a chat line onto
 * {@code EdibleProperty}'s own eating logic - {@code ItemStackMixin#finishUsingItem} calls every
 * active property's {@code onFinishUsingItem} independently via {@code forEachProperty} regardless of
 * what any of them return, so this doesn't need to (and shouldn't) touch the actual eating; it returns
 * {@code false} and leaves that entirely to {@code EdibleProperty}.
 *
 * <p>SBAHJ + {@code TreadmilledProperty} ("Conveyed"): {@link #swerve} adds a random left/right
 * impulse on top of whatever horizontal velocity Conveyed's own {@code onEquippedTick} already set that
 * tick, rather than replacing it - which is why {@link #getPriority()} is overridden to
 * {@code Priority.LOWER} (runs after the default-priority {@code TreadmilledProperty}, per
 * {@code Property.Priority}'s "lower value runs first" ordering) instead of leaving it at the default:
 * Conveyed's own hook unconditionally overwrites {@code getDeltaMovement()} with a fresh forward vector
 * every tick, so building on it instead of fighting over ordering means this has to run strictly after.
 */

public class SBAHJProperty extends Property implements IDataHolder<Boolean> {

    private static final float DROP_CHANCE = 0.25F;
    private static final int DROP_PICKUP_DELAY_TICKS = 40;
    private static final int MESSAGE_COUNT = 6;

    private static final float EQUIP_SUCCESS_CHANCE = 0.65F;
    private static final int EQUIP_FAIL_MESSAGE_COUNT = 4;

    private static final double LIFTOFF_VELOCITY = 0.1D;
    private static final int LIFTOFF_EFFECT_DURATION_TICKS = 10;

    private static final double SWERVE_STRENGTH = 0.3D;

    @Override
    public int getPriority() {
        return Priority.LOWER;
    }

    @Override
    public void modifyAttackDamage(Entity user, ItemStack weapon, LivingDamageEvent.Pre event) {
        if (user.level().isClientSide || user.getRandom().nextFloat() >= DROP_CHANCE)
            return;

        Level level = user.level();

        ItemStack dropped = weapon.copy();
        dropped.setCount(1);
        ItemEntity itemEntity = new ItemEntity(level, user.getX(), user.getY(), user.getZ(), dropped);
        itemEntity.setPickUpDelay(DROP_PICKUP_DELAY_TICKS);
        level.addFreshEntity(itemEntity);
        weapon.shrink(1);

        int variant = user.getRandom().nextInt(MESSAGE_COUNT) + 1;
        user.sendSystemMessage(Component.translatable("message.alchemancystuck.sbahj.drop" + variant));
    }

    @Override
    public void onEquippedTick(LivingEntity user, EquipmentSlot slot, ItemStack stack) {
        if (user.level().isClientSide)
            return;

        if (slot.isArmor() && !getData(stack)) {
            setData(stack, true);

            if (user.getRandom().nextFloat() >= EQUIP_SUCCESS_CHANCE) {
                ejectFromSlot(user, slot, stack);
                return;
            }
        }

        if (InfusedPropertiesHelper.hasProperty(stack, AlchemancyProperties.ANTIGRAV)) {
            boolean wasLifting = user.hasEffect(MobEffects.LEVITATION);
            user.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LIFTOFF_EFFECT_DURATION_TICKS, 0, false, false));

            if (!wasLifting && user instanceof Player player)
                player.sendSystemMessage(Component.translatable("message.alchemancystuck.sbahj.liftoff"));
        }

        if (InfusedPropertiesHelper.hasProperty(stack, AlchemancyProperties.CONVEYED))
            swerve(user);
    }

    private static void swerve(LivingEntity user) {
        Vec3 motion = user.getDeltaMovement();
        Vec3 horizontal = new Vec3(motion.x, 0, motion.z);
        if (horizontal.lengthSqr() < 1.0E-4)
            return;

        Vec3 forward = horizontal.normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        double swerveAmount = (user.getRandom().nextFloat() * 2 - 1) * SWERVE_STRENGTH;

        user.setDeltaMovement(motion.add(right.scale(swerveAmount)));
    }

    private static void ejectFromSlot(LivingEntity user, EquipmentSlot slot, ItemStack stack) {
        Level level = user.level();
        ItemStack ejected = stack.copy();
        user.setItemSlot(slot, ItemStack.EMPTY);

        ItemEntity itemEntity = new ItemEntity(level, user.getX(), user.getY(), user.getZ(), ejected);
        itemEntity.setPickUpDelay(DROP_PICKUP_DELAY_TICKS);
        level.addFreshEntity(itemEntity);

        if (user instanceof Player player) {
            int variant = user.getRandom().nextInt(EQUIP_FAIL_MESSAGE_COUNT) + 1;
            player.sendSystemMessage(Component.translatable("message.alchemancystuck.sbahj.equip_fail" + variant));
        }
    }

    @Override
    public void onInventoryTick(Entity user, ItemStack stack, Level level, int inventorySlot, boolean isCurrentItem) {
        if (!level.isClientSide && getData(stack))
            setData(stack, false);
    }

    @Override
    public void onEntityItemTick(ItemStack stack, ItemEntity itemEntity) {
        if (InfusedPropertiesHelper.hasProperty(stack, AlchemancyProperties.ANTIGRAV))
            liftOff(itemEntity);
    }

    @Override
    public void onProjectileTick(ItemStack stack, Projectile projectile) {
        if (InfusedPropertiesHelper.hasProperty(stack, AlchemancyProperties.ANTIGRAV))
            liftOff(projectile);
    }

    private static void liftOff(Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, LIFTOFF_VELOCITY, motion.z);
    }

    @Override
    public boolean onFinishUsingItem(LivingEntity user, Level level, ItemStack stack) {
        if (!level.isClientSide && user instanceof Player player
                && InfusedPropertiesHelper.hasProperty(stack, AlchemancyProperties.EDIBLE))
            player.sendSystemMessage(Component.translatable("message.alchemancystuck.sbahj.edible"));

        return false;
    }

    @Override
    public Boolean readData(CompoundTag tag) {
        return tag.getBoolean("rolled");
    }

    @Override
    public CompoundTag writeData(Boolean data) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("rolled", data);
        return tag;
    }

    @Override
    public Boolean getDefaultData() {
        return false;
    }

    @Override
    public int getColor(ItemStack stack) {
        return ColorUtils.interpolateColorsAndWait(3.5f, 0xe6032e, 0x023cff, 0xfffc00);
    }
}
