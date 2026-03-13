package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicIndexMessage;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class MagicRadialMenuScreen extends Screen {
   private final List<String> availableMagics;
   private final List<String> displayNames;
   private final List<Boolean> crestSourceFlags;
   private final List<String> crestPresetHints;
   private int selectedIndex = -1;
   private boolean isScrollMode = false;
   private double scrollOriginX = 0.0;
   private double scrollOriginY = 0.0;

   public MagicRadialMenuScreen(List<String> availableMagics) {
      this(availableMagics, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
   }

   public MagicRadialMenuScreen(List<String> availableMagics, List<String> displayNames) {
      this(availableMagics, displayNames, Collections.emptyList(), Collections.emptyList());
   }

   public MagicRadialMenuScreen(List<String> availableMagics, List<String> displayNames, List<Boolean> crestSourceFlags, List<String> crestPresetHints) {
      super(Component.translatable("gui.typemoonworld.radial_menu.title"));
      this.availableMagics = availableMagics;
      this.displayNames = displayNames == null ? Collections.emptyList() : displayNames;
      this.crestSourceFlags = crestSourceFlags == null ? Collections.emptyList() : crestSourceFlags;
      this.crestPresetHints = crestPresetHints == null ? Collections.emptyList() : crestPresetHints;
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      if (!this.availableMagics.isEmpty()) {
         int centerX = this.width / 2;
         int centerY = (int)(this.height * 0.45);
         double radius = Math.min(this.width, this.height) / 2.6;
         double innerRadius = radius * 0.4;
         int count = this.availableMagics.size();
         float angleStep = 360.0F / count;
         double dx = mouseX - centerX;
         double dy = mouseY - centerY;
         double dist = Math.sqrt(dx * dx + dy * dy);
         if (this.isScrollMode) {
            double moveFromOrigin = Math.sqrt(Math.pow(mouseX - this.scrollOriginX, 2.0) + Math.pow(mouseY - this.scrollOriginY, 2.0));
            if (moveFromOrigin > 20.0) {
               this.isScrollMode = false;
            }
         }

         if (!this.isScrollMode && dist > innerRadius) {
            double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
            if (angleDeg < 0.0) {
               angleDeg += 360.0;
            }

            angleDeg += 90.0;
            if (angleDeg >= 360.0) {
               angleDeg -= 360.0;
            }

            int newIndex = (int)(angleDeg / angleStep);
            if (newIndex >= count) {
               newIndex = 0;
            }

            if (this.selectedIndex != newIndex) {
               this.selectedIndex = newIndex;
               this.performAction(false);
            }
         }

         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         Tesselator tesselator = Tesselator.getInstance();
         BufferBuilder bufferbuilder = tesselator.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
         Matrix4f matrix = guiGraphics.pose().last().pose();
         this.drawSector(bufferbuilder, matrix, centerX, centerY, innerRadius, radius, 0.0F, 360.0F, 30, 30, 30, 100);
         this.drawCircleStroke(bufferbuilder, matrix, centerX, centerY, radius, 0.0F, 360.0F, 200, 200, 200, 150, 2.0F);
         this.drawCircleStroke(bufferbuilder, matrix, centerX, centerY, innerRadius, 0.0F, 360.0F, 200, 200, 200, 150, 2.0F);

         for (int i = 0; i < count; i++) {
            if (i != this.selectedIndex) {
               boolean crest = this.isCrestMagic(i);
               int baseR = crest ? 185 : 46;
               int baseG = crest ? 58 : 116;
               int baseB = crest ? 70 : 210;
               int baseA = crest ? 110 : 115;
               float startAngle = i * angleStep - 90.0F;
               float endAngle = (i + 1) * angleStep - 90.0F;
               this.drawSector(bufferbuilder, matrix, centerX, centerY, innerRadius, radius, startAngle, endAngle, baseR, baseG, baseB, baseA);
               float angle = i * angleStep - 90.0F;
               this.drawLine(bufferbuilder, matrix, centerX, centerY, innerRadius, radius, angle, 200, 200, 200, 100, 1.5F);
            }
         }

         if (this.selectedIndex >= 0 && this.selectedIndex < count) {
            float startAngle = this.selectedIndex * angleStep - 90.0F;
            float endAngle = (this.selectedIndex + 1) * angleStep - 90.0F;
            boolean crest = this.isCrestMagic(this.selectedIndex);
            double popRadius = radius + 15.0;
            if (crest) {
               this.drawSector(bufferbuilder, matrix, centerX, centerY, innerRadius, popRadius, startAngle, endAngle, 232, 80, 92, 225);
            } else {
               this.drawSector(bufferbuilder, matrix, centerX, centerY, innerRadius, popRadius, startAngle, endAngle, 0, 200, 255, 220);
            }

            this.drawSectorStroke(bufferbuilder, matrix, centerX, centerY, innerRadius, popRadius, startAngle, endAngle, 255, 255, 255, 255, 3.0F);
         }

         BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());

         for (int ix = 0; ix < count; ix++) {
            float startAngle = ix * angleStep - 90.0F;
            double midAngleRad = Math.toRadians(startAngle + angleStep / 2.0F);
            double textRadius = radius * 0.75;
            int textX = centerX + (int)(Math.cos(midAngleRad) * textRadius);
            int textY = centerY + (int)(Math.sin(midAngleRad) * textRadius);
            String fullText = this.getDisplayNameComponent(ix).getString();
            boolean isSelected = ix == this.selectedIndex;
            int textColor = isSelected ? -1 : (this.isCrestMagic(ix) ? -19790 : -5056001);
            int approxArcWidth = Math.max(36, (int)((Math.PI * 2) * textRadius / Math.max(1, count) * 0.78));
            List<FormattedCharSequence> lines = this.wrapText(fullText, approxArcWidth, 3);
            int lineHeight = 9 + 1;
            int totalTextHeight = lines.size() * lineHeight;
            int startY = textY - totalTextHeight / 2;

            for (int k = 0; k < lines.size(); k++) {
               FormattedCharSequence line = lines.get(k);
               int lineWidth = this.font.width(line);
               guiGraphics.drawString(this.font, line, textX - lineWidth / 2, startY + k * lineHeight, textColor, false);
            }
         }

         if (this.selectedIndex >= 0 && this.selectedIndex < this.availableMagics.size()) {
            String centerText = this.getDisplayNameComponent(this.selectedIndex).getString();
            int centerColor = this.isCrestMagic(this.selectedIndex) ? -32640 : -16711681;
            int centerMaxWidth = Math.max(60, (int)(innerRadius * 1.7));
            List<FormattedCharSequence> centerLines = this.wrapText(centerText, centerMaxWidth, 4);
            int lineHeight = 9 + 1;
            int totalHeight = centerLines.size() * lineHeight;
            int startY = centerY - totalHeight / 2;

            for (int ix = 0; ix < centerLines.size(); ix++) {
               FormattedCharSequence line = centerLines.get(ix);
               int lineWidth = this.font.width(line);
               guiGraphics.drawString(this.font, line, centerX - lineWidth / 2, startY + ix * lineHeight, centerColor, false);
            }
         }

         RenderSystem.disableBlend();
      }
   }

   private Component getDisplayNameComponent(int index) {
      String baseDisplay = "";
      if (index >= 0 && index < this.displayNames.size()) {
         String display = this.displayNames.get(index);
         if (display != null && !display.isBlank()) {
            baseDisplay = display;
         }
      }

      String magicId = this.getMagicId(index);
      String fullName = Component.translatable("magic.typemoonworld." + magicId + ".name").getString();
      if (baseDisplay.isBlank()) {
         baseDisplay = fullName;
      }

      String shortName = Component.translatable("key.typemoonworld.magic." + magicId + ".short").getString();
      String selectedName = Component.translatable("key.typemoonworld.magic." + magicId + ".selected").getString();
      baseDisplay = normalizeDisplayName(baseDisplay, fullName, shortName, selectedName, magicId);
      if (this.isCrestMagic(index)) {
         String hint = this.getCrestPresetHint(index);
         if (!hint.isBlank() && !baseDisplay.contains(hint)) {
            baseDisplay = baseDisplay + " [" + hint + "]";
         }
      }

      return Component.literal(baseDisplay);
   }

   private String getMagicId(int index) {
      if (index >= 0 && index < this.availableMagics.size()) {
         String id = this.availableMagics.get(index);
         return id == null ? "" : id;
      } else {
         return "";
      }
   }

   private static String normalizeDisplayName(String current, String fullName, String shortName, String selectedName, String magicId) {
      String display = current == null ? "" : current.trim();
      if (display.isEmpty()) {
         return fullName;
      } else if (display.equals(shortName) || display.equals(selectedName) || display.equals(magicId)) {
         return fullName;
      } else if (!fullName.isEmpty() && !shortName.isEmpty() && display.startsWith(shortName) && !display.startsWith(fullName)) {
         return fullName + display.substring(shortName.length());
      } else {
         return !fullName.isEmpty() && !selectedName.isEmpty() && display.startsWith(selectedName) && !display.startsWith(fullName)
            ? fullName + display.substring(selectedName.length())
            : display;
      }
   }

   private List<FormattedCharSequence> wrapText(String text, int maxWidth, int maxLines) {
      int safeWidth = Math.max(16, maxWidth);
      List<FormattedCharSequence> wrapped = new ArrayList<>(this.font.split(Component.literal(text), safeWidth));
      if (wrapped.isEmpty()) {
         wrapped.add(FormattedCharSequence.EMPTY);
         return wrapped;
      } else {
         return (List<FormattedCharSequence>)(maxLines > 0 && wrapped.size() > maxLines ? new ArrayList<>(wrapped.subList(0, maxLines)) : wrapped);
      }
   }

   private boolean isCrestMagic(int index) {
      return index >= 0 && index < this.crestSourceFlags.size() && Boolean.TRUE.equals(this.crestSourceFlags.get(index));
   }

   private String getCrestPresetHint(int index) {
      if (index >= 0 && index < this.crestPresetHints.size()) {
         String hint = this.crestPresetHints.get(index);
         return hint == null ? "" : hint.trim();
      } else {
         return "";
      }
   }

   private void drawSector(
      BufferBuilder buffer, Matrix4f matrix, float cx, float cy, double rIn, double rOut, float startAngle, float endAngle, int r, int g, int b, int a
   ) {
      float angleDiff = endAngle - startAngle;
      int steps = Math.max(1, (int)(angleDiff / 2.0F));

      for (int i = 0; i < steps; i++) {
         float a1 = startAngle + angleDiff * i / steps;
         float a2 = startAngle + angleDiff * (i + 1) / steps;
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
         buffer.addVertex(matrix, x1_in, y1_in, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x1_out, y1_out, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x2_out, y2_out, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x1_in, y1_in, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x2_out, y2_out, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x2_in, y2_in, 0.0F).setColor(r, g, b, a);
      }
   }

   private void drawCircleStroke(
      BufferBuilder buffer, Matrix4f matrix, float cx, float cy, double radius, float startAngle, float endAngle, int r, int g, int b, int a, float width
   ) {
      float angleDiff = endAngle - startAngle;
      int steps = Math.max(1, (int)(angleDiff / 2.0F));
      double halfWidth = width / 2.0;
      double rIn = radius - halfWidth;
      double rOut = radius + halfWidth;

      for (int i = 0; i < steps; i++) {
         float a1 = startAngle + angleDiff * i / steps;
         float a2 = startAngle + angleDiff * (i + 1) / steps;
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
         buffer.addVertex(matrix, x1_in, y1_in, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x1_out, y1_out, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x2_out, y2_out, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x1_in, y1_in, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x2_out, y2_out, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x2_in, y2_in, 0.0F).setColor(r, g, b, a);
      }
   }

   private void drawLine(
      BufferBuilder buffer, Matrix4f matrix, float cx, float cy, double rIn, double rOut, float angle, int r, int g, int b, int a, float width
   ) {
      double rad = Math.toRadians(angle);
      double cos = Math.cos(rad);
      double sin = Math.sin(rad);
      double perpX = -sin * (width / 2.0);
      double perpY = cos * (width / 2.0);
      float x1 = cx + (float)(cos * rIn);
      float y1 = cy + (float)(sin * rIn);
      float x2 = cx + (float)(cos * rOut);
      float y2 = cy + (float)(sin * rOut);
      buffer.addVertex(matrix, x1 + (float)perpX, y1 + (float)perpY, 0.0F).setColor(r, g, b, a);
      buffer.addVertex(matrix, x2 + (float)perpX, y2 + (float)perpY, 0.0F).setColor(r, g, b, a);
      buffer.addVertex(matrix, x2 - (float)perpX, y2 - (float)perpY, 0.0F).setColor(r, g, b, a);
      buffer.addVertex(matrix, x1 + (float)perpX, y1 + (float)perpY, 0.0F).setColor(r, g, b, a);
      buffer.addVertex(matrix, x2 - (float)perpX, y2 - (float)perpY, 0.0F).setColor(r, g, b, a);
      buffer.addVertex(matrix, x1 - (float)perpX, y1 - (float)perpY, 0.0F).setColor(r, g, b, a);
   }

   private void drawSectorStroke(
      BufferBuilder buffer,
      Matrix4f matrix,
      float cx,
      float cy,
      double rIn,
      double rOut,
      float startAngle,
      float endAngle,
      int r,
      int g,
      int b,
      int a,
      float width
   ) {
      this.drawCircleStroke(buffer, matrix, cx, cy, rIn, startAngle, endAngle, r, g, b, a, width);
      this.drawCircleStroke(buffer, matrix, cx, cy, rOut, startAngle, endAngle, r, g, b, a, width);
      this.drawLine(buffer, matrix, cx, cy, rIn, rOut, startAngle, r, g, b, a, width);
      this.drawLine(buffer, matrix, cx, cy, rIn, rOut, endAngle, r, g, b, a, width);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void tick() {
      super.tick();
      if (this.minecraft != null) {
         long window = this.minecraft.getWindow().getWindow();
         KeyMapping keyMapping = TypeMoonWorldModKeyMappings.CYCLE_MAGIC;
         Type type = keyMapping.getKey().getType();
         int code = keyMapping.getKey().getValue();
         boolean isDown = false;
         if (type == Type.KEYSYM) {
            isDown = GLFW.glfwGetKey(window, code) == 1;
         } else if (type == Type.MOUSE) {
            isDown = GLFW.glfwGetMouseButton(window, code) == 1;
         }

         if (!isDown) {
            this.onClose();
         }
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.selectedIndex != -1) {
         this.performAction();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.availableMagics.isEmpty()) {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      } else {
         int direction = (int)Math.signum(scrollY) * -1;
         if (direction != 0) {
            if (this.selectedIndex == -1) {
               this.selectedIndex = 0;
            }

            this.selectedIndex = (this.selectedIndex + direction + this.availableMagics.size()) % this.availableMagics.size();
            this.isScrollMode = true;
            this.scrollOriginX = mouseX;
            this.scrollOriginY = mouseY;
            this.performAction(false);
         }

         return true;
      }
   }

   private void performAction() {
      this.performAction(true);
   }

   private void performAction(boolean close) {
      if (this.minecraft != null && this.minecraft.player != null) {
         if (this.selectedIndex >= 0 && this.selectedIndex < this.availableMagics.size()) {
            PacketDistributor.sendToServer(new SwitchMagicIndexMessage(this.selectedIndex), new CustomPacketPayload[0]);
         }

         if (close) {
            this.onClose();
         }
      }
   }
}
