package com.qendolin.betterclouds.gui;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.DescriptionWithName;
import dev.isxander.yacl3.gui.OptionListWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.LabelController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class CustomOptionListWidget extends OptionListWidget {

    public CustomOptionListWidget(YACLScreen screen, ConfigCategory category, Minecraft client, int x, int y, int width, int height, Consumer<DescriptionWithName> hoverEvent) {
        super(screen, category, client, x, y, width, height, hoverEvent);
    }

    @Override
    public void refreshOptions() {
        super.refreshOptions();
        addEntry(new PaddingEntry());
        for (Entry child : children()) {
            if (child instanceof OptionEntry optionEntry && optionEntry.option.controller() instanceof LabelController) {
                addEntryBelow(optionEntry, new ProxyEntry<OptionEntry>(optionEntry)
                    .onBeforeRender((delegate, context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta) -> {
                        if (minecraft.level == null) return;
                        Dimension<Integer> dim = delegate.widget.getDimension();
                        context.fill(dim.x(), dim.y(), dim.xLimit(), dim.yLimit(), 0x6b000000);
                    }));
                removeEntry(optionEntry);
            } else if (child instanceof GroupSeparatorEntry groupSeparatorEntry) {
                addEntryBelow(groupSeparatorEntry, new ProxyEntry<GroupSeparatorEntry>(groupSeparatorEntry)
                    .onBeforeRender((delegate, context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta) -> {
                        if (minecraft.level == null) return;
                        context.fill(x, y, x + entryWidth, y + 19, 0x6b000000);
                    }));
                removeEntry(groupSeparatorEntry);
            }
        }

        recacheViewableChildren();
        setScrollAmount(0);
        resetSmoothScrolling();
    }

    @Override
    protected void renderBackground(GuiGraphics context) {
        if (minecraft == null || minecraft.level == null) {
            super.renderBackground(context);
            setRenderBackground(true);
        } else {
            setRenderBackground(false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (Entry child : children()) {
            if (child.mouseScrolled(mouseX, mouseY, amount)) {
                return true;
            }
        }

        this.setScrollAmount(this.getScrollAmount() - amount * 20);
        return true;
    }

    // It is super annoying that Entry is not declared as a static class
    public class ProxyEntry<T extends Entry> extends Entry {
        private final T delegate;

        public BeforeRenderCallback<T> beforeRender;
        public AfterRenderCallback<T> afterRender;

        public ProxyEntry(T delegate) {
            super();
            this.delegate = delegate;
        }

        public ProxyEntry<T> onBeforeRender(BeforeRenderCallback<T> callback) {
            this.beforeRender = callback;
            return this;
        }

        public ProxyEntry<T> onAfterRender(AfterRenderCallback<T> callback) {
            this.afterRender = callback;
            return this;
        }

        @Override
        public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (beforeRender != null)
                beforeRender.onBeforeRender(delegate, context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
            delegate.render(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
            if (afterRender != null)
                afterRender.onAfterRender(delegate, context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return delegate.narratables();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return delegate.children();
        }

        @Override
        public Optional<GuiEventListener> getChildAt(double p_94730_, double p_94731_) {
            return delegate.getChildAt(p_94730_, p_94731_);
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            delegate.mouseMoved(mouseX, mouseY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return delegate.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            return delegate.mouseScrolled(mouseX, mouseY, amount);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return delegate.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
            return delegate.keyReleased(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return delegate.charTyped(chr, modifiers);
        }

        @Nullable
        @Override
        public ComponentPath getCurrentFocusPath() {
            return delegate.getCurrentFocusPath();
        }

        @Override
        public ScreenRectangle getRectangle() {
            return delegate.getRectangle();
        }
        @Override
        public void magicalSpecialHackyFocus(@Nullable GuiEventListener p_94726_) {
            delegate.magicalSpecialHackyFocus(p_94726_);
        }

        @Override
        public boolean isViewable() {
            return delegate.isViewable();
        }

        @Override
        public boolean isHovered() {
            return Objects.equals(getHovered(), this);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return delegate.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            return delegate.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean isDragging() {
            return delegate.isDragging();
        }

        @Override
        public void setDragging(boolean dragging) {
            delegate.setDragging(dragging);
        }

        @Override
        @Nullable
        public GuiEventListener getFocused() {
            return delegate.getFocused();
        }

        @Nullable
        @Override
        public ComponentPath focusPathAtIndex(FocusNavigationEvent p_265435_, int p_265432_) {
            return delegate.focusPathAtIndex(p_265435_, p_265432_);
        }

        @Nullable
        @Override
        public ComponentPath nextFocusPath(FocusNavigationEvent p_265672_) {
            return delegate.nextFocusPath(p_265672_);
        }

        @Override
        public boolean isFocused() {
            return delegate.isFocused();
        }

        @Override
        public void setFocused(@Nullable GuiEventListener focused) {
            delegate.setFocused(focused);
        }

        @Override
        public void setFocused(boolean focused) {
            delegate.setFocused(focused);
        }

        @Override
        public void renderBack(GuiGraphics p_282673_, int p_275556_, int p_275667_, int p_275713_, int p_275408_, int p_275330_, int p_275603_, int p_275450_, boolean p_275434_, float p_275384_) {
            delegate.renderBack(p_282673_, p_275556_, p_275667_, p_275713_, p_275408_, p_275330_, p_275603_, p_275450_, p_275434_, p_275384_);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return delegate.isMouseOver(mouseX, mouseY);
        }

        @Override
        public int getItemHeight() {
            return delegate.getItemHeight();
        }

        @Override
        public int getTabOrderGroup() {
            return delegate.getTabOrderGroup();
        }
    }

    @FunctionalInterface
    public interface BeforeRenderCallback<T extends Entry> {
        void onBeforeRender(T delegate, GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta);
    }

    @FunctionalInterface
    public interface AfterRenderCallback<T extends Entry> {
        void onAfterRender(T delegate, GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta);
    }

    private class PaddingEntry extends Entry {
        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        }

        @Override
        public int getItemHeight() {
            return 5;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }
}
