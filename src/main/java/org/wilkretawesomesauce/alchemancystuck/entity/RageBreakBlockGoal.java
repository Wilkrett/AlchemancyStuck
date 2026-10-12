package org.wilkretawesomesauce.alchemancystuck.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class RageBreakBlockGoal extends Goal {

    private static final int MAX_TICKS_TO_BREAK = 100;

    private final RagePathBrainEntity mob;
    private int breakingTicks;
    private int ticksToBreak;
    @Nullable
    private BlockPos targetBlock;

    public RageBreakBlockGoal(RagePathBrainEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !this.mob.getNavigation().isStuck() || !EventHooks.canEntityGrief(this.mob.level(), this.mob))
            return false;

        this.targetBlock = findBreakableBlock(target);
        return this.targetBlock != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetBlock != null && this.mob.getTarget() != null && this.breakingTicks < this.ticksToBreak
                && !this.mob.level().getBlockState(this.targetBlock).isAir();
    }

    @Override
    public void start() {
        this.breakingTicks = 0;
    }

    @Override
    public void stop() {
        if (this.targetBlock != null)
            this.mob.level().destroyBlockProgress(this.mob.getId(), this.targetBlock, -1);
        this.targetBlock = null;
    }

    @Override
    public void tick() {
        if (this.targetBlock == null)
            return;

        this.mob.getLookControl().setLookAt(this.targetBlock.getX() + 0.5, this.targetBlock.getY() + 0.5, this.targetBlock.getZ() + 0.5);

        this.breakingTicks++;
        if (this.breakingTicks % 5 == 0)
            this.mob.swing(InteractionHand.MAIN_HAND);

        int progress = Math.min((int) ((float) this.breakingTicks / this.ticksToBreak * 10.0F), 9);
        this.mob.level().destroyBlockProgress(this.mob.getId(), this.targetBlock, progress);

        if (this.breakingTicks >= this.ticksToBreak) {
            this.mob.level().destroyBlock(this.targetBlock, false, this.mob, 512);
            this.targetBlock = null;
        }
    }

    @Nullable
    private BlockPos findBreakableBlock(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(this.mob.position());
        if (toTarget.lengthSqr() < 1.0E-4)
            return null;

        Direction facing = Direction.getNearest(toTarget.x, 0.0, toTarget.z);
        BlockPos inFront = this.mob.blockPosition().relative(facing);

        for (BlockPos pos : new BlockPos[]{inFront, inFront.above()}) {
            BlockState state = this.mob.level().getBlockState(pos);
            if (state.isAir())
                continue;

            int ticks = computeTicksToBreak(state, pos);
            if (ticks > 0 && ticks <= MAX_TICKS_TO_BREAK) {
                this.ticksToBreak = ticks;
                return pos;
            }
        }

        return null;
    }

    private int computeTicksToBreak(BlockState state, BlockPos pos) {
        float hardness = state.getDestroySpeed(this.mob.level(), pos);
        if (hardness < 0.0F)
            return 0;

        ItemStack tool = this.mob.getMainHandItem();
        float toolSpeed = tool.getDestroySpeed(state);
        int divisor = tool.isCorrectToolForDrops(state) ? 30 : 100;

        float progressPerTick = toolSpeed / hardness / divisor;
        return progressPerTick <= 0.0F ? 0 : (int) Math.ceil(1.0F / progressPerTick);
    }
}
