package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantGuardPoseClient;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;

@Mod(value = "typemoonworld_guard_smoke", dist = Dist.CLIENT)
public final class ServantGuardClientSmoke {
   private static final String WORLD = "Servant-Guard-QA";
   private boolean opened, done;
   private int ticks;
   private volatile String failure;

   public ServantGuardClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent public void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (failure != null) throw new AssertionError(failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished(); mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true; mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(3); mc.options.simulationDistance().set(3);
            mc.getWindow().setWindowed(1100, 760);
            if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile()) {
               mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA save failed to open"); });
            } else {
               mc.createWorldOpenFlows().createFreshLevel(WORLD,
                  new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                  new WorldOptions(42, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                     .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
            }
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         if (ticks == 10) server(mc, () -> {
            var server = mc.getSingleplayerServer(); var level = server.overworld();
            var player = server.getPlayerList().getPlayer(mc.player.getUUID());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            level.setDayTime(6000);
            for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
               level.setBlock(new BlockPos(x, 100, z), Blocks.BEDROCK.defaultBlockState(), 2);
               for (int y = 101; y < 106; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
            player.teleportTo(0.5, 101, 0.5); player.setYRot(180);
            require(ServantCardTransformManager.transform(player, "oda_nobunaga"), "transform failed");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
         });
         // Refresh the server hold packet without depending on physical keyboard input.
         if (ticks >= 25 && ticks <= 65 && ticks % 4 == 1) server(mc, () -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            require(ServantCombatDefenseService.setGuarding(player, true), "guard rejected");
         });
         if (ticks == 44 || ticks == 60) {
            require(ServantGuardPoseClient.isGuarding(mc.player), "server guard pose did not synchronize");
            var model = ((PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(mc.player)).getModel();
            model.setupAnim(mc.player, 0, 0, mc.player.tickCount, 0, 0);
            require(Math.abs(model.rightArm.xRot + 1.05F) < .001F, "right arm is not in servant guard pose");
            require(Math.abs(model.leftArm.yRot - .55F) < .001F, "left arm is not in servant guard pose");
            require(Math.abs(model.rightSleeve.xRot - model.rightArm.xRot) < .001F, "guard sleeve detached");
            if (ticks == 44) {
               capture(mc, "guard-first-person"); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            } else capture(mc, "guard-third-person");
         }
         if (ticks == 68) server(mc, () -> ServantCombatDefenseService.setGuarding(
            mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()), false));
         if (ticks == 77) {
            require(!ServantGuardPoseClient.isGuarding(mc.player), "guard pose remained after release");
            TYPE_MOON_WORLD.LOGGER.info("SERVANT_GUARD_CLIENT_SMOKE_PASS synced=true pose=arms,sleeves first_person=true release=true");
            done = true; mc.stop();
         }
         require(ticks < 200, "guard client smoke timed out");
      } catch (Throwable error) {
         done = true; TYPE_MOON_WORLD.LOGGER.error("SERVANT_GUARD_CLIENT_SMOKE_FAIL ticks=" + ticks, error); mc.stop();
      }
   }

   private void server(Minecraft mc, Runnable work) {
      mc.getSingleplayerServer().execute(() -> { try { work.run(); } catch (Throwable error) { failure = error.toString(); } });
   }

   private static void capture(Minecraft mc, String name) {
      File directory = new File(mc.gameDirectory, "qa"); directory.mkdirs();
      Screenshot.grab(directory, name + ".png", mc.getMainRenderTarget(), ignored -> {});
   }

   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
