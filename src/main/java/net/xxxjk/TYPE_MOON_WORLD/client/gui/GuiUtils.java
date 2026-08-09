package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public class GuiUtils {
   /** Opaque central base; the area outside it remains transparent. */
   public static final int ARCANE_OVERLAY = 0xF010151B;
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
   }

   public static void renderScreenBaseBackdrop(GuiGraphics guiGraphics, int width, int height) {
      int baseWidth = Math.min(Math.max(420, width * 4 / 5), Math.max(1, width - 12));
      int baseHeight = Math.min(Math.max(230, height * 4 / 5), Math.max(1, height - 12));
      int baseX = (width - baseWidth) / 2;
      int baseY = (height - baseHeight) / 2;
      guiGraphics.fill(baseX, baseY, baseX + baseWidth, baseY + baseHeight, ARCANE_OVERLAY);
      guiGraphics.renderOutline(baseX, baseY, baseWidth, baseHeight, ARCANE_BORDER);
      if (baseWidth > 18) {
         guiGraphics.fill(baseX + 8, baseY + 8, baseX + Math.min(baseWidth - 8, 96), baseY + 10, ARCANE_CYAN);
      }
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
      guiGraphics.fill(x, y, x + w, y + h, ARCANE_BACKGROUND);
      guiGraphics.renderOutline(x, y, w, h, ARCANE_BORDER);
      guiGraphics.fill(x + 5, y + 25, x + w - 5, y + 26, 0x5534404C);
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
      int outer = active ? accentColor : 0xFF41505D;
      int inner = active ? 0xFF1A2830 : 0xFF11161B;
      int glow = active ? 0xAAFFFFFF : 0xAA65717C;
      int mark = active ? accentColor : 0xFF7A8793;
      guiGraphics.fill(x, y, x + size, y + size, 0xF00A0E12);
      guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, inner);
      guiGraphics.renderOutline(x, y, size, size, outer);
      if (size >= 6) {
         guiGraphics.renderOutline(x + 1, y + 1, size - 2, size - 2, glow);
      }
      if (size >= 8) {
         guiGraphics.fill(x + 2, y + 2, x + Math.min(size - 2, 6), y + 3, mark);
         guiGraphics.fill(x + 2, y + 2, x + 3, y + Math.min(size - 2, 6), mark);
         guiGraphics.fill(x + size - Math.min(size - 2, 6), y + size - 3, x + size - 2, y + size - 2, mark);
         guiGraphics.fill(x + size - 3, y + size - Math.min(size - 2, 6), x + size - 2, y + size - 2, mark);
      }
   }

   public static void renderArcaneSlotMarker(GuiGraphics guiGraphics, int x, int y, int size, int accentColor, boolean active) {
      int border = active ? accentColor : 0xFFE1E8EF;
      int shadow = active ? 0xAA0A0E12 : 0xCC0A0E12;
      int tick = active ? accentColor : 0xFFF2F5F7;
      guiGraphics.renderOutline(x - 1, y - 1, size + 2, size + 2, shadow);
      guiGraphics.renderOutline(x, y, size, size, border);
      if (size >= 8) {
         guiGraphics.fill(x + 1, y + 1, x + 6, y + 2, tick);
         guiGraphics.fill(x + 1, y + 1, x + 2, y + 6, tick);
         guiGraphics.fill(x + size - 6, y + size - 2, x + size - 1, y + size - 1, tick);
         guiGraphics.fill(x + size - 2, y + size - 6, x + size - 1, y + size - 1, tick);
      }
   }
}
