package com.qendolin.betterclouds.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qendolin.betterclouds.Main;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    @Inject(method = "initRenderer", at = @At("TAIL"))
    private static void afterInitRenderer(int debugVerbosity, boolean debugSync, CallbackInfo ci) {
        Main.initGlCompat();
    }
}
