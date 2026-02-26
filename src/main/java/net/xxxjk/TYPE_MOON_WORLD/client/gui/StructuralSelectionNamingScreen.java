package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xxxjk.TYPE_MOON_WORLD.client.projection.StructuralAnalysisSelectionClient;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class StructuralSelectionNamingScreen extends Screen {
    private static final int BOX_W = 280;
    private static final int BOX_H = 130;
    private EditBox nameBox;
    private boolean submitted = false;

    public StructuralSelectionNamingScreen() {
        super(Component.translatable("gui.typemoonworld.structure.naming.title"));
    }

    @Override
    protected void init() {
        int x = (this.width - BOX_W) / 2;
        int y = (this.height - BOX_H) / 2;

        this.nameBox = new EditBox(this.font, x + 20, y + 52, BOX_W - 40, 20, Component.translatable("gui.typemoonworld.structure.naming.input"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setValue("Structure");
        this.addRenderableWidget(this.nameBox);
        this.setInitialFocus(this.nameBox);

        this.addRenderableWidget(new NeonButton(x + 42, y + 90, 86, 20, Component.translatable("gui.typemoonworld.structure.naming.save"), b -> {
            submitAndClose();
        }, 0xFF00FFAA));

        this.addRenderableWidget(new NeonButton(x + 152, y + 90, 86, 20, Component.translatable("gui.typemoonworld.structure.naming.cancel"), b -> {
            this.onClose();
        }, 0xFFFF6677));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = (this.width - BOX_W) / 2;
        int y = (this.height - BOX_H) / 2;

        guiGraphics.fill(x, y, x + BOX_W, y + BOX_H, 0x50000000);
        GuiUtils.renderTechFrame(guiGraphics, x, y, BOX_W, BOX_H, 0xFF00AAAA, 0xFF00FFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.structure.naming.title"), this.width / 2, y + 22, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.structure.naming.subtitle"), this.width / 2, y + 36, 0xFFB5C6CC);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.nameBox != null && this.nameBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submitAndClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (this.nameBox != null && this.nameBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void submitAndClose() {
        if (this.nameBox == null) {
            return;
        }
        boolean sent = StructuralAnalysisSelectionClient.submitNamedSelection(this.nameBox.getValue());
        if (sent) {
            submitted = true;
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }
    }

    @Override
    public void onClose() {
        if (!submitted) {
            StructuralAnalysisSelectionClient.backFromNamingScreen();
            return;
        }
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
        // Keep naming UI without vanilla blur/menu background.
    }
}
