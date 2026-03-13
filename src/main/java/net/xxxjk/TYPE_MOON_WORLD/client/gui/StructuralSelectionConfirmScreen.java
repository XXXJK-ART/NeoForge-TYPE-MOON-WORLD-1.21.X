package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xxxjk.TYPE_MOON_WORLD.client.projection.StructuralAnalysisSelectionClient;
import org.jetbrains.annotations.NotNull;

public class StructuralSelectionConfirmScreen extends Screen {
   private static final int BOX_W = 240;
   private static final int BOX_H = 108;

   public StructuralSelectionConfirmScreen() {
      super(Component.translatable("gui.typemoonworld.structure.confirm.title"));
   }

   protected void init() {
      int x = (this.width - 240) / 2;
      int y = (this.height - 108) / 2;
      this.addRenderableWidget(
         new NeonButton(x + 36, y + 70, 76, 20, Component.translatable("gui.yes"), b -> StructuralAnalysisSelectionClient.onSaveDecision(true), -16711766)
      );
      this.addRenderableWidget(new NeonButton(x + 128, y + 70, 76, 20, Component.translatable("gui.no"), b -> {
         StructuralAnalysisSelectionClient.onSaveDecision(false);
         if (this.minecraft != null) {
            this.minecraft.setScreen(null);
         }
      }, -39305));
   }

   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int x = (this.width - 240) / 2;
      int y = (this.height - 108) / 2;
      guiGraphics.fill(x, y, x + 240, y + 108, 1342177280);
      GuiUtils.renderTechFrame(guiGraphics, x, y, 240, 108, -16733526, -16711681);
      guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.structure.confirm.title"), this.width / 2, y + 24, -1);
      guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.structure.confirm.subtitle"), this.width / 2, y + 42, -4864308);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public void onClose() {
      StructuralAnalysisSelectionClient.backFromConfirmScreen();
      if (this.minecraft != null) {
         this.minecraft.setScreen(null);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
   }
}
