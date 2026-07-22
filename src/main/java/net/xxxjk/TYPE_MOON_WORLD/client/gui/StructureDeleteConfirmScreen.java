package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.DeleteProjectionStructureMessage;
import org.jetbrains.annotations.NotNull;

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

   protected void init() {
      int x = (this.width - 250) / 2;
      int y = (this.height - 108) / 2;
      this.addRenderableWidget(new NeonButton(x + 40, y + 70, 74, 20, Component.translatable("gui.yes"), b -> {
         PacketDistributor.sendToServer(new DeleteProjectionStructureMessage(this.structureId), new CustomPacketPayload[0]);
         this.closeToParent();
      }, GuiUtils.ARCANE_DANGER).setArcaneStyle(true));
      this.addRenderableWidget(new NeonButton(x + 136, y + 70, 74, 20, Component.translatable("gui.no"), b -> this.closeToParent(), GuiUtils.ARCANE_VALID).setArcaneStyle(true));
   }

   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int x = (this.width - 250) / 2;
      int y = (this.height - 108) / 2;
      GuiUtils.renderScreenBackdrop(guiGraphics, this.width, this.height);
      GuiUtils.renderArcaneWindow(guiGraphics, x, y, BOX_W, BOX_H, GuiUtils.ARCANE_DANGER);
      guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.structure.delete.title"), this.width / 2, y + 9, GuiUtils.ARCANE_TEXT);
      String displayName = this.font.plainSubstrByWidth(this.structureName, BOX_W - 32);
      guiGraphics.drawCenteredString(this.font, Component.literal(displayName), this.width / 2, y + 40, GuiUtils.ARCANE_TEXT_MUTED);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.closeToParent();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public void onClose() {
      this.closeToParent();
   }

   private void closeToParent() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
         this.parent.refreshDataAfterStructureChange();
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
   }
}
