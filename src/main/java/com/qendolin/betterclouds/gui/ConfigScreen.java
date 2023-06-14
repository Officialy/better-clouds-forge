package com.qendolin.betterclouds.gui;

import com.qendolin.betterclouds.ConfigGUI;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.PlaceholderCategory;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.utils.OptionUtils;
import dev.isxander.yacl3.gui.TooltipButtonWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigScreen extends YACLScreen {

    public ConfigScreen(YetAnotherConfigLib config, Screen parent) {
        super(config, parent);
    }

    @Override
    protected void init() {
        assert minecraft != null;
        tabNavigationBar = new CustomScrollableNavigationBar(this.width, tabManager, config.categories()
            .stream()
            .map(category -> {
                if (category instanceof PlaceholderCategory placeholder)
                    return new PlaceholderTab(placeholder);
                return new CustomCategoryTab(minecraft, this, () -> tabArea, category);
            }).toList());
        tabNavigationBar.selectTab(0, false);
        tabNavigationBar.arrangeElements();
        ScreenRectangle navBarArea = tabNavigationBar.getRectangle();
        tabArea = new ScreenRectangle(0, navBarArea.height() - 1, this.width, this.height - navBarArea.height() + 1);
        tabManager.setTabArea(tabArea);
        addRenderableWidget(tabNavigationBar);

        config.initConsumer().accept(this);
    }

    public boolean pendingChanges() {
        AtomicBoolean pendingChanges = new AtomicBoolean(false);
        OptionUtils.consumeOptions(config, (option) -> {
            if (option.changed()) {
                pendingChanges.set(true);
                return true;
            }
            return false;
        });

        return pendingChanges.get();
    }

    @Override
    public void cancelOrReset() {
        super.cancelOrReset();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics context) {
        if (minecraft == null || minecraft.level == null) {
            super.renderBackground(context);
        } else {
            context.fill(0, 0, width / 3, height, 0x6b000000);
        }
    }

    public void renderDirtBackground(GuiGraphics context) {
        if (minecraft == null || minecraft.level == null) {
            super.renderDirtBackground(context);
        } else {
            context.fill(width / 3 * 2 + 1, tabArea.top(), width, tabArea.bottom(), 0x6b000000);
        }
    }

    @Override
    protected void finishOrSave() {
        onClose();
    }

    @Override
    public void onClose() {
        config.saveFunction().run();
        super.onClose();
    }

    public static class HiddenScreen extends Screen {
        public HiddenScreen(Component title, Button showButton) {
            super(title);
            addRenderableWidget(showButton);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }

        @Override
        public void renderBackground(GuiGraphics context) {
            if (minecraft == null || minecraft.level == null) {
                super.renderBackground(context);
            } else {
                context.fill(0, 0, width / 3, height, 0x6B000000);
            }
        }
    }
}
