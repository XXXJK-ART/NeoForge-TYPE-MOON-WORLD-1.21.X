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
      }, -39305));
      this.addRenderableWidget(new NeonButton(x + 136, y + 70, 74, 20, Component.translatable("gui.no"), b -> this.closeToParent(), -16711766));
   }

   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int x = (this.width - 250) / 2;
      int y = (this.height - 108) / 2;
      guiGraphics.fill(x, y, x + 250, y + 108, 1342177280);
      GuiUtils.renderTechFrame(guiGraphics, x, y, 250, 108, -16733526, -16711681);
      guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.structure.delete.title"), this.width / 2, y + 24, -1);
      guiGraphics.drawCenteredString(this.font, Component.literal(this.structureName), this.width / 2, y + 40, -4864308);
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
