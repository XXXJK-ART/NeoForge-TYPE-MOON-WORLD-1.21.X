package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class NeonButton extends Button {
    private final int hoverColor;

    public NeonButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        this(x, y, width, height, message, onPress, 0xFF00FFFF); // Default Cyan
    }

    public NeonButton(int x, int y, int width, int height, Component message, OnPress onPress, int color) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.hoverColor = color;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int borderColor = isHoveredOrFocused() ? hoverColor : 0xFF00AAAA;
        int fillColor = isHoveredOrFocused() ? (hoverColor & 0x00FFFFFF) | 0x80000050 : 0x80000000;
        int textColor = isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFAAAAAA;
        
        // Background
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, fillColor);
        
        // Border
        guiGraphics.renderOutline(getX(), getY(), width, height, borderColor);
        
        // Corner accents for buttons too?
        if (isHoveredOrFocused()) {
            int len = 4;
            guiGraphics.fill(getX(), getY(), getX() + len, getY() + 1, hoverColor);
            guiGraphics.fill(getX(), getY(), getX() + 1, getY() + len, hoverColor);
            
            guiGraphics.fill(getX() + width - len, getY() + height - 1, getX() + width, getY() + height, hoverColor);
            guiGraphics.fill(getX() + width - 1, getY() + height - len, getX() + width, getY() + height, hoverColor);
        }
        
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, textColor);
    }
}
