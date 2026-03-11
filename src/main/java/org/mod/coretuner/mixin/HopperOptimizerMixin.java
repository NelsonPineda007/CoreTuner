package org.mod.coretuner.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.mod.coretuner.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(HopperBlockEntity.class)
public class HopperOptimizerMixin {

    @Inject(method = "pushItemsTick", at = @At("HEAD"), cancellable = true)
    private static void onPushItemsTick(Level level, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, CallbackInfo ci) {
        if (level == null || level.isClientSide) return;

        // 1. Reporte para el Dashboard web
        boolean isSleeping = ((HopperAccessor) blockEntity).callIsOnCooldown();
        org.mod.coretuner.profiler.ServerProfiler.countHopper(isSleeping);

        // 2. Guard del config
        if (!Config.OPTIMIZE_HOPPERS.get()) return;

        // 3. CASO 1: Tolva completamente vacía y bloqueada por arriba
        if (blockEntity.isEmpty()) {
            BlockPos posAbove = pos.above();
            BlockState stateAbove = level.getBlockState(posAbove);

            if (!stateAbove.hasBlockEntity() && stateAbove.isCollisionShapeFullBlock(level, posAbove)) {
                blockEntity.setCooldown(Config.HOPPER_SLEEP_TICKS.get());
                ci.cancel();
            }
        }
    }

    @Inject(method = "tryMoveItems", at = @At("RETURN"))
    private static void onTryMoveItems(Level level, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleanSupplier, CallbackInfoReturnable<Boolean> cir) {
        if (level == null || level.isClientSide) return;
        if (!Config.OPTIMIZE_HOPPERS.get()) return;

        boolean isOnCooldown = ((HopperAccessor) blockEntity).callIsOnCooldown();
        if (isOnCooldown) return;

        // 4. CASO 2: Fallo de movimiento (Inventario lleno, tolva llena de basura, etc.)
        if (!cir.getReturnValue()) {
            int sleepTicks = Config.HOPPER_SLEEP_TICKS.get();
            if (sleepTicks > 8) {
                blockEntity.setCooldown(sleepTicks);
            }
        }
    }
}