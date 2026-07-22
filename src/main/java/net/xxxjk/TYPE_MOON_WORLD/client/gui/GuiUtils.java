package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public class GuiUtils {
   public static final int ARCANE_OVERLAY = 0xA8080B0F;
   public static final int ARCANE_BACKGROUND = 0xF010151B;
   public static final int ARCANE_HEADER = 0xF5161C23;
   public static final int ARCANE_PANEL = 0xE8171E26;
   public static final int ARCANE_PANEL_ALT = 0xD91D2630;
   public static final int ARCANE_BORDER = 0xFF34404C;
   public static final int ARCANE_CYAN = 0xFF35C6D0;
   public static final int ARCANE_GOLD = 0xFFD6AE5D;
   public static final int ARCANE_CREST = 0xFFC85E73;
   public static final int ARCANE_VALID = 0xFF53C58B;
   public static final int ARCANE_DANGER = 0xFFE05B67;
   public static final int ARCANE_TEXT = 0xFFF2F5F7;
   public static final int ARCANE_TEXT_MUTED = 0xFF99A6B2;

   public static void renderScreenBackdrop(GuiGraphics guiGraphics, int width, int height) {
      guiGraphics.fill(0, 0, width, height, ARCANE_OVERLAY);
      guiGraphics.fill(0, 0, width, Math.max(1, height / 7), 0x40151D24);
      guiGraphics.fill(0, Math.max(0, height - height / 8), width, height, 0x50070A0D);
      int centerX = width / 2;
      guiGraphics.fill(centerX - 1, 0, centerX, height, 0x1835C6D0);
      guiGraphics.fill(0, height / 2, width, height / 2 + 1, 0x1235C6D0);
   }

   public static void renderArcaneWindow(GuiGraphics guiGraphics, int x, int y, int w, int h, int accentColor) {
      guiGraphics.fill(x, y, x + w, y + h, ARCANE_BACKGROUND);
      guiGraphics.fill(x + 1, y + 1, x + w - 1, y + 27, ARCANE_HEADER);
      guiGraphics.renderOutline(x, y, w, h, ARCANE_BORDER);
      guiGraphics.fill(x + 8, y + 27, x + w - 8, y + 28, 0x5534404C);
      guiGraphics.fill(x + 8, y + 27, x + Math.min(w - 8, 94), y + 28, accentColor);
      guiGraphics.fill(x, y, x + 2, y + h, accentColor);
   }

   public static void renderChoiceTile(
      GuiGraphics guiGraphics, int x, int y, int w, int h, int accentColor, boolean selected, boolean enabled
   ) {
      int fill = !enabled ? 0xD012171D : selected ? ARCANE_PANEL_ALT : ARCANE_PANEL;
      int border = !enabled ? ARCANE_BORDER : selected ? accentColor : ARCANE_BORDER;
      guiGraphics.fill(x, y, x + w, y + h, fill);
      guiGraphics.renderOutline(x, y, w, h, border);
      guiGraphics.fill(x + 1, y + 1, x + 3, y + h - 1, enabled ? accentColor : 0xFF65717C);
      if (selected && enabled) {
         guiGraphics.fill(x + 3, y + h - 2, x + w - 2, y + h, accentColor);
      }
   }

   public static void renderScrollBar(GuiGraphics guiGraphics, int x, int y, int h, float progress, float visibleFraction, int accentColor) {
      int trackHeight = Math.max(1, h);
      int thumbHeight = Math.max(12, Math.min(trackHeight, Math.round(trackHeight * Math.max(0.0F, Math.min(1.0F, visibleFraction)))));
      int travel = trackHeight - thumbHeight;
      int thumbY = y + Math.round(travel * Math.max(0.0F, Math.min(1.0F, progress)));
      guiGraphics.fill(x, y, x + 4, y + trackHeight, 0xC00C1116);
      guiGraphics.renderOutline(x, y, 4, trackHeight, ARCANE_BORDER);
      guiGraphics.fill(x + 1, thumbY + 1, x + 3, thumbY + thumbHeight - 1, accentColor);
   }

   public static void renderHudPanel(GuiGraphics guiGraphics, int x, int y, int w, int h, int accentColor) {
      guiGraphics.fill(x, y, x + w, y + h, 0xC910151B);
      guiGraphics.renderOutline(x, y, w, h, 0xCC34404C);
      guiGraphics.fill(x, y, x + 2, y + h, accentColor);
      guiGraphics.fill(x + 2, y, x + Math.min(w, 30), y + 1, accentColor);
   }

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

   public static void renderArcaneBackground(GuiGraphics guiGraphics, int x, int y, int w, int h) {
      guiGraphics.fill(x, y, x + w, y + h, ARCANE_BACKGROUND);
      guiGraphics.fill(x + 1, y + 1, x + w - 1, y + 28, ARCANE_HEADER);
      guiGraphics.renderOutline(x, y, w, h, ARCANE_BORDER);
      guiGraphics.fill(x + 8, y + 27, x + w - 8, y + 28, ARCANE_CYAN);
      guiGraphics.fill(x + 10, y + 31, x + 102, y + 32, 0x6635C6D0);
   }

   public static void renderArcanePanel(GuiGraphics guiGraphics, int x, int y, int w, int h) {
      renderArcanePanel(guiGraphics, x, y, w, h, ARCANE_BORDER);
   }

   public static void renderArcanePanel(GuiGraphics guiGraphics, int x, int y, int w, int h, int accentColor) {
      guiGraphics.fill(x, y, x + w, y + h, ARCANE_PANEL);
      guiGraphics.renderOutline(x, y, w, h, ARCANE_BORDER);
      guiGraphics.fill(x, y, x + 2, y + h, accentColor);
      guiGraphics.fill(x + 2, y, x + 14, y + 1, accentColor);
   }

   public static void renderSectionHeader(GuiGraphics guiGraphics, int x, int y, int w, int accentColor) {
      guiGraphics.fill(x, y + 10, x + w, y + 11, 0x5534404C);
      guiGraphics.fill(x, y + 10, x + Math.min(24, w), y + 11, accentColor);
   }

   public static void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int w, int h, float progress, int color) {
      float clamped = Math.max(0.0F, Math.min(1.0F, progress));
      guiGraphics.fill(x, y, x + w, y + h, 0xA00C1116);
      int fillWidth = Math.round((w - 2) * clamped);
      if (fillWidth > 0) {
         guiGraphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + h - 1, color);
      }

      guiGraphics.renderOutline(x, y, w, h, ARCANE_BORDER);
   }

   public static void renderArcaneSlot(GuiGraphics guiGraphics, int x, int y, int size, int accentColor, boolean active) {
      guiGraphics.fill(x, y, x + size, y + size, active ? ARCANE_PANEL_ALT : 0xD015191F);
      guiGraphics.renderOutline(x, y, size, size, active ? accentColor : ARCANE_BORDER);
      guiGraphics.fill(x + 2, y + 2, x + 6, y + 3, active ? accentColor : ARCANE_TEXT_MUTED);
   }
}
