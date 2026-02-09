package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicModeSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

import java.util.ArrayList;
import java.util.List;

public class MagicModeSwitcherScreen extends Screen {
    private final List<Component> modes = new ArrayList<>();
    private static final ResourceLocation[] MODE_ICONS = new ResourceLocation[] {
        ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/gui/mode/mode_0.png"),
        ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/gui/mode/mode_1.png"),
        ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/gui/mode/mode_2.png"),
        ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/gui/mode/mode_3.png"),
        ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/gui/mode/mode_4.png")
    };
    private int selectedIndex = 0;
    private boolean isClosing = false;

    public MagicModeSwitcherScreen(int currentMode) {
        super(Component.translatable("gui.typemoonworld.mode_switcher.title"));
        this.selectedIndex = currentMode;
        
        // Define modes
        // 0: AOE, 1: Aiming, 2: Focus, 3: Broken Phantasm, 4: Clear
        modes.add(Component.literal("AOE")); // 0
        modes.add(Component.literal("Aiming")); // 1
        modes.add(Component.literal("Focus")); // 2
        modes.add(Component.literal("Broken\nPhantasm")); // 3
        modes.add(Component.literal("Clear")); // 4
        
        // Ensure index is valid
        if (this.selectedIndex < 0) this.selectedIndex = 0;
        if (this.selectedIndex >= modes.size()) this.selectedIndex = 0;
    }

