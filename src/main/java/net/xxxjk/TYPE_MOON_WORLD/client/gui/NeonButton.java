package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;

public class NeonButton extends Button {
   private final int hoverColor;

   public NeonButton(int x, int y, int width, int height, Component message, OnPress onPress) {
      this(x, y, width, height, message, onPress, -16711681);
   }

   public NeonButton(int x, int y, int width, int height, Component message, OnPress onPress, int color) {
      super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
      this.hoverColor = color;
   }

   public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int borderColor = this.isHoveredOrFocused() ? this.hoverColor : -16733526;
      int fillColor = this.isHoveredOrFocused() ? this.hoverColor & 16777215 | -2147483568 : Integer.MIN_VALUE;
      int textColor = this.isHoveredOrFocused() ? -1 : -5592406;
      guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, fillColor);
      guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);
      if (this.isHoveredOrFocused()) {
         int len = 4;
         guiGraphics.fill(this.getX(), this.getY(), this.getX() + len, this.getY() + 1, this.hoverColor);
         guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + len, this.hoverColor);
         guiGraphics.fill(this.getX() + this.width - len, this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, this.hoverColor);
         guiGraphics.fill(this.getX() + this.width - 1, this.getY() + this.height - len, this.getX() + this.width, this.getY() + this.height, this.hoverColor);
      }

      guiGraphics.drawCenteredString(
         Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor
      );
   }
}
