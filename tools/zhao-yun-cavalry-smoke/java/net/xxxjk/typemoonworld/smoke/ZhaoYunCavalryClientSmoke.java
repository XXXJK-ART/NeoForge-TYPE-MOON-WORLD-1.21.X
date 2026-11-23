package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardZhaoYunSkills;

@Mod(value = "typemoonworld_zhao_yun_smoke", dist = Dist.CLIENT)
public final class ZhaoYunCavalryClientSmoke {
   private static final String WORLD = "Zhao-Yun-Cavalry-QA";
   private boolean opened, done, started;
   private int ticks, chants, legacyVoices;
   private SoundInstance chant;
   private volatile boolean released;
   private UUID horseUuid;

   public ZhaoYunCavalryClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent public void sound(PlaySoundEvent event) {
      var sound = event.getSound();
      if (sound == null) return;
      String path = sound.getLocation().getPath();
      if (path.equals("np_chant_zhao_yun_changbanpo")) { chants++; chant = sound; }
      if (path.equals("zhao_yun_voice_np")) legacyVoices++;
   }

   @SubscribeEvent public void tick(ClientTickEvent.Post event) {
      var mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished(); mc.setScreen(new TitleScreen());
         }
         if (!opened && mc.screen instanceof TitleScreen) open(mc);
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         if (!started) { started = true; setup(mc); }
         if (chant == null) {
            require(++ticks < 300, "chant never reached client");
            return;
         }
         ticks++;
         if (ticks == 40) {
            require(chant instanceof EntityBoundSoundInstance, "chant does not follow mounted caster");
            require(Math.abs(chant.getPitch() - 1.0F) < 0.001F, "chant still plays at altered pitch: " + chant.getPitch());
            require(mc.getSoundManager().isActive(chant), "chant stopped before NP release");
            require(mc.player.getVehicle() instanceof ZhaoYunHakuryuEntity, "player forcibly dismounted during chant");
            Screenshot.grab(mc.gameDirectory, "zhao-yun-mounted-chant.png", mc.getMainRenderTarget(), ignored -> { });
         }
         if (ticks == 180) {
            require(mc.getSoundManager().isActive(chant), "full-length voice cut out early");
            require(mc.player.getVehicle() instanceof ZhaoYunHakuryuEntity, "player lost horse before release");
         }
         if (ticks >= 300 && released) {
            require(chants == 1 && legacyVoices == 0, "duplicate NP audio: chants=" + chants + " legacy=" + legacyVoices);
            require(mc.player.getVehicle() != null && horseUuid.equals(mc.player.getVehicle().getUUID()), "client lost original horse at release");
            Screenshot.grab(mc.gameDirectory, "zhao-yun-mounted-np.png", mc.getMainRenderTarget(), ignored -> { });
            TYPE_MOON_WORLD.LOGGER.info("ZHAO_YUN_CAVALRY_CLIENT_PASS natural_pitch=1.0 chants=1 legacy_voices=0 same_horse=true release=true");
            done = true; mc.stop();
         }
         require(ticks < 440, "NP release did not reach client");
      } catch (Throwable error) { fail(mc, error); }
   }

   private void setup(Minecraft mc) {
      mc.getSingleplayerServer().execute(() -> {
         try {
            var server = mc.getSingleplayerServer(); var level = server.overworld();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            level.setDayTime(6000);
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-12, 100, -12), new BlockPos(12, 100, 75)))
               level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            var player = server.getPlayerList().getPlayer(mc.player.getUUID());
            player.connection.teleport(0.5, 101, 0.5, 0, 12);
            require(ServantCardTransformManager.transform(player, "zhao_yun_rider"), "card transform failed");
            var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            vars.servantCardState().setMaxMana(1000); vars.servantCardState().setMana(1000);
            require(ServantCardZhaoYunSkills.summonSkillHakuryu(player), "skill mount failed");
            horseUuid = player.getVehicle().getUUID();
            require(ServantCardTransformManager.triggerAction(player, 9), "mounted NP rejected");
            TYPE_MOON_WORLD.queueServerWork(265, () -> {
               try {
                  require(ServantCardZhaoYunSkills.isNpActive(player), "server failed to release NP");
                  require(player.getVehicle() != null && horseUuid.equals(player.getVehicle().getUUID()), "server replaced original horse");
                  released = true;
               } catch (Throwable error) { mc.execute(() -> fail(mc, error)); }
            });
         } catch (Throwable error) { mc.execute(() -> fail(mc, error)); }
      });
   }

   private void open(Minecraft mc) {
      opened = true; mc.options.pauseOnLostFocus = false;
      mc.options.renderDistance().set(4); mc.options.simulationDistance().set(4);
      mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
      mc.getWindow().setWindowed(1280, 900);
      if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile()) {
         mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA save failed"); });
      } else {
         mc.createWorldOpenFlows().createFreshLevel(WORLD,
            new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(42, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
               .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
      }
   }

   private void fail(Minecraft mc, Throwable error) {
      TYPE_MOON_WORLD.LOGGER.error("ZHAO_YUN_CAVALRY_CLIENT_FAIL tick=" + ticks, error);
      done = true; mc.stop();
   }
   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
