package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicWheelMessage;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class MagicWheelSwitchScreen extends Screen {
   private static final int WHEEL_COUNT = 10;
   private final int currentWheel;
   private int selectedWheel = -1;
   private boolean selectionSent = false;

   public MagicWheelSwitchScreen(int currentWheel) {
      super(Component.translatable("gui.typemoonworld.magic_wheel_switch"));
      this.currentWheel = currentWheel;
      this.selectedWheel = currentWheel;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void tick() {
      super.tick();
      if (this.minecraft != null) {
         long window = this.minecraft.getWindow().getWindow();
         Key key = TypeMoonWorldModKeyMappings.MAGIC_WHEEL_SWITCH.getKey();
         boolean down = false;
         if (key.getType() == Type.KEYSYM) {
            down = GLFW.glfwGetKey(window, key.getValue()) == 1;
         } else if (key.getType() == Type.MOUSE) {
            down = GLFW.glfwGetMouseButton(window, key.getValue()) == 1;
         }

         if (!down) {
            this.commitSelection();
            this.onClose();
         }
      }
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int centerX = this.width / 2;
      int centerY = (int)(this.height * 0.45F);
      double radius = Math.min(this.width, this.height) / 2.6;
      double innerRadius = radius * 0.4;
      float angleStep = 36.0F;
      double dx = mouseX - centerX;
      double dy = mouseY - centerY;
      double dist = Math.sqrt(dx * dx + dy * dy);
      if (dist > innerRadius) {
         double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
         if (angleDeg < 0.0) {
            angleDeg += 360.0;
         }

         angleDeg += 90.0;
         if (angleDeg >= 360.0) {
            angleDeg -= 360.0;
         }

         this.selectedWheel = (int)(angleDeg / angleStep);
      }

      guiGraphics.fill(0, 0, this.width, this.height, 1241513984);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
      Matrix4f matrix = guiGraphics.pose().last().pose();
      drawSector(buffer, matrix, centerX, centerY, innerRadius, radius, 0.0F, 360.0F, 30, 30, 30, 120);

      for (int i = 0; i < 10; i++) {
         float start = i * angleStep - 90.0F;
         float end = (i + 1) * angleStep - 90.0F;
         if (i == this.selectedWheel) {
            drawSector(buffer, matrix, centerX, centerY, innerRadius, radius + 14.0, start, end, 0, 200, 255, 230);
         } else if (i == this.currentWheel) {
            drawSector(buffer, matrix, centerX, centerY, innerRadius, radius, start, end, 40, 130, 210, 190);
         }
      }

      BufferUploader.drawWithShader(buffer.buildOrThrow());
      BufferBuilder lineBuffer = tesselator.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
      drawSector(lineBuffer, matrix, centerX, centerY, radius - 2.0, radius + 2.0, 0.0F, 360.0F, 220, 220, 220, 120);
      drawSector(lineBuffer, matrix, centerX, centerY, innerRadius - 2.0, innerRadius + 2.0, 0.0F, 360.0F, 220, 220, 220, 120);

      for (int ix = 0; ix < 10; ix++) {
         float lineAngle = ix * angleStep - 90.0F;
         double rad = Math.toRadians(lineAngle);
         float x1 = centerX + (float)(Math.cos(rad) * innerRadius);
         float y1 = centerY + (float)(Math.sin(rad) * innerRadius);
         float x2 = centerX + (float)(Math.cos(rad) * radius);
         float y2 = centerY + (float)(Math.sin(rad) * radius);
         float px = (float)(-Math.sin(rad) * 1.2);
         float py = (float)(Math.cos(rad) * 1.2);
         lineBuffer.addVertex(matrix, x1 + px, y1 + py, 0.0F).setColor(180, 180, 180, 120);
         lineBuffer.addVertex(matrix, x2 + px, y2 + py, 0.0F).setColor(180, 180, 180, 120);
         lineBuffer.addVertex(matrix, x2 - px, y2 - py, 0.0F).setColor(180, 180, 180, 120);
         lineBuffer.addVertex(matrix, x1 + px, y1 + py, 0.0F).setColor(180, 180, 180, 120);
         lineBuffer.addVertex(matrix, x2 - px, y2 - py, 0.0F).setColor(180, 180, 180, 120);
         lineBuffer.addVertex(matrix, x1 - px, y1 - py, 0.0F).setColor(180, 180, 180, 120);
      }

      BufferUploader.drawWithShader(lineBuffer.buildOrThrow());

      for (int ix = 0; ix < 10; ix++) {
         float start = ix * angleStep - 90.0F;
         double midRad = Math.toRadians(start + angleStep / 2.0F);
         int textX = centerX + (int)(Math.cos(midRad) * (radius * 0.75));
         int textY = centerY + (int)(Math.sin(midRad) * (radius * 0.75));
         int color = ix == this.selectedWheel ? -1 : (ix == this.currentWheel ? -7416065 : -5592406);
         guiGraphics.drawCenteredString(this.font, String.valueOf(ix), textX, textY, color);
      }

      guiGraphics.drawCenteredString(
         this.font, Component.translatable("gui.typemoonworld.magic_wheel.current", new Object[]{this.currentWheel}), centerX, centerY - 5, -16711681
      );
      guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 18, -1250068);
      RenderSystem.disableBlend();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.selectedWheel >= 0 && this.selectedWheel < 10) {
         this.commitSelection();
         this.onClose();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private void commitSelection() {
      if (!this.selectionSent) {
         if (this.selectedWheel >= 0 && this.selectedWheel < 10) {
            if (this.selectedWheel == this.currentWheel) {
               this.selectionSent = true;
            } else {
               PacketDistributor.sendToServer(new SwitchMagicWheelMessage(this.selectedWheel), new CustomPacketPayload[0]);
               this.selectionSent = true;
            }
         }
      }
   }

   private static void drawSector(
      BufferBuilder buffer, Matrix4f matrix, float cx, float cy, double rIn, double rOut, float startAngle, float endAngle, int r, int g, int b, int a
   ) {
      float diff = endAngle - startAngle;
      int steps = Math.max(1, (int)(Math.abs(diff) / 2.0F));

      for (int i = 0; i < steps; i++) {
         float a1 = startAngle + diff * i / steps;
         float a2 = startAngle + diff * (i + 1) / steps;
         double rad1 = Math.toRadians(a1);
         double rad2 = Math.toRadians(a2);
         float x1Out = cx + (float)(Math.cos(rad1) * rOut);
         float y1Out = cy + (float)(Math.sin(rad1) * rOut);
         float x2Out = cx + (float)(Math.cos(rad2) * rOut);
         float y2Out = cy + (float)(Math.sin(rad2) * rOut);
         float x1In = cx + (float)(Math.cos(rad1) * rIn);
         float y1In = cy + (float)(Math.sin(rad1) * rIn);
         float x2In = cx + (float)(Math.cos(rad2) * rIn);
         float y2In = cy + (float)(Math.sin(rad2) * rIn);
         buffer.addVertex(matrix, x1In, y1In, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x1Out, y1Out, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x2Out, y2Out, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x1In, y1In, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x2Out, y2Out, 0.0F).setColor(r, g, b, a);
         buffer.addVertex(matrix, x2In, y2In, 0.0F).setColor(r, g, b, a);
      }
   }
}
