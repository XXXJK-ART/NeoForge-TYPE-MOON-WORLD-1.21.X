package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xxxjk.TYPE_MOON_WORLD.client.projection.StructuralAnalysisSelectionClient;
import org.jetbrains.annotations.NotNull;

public class StructuralSelectionNamingScreen extends Screen {
   private static final int BOX_W = 280;
   private static final int BOX_H = 130;
   private EditBox nameBox;
   private boolean submitted = false;

   public StructuralSelectionNamingScreen() {
      super(Component.translatable("gui.typemoonworld.structure.naming.title"));
   }

   protected void init() {
      int x = (this.width - 280) / 2;
      int y = (this.height - 130) / 2;
      this.nameBox = new EditBox(this.font, x + 20, y + 52, 240, 20, Component.translatable("gui.typemoonworld.structure.naming.input"));
      this.nameBox.setMaxLength(32);
      this.nameBox.setValue("Structure");
      this.nameBox.setTextColor(GuiUtils.ARCANE_TEXT);
      this.addRenderableWidget(this.nameBox);
      this.setInitialFocus(this.nameBox);
      this.addRenderableWidget(
         new NeonButton(x + 42, y + 90, 86, 20, Component.translatable("gui.typemoonworld.structure.naming.save"), b -> this.submitAndClose(), GuiUtils.ARCANE_VALID).setArcaneStyle(true)
      );
      this.addRenderableWidget(
         new NeonButton(x + 152, y + 90, 86, 20, Component.translatable("gui.typemoonworld.structure.naming.cancel"), b -> this.onClose(), GuiUtils.ARCANE_DANGER).setArcaneStyle(true)
      );
   }

   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int x = (this.width - 280) / 2;
      int y = (this.height - 130) / 2;
      GuiUtils.renderScreenBackdrop(guiGraphics, this.width, this.height);
      GuiUtils.renderArcaneWindow(guiGraphics, x, y, BOX_W, BOX_H, GuiUtils.ARCANE_CYAN);
      guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.structure.naming.title"), this.width / 2, y + 9, GuiUtils.ARCANE_TEXT);
      guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.structure.naming.subtitle"), this.width / 2, y + 36, GuiUtils.ARCANE_TEXT_MUTED);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
   }

   public boolean charTyped(char codePoint, int modifiers) {
      return this.nameBox != null && this.nameBox.charTyped(codePoint, modifiers) ? true : super.charTyped(codePoint, modifiers);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 257 || keyCode == 335) {
         this.submitAndClose();
         return true;
      } else if (keyCode == 256) {
         this.onClose();
         return true;
      } else {
         return this.nameBox != null && this.nameBox.keyPressed(keyCode, scanCode, modifiers) ? true : super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private void submitAndClose() {
      if (this.nameBox != null) {
         boolean sent = StructuralAnalysisSelectionClient.submitNamedSelection(this.nameBox.getValue());
         if (sent) {
            this.submitted = true;
            if (this.minecraft != null) {
               this.minecraft.setScreen(null);
            }
         }
      }
   }

   public void onClose() {
      if (!this.submitted) {
         StructuralAnalysisSelectionClient.backFromNamingScreen();
      } else {
         if (this.minecraft != null) {
            this.minecraft.setScreen(null);
         }
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
   }
}
