package com.qendolin.betterclouds.mixin;

import com.qendolin.betterclouds.gui.ConfigScreen;
import dev.isxander.yacl3.gui.AbstractWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
    @Shadow
    @Final
    protected Minecraft client;

    @Inject(remap = false, method = "drawButtonRect", at = @At("HEAD"), cancellable = true)
    private void onDrawButtonRect(GuiGraphics context, int x1, int y1, int x2, int y2, boolean hovered, boolean enabled, CallbackInfo ci) {
        // This is so hacky, but I don't expect it to bread until YACL 3.0.0 is released
        if (client == null || client.level == null || !(client.screen instanceof ConfigScreen)) {
            return;
        }
        ci.cancel();

        if (x1 > x2) {
            int xx1 = x1;
            x1 = x2;
            x2 = xx1;
        }
        if (y1 > y2) {
            int yy1 = y1;
            y1 = y2;
            y2 = yy1;
        }

        int color = 0;
        if (!enabled) {
            color = 0xffa0a0a0;
        } else if (hovered) {
            color = -1;
        }

        context.fill(x1, y1, x2, y2, 0x6b000000);
        if (color != 0) {
            drawOutline(context, x1, y1, x2, y2, 1, color);
        }
    }

    private static void drawOutline(GuiGraphics context, int x1, int y1, int x2, int y2, int width, int color) {
        context.fill(x1, y1, x2, y1 + width, color);
        context.fill(x2, y1, x2 - width, y2, color);
        context.fill(x1, y2, x2, y2 - width, color);
        context.fill(x1, y1, x1 + width, y2, color);
    }
}
