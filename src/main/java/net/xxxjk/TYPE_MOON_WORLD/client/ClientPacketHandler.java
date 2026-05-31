package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.LeylineSurveyMapScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.ProjectionPresetScreen;

public class ClientPacketHandler {
   public static void openProjectionGui() {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         Minecraft.getInstance().setScreen(new ProjectionPresetScreen(player));
      }
   }

   public static void openLeylineSurveyMap(int gridSize, int centerChunkX, int centerChunkZ, String dimensionId, byte[] concentrations) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.setScreen(new LeylineSurveyMapScreen(gridSize, centerChunkX, centerChunkZ, dimensionId, concentrations));
      }
   }
}
