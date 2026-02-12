package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class MagicRadialMenuScreen extends Screen {
    private final List<String> availableMagics;
    private int selectedIndex = -1;

    public MagicRadialMenuScreen(List<String> availableMagics) {
        super(Component.translatable("gui.typemoonworld.radial_menu.title"));
        this.availableMagics = availableMagics;
    }

    private boolean isScrollMode = false;
    private double scrollOriginX = 0;
    private double scrollOriginY = 0;

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Do NOT call super.render to avoid default full-screen gradient overlay
        // super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (availableMagics.isEmpty()) return;

        int centerX = this.width / 2;
        // Shift centerY upwards to avoid overlapping with hotbar
        // 0.4 was a bit too high, let's try 0.45 (slightly above center)
        int centerY = (int)(this.height * 0.45); 
        
        // Calculate radius as 1/4 of screen dimensions (Diameter = 1/2 screen)
        // Reduce radius slightly to be "smaller" but still legible
        // Previous was height / 2.2 (~45%). Let's go back to ~ height / 2.8 (~35%)
        double radius = Math.min(this.width, this.height) / 2.6; 
        double innerRadius = radius * 0.4; // Increase inner radius to prevent mis-selection

        int count = availableMagics.size();
        float angleStep = 360.0f / count;

        // Mouse Handling
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        
        // Input isolation logic:
        // If in scroll mode, require significant mouse movement (recapture) to switch back to mouse mode
        if (isScrollMode) {
            double moveFromOrigin = Math.sqrt(Math.pow(mouseX - scrollOriginX, 2) + Math.pow(mouseY - scrollOriginY, 2));
            if (moveFromOrigin > 20.0) { // Require 20px movement to recapture
                isScrollMode = false;
            }
        }
        
        // If NOT in scroll mode (or just recaptured), allow mouse hover selection
        // Only trigger if mouse is OUTSIDE the inner dead zone
        if (!isScrollMode && dist > innerRadius) { 
            double angleDeg = Math.toDegrees(Math.atan2(dy, dx)); 
            if (angleDeg < 0) angleDeg += 360;
            
            // Normalize: Right(0) -> Top(0)
            angleDeg += 90;
            if (angleDeg >= 360) angleDeg -= 360;

            int newIndex = (int)(angleDeg / angleStep);
            if (newIndex >= count) newIndex = 0;

            if (this.selectedIndex != newIndex) {
                this.selectedIndex = newIndex;
                // Immediately sync selection on mouse hover
                performAction(false);
            }
        }
        
        // Setup Rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull(); // Disable culling to ensure both sides are visible
        RenderSystem.disableDepthTest(); // Disable depth test to draw on top
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = guiGraphics.pose().last().pose();

        // 1. Draw Background Ring (Full 360 semi-transparent ring)
        // Draw a single complete ring first to ensure continuous background
        // Color: Dark Grey (30, 30, 30) with Alpha ~100
        drawSector(bufferbuilder, matrix, centerX, centerY, innerRadius, radius, 0, 360, 30, 30, 30, 100);
        
        // Draw Borders for Background Ring
        // Outer Border
        drawCircleStroke(bufferbuilder, matrix, centerX, centerY, radius, 0, 360, 200, 200, 200, 150, 2.0f);
        // Inner Border
        drawCircleStroke(bufferbuilder, matrix, centerX, centerY, innerRadius, 0, 360, 200, 200, 200, 150, 2.0f);

        // 2. Draw Sectors (Selection Highlights or Separators)
        for (int i = 0; i < count; i++) {
            if (i == selectedIndex) continue; // Skip selected for now
            
            // Draw separator lines
            float angle = i * angleStep - 90;
            drawLine(bufferbuilder, matrix, centerX, centerY, innerRadius, radius, angle, 200, 200, 200, 100, 1.5f);
        }
        
        // 3. Draw Selected Sector (Pop-out effect with brighter color)
        if (selectedIndex >= 0 && selectedIndex < count) {
            float startAngle = selectedIndex * angleStep - 90; 
            float endAngle = (selectedIndex + 1) * angleStep - 90;
            
            // Selected: Bright Magic Cyan/Blue
            // Color: Cyan (0, 200, 255) with Alpha ~220
            // Pop out by 15 pixels (scaled up)
            double popRadius = radius + 15;
            drawSector(bufferbuilder, matrix, centerX, centerY, innerRadius, popRadius, startAngle, endAngle, 0, 200, 255, 220);
            
            // Draw Border for Selected Sector
            drawSectorStroke(bufferbuilder, matrix, centerX, centerY, innerRadius, popRadius, startAngle, endAngle, 255, 255, 255, 255, 3.0f);
        }

        // End Batch
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());

        // Draw Separator Lines (Optional, separate pass for crisp lines)
        // For simplicity, we can rely on the color contrast for now. 
        // Or we can draw thin lines between sectors if needed.
        
        // Draw Text (Separate Pass)
        for (int i = 0; i < count; i++) {
            float startAngle = i * angleStep - 90; 
            
            // Text Position
            double midAngleRad = Math.toRadians(startAngle + angleStep / 2);
            // Place text closer to center of sector (radius * 0.75 is mid-point of ring if inner is 0.4)
            // But user wants text INSIDE the sector.
            // Let's position it at 0.7 * radius
            double textRadius = radius * 0.75;
            int textX = centerX + (int)(Math.cos(midAngleRad) * textRadius);
            int textY = centerY + (int)(Math.sin(midAngleRad) * textRadius);
            
            String magicId = availableMagics.get(i);
            String translationKey = "magic.typemoonworld." + magicId + ".name";
            Component name = Component.translatable(translationKey);
            
            boolean isSelected = (i == selectedIndex);
            int textColor = isSelected ? 0xFFFFFFFF : 0xFFAAAAAA;
            
            // Handle multiline text (e.g. split by brackets or length)
            String fullText = name.getString();
            // First split by brackets
            String[] bracketParts = fullText.split("(?=[\\(（])");
            
            java.util.List<String> lines = new java.util.ArrayList<>();
            for (String part : bracketParts) {
                // If the part does NOT start with a bracket (meaning it's the main name, likely CJK)
                // AND it contains non-ASCII characters (heuristic for CJK)
                // AND it's longer than 5 chars
                // Then split it into chunks of 5
                boolean isBracketPart = part.startsWith("(") || part.startsWith("（");
                boolean hasNonAscii = part.chars().anyMatch(c -> c > 127);
                
                if (!isBracketPart && hasNonAscii && part.length() > 5) {
                    for (int j = 0; j < part.length(); j += 5) {
                        lines.add(part.substring(j, Math.min(j + 5, part.length())));
                    }
                } else {
                    lines.add(part);
                }
            }
            
            int lineHeight = this.font.lineHeight;
            int totalTextHeight = lines.size() * lineHeight;
            int startY = textY - totalTextHeight / 2;
            
            for (int k = 0; k < lines.size(); k++) {
                String line = lines.get(k);
                // Draw drop shadow manually for better visibility on transparent background
                // Or use drawCenteredString which usually has shadow if dropShadow parameter is true (but vanilla method doesn't expose it easily in all versions)
                // Default drawCenteredString has shadow.
                guiGraphics.drawCenteredString(this.font, line, textX, startY + (k * lineHeight), textColor);
            }
        }

        // Draw Center Info
        if (selectedIndex >= 0 && selectedIndex < availableMagics.size()) {
             String selectedId = availableMagics.get(selectedIndex);
             Component centerText = Component.translatable("magic.typemoonworld." + selectedId + ".name");
             guiGraphics.drawCenteredString(this.font, centerText, centerX, centerY - 4, 0xFF00FFFF);
        }

        RenderSystem.disableBlend();
    }
    
    private void drawSector(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, double rIn, double rOut, float startAngle, float endAngle, int r, int g, int b, int a) {
        float angleDiff = endAngle - startAngle;
        int steps = Math.max(1, (int)(angleDiff / 2)); // 2 degree steps for smoother circle
        
        for (int i = 0; i < steps; i++) {
            float a1 = startAngle + (angleDiff * i / steps);
            float a2 = startAngle + (angleDiff * (i + 1) / steps);
            
            double rad1 = Math.toRadians(a1);
            double rad2 = Math.toRadians(a2);
            
            float x1_out = cx + (float)(Math.cos(rad1) * rOut);
            float y1_out = cy + (float)(Math.sin(rad1) * rOut);
            float x2_out = cx + (float)(Math.cos(rad2) * rOut);
            float y2_out = cy + (float)(Math.sin(rad2) * rOut);
            
            float x1_in = cx + (float)(Math.cos(rad1) * rIn);
            float y1_in = cy + (float)(Math.sin(rad1) * rIn);
            float x2_in = cx + (float)(Math.cos(rad2) * rIn);
            float y2_in = cy + (float)(Math.sin(rad2) * rIn);
            
            // 2 Triangles per step to form a quad
            // Triangle 1: In1 -> Out1 -> Out2
            buffer.addVertex(matrix, x1_in, y1_in, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1_out, y1_out, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2_out, y2_out, 0).setColor(r, g, b, a);
            
            // Triangle 2: In1 -> Out2 -> In2
            buffer.addVertex(matrix, x1_in, y1_in, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2_out, y2_out, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2_in, y2_in, 0).setColor(r, g, b, a);
        }
    }

    private void drawCircleStroke(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, double radius, float startAngle, float endAngle, int r, int g, int b, int a, float width) {
        float angleDiff = endAngle - startAngle;
        int steps = Math.max(1, (int)(angleDiff / 2)); 
        
        // Simple thick line using triangle strip along the path
        double halfWidth = width / 2.0;
        double rIn = radius - halfWidth;
        double rOut = radius + halfWidth;

        for (int i = 0; i < steps; i++) {
            float a1 = startAngle + (angleDiff * i / steps);
            float a2 = startAngle + (angleDiff * (i + 1) / steps);
            
            double rad1 = Math.toRadians(a1);
            double rad2 = Math.toRadians(a2);
            
            float x1_out = cx + (float)(Math.cos(rad1) * rOut);
            float y1_out = cy + (float)(Math.sin(rad1) * rOut);
            float x2_out = cx + (float)(Math.cos(rad2) * rOut);
            float y2_out = cy + (float)(Math.sin(rad2) * rOut);
            
            float x1_in = cx + (float)(Math.cos(rad1) * rIn);
            float y1_in = cy + (float)(Math.sin(rad1) * rIn);
            float x2_in = cx + (float)(Math.cos(rad2) * rIn);
            float y2_in = cy + (float)(Math.sin(rad2) * rIn);
            
            buffer.addVertex(matrix, x1_in, y1_in, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1_out, y1_out, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2_out, y2_out, 0).setColor(r, g, b, a);
            
            buffer.addVertex(matrix, x1_in, y1_in, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2_out, y2_out, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2_in, y2_in, 0).setColor(r, g, b, a);
        }
    }
    
    private void drawLine(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, double rIn, double rOut, float angle, int r, int g, int b, int a, float width) {
        double rad = Math.toRadians(angle);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        
        double perpX = -sin * (width / 2.0);
        double perpY = cos * (width / 2.0);
        
        float x1 = cx + (float)(cos * rIn);
        float y1 = cy + (float)(sin * rIn);
        float x2 = cx + (float)(cos * rOut);
        float y2 = cy + (float)(sin * rOut);
        
        buffer.addVertex(matrix, x1 + (float)perpX, y1 + (float)perpY, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2 + (float)perpX, y2 + (float)perpY, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2 - (float)perpX, y2 - (float)perpY, 0).setColor(r, g, b, a);
        
        buffer.addVertex(matrix, x1 + (float)perpX, y1 + (float)perpY, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2 - (float)perpX, y2 - (float)perpY, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x1 - (float)perpX, y1 - (float)perpY, 0).setColor(r, g, b, a);
    }
    
    private void drawSectorStroke(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, double rIn, double rOut, float startAngle, float endAngle, int r, int g, int b, int a, float width) {
        // Draw Inner Arc
        drawCircleStroke(buffer, matrix, cx, cy, rIn, startAngle, endAngle, r, g, b, a, width);
        // Draw Outer Arc
        drawCircleStroke(buffer, matrix, cx, cy, rOut, startAngle, endAngle, r, g, b, a, width);
        // Draw Side Lines
        drawLine(buffer, matrix, cx, cy, rIn, rOut, startAngle, r, g, b, a, width);
        drawLine(buffer, matrix, cx, cy, rIn, rOut, endAngle, r, g, b, a, width);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        
        // Robust key release detection to prevent accidental closing/reopening
        // which causes mouse recentering
        if (this.minecraft != null) {
            long window = this.minecraft.getWindow().getWindow();
            var keyMapping = TypeMoonWorldModKeyMappings.CYCLE_MAGIC;
            var type = keyMapping.getKey().getType();
            var code = keyMapping.getKey().getValue();
            boolean isDown = false;
            
            if (type == InputConstants.Type.KEYSYM) {
                isDown = GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
            } else if (type == InputConstants.Type.MOUSE) {
                isDown = GLFW.glfwGetMouseButton(window, code) == GLFW.GLFW_PRESS;
            }

            if (!isDown) {
                this.onClose(); 
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && selectedIndex != -1) {
            performAction();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (availableMagics.isEmpty()) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        
        // Use signum to ensure one tick per scroll event regardless of speed
        int direction = (int)Math.signum(scrollY) * -1; // UP (positive) -> Previous (-1), DOWN (negative) -> Next (1)
        
        if (direction != 0) {
            if (selectedIndex == -1) selectedIndex = 0;
            // Correct logic: Previous (-1) or Next (+1)
            // Note: In Java, -1 % N = -1, so we add N before modulo
            selectedIndex = (selectedIndex + direction + availableMagics.size()) % availableMagics.size();
            
            // Activate Scroll Mode to prevent mouse hover conflict
            isScrollMode = true;
            scrollOriginX = mouseX;
            scrollOriginY = mouseY;
            
            // Sync scroll selection to server immediately
            performAction(false);
        }
        
        return true;
    }

    private void performAction() {
        performAction(true);
    }

    private void performAction(boolean close) {
        if (this.minecraft != null && this.minecraft.player != null) {
            if (selectedIndex >= 0 && selectedIndex < availableMagics.size()) {
                String magicId = availableMagics.get(selectedIndex);
                PacketDistributor.sendToServer(new SwitchMagicMessage(magicId));
            }
            if (close) {
                this.onClose();
            }
        }
    }
}