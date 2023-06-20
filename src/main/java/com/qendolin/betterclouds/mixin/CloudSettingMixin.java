package com.qendolin.betterclouds.mixin;

import com.qendolin.betterclouds.Main;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Run before Iris (priority 1010)
@Mixin(value = Options.class, priority = 1000)
public abstract class CloudSettingMixin {

    @Shadow
    @Final
    private OptionInstance<Integer> renderDistance;

    @Shadow
    @Final
    private OptionInstance<CloudStatus> cloudStatus;

    @Inject(method = "getCloudsType", at = @At(value = "HEAD", target = "Lnet/minecraft/client/Options;getCloudsType()Lnet/minecraft/client/CloudStatus;"), cancellable = true)
    private void overrideCloudSetting(CallbackInfoReturnable<CloudStatus> cir) {
        if (!Main.getConfig().cloudOverride) return;
        if (renderDistance.get() < 4) {
            return;
        }
        cir.setReturnValue(cloudStatus.get());
    }
}