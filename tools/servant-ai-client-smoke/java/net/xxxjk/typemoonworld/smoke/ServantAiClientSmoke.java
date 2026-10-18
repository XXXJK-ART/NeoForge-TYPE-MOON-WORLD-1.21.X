package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;

/** Only compiled by the opt-in init script, never packaged with the mod. */
@Mod(value = "typemoonworld_ai_smoke", dist = Dist.CLIENT)
public final class ServantAiClientSmoke {
   private static final String WORLD = "Servant-AI-QA";
   private boolean opened, done, captured;
   private int ticks;
   private UUID gilUuid, enemyUuid;
   private volatile String failure;
   private volatile boolean flightVerified;

   public ServantAiClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent
   public void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (failure != null) throw new AssertionError(failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished(); mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true; mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(4); mc.options.simulationDistance().set(4);
            mc.getWindow().setWindowed(1280, 720);
            if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile()) {
               mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA world failed to open"); });
            } else {
               mc.createWorldOpenFlows().createFreshLevel(WORLD,
                  new LevelSettings(WORLD, GameType.SPECTATOR, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                  new WorldOptions(42L, false, false),
                  registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
            }
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         if (ticks == 1) onServer(mc, () -> initialize(mc));
         if (ticks == 60) onServer(mc, () -> {
            var level = mc.getSingleplayerServer().overworld();
            var gil = (GilgameshEntity)level.getEntity(gilUuid);
            gil.setTarget((net.minecraft.world.entity.LivingEntity)level.getEntity(enemyUuid));
         });
         if (ticks == 160) onServer(mc, () -> {
            var gil = (GilgameshEntity)mc.getSingleplayerServer().overworld().getEntity(gilUuid);
            require(gil != null && gil.isFlyingMode() && gil.getY() > 103.0, "Gilgamesh did not sustain flight");
            require(gil.getPersistentData().contains("GilgameshLastGate"), "native treasury did not fire");
            flightVerified = true;
         });
         if (ticks == 190) capture(mc, "gilgamesh-flight");
         if (ticks == 200) onServer(mc, () -> {
            var level = mc.getSingleplayerServer().overworld();
            var gil = (GilgameshEntity)level.getEntity(gilUuid);
            var enemy = (net.minecraft.world.entity.LivingEntity)level.getEntity(enemyUuid);
            require(gil != null && enemy != null, "combatants disappeared");
            gil.setNoAi(true); gil.setPos(-5, 105, 0); gil.setDeltaMovement(Vec3.ZERO);
            var player = level.getServer().getPlayerList().getPlayer(mc.player.getUUID());
            player.teleportTo(-18, 107, 6); player.setYRot(-115); player.setXRot(0);
            for (var entity : level.getAllEntities()) if (entity instanceof GilgameshGateWeaponProjectileEntity) entity.discard();
            GilgameshCombatHelper.fireGateVolley(gil, level, enemy, 5, 18);
         });
         if (ticks > 205 && ticks < 230 && !captured) {
            var gates = mc.level.getEntitiesOfClass(GilgameshGateWeaponProjectileEntity.class, mc.player.getBoundingBox().inflate(50));
            if (gates.size() == 5) {
               require(gates.stream().allMatch(gate -> gate.preservesFullEffects() && gate.noCulling), "small gates entered client effect reduction");
               capture(mc, "five-gates-full-effects"); captured = true;
            }
         }
         if (ticks >= 260) {
            require(flightVerified && captured, "flight or five-gate client verification did not complete");
            done = true; TYPE_MOON_WORLD.LOGGER.info("SERVANT_AI_CLIENT_SMOKE_PASS flight=native treasury=firing small_volley=5"); mc.stop();
         }
      } catch (Throwable error) {
         done = true; TYPE_MOON_WORLD.LOGGER.error("SERVANT_AI_CLIENT_SMOKE_FAIL ticks=" + ticks, error); mc.stop();
      }
   }

   private void initialize(Minecraft mc) {
      var server = mc.getSingleplayerServer();
      var level = server.overworld();
      var player = server.getPlayerList().getPlayer(mc.player.getUUID());
      player.setGameMode(GameType.SPECTATOR); player.teleportTo(0, 106, 19); player.setYRot(180); player.setXRot(12);
      level.setDayTime(6000); level.setWeatherParameters(99999, 0, false, false);
      level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
      for (var entity : level.getAllEntities()) if (entity instanceof net.minecraft.world.entity.Mob) entity.discard();
      for (int x = -32; x <= 32; x++) for (int z = -32; z <= 32; z++) level.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
      var gil = ModEntities.GILGAMESH.get().create(level);
      var enemy = ModEntities.SASAKI_KOJIRO.get().create(level);
      require(gil != null && enemy != null, "could not create servants");
      gil.setPos(-7, 101, 0); enemy.setPos(10, 101, 0);
      gil.finalizeSpawn(level, level.getCurrentDifficultyAt(gil.blockPosition()), MobSpawnType.COMMAND, null);
      enemy.finalizeSpawn(level, level.getCurrentDifficultyAt(enemy.blockPosition()), MobSpawnType.COMMAND, null);
      enemy.setNoAi(true); enemy.setInvulnerable(true);
      level.addFreshEntity(gil); level.addFreshEntity(enemy); gil.setTarget(enemy); gil.getRandom().setSeed(173L);
      gilUuid = gil.getUUID(); enemyUuid = enemy.getUUID();
   }

   private void onServer(Minecraft mc, Runnable work) {
      mc.getSingleplayerServer().execute(() -> {
         try { work.run(); } catch (Throwable error) { failure = error.toString(); }
      });
   }

   private static void capture(Minecraft mc, String name) {
      File directory = new File(mc.gameDirectory, "qa"); directory.mkdirs();
      Screenshot.grab(directory, name + ".png", mc.getMainRenderTarget(), ignored -> { });
   }

   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
