package com.qendolin.betterclouds.gui;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ActionController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class CustomActionController extends ActionController {
    public CustomActionController(ButtonOption option) {
        super(option, Component.empty());
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new CustomActionControllerElement(this, screen, widgetDimension);
    }

    public static class CustomActionControllerElement extends ActionControllerElement {

        public CustomActionControllerElement(ActionController control, YACLScreen screen, Dimension<Integer> dim) {
            super(control, screen, dim);
        }

        @Override
        public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
            hovered = isMouseOver(mouseX, mouseY);

            Component name = control.option().changed() ? modifiedOptionName : control.option().name();

            drawButtonRect(context, getDimension().x(), getDimension().y(), getDimension().xLimit(), getDimension().yLimit(), isHovered(), isAvailable());
            context.pose().pushPose();
            context.pose().translate(getDimension().x() + getDimension().width() / 2f - textRenderer.width(name) / 2f, getTextY(), 0);
            context.drawString(textRenderer, name, 0, 0, getValueColor(),true);
            context.pose().popPose();

            if (isHovered()) {
                drawHoveredControl(context, mouseX, mouseY, delta);
            }
        }

        @Override
        protected void drawValueText(GuiGraphics context, int mouseX, int mouseY, float delta) {

        }
    }
}
