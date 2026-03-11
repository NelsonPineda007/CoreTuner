package org.mod.coretuner.mixin;

import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class CoreTunerMixinConnector implements IMixinConnector {
    @Override
    public void connect() {
        // vacío — el mods.toml [[mixins]] ya registra el config
    }
}