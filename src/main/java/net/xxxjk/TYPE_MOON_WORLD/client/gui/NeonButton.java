package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;

public class NeonButton extends Button {
   private final int hoverColor;
   private boolean arcaneStyle;
   private boolean compactStyle;
   private boolean selected;
   private int selectedColor = GuiUtils.ARCANE_GOLD;

   public NeonButton(int x, int y, int width, int height, Component message, OnPress onPress) {
      this(x, y, width, height, message, onPress, -16711681);
   }

   public NeonButton(int x, int y, int width, int height, Component message, OnPress onPress, int color) {
      super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
      this.hoverColor = color;
   }

   public NeonButton setArcaneStyle(boolean arcaneStyle) {
      this.arcaneStyle = arcaneStyle;
      return this;
   }

   public NeonButton setSelected(boolean selected) {
      this.selected = selected;
      return this;
   }

   public NeonButton setCompactStyle(boolean compactStyle) {
      this.compactStyle = compactStyle;
      return this;
   }

   public NeonButton setSelectedColor(int selectedColor) {
      this.selectedColor = selectedColor;
      return this;
   }

   public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      if (this.arcaneStyle) {
         this.renderArcaneWidget(guiGraphics);
         return;
      }

      int borderColor = this.isHoveredOrFocused() ? this.hoverColor : -16733526;
      int fillColor = this.isHoveredOrFocused() ? this.hoverColor & 16777215 | -2147483568 : Integer.MIN_VALUE;
      int textColor = this.isHoveredOrFocused() ? GuiUtils.ARCANE_TEXT : GuiUtils.ARCANE_TEXT_MUTED;
      guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, fillColor);
      guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);
      if (this.isHoveredOrFocused()) {
         int len = 4;
         guiGraphics.fill(this.getX(), this.getY(), this.getX() + len, this.getY() + 1, this.hoverColor);
         guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + len, this.hoverColor);
         guiGraphics.fill(this.getX() + this.width - len, this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, this.hoverColor);
         guiGraphics.fill(this.getX() + this.width - 1, this.getY() + this.height - len, this.getX() + this.width, this.getY() + this.height, this.hoverColor);
      }

      this.drawCenteredFittedString(guiGraphics, textColor);
   }

   private void drawCenteredFittedString(GuiGraphics guiGraphics, int textColor) {
      var font = Minecraft.getInstance().font;
      int maxWidth = Math.max(12, this.width - 8);
      int textWidth = font.width(this.getMessage());
      float scale = textWidth <= maxWidth ? 1.0F : Math.max(0.62F, (float)maxWidth / textWidth);
      if (scale >= 0.999F) {
         guiGraphics.drawCenteredString(font, this.getMessage(), this.getX() + this.width / 2,
            this.getY() + (this.height - 8) / 2, textColor);
         return;
      }
      guiGraphics.pose().pushPose();
      guiGraphics.pose().translate(this.getX() + this.width / 2.0F, this.getY() + this.height / 2.0F, 0.0F);
      guiGraphics.pose().scale(scale, scale, 1.0F);
      guiGraphics.drawCenteredString(font, this.getMessage(), 0, -4, textColor);
      guiGraphics.pose().popPose();
   }

   private void renderArcaneWidget(GuiGraphics guiGraphics) {
      boolean hovered = this.isHoveredOrFocused();
      int fillColor;
      int borderColor;
      int textColor;
      if (!this.active) {
         fillColor = 0xA00F1419;
         borderColor = GuiUtils.ARCANE_BORDER;
         textColor = 0xFF8FA0AB;
      } else if (this.selected) {
         fillColor = 0xD0202B34;
         borderColor = this.selectedColor;
         textColor = GuiUtils.ARCANE_TEXT;
      } else if (hovered) {
         fillColor = 0xC91F2931;
         borderColor = this.hoverColor;
         textColor = GuiUtils.ARCANE_TEXT;
      } else {
         fillColor = GuiUtils.ARCANE_PANEL;
         borderColor = GuiUtils.ARCANE_BORDER;
         textColor = GuiUtils.ARCANE_TEXT_MUTED;
      }

      guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, fillColor);
      guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);
      if (this.selected) {
         int lineHeight = this.compactStyle ? 1 : 2;
         guiGraphics.fill(this.getX() + 2, this.getY() + this.height - lineHeight, this.getX() + this.width - 2, this.getY() + this.height, this.selectedColor);
      } else if (hovered && this.active) {
         int railWidth = this.compactStyle ? 2 : 3;
         guiGraphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + railWidth, this.getY() + this.height - 1, this.hoverColor);
      }

      guiGraphics.drawCenteredString(
         Minecraft.getInstance().font,
         this.fitMessage(Minecraft.getInstance().font, this.getMessage(), Math.max(12, this.width - 10)),
         this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor
      );
   }

   private Component fitMessage(net.minecraft.client.gui.Font font, Component message, int maxWidth) {
      if (font.width(message) <= maxWidth) return message;
      return Component.literal(font.plainSubstrByWidth(message.getString(), maxWidth - font.width("...")) + "...");
   }
}
