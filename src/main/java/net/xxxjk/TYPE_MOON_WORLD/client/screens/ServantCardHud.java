package net.xxxjk.TYPE_MOON_WORLD.client.screens;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.xxxjk.TYPE_MOON_WORLD.client.ReplayUiSuppressor;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;

@EventBusSubscriber({Dist.CLIENT})
public class ServantCardHud {
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed) {
         return;
      }
      ResourceLocation layer = event.getName();
      if (VanillaGuiLayers.PLAYER_HEALTH.equals(layer) || VanillaGuiLayers.ARMOR_LEVEL.equals(layer) || VanillaGuiLayers.FOOD_LEVEL.equals(layer)) {
         event.setCanceled(true);
      }
   }

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
      int guiWidth = gui.guiWidth();
      int guiHeight = gui.guiHeight();
      if (isSurvivalLike(minecraft)) {
         int statusWidth = 81;
         int leftStatusX = guiWidth / 2 - 91;
         int baseStatusY = guiHeight - 39;
         drawBar(gui, minecraft, leftStatusX, baseStatusY, statusWidth, "HP", player.getHealth(), player.getMaxHealth(), 0xFFB71C1C, 0xFFE53935);
         drawBar(gui, minecraft, leftStatusX, baseStatusY - 10, statusWidth, "DEF", player.getArmorValue(), 20.0, 0xFF607D8B, 0xFFECEFF1);
      }

      int manaX = 10;
      int manaY = guiHeight - 20;
      drawBar(gui, minecraft, manaX, manaY, 120, "MP", vars.servant_card_mana, vars.servant_card_max_mana, 0xFF00838F, 0xFF00E5FF);
      Player master = findMaster(minecraft, vars);
      if (master != null) {
         TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         drawBar(gui, minecraft, manaX, manaY - 11, 120, "Master", masterVars.player_mana, masterVars.player_max_mana, 0xFF6A1B9A, 0xFFCE93D8);
      }

      int x = 6;
      int y = 10;
      String servant = vars.servant_card_id == null || vars.servant_card_id.isBlank() ? "servant" : vars.servant_card_id;
      Component servantName = "servant".equals(servant) ? Component.literal(servant) : Component.translatable("item.typemoonworld.servant_card_" + servant);
      drawScaledString(gui, minecraft, servantName, x, y + 38, 0xFFFFFFFF, 0.72F);
      drawScaledString(
         gui,
         minecraft,
         Component.translatable("hud.typemoonworld.servant_card.jump_np", vars.servant_card_jump_charges, ticksToSeconds(vars.servant_card_np_cooldown)),
         x,
         y + 48,
         0xFFE0E0E0,
         0.62F
      );
      drawCooldownGrid(gui, minecraft, vars, 5, 68);
      drawMedeaStocks(gui, minecraft, vars, guiWidth, 36);
   }

   private static boolean isSurvivalLike(Minecraft minecraft) {
      if (minecraft.gameMode == null) {
         return true;
      }
      GameType mode = minecraft.gameMode.getPlayerMode();
      return mode == GameType.SURVIVAL || mode == GameType.ADVENTURE;
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
      int height = 8;
      gui.fill(x, y, x + width, y + height, 0x90000000);
      if (max > 0.0) {
         int fill = (int)(width * Math.max(0.0, Math.min(1.0, value / max)));
         gui.fillGradient(x, y, x + fill, y + height, startColor, endColor);
      }
      gui.renderOutline(x - 1, y - 1, width + 2, height + 2, 0xCCFFFFFF);
      String text = label + " " + (int)value + "/" + (int)Math.max(0.0, max);
      float scale = 0.62F;
      int textX = x + (int)((width - minecraft.font.width(text) * scale) / 2.0F);
      drawScaledString(gui, minecraft, Component.literal(text), textX, y + 1, 0xFFFFFFFF, scale);
   }

   private static String ticksToSeconds(int ticks) {
      if (ticks <= 0) {
         return Component.translatable("hud.typemoonworld.servant_card.ready").getString();
      }
      return String.format(java.util.Locale.ROOT, "%.1fs", ticks / 20.0F);
   }

   private static void drawCooldownGrid(GuiGraphics gui, Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars, int x, int y) {
      int[] cooldowns = parseCooldowns(vars.servant_card_skill_cooldowns);
      for (int i = 0; i < 10; i++) {
         int drawX = x;
         int drawY = y + i * 8;
         int ticks = i == 9 ? vars.servant_card_np_cooldown : cooldowns[i];
         String skillKey = ServantCardTransformManager.skillTranslationKey(vars.servant_card_id, i, false);
         boolean empty = skillKey.isBlank();
         Component label = empty
            ? Component.translatable("hud.typemoonworld.servant_card.none")
            : (ticks <= 0 ? Component.translatable(skillKey) : Component.literal(ticksToSeconds(ticks)));
         Component text = Component.literal(i + ":").append(label);
         int color = empty ? 0xFF888888 : ticks <= 0 ? 0xFFD8F8D8 : 0xFFFFD180;
         gui.fill(drawX - 1, drawY - 1, drawX + 36, drawY + 6, 0x44000000);
         drawScaledString(gui, minecraft, text, drawX + 1, drawY - 1, color, 0.58F);
      }
   }

   private static void drawMedeaStocks(GuiGraphics gui, Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars, int guiWidth, int y) {
      if (!"medea".equals(vars.servant_card_id)) {
         return;
      }
      int x = guiWidth - 66;
      gui.fill(x - 3, y - 4, guiWidth - 5, y + 31, 0x44000000);
      drawScaledString(gui, minecraft, Component.translatable("hud.typemoonworld.servant_card.medea_items"), x, y - 2, 0xFFE6D8FF, 0.62F);
      drawScaledString(
         gui,
         minecraft,
         Component.translatable("hud.typemoonworld.servant_card.medea_dragonfang_count", vars.servant_card_medea_dragonfang_stock),
         x,
         y + 8,
         0xFFD8F8FF,
         0.58F
      );
      drawScaledString(
         gui,
         minecraft,
         Component.translatable("hud.typemoonworld.servant_card.medea_mana_charm_count", vars.servant_card_medea_mana_charm_stock),
         x,
         y + 17,
         0xFFBFEFFF,
         0.58F
      );
      drawScaledString(
         gui,
         minecraft,
         Component.translatable("hud.typemoonworld.servant_card.medea_heal_charm_count", vars.servant_card_medea_heal_charm_stock),
         x,
         y + 26,
         0xFFC8FFC8,
         0.58F
      );
   }

   private static void drawScaledString(GuiGraphics gui, Minecraft minecraft, Component text, int x, int y, int color, float scale) {
      gui.pose().pushPose();
      gui.pose().scale(scale, scale, 1.0F);
      gui.drawString(minecraft.font, text, (int)(x / scale), (int)(y / scale), color, true);
      gui.pose().popPose();
   }

   private static int[] parseCooldowns(String raw) {
      int[] result = new int[9];
      if (raw == null || raw.isBlank()) {
         return result;
      }
      String[] parts = raw.split(",");
      for (int i = 0; i < result.length && i < parts.length; i++) {
         try {
            result[i] = Math.max(0, Integer.parseInt(parts[i]));
         } catch (NumberFormatException ignored) {
            result[i] = 0;
         }
      }
      return result;
   }
}
