package com.example.typemoonaddon.client.gui;

import com.example.typemoonaddon.imaginary_space.ImaginarySpaceData;
import com.example.typemoonaddon.network.ImaginaryDiveDepthPayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.GuiUtils;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.NeonButton;

/** Precise 0-100 depth editor styled after the main mod's time-alter panel. */
public final class ImaginaryDiveScreen extends Screen {
    private static final int MAX_INPUT_LENGTH = 16;
    private final double initialDepth;
    private EditBox depth;
    private boolean invalid;

    public ImaginaryDiveScreen(double initialDepth) {
        super(Component.translatable("gui.typemoonworld.imaginary_dive.title"));
        this.initialDepth = sanitize(initialDepth);
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 52;
        this.depth = new EditBox(this.font, x, y, 200, 20,
                Component.translatable("gui.typemoonworld.imaginary_dive.depth"));
        this.depth.setMaxLength(MAX_INPUT_LENGTH);
        this.depth.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,3}(\\.[0-9]{0,6})?"));
        this.depth.setValue(format(this.initialDepth));
        this.addRenderableWidget(this.depth);
        this.addRenderableWidget(new NeonButton(
                x, y + 28, 96, 20,
                Component.translatable("gui.typemoonworld.imaginary_dive.confirm"),
                button -> this.save(), GuiUtils.ARCANE_VALID).setArcaneStyle(true));
        this.addRenderableWidget(new NeonButton(
                x + 104, y + 28, 96, 20,
                Component.translatable("gui.typemoonworld.imaginary_dive.reset"),
                button -> this.resetValue(), GuiUtils.ARCANE_GOLD).setArcaneStyle(true));
        this.addRenderableWidget(new NeonButton(
                x, y + 56, 200, 20,
                Component.translatable("gui.typemoonworld.imaginary_dive.cancel"),
                button -> this.onClose(), GuiUtils.ARCANE_DANGER).setArcaneStyle(true));
        this.setInitialFocus(this.depth);
    }

    private void resetValue() {
        this.invalid = false;
        this.depth.setValue(format(ImaginarySpaceData.INITIAL_DEPTH));
        this.depth.setCursorPosition(this.depth.getValue().length());
    }

    private void save() {
        try {
            double value = Double.parseDouble(this.depth.getValue());
            if (!Double.isFinite(value) || value < 0.0D || value > 100.0D) {
                this.invalid = true;
                return;
            }
            PacketDistributor.sendToServer(new ImaginaryDiveDepthPayload(value));
            this.onClose();
        } catch (NumberFormatException ignored) {
            this.invalid = true;
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui, mouseX, mouseY, partialTick);
        GuiUtils.renderScreenBackdrop(gui, this.width, this.height);
        int panelX = this.width / 2 - 120;
        int panelY = this.height / 2 - 96;
        GuiUtils.renderArcaneWindow(gui, panelX, panelY, 240, 194, GuiUtils.ARCANE_CYAN);
        gui.drawCenteredString(this.font, this.title, this.width / 2, panelY + 9, GuiUtils.ARCANE_TEXT);
        gui.drawString(this.font,
                Component.translatable("gui.typemoonworld.imaginary_dive.depth"),
                this.width / 2 - 100, this.height / 2 - 64, GuiUtils.ARCANE_TEXT_MUTED);
        gui.drawString(this.font,
                Component.translatable("gui.typemoonworld.imaginary_dive.range"),
                this.width / 2 - 100, panelY + 174, GuiUtils.ARCANE_TEXT_MUTED);
        if (this.invalid) {
            gui.drawCenteredString(this.font,
                    Component.translatable("gui.typemoonworld.imaginary_dive.invalid"),
                    this.width / 2, panelY + 154, GuiUtils.ARCANE_DANGER);
        }
        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(100.0D, value)) : ImaginarySpaceData.INITIAL_DEPTH;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", sanitize(value));
    }
}
