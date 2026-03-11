package org.mod.coretuner.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.mod.coretuner.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerOptimizerMixin {

    @Unique private boolean coreTuner$isConfined      = false;
    @Unique private boolean coreTuner$cachedPanicking = false;
    @Unique private int     coreTuner$checkTimer      = 0;
    @Unique private int     coreTuner$stillCheckCount = 0;
    @Unique private int     coreTuner$restockTimer    = 0;
    @Unique private int     coreTuner$brainPhase      = -1;

    @Unique private double  coreTuner$refX = Double.NaN;
    @Unique private double  coreTuner$refY = Double.NaN;
    @Unique private double  coreTuner$refZ = Double.NaN;

    @Unique private static final int    CHECK_INTERVAL       = 60;
    @Unique private static final double CONFINEMENT_DIST_SQR = 0.01;
    @Unique private static final int    CONFIRM_CHECKS       = 2;
    @Unique private static final int    BRAIN_SKIP_RATIO     = 20;
    @Unique private static final int    RESTOCK_INTERVAL     = 6000;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Villager self = (Villager)(Object) this;
        if (self.level().isClientSide) return;
        if (!Config.OPTIMIZE_VILLAGERS.get()) return;

        if (self.isSleeping()) {
            coreTuner$resetRef(self);
            return;
        }

        if (Double.isNaN(coreTuner$refX)) coreTuner$resetRef(self);

        if (coreTuner$brainPhase < 0) {
            coreTuner$brainPhase = (int)(self.getId() % BRAIN_SKIP_RATIO);
        }

        coreTuner$checkTimer++;
        if (coreTuner$checkTimer >= CHECK_INTERVAL) {
            coreTuner$checkTimer = 0;
            coreTuner$cachedPanicking = coreTuner$isPanicking(self);

            if (self.isPassenger()) {
                // 1. PASAJERO (Vagonetas / Botes)
                coreTuner$isConfined = true;
                coreTuner$stillCheckCount = CONFIRM_CHECKS;
                coreTuner$resetRef(self);

            } else if (self.level() instanceof ServerLevel level && coreTuner$checkBlockConfinement(self, level)) {
                // 2. CELDA FÍSICA (4 paredes sólidas)
                coreTuner$isConfined = true;
                coreTuner$stillCheckCount = CONFIRM_CHECKS;
                coreTuner$resetRef(self);

            } else {
                // 3. FALLBACK DE MOVIMIENTO (Atrapado con vallas, trampillas o miel)
                double dSqr = self.distanceToSqr(coreTuner$refX, coreTuner$refY, coreTuner$refZ);
                if (dSqr < CONFINEMENT_DIST_SQR) {
                    if (coreTuner$stillCheckCount < CONFIRM_CHECKS) coreTuner$stillCheckCount++;
                    if (coreTuner$stillCheckCount >= CONFIRM_CHECKS) coreTuner$isConfined = true;
                } else {
                    coreTuner$isConfined      = false;
                    coreTuner$stillCheckCount = 0;
                    coreTuner$resetRef(self);
                }
            }

            if (coreTuner$isConfined && !coreTuner$cachedPanicking) {
                self.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                self.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
                if (!self.getNavigation().isDone()) self.getNavigation().stop();
            }
        }

        if (coreTuner$isConfined && !coreTuner$cachedPanicking) {
            coreTuner$restockTimer++;
            if (coreTuner$restockTimer >= RESTOCK_INTERVAL) {
                coreTuner$restockTimer = 0;
                self.restock();
            }
        } else {
            coreTuner$restockTimer = 0;
        }
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void onCustomServerAiStep(CallbackInfo ci) {
        org.mod.coretuner.profiler.ServerProfiler.countVillager(coreTuner$isConfined);
        if (!Config.OPTIMIZE_VILLAGERS.get()) return;
        Villager self = (Villager)(Object) this;
        if (!coreTuner$isConfined) return;
        if (coreTuner$cachedPanicking) return;
        if (coreTuner$brainPhase < 0) return;
        if (!(self.level() instanceof ServerLevel level)) return;
        if ((level.getGameTime() % BRAIN_SKIP_RATIO) != coreTuner$brainPhase) ci.cancel();
    }

    @Unique
    private boolean coreTuner$checkBlockConfinement(Villager villager, ServerLevel level) {
        BlockPos pos = villager.blockPosition();
        int blocked = 0;
        for (Direction dir : new Direction[]{ Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST }) {
            BlockPos neighbor = pos.relative(dir);
            VoxelShape shape = level.getBlockState(neighbor).getCollisionShape(level, neighbor);
            if (!shape.isEmpty()) blocked++;
        }
        return blocked >= 4;
    }

    @Unique
    private void coreTuner$resetRef(Villager villager) {
        coreTuner$refX = villager.getX();
        coreTuner$refY = villager.getY();
        coreTuner$refZ = villager.getZ();
    }

    @Unique
    private boolean coreTuner$isPanicking(Villager villager) {
        return villager.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE)
                || villager.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY_ENTITY)
                || villager.getBrain().hasMemoryValue(MemoryModuleType.AVOID_TARGET);
    }
}