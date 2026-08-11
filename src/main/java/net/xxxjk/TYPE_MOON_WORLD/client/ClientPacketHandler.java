package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.UUID;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.LeylineSurveyMapScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MedeaCraftSelectScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.MuramasaForgeSelectScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.EnkiduTransfigurationScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.ParacelsusCraftSelectScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.ParacelsusElementSelectScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.ProjectionPresetScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.GilgameshVaultScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.HundredFacesScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.PaleRiderScreen;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesOpenScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderOpenScreenMessage;

public class ClientPacketHandler {
   public static void openProjectionGui() {
      if (ReplayUiSuppressor.shouldSuppressTypeMoonScreens()) {
         return;
      }

      Player player = Minecraft.getInstance().player;
      if (player != null) {
         Minecraft.getInstance().setScreen(new ProjectionPresetScreen(player));
      }
   }

   public static void openLeylineSurveyMap(int gridSize, int centerChunkX, int centerChunkZ, String dimensionId, byte[] concentrations) {
      if (ReplayUiSuppressor.shouldSuppressTypeMoonScreens()) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.setScreen(new LeylineSurveyMapScreen(gridSize, centerChunkX, centerChunkZ, dimensionId, concentrations));
      }
   }

   public static void openMedeaCraftScreen(int dragonfangStock, int manaCharmStock, int healCharmStock, int leylineMapStock) {
      if (ReplayUiSuppressor.shouldSuppressTypeMoonScreens()) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.setScreen(new MedeaCraftSelectScreen(dragonfangStock, manaCharmStock, healCharmStock, leylineMapStock));
      }
   }

   public static void openMuramasaForgeScreen() {
      if (!ReplayUiSuppressor.shouldSuppressTypeMoonScreens()) {
         Minecraft.getInstance().setScreen(new MuramasaForgeSelectScreen());
      }
   }

   public static void openParacelsusCraftScreen(int stoneStock, int diamondShieldStock, int leylineMapStock) {
      if (ReplayUiSuppressor.shouldSuppressTypeMoonScreens()) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.setScreen(new ParacelsusCraftSelectScreen(stoneStock, diamondShieldStock, leylineMapStock));
      }
   }

   public static void openParacelsusElementScreen() {
      if (ReplayUiSuppressor.shouldSuppressTypeMoonScreens()) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.setScreen(new ParacelsusElementSelectScreen());
      }
   }

   public static void openEnkiduTransfigurationScreen() {
      if (ReplayUiSuppressor.shouldSuppressTypeMoonScreens()) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.setScreen(new EnkiduTransfigurationScreen());
      }
   }

   public static void openGilgameshVaultScreen(int usedMask) {
      if (!ReplayUiSuppressor.shouldSuppressTypeMoonScreens()) Minecraft.getInstance().setScreen(new GilgameshVaultScreen(usedMask));
   }

   public static void openPaleRiderScreen(int kind, List<PaleRiderOpenScreenMessage.Target> targets) {
      if (ReplayUiSuppressor.shouldSuppressTypeMoonScreens()) return;
      Minecraft mc = Minecraft.getInstance();
      if (kind == 4 || kind == 5) {
         PaleRiderClientState.possessing = kind == 4;
         if (mc.player != null) mc.setCameraEntity(mc.player);
         return;
      }
      if (mc.player != null) mc.setScreen(new PaleRiderScreen(kind, targets));
   }

   public static void openHundredFacesScreen(int kind, List<HundredFacesOpenScreenMessage.Target> targets) {
      if (ReplayUiSuppressor.shouldSuppressTypeMoonScreens()) return;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) mc.setScreen(new HundredFacesScreen(kind, targets));
   }

   public static void handleMasterVisualState(UUID playerId, boolean masterActive, int commandSpells, String style, boolean poseActive) {
      CommandSpellVisualClient.apply(playerId, masterActive, commandSpells, style, poseActive);
   }

   public static void handleConcealmentState(UUID entityId, boolean concealed) {
      ObserverConcealmentClient.apply(entityId, concealed);
   }

   public static void handleEnkiduDetectionHighlight(List<Integer> entityIds, int ticks) {
      EnkiduDetectionHighlightClient.apply(entityIds, ticks);
   }

   public static void handleDuelScreenFlash(int ticks, float strength) {
      DuelScreenFlashClient.apply(ticks, strength);
   }
}
