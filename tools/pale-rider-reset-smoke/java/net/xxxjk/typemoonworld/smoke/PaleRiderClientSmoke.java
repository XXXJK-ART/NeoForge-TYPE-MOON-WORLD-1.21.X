package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;

@Mod(value = "typemoonworld_palerider_smoke", dist = Dist.CLIENT)
public final class PaleRiderClientSmoke {
   private static final String WORLD = "Pale-Rider-QA";
   private boolean opened, submitted, done;
   private int ticks;
   private volatile boolean passed;
   private volatile Throwable failure;

   public PaleRiderClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent
   public void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (failure != null) throw new AssertionError("integrated server checks failed", failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished();
            mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(3);
            mc.options.simulationDistance().set(4);
            if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile())
               mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA save failed"); });
            else mc.createWorldOpenFlows().createFreshLevel(WORLD,
               new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
               new WorldOptions(42, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                  .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         if (++ticks > 400) throw new AssertionError("integrated server checks timed out");
         if (ticks >= 20 && !submitted) {
            submitted = true;
            mc.getSingleplayerServer().execute(() -> {
               try {
                  var server = mc.getSingleplayerServer();
                  var level = server.overworld();
                  level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                  var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                  player.setGameMode(GameType.SURVIVAL);
                  for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++)
                     level.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
                  player.teleportTo(0.5, 101, 0.5);
                  if (player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_transformed)
                     ServantCardTransformManager.release(player, false);
                  require(ServantCardTransformManager.transform(player, "pale_rider"), "card transform failed");
                  var rider = ModEntities.PALE_RIDER.get().spawn(level, new BlockPos(5, 101, 0), MobSpawnType.COMMAND);
                  require(rider != null, "NPC spawn failed");
                  rider.setNoAi(true);
                  close(player.getMaxHealth(), 500.0, "card health");
                  close(rider.getMaxHealth(), 500.0, "NPC health");
                  close(MagicResistanceHelper.getDamageReduction(player),
                     MagicResistanceHelper.damageReductionForRank(MagicResistanceRank.C), "card shared C resistance");
                  close(MagicResistanceHelper.getDamageReduction(rider),
                     MagicResistanceHelper.getDamageReduction(player), "NPC shared C resistance");
                  var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                  vars.servantCardState().setMana(500.0);
                  require(ServantCardPaleRiderSkills.spawn(player, 0), "rat spawn failed");
                  require(ServantCardPaleRiderSkills.spawn(player, 1), "crow spawn failed");
                  require(ServantCardPaleRiderSkills.toggleUnderworld(player), "underworld failed");
                  require(ServantCardPaleRiderSkills.toggleCalamity(player), "calamity failed");
                  passed = true;
               } catch (Throwable error) { failure = error; }
            });
         }
         if (passed && ticks >= 80) {
            require(mc.player.getMaxHealth() == 500.0F, "card health did not synchronize");
            TYPE_MOON_WORLD.LOGGER.info("PALE_RIDER_CLIENT_SMOKE_PASS health=500 shared_resistance=C rats=true crows=true domains=true");
            done = true;
            mc.stop();
         }
      } catch (Throwable error) {
         done = true;
         TYPE_MOON_WORLD.LOGGER.error("PALE_RIDER_CLIENT_SMOKE_FAIL", error);
         mc.stop();
      }
   }

   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
   private static void close(double actual, double expected, String message) {
      require(Math.abs(actual - expected) < 0.001, message + ": " + actual + " != " + expected);
   }
}
