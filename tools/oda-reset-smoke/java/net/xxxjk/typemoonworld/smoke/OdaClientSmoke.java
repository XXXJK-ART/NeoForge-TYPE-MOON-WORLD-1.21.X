package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.OdaProfileScreen;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.*;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@Mod(value = "typemoonworld_oda_smoke", dist = Dist.CLIENT)
public final class OdaClientSmoke {
   private boolean opened, done;
   private int ticks;
   private volatile String failure;
   private volatile boolean verified;
   public OdaClientSmoke() { NeoForge.EVENT_BUS.register(this); }
   @SubscribeEvent public void tick(ClientTickEvent.Post event) {
      var mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (failure != null) throw new AssertionError(failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished(); mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true; mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(4); mc.options.simulationDistance().set(5);
            mc.getWindow().setWindowed(1280, 900);
            require(new File(mc.gameDirectory, "saves/Oda-Old-Save/level.dat").isFile(), "Missing isolated old save copy");
            mc.createWorldOpenFlows().openWorld("Oda-Old-Save", () -> { throw new IllegalStateException("Old save failed to load"); });
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         if (ticks == 10) mc.getSingleplayerServer().execute(() -> {
            try { verify(mc); verified = true; } catch (Throwable error) { failure = error.toString(); }
         });
         if (ticks == 60) { require(verified, "Server checks incomplete"); mc.setScreen(new OdaProfileScreen(null)); }
         if (ticks == 70) capture(mc, "oda-profile-parameters");
         if (ticks == 75) mc.screen.mouseScrolled(300, 300, 0, -12);
         if (ticks == 80) capture(mc, "oda-profile-skills");
         if (ticks == 85) mc.screen.mouseScrolled(300, 300, 0, -30);
         if (ticks == 90) capture(mc, "oda-profile-noble-phantasms");
         if (ticks == 100) mc.setScreen(null);
         if (ticks == 110) capture(mc, "oda-hud");
         if (ticks >= 120) {
            done = true; TYPE_MOON_WORLD.LOGGER.info("ODA_CLIENT_SMOKE_PASS old_save=loaded flight=start,hover,stop,diagonal,vertical,sprint,collision profile=rendered"); mc.stop();
         }
      } catch (Throwable error) {
         done = true; TYPE_MOON_WORLD.LOGGER.error("ODA_CLIENT_SMOKE_FAIL ticks=" + ticks, error); mc.stop();
      }
   }
   private static void verify(Minecraft mc) {
      var server = mc.getSingleplayerServer(); var level = server.overworld();
      var player = server.getPlayerList().getPlayer(mc.player.getUUID());
      player.setGameMode(GameType.SURVIVAL);
      if (player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_transformed) ServantCardTransformManager.release(player, false);
      for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++) {
         level.setBlock(new BlockPos(x,100,z), Blocks.STONE.defaultBlockState(), 2);
         for (int y = 101; y <= 107; y++) level.setBlock(new BlockPos(x,y,z), Blocks.AIR.defaultBlockState(), 2);
      }
      player.teleportTo(.5, 101, .5); player.setYRot(0); player.setXRot(0);
      require(ServantCardTransformManager.transform(player,"oda_nobunaga"), "Transformation failed");
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servantCardState().setMana(1000);
      ServantCardFlightController.setInput(player, true, 0, 0, 0);
      require(player.getVehicle() instanceof OdaMatchlockGunEntity, "Flight did not create mount");
      var mount = (OdaMatchlockGunEntity)player.getVehicle();
      mount.setPos(.5,102,.5); mount.setDeltaMovement(Vec3.ZERO); mount.setMountInput(1,0,0);
      Vec3 start = mount.position(); mount.tick();
      double normal = mount.position().subtract(start).horizontalDistance();
      require(Math.abs(normal - .049) < .0001, "Default flight acceleration " + normal);
      mount.setDeltaMovement(Vec3.ZERO); mount.setMountInput(1,0,0); player.getPersistentData().putBoolean("OdaFlightSprint",true);
      start = mount.position(); mount.tick();
      require(Math.abs(mount.position().subtract(start).horizontalDistance() / normal - 2) < .001, "Sprint was not vanilla x2");
      player.getPersistentData().putBoolean("OdaFlightSprint",false);
      mount.setDeltaMovement(Vec3.ZERO); mount.setMountInput(1,1,0); start = mount.position(); mount.tick();
      Vec3 diagonal = mount.position().subtract(start);
      require(Math.abs(Math.abs(diagonal.x) - Math.abs(diagonal.z)) < .001 && Math.abs(diagonal.horizontalDistance() - .05) < .001, "Diagonal normalization failed");
      mount.setDeltaMovement(Vec3.ZERO); mount.setMountInput(0,0,1); start = mount.position(); mount.tick();
      require(Math.abs(mount.getY() - start.y - .15) < .001, "Ascending speed");
      mount.setDeltaMovement(Vec3.ZERO); mount.setMountInput(0,0,-1); start = mount.position(); mount.tick();
      require(Math.abs(mount.getY() - start.y + .15) < .001, "Descending speed");
      mount.setMountInput(0,0,0); mount.setDeltaMovement(new Vec3(.1,-.1,.1));
      for (int i = 0; i < 40; i++) mount.tick();
      double hover = mount.getY(); for (int i = 0; i < 10; i++) mount.tick();
      require(Math.abs(mount.getY() - hover) < .0001, "Released input continued sinking");
      for (int y = 101; y < 107; y++) for (int z = -3; z <= 3; z++) level.setBlock(new BlockPos(3,y,z), Blocks.STONE.defaultBlockState(),2);
      mount.setPos(1.5,102,.5); mount.setDeltaMovement(new Vec3(2,0,0)); mount.tick();
      require(mount.getBoundingBox().maxX <= 3.0001, "Mount passed through a wall");
      ServantCardFlightController.stop(player, vars, true);
      require(player.getVehicle() == null && !vars.servantCardState().flying(), "Flight did not stop");
      player.teleportTo(.5,101,.5); player.setYRot(0); player.setXRot(0);
      vars.syncPlayerVariables(player);
   }
   private static void capture(Minecraft mc, String name) {
      File directory = new File(mc.gameDirectory,"qa"); directory.mkdirs();
      Screenshot.grab(directory,name + ".png",mc.getMainRenderTarget(),ignored -> {});
   }
   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
