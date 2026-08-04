package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.EnkiduTransfigurationSetMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class EnkiduTransfigurationScreen extends Screen {
   private static final String[] KEYS = {"strength", "endurance", "agility", "mana", "luck"};
   private static final int[] COLORS = {0xFFE57373, 0xFF81C784, 0xFF64B5F6, 0xFFBA68C8, 0xFFFFD54F};
   private static final int TOTAL_POINTS = 30;
   private static final int RADIUS = 74;
   private static final int INNER_RADIUS = 20;
   private int[] localPoints = new int[]{6, 6, 6, 6, 6};
   private boolean submitted = false;

   public EnkiduTransfigurationScreen() {
      super(Component.translatable("gui.typemoonworld.enkidu_transfiguration.title"));
   }

   @Override
   protected void init() {
      this.localPoints = points();
   }

   @Override
   public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
   }

   @Override
   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      int centerX = this.width / 2;
      int centerY = this.height / 2 + 8;
      int panelX = centerX - 126;
      int panelY = centerY - 111;
      GuiUtils.renderScreenBackdrop(gui, this.width, this.height);
      GuiUtils.renderArcaneWindow(gui, panelX, panelY, 252, 222, GuiUtils.ARCANE_VALID);
      for (int y = -RADIUS; y <= RADIUS; y += 2) {
         for (int x = -RADIUS; x <= RADIUS; x += 2) {
            int distSqr = x * x + y * y;
            if (distSqr <= RADIUS * RADIUS && distSqr >= INNER_RADIUS * INNER_RADIUS) {
               int sector = sectorFor(centerX + x, centerY + y, centerX, centerY);
               int alpha = sector == sectorFor(mouseX, mouseY, centerX, centerY) ? 0x90000000 : 0x58000000;
               gui.fill(centerX + x, centerY + y, centerX + x + 2, centerY + y + 2, alpha | (COLORS[sector] & 0x00FFFFFF));
            }
         }
      }
      gui.drawCenteredString(this.font, this.title, centerX, panelY + 9, GuiUtils.ARCANE_TEXT);
      gui.renderOutline(centerX - RADIUS, centerY - RADIUS, RADIUS * 2, RADIUS * 2, 0x6653C58B);
      int remaining = Math.max(0, TOTAL_POINTS - sum(this.localPoints));
      gui.fill(centerX - 24, centerY - 9, centerX + 24, centerY + 9, GuiUtils.ARCANE_BACKGROUND);
      gui.renderOutline(centerX - 24, centerY - 9, 48, 18, GuiUtils.ARCANE_VALID);
      gui.drawCenteredString(this.font, Component.translatable("gui.typemoonworld.enkidu_transfiguration.remaining", remaining), centerX, centerY - 4, GuiUtils.ARCANE_TEXT);
      for (int i = 0; i < KEYS.length; i++) {
         double angle = -Math.PI / 2.0 + (i + 0.5) * Math.PI * 2.0 / KEYS.length;
         int labelX = centerX + (int)(Math.cos(angle) * 96.0);
         int labelY = centerY + (int)(Math.sin(angle) * 96.0);
         Component label = Component.translatable("gui.typemoonworld.enkidu_transfiguration." + KEYS[i], this.localPoints[i], rank(this.localPoints[i]));
         gui.drawCenteredString(this.font, label, labelX, labelY, 0xFFFFFFFF);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int centerX = this.width / 2;
      int centerY = this.height / 2 + 8;
      double dx = mouseX - centerX;
      double dy = mouseY - centerY;
      double distSqr = dx * dx + dy * dy;
      if (distSqr > RADIUS * RADIUS || distSqr < INNER_RADIUS * INNER_RADIUS) {
         if (button != 0 && button != 1) {
            this.onClose();
         }
         return true;
      }
      int stat = sectorFor(mouseX, mouseY, centerX, centerY);
      if (button == 0) {
         if (sum(this.localPoints) < TOTAL_POINTS) {
            this.localPoints[stat]++;
         }
      } else if (button == 1 && this.localPoints[stat] > 0) {
         this.localPoints[stat]--;
      }
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.onClose();
      return true;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void removed() {
      submit();
      super.removed();
   }

   private void submit() {
      if (this.submitted) {
         return;
      }
      this.submitted = true;
      averageRemaining(this.localPoints);
      PacketDistributor.sendToServer(
         new EnkiduTransfigurationSetMessage(this.localPoints[0], this.localPoints[1], this.localPoints[2], this.localPoints[3], this.localPoints[4]),
         new CustomPacketPayload[0]
      );
   }

   private static int sectorFor(double mouseX, double mouseY, int centerX, int centerY) {
      double angle = Math.atan2(mouseY - centerY, mouseX - centerX) + Math.PI / 2.0;
      if (angle < 0.0) {
         angle += Math.PI * 2.0;
      }
      return Math.min(4, (int)(angle / (Math.PI * 2.0 / KEYS.length)));
   }

   private static int sum(int[] points) {
      int total = 0;
      for (int point : points) {
         total += Math.max(0, point);
      }
      return total;
   }

   private static void averageRemaining(int[] points) {
      int remaining = Math.max(0, TOTAL_POINTS - sum(points));
      int index = 0;
      while (remaining-- > 0) {
         points[index++ % points.length]++;
      }
   }

   private static int[] points() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return new int[]{6, 6, 6, 6, 6};
      }
      TypeMoonWorldModVariables.PlayerVariables vars = mc.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int[] result = new int[]{6, 6, 6, 6, 6};
      String[] parts = (vars.servant_card_enkidu_transfiguration_points == null ? "" : vars.servant_card_enkidu_transfiguration_points).split(",");
      for (int i = 0; i < result.length && i < parts.length; i++) {
         try {
            result[i] = Math.max(0, Integer.parseInt(parts[i]));
         } catch (NumberFormatException ignored) {
            result[i] = 6;
         }
      }
      averageRemaining(result);
      return result;
   }

   private static String rank(int points) {
      if (points < 3) {
         return "E-";
      }
      if (points < 4) {
         return "E";
      }
      if (points < 5) {
         return "D";
      }
      if (points < 6) {
         return "C";
      }
      if (points < 7) {
         return "B";
      }
      if (points < 9) {
         return "A";
      }
      return "A+";
   }
}
