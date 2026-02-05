package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public class GuiUtils {
    public static void renderTechFrame(GuiGraphics guiGraphics, int x, int y, int w, int h, int color, int cornerColor) {
        // Background (Semi-transparent dark)
        guiGraphics.fill(x, y, x + w, y + h, 0x60000000);
        
        // Main Border (Thin)
        guiGraphics.renderOutline(x, y, w, h, color);
        
        // Corner Accents (Thick and Bold)
        int len = 8; // Reduced length (15 -> 8)
        int thick = 2; // Reduced thickness (3 -> 2)
        
        // Top-Left
        guiGraphics.fill(x - 1, y - 1, x + len, y + thick - 1, cornerColor);
        guiGraphics.fill(x - 1, y - 1, x + thick - 1, y + len, cornerColor);
        // Decoration dot
        guiGraphics.fill(x + len + 1, y - 1, x + len + 3, y + thick - 1, cornerColor);
        
        // Top-Right
        guiGraphics.fill(x + w - len, y - 1, x + w + 1, y + thick - 1, cornerColor);
        guiGraphics.fill(x + w - thick + 1, y - 1, x + w + 1, y + len, cornerColor);
        // Decoration dot
        guiGraphics.fill(x + w - len - 3, y - 1, x + w - len - 1, y + thick - 1, cornerColor);
        
        // Bottom-Left
        guiGraphics.fill(x - 1, y + h - thick + 1, x + len, y + h + 1, cornerColor);
        guiGraphics.fill(x - 1, y + h - len, x + thick - 1, y + h + 1, cornerColor);
        // Decoration dot
        guiGraphics.fill(x + len + 1, y + h - thick + 1, x + len + 3, y + h + 1, cornerColor);
        
        // Bottom-Right
        guiGraphics.fill(x + w - len, y + h - thick + 1, x + w + 1, y + h + 1, cornerColor);
        guiGraphics.fill(x + w - thick + 1, y + h - len, x + w + 1, y + h + 1, cornerColor);
        // Decoration dot
        guiGraphics.fill(x + w - len - 3, y + h - thick + 1, x + w - len - 1, y + h + 1, cornerColor);
        
        // Inner thin lines for "tech" feel
        // int innerOffset = 5;
        // guiGraphics.renderOutline(x + innerOffset, y + innerOffset, w - innerOffset * 2, h - innerOffset * 2, color & 0x80FFFFFF);
    }
    
    public static void renderBackground(GuiGraphics guiGraphics, int x, int y, int w, int h) {
         // Gradient: Dark Blue/Purple to Black
         guiGraphics.fillGradient(x, y, x + w, y + h, 0xF0101020, 0xF0050510);
         
         // Outer Border
         int borderColor = 0xFF00FFFF;
         guiGraphics.renderOutline(x, y, w, h, borderColor);
         
         // Header Line
         guiGraphics.fill(x + 5, y + 25, x + w - 5, y + 26, 0x8000FFFF);
    }
}
