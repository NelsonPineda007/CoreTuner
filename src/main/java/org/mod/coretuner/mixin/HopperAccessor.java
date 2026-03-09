package org.mod.coretuner.mixin;

import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// Le decimos al Mixin que se conecte a la Tolva
@Mixin(HopperBlockEntity.class)
public interface HopperAccessor {

    // Esta anotación mágica crea un túnel directo al método privado de Minecraft
    @Invoker("isOnCooldown")
    boolean callIsOnCooldown();
}