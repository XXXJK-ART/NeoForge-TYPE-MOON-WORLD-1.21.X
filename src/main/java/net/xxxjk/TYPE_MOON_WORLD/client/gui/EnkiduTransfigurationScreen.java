package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.network.EnkiduTransfigurationPointMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class EnkiduTransfigurationScreen extends Screen {
   private static final String[] KEYS = {"strength", "endurance", "agility", "mana", "luck"};
   private static final int[] COLORS = {0xFFE57373, 0xFF81C784, 0xFF64B5F6, 0xFFBA68C8, 0xFFFFD54F};

   public EnkiduTransfigurationScreen() {
      super(Component.translatable("gui.typemoonworld.enkidu_transfiguration.title"));
   }

   @Override
   protected void init() {
      int x = this.width / 2 - 95;
      int y = this.height / 2 - 58;
      for (int i = 0; i < KEYS.length; i++) {
         final int stat = i;
         this.addRenderableWidget(Button.builder(Component.literal("-"), button -> adjust(stat, -1)).bounds(x + 122, y + i * 24, 24, 20).build());
         this.addRenderableWidget(Button.builder(Component.literal("+"), button -> adjust(stat, 1)).bounds(x + 150, y + i * 24, 24, 20).build());
      }
   }

   @Override
   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui, mouseX, mouseY, partialTick);
      gui.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 82, 0xFFE8F5E9);
      int[] points = points();
      int x = this.width / 2 - 95;
      int y = this.height / 2 - 56;
      for (int i = 0; i < KEYS.length; i++) {
         gui.fill(x - 6, y + i * 24 - 2, x + 116, y + i * 24 + 18, 0x66000000 | (COLORS[i] & 0x00FFFFFF));
         Component label = Component.translatable("gui.typemoonworld.enkidu_transfiguration." + KEYS[i], points[i], rank(points[i]));
         gui.drawString(this.font, label, x, y + i * 24 + 4, 0xFFFFFFFF, true);
      }
      super.render(gui, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void tick() {
      super.tick();
      if (!TypeMoonWorldModKeyMappings.ENKIDU_TRANSFIGURATION_WHEEL.isDown()) {
         this.onClose();
      }
   }

   private static void adjust(int stat, int delta) {
      PacketDistributor.sendToServer(new EnkiduTransfigurationPointMessage(stat, delta), new CustomPacketPayload[0]);
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
            result[i] = Integer.parseInt(parts[i]);
         } catch (NumberFormatException ignored) {
            result[i] = 6;
         }
      }
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
