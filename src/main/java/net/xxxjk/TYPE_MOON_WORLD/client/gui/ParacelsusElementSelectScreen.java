package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.ParacelsusElementSelectionMessage;
import org.jetbrains.annotations.NotNull;

public class ParacelsusElementSelectScreen extends Screen {
   private int selectedIndex = 0;

   public ParacelsusElementSelectScreen() {
      super(Component.translatable("gui.typemoonworld.paracelsus_element.title"));
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.updateSelectionAt(mouseX, mouseY)) {
         PacketDistributor.sendToServer(new ParacelsusElementSelectionMessage(this.selectedIndex), new CustomPacketPayload[0]);
         this.onClose();
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY > 0.0) {
         this.selectedIndex = (this.selectedIndex + 3) % 4;
      } else if (scrollY < 0.0) {
         this.selectedIndex = (this.selectedIndex + 1) % 4;
      }
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.onClose();
         return true;
      }
      if (keyCode == 257 || keyCode == 335) {
         PacketDistributor.sendToServer(new ParacelsusElementSelectionMessage(this.selectedIndex), new CustomPacketPayload[0]);
         this.onClose();
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int itemWidth = 72;
      int itemHeight = 48;
      int gap = 8;
      int totalWidth = itemWidth * 4 + gap * 3;
      int startX = (this.width - totalWidth) / 2;
      int startY = this.height / 2 - itemHeight / 2;
      int bgX1 = startX - 12;
      int bgY1 = startY - 28;
      int bgX2 = startX + totalWidth + 12;
      int bgY2 = startY + itemHeight + 12;

      this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.fill(bgX1, bgY1, bgX2, bgY2, 0xB0000000);
      guiGraphics.renderOutline(bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, 0x88BFEFFF);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, bgY1 + 8, 0xFFEDE6FF);
      this.updateSelectionAt(mouseX, mouseY);

      for (int i = 0; i < 4; i++) {
         int x = startX + i * (itemWidth + gap);
         this.renderChoice(guiGraphics, i, x, startY, itemWidth, itemHeight);
      }
   }

   private void renderChoice(GuiGraphics guiGraphics, int index, int x, int y, int width, int height) {
      boolean selected = index == this.selectedIndex;
      int color = switch (index) {
         case 0 -> 0xAA5A2020;
         case 1 -> 0xAA20365A;
         case 2 -> 0xAA285A28;
         default -> 0xAAE6EAF5;
      };
      int fill = selected ? color : (color & 0x77FFFFFF);
      int border = selected ? 0xFFFFFFFF : 0x8890CFEA;
      int text = index == 3 ? 0xFF203040 : 0xFFFFFFFF;
      guiGraphics.fill(x, y, x + width, y + height, fill);
      guiGraphics.renderOutline(x, y, width, height, border);
      guiGraphics.drawCenteredString(this.font, this.label(index), x + width / 2, y + 10, text);
      guiGraphics.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.paracelsus_element.deploy"), x + width / 2, y + 29, index == 3 ? 0xFF345060 : 0xFFBFEFBF);
   }

   private boolean updateSelectionAt(double mouseX, double mouseY) {
      int itemWidth = 72;
      int itemHeight = 48;
      int gap = 8;
      int totalWidth = itemWidth * 4 + gap * 3;
      int startX = (this.width - totalWidth) / 2;
      int startY = this.height / 2 - itemHeight / 2;
      for (int i = 0; i < 4; i++) {
         int x = startX + i * (itemWidth + gap);
         if (mouseX >= x && mouseX < x + itemWidth && mouseY >= startY && mouseY < startY + itemHeight) {
            this.selectedIndex = i;
            return true;
         }
      }
      return false;
   }

   private Component label(int index) {
      return switch (index) {
         case 0 -> Component.translatable("hud.typemoonworld.servant_card.paracelsus_element_fire");
         case 1 -> Component.translatable("hud.typemoonworld.servant_card.paracelsus_element_water");
         case 2 -> Component.translatable("hud.typemoonworld.servant_card.paracelsus_element_earth");
         default -> Component.translatable("hud.typemoonworld.servant_card.paracelsus_element_wind");
      };
   }
}
