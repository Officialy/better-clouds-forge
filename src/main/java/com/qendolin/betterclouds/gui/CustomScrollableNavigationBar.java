package com.qendolin.betterclouds.gui;

import com.qendolin.betterclouds.mixin.TabNavigationWidgetAccessor;
import dev.isxander.yacl3.gui.tab.ScrollableNavigationBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;

public class CustomScrollableNavigationBar extends ScrollableNavigationBar {

    private final int width;

    public CustomScrollableNavigationBar(int width, TabManager tabManager, Iterable<? extends Tab> tabs) {
        super(width, tabManager, tabs);
        this.width = width;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || !(client.screen instanceof ConfigScreen)) {
            super.render(context, mouseX, mouseY, delta);
        } else {
            context.fill(0, 0, this.width, 22, 0x6b000000);
            context.fill(0, 22, this.width, 23, 0xff000000);
            for (TabButton tabButtonWidget : ((TabNavigationWidgetAccessor) this).getTabButtons()) {
                tabButtonWidget.render(context, mouseX, mouseY, delta);
            }
        }
    }
}
