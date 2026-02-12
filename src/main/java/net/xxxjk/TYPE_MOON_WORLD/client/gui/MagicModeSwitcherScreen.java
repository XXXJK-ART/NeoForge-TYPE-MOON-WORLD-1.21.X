package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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
    private final List<Integer> modeIds = new ArrayList<>();
    private static final ResourceLocation[] MODE_ICONS = new ResourceLocation[] {
        ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/gui/mode/mode_0.png"),
        ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/gui/mode/mode_1.png"),
        ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/gui/mode/mode_2.png"),
        ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/gui/mode/mode_3.png"),
        ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/gui/mode/mode_4.png")
    };
    
    private static final ResourceLocation[] JEWEL_ICONS = new ResourceLocation[] {
        ResourceLocation.parse("typemoonworld:textures/item/carved_ruby.png"),
        ResourceLocation.parse("typemoonworld:textures/item/carved_sapphire.png"),
        ResourceLocation.parse("typemoonworld:textures/item/carved_emerald.png"),
        ResourceLocation.parse("typemoonworld:textures/item/carved_topaz.png"),
        ResourceLocation.parse("typemoonworld:textures/item/carved_cyan_gemstone.png"),
        ResourceLocation.parse("typemoonworld:textures/item/carved_white_gemstone.png")
    };
    private int selectedIndex = 0;
    private boolean isClosing = false;

    public MagicModeSwitcherScreen(int currentMode) {
        super(Component.translatable("gui.typemoonworld.mode_switcher.title"));
        
        // Define modes
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            String currentMagic = "";
            if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
                currentMagic = vars.selected_magics.get(vars.current_magic_index);
            }
            
            if ("sword_barrel_full_open".equals(currentMagic)) {
                // 0: AOE, 1: Aiming, 2: Focus, 3: Broken Phantasm, 4: Clear
                addMode(0, Component.literal("AOE"));
                addMode(1, Component.literal("Aiming"));
                addMode(2, Component.literal("Focus"));
                addMode(3, Component.literal("Broken\nPhantasm"));
                addMode(4, Component.literal("Clear"));
            } else if ("jewel_magic_shoot".equals(currentMagic)) {
                // 0: Ruby, 1: Sapphire, 2: Emerald, 3: Topaz, 4: Cyan, 5: Random
                if (vars.learned_magics.contains("ruby_throw")) addMode(0, Component.literal("Ruby").withStyle(net.minecraft.ChatFormatting.RED));
                if (vars.learned_magics.contains("sapphire_throw")) addMode(1, Component.literal("Sapphire").withStyle(net.minecraft.ChatFormatting.BLUE));
                if (vars.learned_magics.contains("emerald_use")) addMode(2, Component.literal("Emerald").withStyle(net.minecraft.ChatFormatting.GREEN));
                if (vars.learned_magics.contains("topaz_throw")) addMode(3, Component.literal("Topaz").withStyle(net.minecraft.ChatFormatting.YELLOW));
                if (vars.learned_magics.contains("cyan_throw")) addMode(4, Component.literal("Cyan").withStyle(net.minecraft.ChatFormatting.AQUA));
                addMode(5, Component.literal("Random").withStyle(net.minecraft.ChatFormatting.WHITE));
            } else if ("jewel_magic_release".equals(currentMagic)) {
                // 0: Ruby, 1: Sapphire, 2: Emerald, 3: Topaz, 4: Cyan
                if (vars.learned_magics.contains("ruby_flame_sword")) addMode(0, Component.literal("Ruby").withStyle(net.minecraft.ChatFormatting.RED));
                if (vars.learned_magics.contains("sapphire_winter_frost")) addMode(1, Component.literal("Sapphire").withStyle(net.minecraft.ChatFormatting.BLUE));
                if (vars.learned_magics.contains("emerald_winter_river")) addMode(2, Component.literal("Emerald").withStyle(net.minecraft.ChatFormatting.GREEN));
                if (vars.learned_magics.contains("topaz_reinforcement")) addMode(3, Component.literal("Topaz").withStyle(net.minecraft.ChatFormatting.YELLOW));
                if (vars.learned_magics.contains("cyan_wind")) addMode(4, Component.literal("Cyan").withStyle(net.minecraft.ChatFormatting.AQUA));
            }
        }
        
        if (modes.isEmpty()) {
            addMode(0, Component.literal("None"));
        }
        
        // Find index matching currentMode
        this.selectedIndex = 0;
        for (int i = 0; i < modeIds.size(); i++) {
            if (modeIds.get(i) == currentMode) {
                this.selectedIndex = i;
                break;
            }
        }
    }

    private void addMode(int id, Component component) {
        modeIds.add(id);
        modes.add(component);
    }

    @Override
    public void init() {
        super.init();
        // Inherit mouse position to prevent recentering
        Minecraft mc = this.minecraft;
        if (mc != null) {
            // Mouse position tracking removed as unused
        }
    }

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
        int targetMode = 0;
        if (selectedIndex >= 0 && selectedIndex < modeIds.size()) {
            targetMode = modeIds.get(selectedIndex);
        }
        PacketDistributor.sendToServer(new MagicModeSwitchMessage(true, targetMode));
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
        
        boolean showIcons = true;
        boolean isSwordBarrel = false;
        boolean isJewelMagic = false;
        if (!modes.isEmpty()) {
            // Check magic type from player vars directly for accuracy
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                String currentMagic = "";
                if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
                    currentMagic = vars.selected_magics.get(vars.current_magic_index);
                }
                
                if ("sword_barrel_full_open".equals(currentMagic)) {
                    isSwordBarrel = true;
                } else if (currentMagic.startsWith("jewel_magic")) {
                    isJewelMagic = true;
                } else {
                    showIcons = false;
                }
            }
        }
        
        // Title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, bgY1 + 5, 0xFFFFFF);
        
        for (int i = 0; i < modes.size(); i++) {
            int x = startX + i * (itemWidth + gap);
            int y = startY;
            boolean isSelected = (i == selectedIndex);
            
            // Draw Box
            // Selected: White border, slightly lighter background
            // Unselected: Dark background
            int fillColor = isSelected ? 0x60FFFFFF : 0x40000000; 
            int textColor = isSelected ? 0xFFFFFF55 : 0xFFAAAAAA; // Yellowish if selected, Grey if not
            
            // Special handling for Broken Phantasm (modeId 3)
            int modeId = (i < modeIds.size()) ? modeIds.get(i) : -1;
            
            if (isSwordBarrel && modeId == 3) {
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
            if (showIcons) {
                RenderSystem.enableBlend();
                int iconSize = 24;
                int iconX = x + (itemWidth - iconSize) / 2;
                int iconY = y + 4; 
                
                if (isSwordBarrel && modeId >= 0 && modeId < MODE_ICONS.length) {
                    guiGraphics.blit(MODE_ICONS[modeId], iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                } else if (isJewelMagic && modeId >= 0 && modeId < JEWEL_ICONS.length) {
                    if (modeId != 5) {
                        guiGraphics.blit(JEWEL_ICONS[modeId], iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                    } else { // Random: Stacked 5 icons (Ruby, Sapphire, Emerald, Topaz, White)
                        // Draw 5 small icons arranged in a pattern
                        int smallSize = 12; // Even smaller to fit 5
                        int cx = iconX + (iconSize - smallSize) / 2;
                        int cy = iconY + (iconSize - smallSize) / 2;
                        
                        // Center: White
                        guiGraphics.blit(JEWEL_ICONS[5], cx, cy, 0, 0, smallSize, smallSize, smallSize, smallSize);
                        
                        // Top-Left: Ruby
                        guiGraphics.blit(JEWEL_ICONS[0], cx - 6, cy - 6, 0, 0, smallSize, smallSize, smallSize, smallSize);
                        // Top-Right: Sapphire
                        guiGraphics.blit(JEWEL_ICONS[1], cx + 6, cy - 6, 0, 0, smallSize, smallSize, smallSize, smallSize);
                        // Bottom-Left: Emerald
                        guiGraphics.blit(JEWEL_ICONS[2], cx - 6, cy + 6, 0, 0, smallSize, smallSize, smallSize, smallSize);
                        // Bottom-Right: Topaz
                        guiGraphics.blit(JEWEL_ICONS[3], cx + 6, cy + 6, 0, 0, smallSize, smallSize, smallSize, smallSize);
                    }
                }
                RenderSystem.disableBlend();
            }
            
            // Text
            Component text = modes.get(i);
            String textStr = text.getString();
            
            // Manual multiline handling for "\n"
            if (textStr.contains("\n")) {
                String[] lines = textStr.split("\n");
                guiGraphics.drawCenteredString(this.font, lines[0], x + itemWidth / 2, y + itemHeight - 18, textColor);
                guiGraphics.drawCenteredString(this.font, lines[1], x + itemWidth / 2, y + itemHeight - 9, textColor);
            } else {
                // Draw single line text at bottom
                // If no icons, draw centered?
                int textY = y + itemHeight - 10;
                if (!showIcons) {
                    textY = y + (itemHeight - 8) / 2; // Center vertically
                }
                guiGraphics.drawCenteredString(this.font, text, x + itemWidth / 2, textY, textColor);
            }
        }
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
