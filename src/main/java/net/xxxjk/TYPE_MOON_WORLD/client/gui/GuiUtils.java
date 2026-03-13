package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public class GuiUtils {
   public static void renderTechFrame(GuiGraphics guiGraphics, int x, int y, int w, int h, int color, int cornerColor) {
      guiGraphics.fill(x, y, x + w, y + h, 1610612736);
      guiGraphics.renderOutline(x, y, w, h, color);
      int len = 8;
      int thick = 2;
      guiGraphics.fill(x - 1, y - 1, x + len, y + thick - 1, cornerColor);
      guiGraphics.fill(x - 1, y - 1, x + thick - 1, y + len, cornerColor);
      guiGraphics.fill(x + len + 1, y - 1, x + len + 3, y + thick - 1, cornerColor);
      guiGraphics.fill(x + w - len, y - 1, x + w + 1, y + thick - 1, cornerColor);
      guiGraphics.fill(x + w - thick + 1, y - 1, x + w + 1, y + len, cornerColor);
      guiGraphics.fill(x + w - len - 3, y - 1, x + w - len - 1, y + thick - 1, cornerColor);
      guiGraphics.fill(x - 1, y + h - thick + 1, x + len, y + h + 1, cornerColor);
      guiGraphics.fill(x - 1, y + h - len, x + thick - 1, y + h + 1, cornerColor);
      guiGraphics.fill(x + len + 1, y + h - thick + 1, x + len + 3, y + h + 1, cornerColor);
      guiGraphics.fill(x + w - len, y + h - thick + 1, x + w + 1, y + h + 1, cornerColor);
      guiGraphics.fill(x + w - thick + 1, y + h - len, x + w + 1, y + h + 1, cornerColor);
      guiGraphics.fill(x + w - len - 3, y + h - thick + 1, x + w - len - 1, y + h + 1, cornerColor);
   }

   public static void renderBackground(GuiGraphics guiGraphics, int x, int y, int w, int h) {
      guiGraphics.fillGradient(x, y, x + w, y + h, -267382752, -268106480);
      int borderColor = -16711681;
      guiGraphics.renderOutline(x, y, w, h, borderColor);
      guiGraphics.fill(x + 5, y + 25, x + w - 5, y + 26, -2147418113);
   }
}
