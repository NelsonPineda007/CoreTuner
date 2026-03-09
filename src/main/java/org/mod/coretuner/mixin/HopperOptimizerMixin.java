package org.mod.coretuner.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(HopperBlockEntity.class)
public class HopperOptimizerMixin {

    // OPTIMIZACIÓN 1: Caché de Colisión
    @Inject(method = "pushItemsTick", at = @At("HEAD"), cancellable = true)
    private static void onPushItemsTick(Level level, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, CallbackInfo ci) {
        if (level == null || level.isClientSide) return;

        if (blockEntity.isEmpty()) {
            BlockPos posAbove = pos.above();
            BlockState stateAbove = level.getBlockState(posAbove);

            if (!stateAbove.hasBlockEntity() && stateAbove.isCollisionShapeFullBlock(level, posAbove)) {
                blockEntity.setCooldown(8);
                ci.cancel();
            }
        }
    }

    // OPTIMIZACIÓN 2: Algoritmo de "Dormir" (Sleep State al fallar)
    @Inject(method = "tryMoveItems", at = @At("RETURN"))
    private static void onTryMoveItems(Level level, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleanSupplier, CallbackInfoReturnable<Boolean> cir) {
        if (level == null || level.isClientSide) return;

        // AQUÍ ESTÁ LA MAGIA:
        // "Disfrazamos" temporalmente a la blockEntity como nuestro HopperAccessor
        // para poder usar el túnel que acabamos de crear hacia el método privado.
        boolean isOnCooldown = ((HopperAccessor) blockEntity).callIsOnCooldown();

        // Si fracasó en mover ítems Y no tiene un descanso asignado...
        if (!cir.getReturnValue() && !isOnCooldown) {
            // ...le forzamos un descanso de 8 ticks.
            blockEntity.setCooldown(8);
        }

    }
}