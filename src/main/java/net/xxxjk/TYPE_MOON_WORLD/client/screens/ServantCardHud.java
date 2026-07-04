package net.xxxjk.TYPE_MOON_WORLD.client.screens;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.xxxjk.TYPE_MOON_WORLD.client.ReplayUiSuppressor;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber({Dist.CLIENT})
public class ServantCardHud {
   @SubscribeEvent(priority = EventPriority.NORMAL)
   public static void onRenderGui(RenderGuiEvent.Pre event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.options.hideGui || ReplayUiSuppressor.shouldHideTypeMoonHud()) {
         return;
      }
      Player player = minecraft.player;
      if (player == null) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed) {
         return;
      }

      GuiGraphics gui = event.getGuiGraphics();
      int x = 10;
      int y = 10;
      int width = 142;
      drawBar(gui, minecraft, x, y, width, "HP", player.getHealth(), player.getMaxHealth(), 0xFFB71C1C, 0xFFE53935);
      drawBar(gui, minecraft, x, y + 14, width, "MP", vars.servant_card_mana, vars.servant_card_max_mana, 0xFF00838F, 0xFF00E5FF);
      Player master = findMaster(minecraft, vars);
      if (master != null) {
         TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         drawBar(gui, minecraft, x, y + 28, width, "Master", masterVars.player_mana, masterVars.player_max_mana, 0xFF6A1B9A, 0xFFCE93D8);
      }

      String servant = vars.servant_card_id == null || vars.servant_card_id.isBlank() ? "servant" : vars.servant_card_id;
      gui.drawString(minecraft.font, Component.literal(servant), x, y + 44, 0xFFFFFFFF, true);
      gui.drawString(
         minecraft.font,
         Component.literal("Jump " + vars.servant_card_jump_charges + "/4  NP " + ticksToSeconds(vars.servant_card_np_cooldown)),
         x,
         y + 55,
         0xFFE0E0E0,
         true
      );
   }

   private static Player findMaster(Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (minecraft.level == null || vars.servant_card_master_uuid == null || vars.servant_card_master_uuid.isBlank()) {
         return null;
      }
      try {
         UUID uuid = UUID.fromString(vars.servant_card_master_uuid);
         return minecraft.level.getPlayerByUUID(uuid);
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }

   private static void drawBar(GuiGraphics gui, Minecraft minecraft, int x, int y, int width, String label, double value, double max, int startColor, int endColor) {
      int height = 10;
      gui.fill(x, y, x + width, y + height, 0x90000000);
      if (max > 0.0) {
         int fill = (int)(width * Math.max(0.0, Math.min(1.0, value / max)));
         gui.fillGradient(x, y, x + fill, y + height, startColor, endColor);
      }
      gui.renderOutline(x - 1, y - 1, width + 2, height + 2, 0xCCFFFFFF);
      String text = label + " " + (int)value + "/" + (int)Math.max(0.0, max);
      int textX = x + (width - minecraft.font.width(text)) / 2;
      gui.drawString(minecraft.font, text, textX, y + 1, 0xFFFFFFFF, true);
   }

   private static String ticksToSeconds(int ticks) {
      if (ticks <= 0) {
         return "ready";
      }
      return String.format(java.util.Locale.ROOT, "%.1fs", ticks / 20.0F);
   }
}
