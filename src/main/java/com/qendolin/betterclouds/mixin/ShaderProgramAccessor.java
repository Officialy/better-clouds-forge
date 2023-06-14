package com.qendolin.betterclouds.mixin;

import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderInstance.class)
public interface ShaderProgramAccessor {
    @Accessor("lastProgramId")
    static int getActiveProgramGlRef() {
        throw new AssertionError();
    }

    @Accessor("lastProgramId")
    static void setActiveProgramGlRef(int id) {
        throw new AssertionError();
    }
}
