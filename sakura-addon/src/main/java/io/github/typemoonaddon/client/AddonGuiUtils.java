package io.github.typemoonaddon.client;

import net.minecraft.client.gui.GuiGraphics;

/** Small addon-owned drawing palette used by custom screens. */
public final class AddonGuiUtils {
    public static final int ARCANE_TEXT = 0xFFF2E8EA;
    public static final int ARCANE_TEXT_MUTED = 0xFFB8A8AE;
    public static final int ARCANE_GOLD = 0xFFFFD76A;
    public static final int ARCANE_VALID = 0xFF68E0B0;
    public static final int ARCANE_PANEL_ALT = 0xE0181018;

    public static void renderScreenBackdrop(GuiGraphics gui, int width, int height) {
        gui.fill(0, 0, width, height, 0x98000000);
    }

    public static void renderArcaneWindow(GuiGraphics gui, int x, int y, int width, int height, int accent) {
        gui.fill(x, y, x + width, y + height, 0xEC100A10);
        gui.renderOutline(x, y, width, height, accent);
    }

    public static void renderArcanePanel(GuiGraphics gui, int x, int y, int width, int height, int accent) {
        gui.fill(x, y, x + width, y + height, ARCANE_PANEL_ALT);
        gui.renderOutline(x, y, width, height, accent);
    }

    private AddonGuiUtils() {
    }
}
