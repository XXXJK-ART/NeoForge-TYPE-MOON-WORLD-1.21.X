package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xxxjk.TYPE_MOON_WORLD.client.projection.StructuralAnalysisSelectionClient;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class StructuralSelectionConfirmScreen extends Screen {
    private static final int BOX_W = 240;
    private static final int BOX_H = 108;

    public StructuralSelectionConfirmScreen() {
        super(Component.translatable("gui.typemoonworld.structure.confirm.title"));
    }

    @Override
    protected void init() {
        int x = (this.width - BOX_W) / 2;
        int y = (this.height - BOX_H) / 2;

        this.addRenderableWidget(new NeonButton(x + 36, y + 70, 76, 20, Component.translatable("gui.yes"), b -> {
            StructuralAnalysisSelectionClient.onSaveDecision(true);
        }, 0xFF00FFAA));

        this.addRenderableWidget(new NeonButton(x + 128, y + 70, 76, 20, Component.translatable("gui.no"), b -> {
            StructuralAnalysisSelectionClient.onSaveDecision(false);
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }, 0xFFFF6677));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = (this.width - BOX_W) / 2;
        int y = (this.height - BOX_H) / 2;

        guiGraphics.fill(x, y, x + BOX_W, y + BOX_H, 0x50000000);
        GuiUtils.renderTechFrame(guiGraphics, x, y, BOX_W, BOX_H, 0xFF00AAAA, 0xFF00FFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.structure.confirm.title"), this.width / 2, y + 24, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.structure.confirm.subtitle"), this.width / 2, y + 42, 0xFFB5C6CC);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        StructuralAnalysisSelectionClient.backFromConfirmScreen();
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Keep confirmation UI without vanilla blur/menu background.
    }
}
