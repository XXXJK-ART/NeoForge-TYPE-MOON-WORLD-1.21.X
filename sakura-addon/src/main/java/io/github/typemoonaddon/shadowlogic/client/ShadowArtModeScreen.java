package io.github.typemoonaddon.shadowlogic.client;

import io.github.typemoonaddon.data.ImaginarySpaceData.ShadowArtMode;
import io.github.typemoonaddon.shadowlogic.network.SetShadowArtModePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ShadowArtModeScreen extends Screen {
    private ShadowArtMode selected;
    public ShadowArtModeScreen() { super(Component.translatable("screen.typemoonaddon.shadow_art.title")); }
    @Override protected void init() {
        selected = minecraft.player.getData(io.github.typemoonaddon.registry.ModAttachments.IMAGINARY_SPACE.get()).shadowArtMode();
        int x = width / 2 - 85, y = height / 2 - 48;
        for (ShadowArtMode mode : ShadowArtMode.values()) {
            addRenderableWidget(Button.builder(Component.translatable("screen.typemoonaddon.shadow_art." + mode.serializedName()), button -> select(mode)).bounds(x, y, 170, 20).build()); y += 25;
        }
    }
    private void select(ShadowArtMode mode) { selected = mode; PacketDistributor.sendToServer(new SetShadowArtModePayload(mode.ordinal())); onClose(); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { renderBackground(graphics, mouseX, mouseY, partialTick); graphics.drawCenteredString(font, title, width / 2, height / 2 - 72, 0xF3DDE5); super.render(graphics, mouseX, mouseY, partialTick); }
    @Override public boolean isPauseScreen() { return false; }
}