    @Override
    public void init() {
        super.init();
        // Inherit mouse position to prevent recentering
        if (this.minecraft != null) {
            this.lastMouseX = this.minecraft.mouseHandler.xpos() * (double)this.width / (double)this.minecraft.getWindow().getWidth();
            this.lastMouseY = this.minecraft.mouseHandler.ypos() * (double)this.height / (double)this.minecraft.getWindow().getHeight();
        }
    }
    
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        
        if (this.minecraft != null) {
            long window = this.minecraft.getWindow().getWindow();
            
            var keyMapping = TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH;
            var type = keyMapping.getKey().getType();
            var code = keyMapping.getKey().getValue();
            boolean isDown = false;
            
            if (type == InputConstants.Type.KEYSYM) {
                isDown = GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
            } else if (type == InputConstants.Type.MOUSE) {
                isDown = GLFW.glfwGetMouseButton(window, code) == GLFW.GLFW_PRESS;
            }
            
            if (!isDown && !isClosing) {
                closeAndSelect();
            }
        }
    }
    
    private void closeAndSelect() {
        isClosing = true;
        // Send packet to set mode
        PacketDistributor.sendToServer(new MagicModeSwitchMessage(true, selectedIndex));
        this.onClose();
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            // Scroll Up -> Previous (to match typical vertical lists, or Left?)
            // Usually Scroll Up = Previous Index
            selectedIndex = (selectedIndex - 1 + modes.size()) % modes.size();
        } else if (scrollY < 0) {
            // Scroll Down -> Next Index
            selectedIndex = (selectedIndex + 1) % modes.size();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Click to select
            if (updateSelectionAt(mouseX, mouseY)) {
                closeAndSelect();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Do NOT call renderBackground to avoid full screen darkening
        // We will draw a semi-transparent background box behind the items instead
        // this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        int itemWidth = 50;  // Reduced from 60
        int itemHeight = 50; // Reduced from 60
        int gap = 8;         // Reduced from 10
        int totalWidth = (itemWidth * modes.size()) + (gap * (modes.size() - 1));
        int startX = (this.width - totalWidth) / 2;
        int centerY = this.height / 2;
        int startY = centerY - itemHeight / 2;
        
        // Draw container background (Semi-transparent black box around all items)
        int padding = 10;
        int bgX1 = startX - padding;
        int bgY1 = startY - padding - 20; // Extra top padding for title
        int bgX2 = startX + totalWidth + padding;
        int bgY2 = startY + itemHeight + padding;
        
        // Background: Black with alpha ~180
        guiGraphics.fill(bgX1, bgY1, bgX2, bgY2, 0xB4000000);
        // Border: White/Light Grey
        guiGraphics.renderOutline(bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, 0xFF888888);
        
        // Update selection on hover
        updateSelectionAt(mouseX, mouseY);
        
        for (int i = 0; i < modes.size(); i++) {
            int x = startX + i * (itemWidth + gap);
            int y = startY;
            boolean isSelected = (i == selectedIndex);
            
            // Draw Box
            // Selected: White border, slightly lighter background
            // Unselected: Dark background
            int fillColor = isSelected ? 0x60FFFFFF : 0x40000000; 
            int textColor = isSelected ? 0xFFFFFF55 : 0xFFAAAAAA; // Yellowish if selected, Grey if not
            
            // Special handling for Broken Phantasm (index 3)
            if (i == 3) {
                // Check if BP is enabled
                if (this.minecraft != null && this.minecraft.player != null) {
                    TypeMoonWorldModVariables.PlayerVariables vars = this.minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    boolean isBP = vars.ubw_broken_phantasm_enabled;
                    if (isBP) {
                        fillColor = 0x60FF0000; // Red tint if enabled
                        textColor = 0xFFFF5555;
                    }
                }
            }
            
            guiGraphics.fill(x, y, x + itemWidth, y + itemHeight, fillColor);
            
            // Draw Border
            int borderColor = isSelected ? 0xFFFFFFFF : 0xFF555555;
            guiGraphics.renderOutline(x, y, itemWidth, itemHeight, borderColor);
            
            // Draw Icon
            // Icon size 24x24 (scaled down)
            if (i < MODE_ICONS.length) {
                RenderSystem.enableBlend();
                int iconSize = 24;
                int iconX = x + (itemWidth - iconSize) / 2;
                int iconY = y + 4; 
                
                guiGraphics.blit(MODE_ICONS[i], iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                RenderSystem.disableBlend();
            }
            
            // Text
            Component text = modes.get(i);
            String textStr = text.getString();
            
            // Manual multiline handling for "\n"
            if (textStr.contains("\n")) {
                String[] lines = textStr.split("\n");
                // Draw 2 lines. 
                // Line 1 at y + itemHeight - 16
                // Line 2 at y + itemHeight - 8
                // Need to use small font? Default font is 9px high. 
                // 50px height. Icon at y+4 (24px). Space left: 50-4-24 = 22px.
                // 2 lines take 18px. It will be tight.
                // Let's move text slightly up or scale down.
                // Or just draw them.
                
                // Draw line 1
                guiGraphics.drawCenteredString(this.font, lines[0], x + itemWidth / 2, y + itemHeight - 18, textColor);
                // Draw line 2
                guiGraphics.drawCenteredString(this.font, lines[1], x + itemWidth / 2, y + itemHeight - 9, textColor);
            } else {
                // Draw single line text at bottom
                int textY = y + itemHeight - 10;
                guiGraphics.drawCenteredString(this.font, text, x + itemWidth / 2, textY, textColor);
            }
        }
        
        // Title
        guiGraphics.drawCenteredString(this.font, Component.literal("Select Mode"), this.width / 2, startY - 15, 0xFFFFFFFF);
    }
    
    private boolean updateSelectionAt(double mouseX, double mouseY) {
        int itemWidth = 50;
        int itemHeight = 50;
        int gap = 8;
        int totalWidth = (itemWidth * modes.size()) + (gap * (modes.size() - 1));
        int startX = (this.width - totalWidth) / 2;
        int centerY = this.height / 2;
        int startY = centerY - itemHeight / 2;

        for (int i = 0; i < modes.size(); i++) {
             int x = startX + i * (itemWidth + gap);
             int y = startY;
             if (mouseX >= x && mouseX < x + itemWidth && mouseY >= y && mouseY < y + itemHeight) {
                 selectedIndex = i;
                 return true;
             }
        }
        return false;
    }
}
