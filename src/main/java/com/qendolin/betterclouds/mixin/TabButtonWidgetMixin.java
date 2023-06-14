package com.qendolin.betterclouds.mixin;

import com.qendolin.betterclouds.gui.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TabButton.class)
public abstract class TabButtonWidgetMixin extends AbstractWidget {

    public TabButtonWidgetMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Shadow public abstract void renderString(GuiGraphics context, Font textRenderer, int color);

    @Shadow public abstract boolean isSelected();

    @Shadow protected abstract void renderFocusUnderline(GuiGraphics context, Font textRenderer, int color);

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void onRenderButton(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        // I'm gonna go to hell for this
        if (client == null || client.level == null || !(client.screen instanceof ConfigScreen)) {
            return;
        }
        ci.cancel();
        Font textRenderer = Minecraft.getInstance().font;
        int i = active ? -1 : -6250336;
        this.renderString(context, textRenderer, i);
        if (this.isSelected()) {
            this.renderFocusUnderline(context, textRenderer, i);
        }
    }
}
