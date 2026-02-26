package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.DeleteProjectionStructureMessage;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class StructureDeleteConfirmScreen extends Screen {
    private static final int BOX_W = 250;
    private static final int BOX_H = 108;

    private final ProjectionPresetScreen parent;
    private final String structureId;
    private final String structureName;

    public StructureDeleteConfirmScreen(ProjectionPresetScreen parent, String structureId, String structureName) {
        super(Component.translatable("gui.typemoonworld.structure.delete.title"));
        this.parent = parent;
        this.structureId = structureId;
        this.structureName = structureName;
    }

    @Override
    protected void init() {
        int x = (this.width - BOX_W) / 2;
        int y = (this.height - BOX_H) / 2;

        this.addRenderableWidget(new NeonButton(x + 40, y + 70, 74, 20, Component.translatable("gui.yes"), b -> {
            PacketDistributor.sendToServer(new DeleteProjectionStructureMessage(structureId));
            closeToParent();
        }, 0xFFFF6677));

        this.addRenderableWidget(new NeonButton(x + 136, y + 70, 74, 20, Component.translatable("gui.no"), b -> {
            closeToParent();
        }, 0xFF00FFAA));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = (this.width - BOX_W) / 2;
        int y = (this.height - BOX_H) / 2;

        guiGraphics.fill(x, y, x + BOX_W, y + BOX_H, 0x50000000);
        GuiUtils.renderTechFrame(guiGraphics, x, y, BOX_W, BOX_H, 0xFF00AAAA, 0xFF00FFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.structure.delete.title"), this.width / 2, y + 24, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal(structureName), this.width / 2, y + 40, 0xFFB5C6CC);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeToParent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    private void closeToParent() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
            parent.refreshDataAfterStructureChange();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Keep delete confirmation UI without vanilla blur/menu background.
    }
}
