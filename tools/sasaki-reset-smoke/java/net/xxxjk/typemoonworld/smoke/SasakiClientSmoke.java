package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import java.util.UUID;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroActionAnimationService;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroCombatService;

@Mod(value = "typemoonworld_sasaki_smoke", dist = Dist.CLIENT)
public final class SasakiClientSmoke {
   private static final String WORLD = "Sasaki-QA";
   private boolean opened;
   private boolean done;
   private int ticks;
   private volatile String failure;
   private volatile boolean verified;
   private UUID npcId;

   public SasakiClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent
   public void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         require(failure == null, failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished();
            mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(4);
            mc.options.simulationDistance().set(5);
            mc.getWindow().setWindowed(1280, 720);
            if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile()) {
               mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA save open failed"); });
            } else {
               mc.createWorldOpenFlows().createFreshLevel(WORLD,
                  new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                  new WorldOptions(42, false, false),
                  registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                  new TitleScreen());
            }
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         if (ticks == 10) {
            for (var action : SasakiKojiroAction.values()) {
               require(PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld", action.playerAnimationPath()))
                  instanceof KeyframeAnimation, "Missing or undecodable player animation: " + action);
            }
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            server(mc, () -> setup(mc));
         }
         if (ticks == 18) {
            require(net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient.afterimageCount(mc.player.getUUID()) == 2,
               "Player Tsubame did not render two afterimages");
            require(net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient.afterimageCount(npcId) == 2,
               "NPC Tsubame did not render two afterimages");
            capture(mc, "sasaki-tsubame");
         }
         if (ticks == 35) {
            require(net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient.afterimageCount(mc.player.getUUID()) == 0
               && net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient.afterimageCount(npcId) == 0,
               "Tsubame afterimages did not expire");
         }
         if (ticks >= 40 && (ticks - 40) % 25 == 0) {
            int index = (ticks - 40) / 25;
            if (index < SasakiKojiroAction.values().length) {
               var action = SasakiKojiroAction.values()[index];
               server(mc, () -> {
                  var server = mc.getSingleplayerServer();
                  var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                  var npc = (SasakiKojiroEntity)server.overworld().getEntity(npcId);
                  SasakiKojiroActionAnimationService.play(player, action);
                  SasakiKojiroActionAnimationService.play(npc, action);
                  if (action == SasakiKojiroAction.PRESENCE_CONCEALMENT) SasakiKojiroCombatService.setConcealed(player, true);
                  else if (SasakiKojiroCombatService.isConcealed(player)) SasakiKojiroCombatService.setConcealed(player, false);
               });
            }
         }
         if (ticks == 70) capture(mc, "sasaki-concealment");
         if (ticks == 120) capture(mc, "sasaki-slash-pose");
         if (ticks == 330) {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            server(mc, () -> {
               var server = mc.getSingleplayerServer();
               var player = server.getPlayerList().getPlayer(mc.player.getUUID());
               var npc = (SasakiKojiroEntity)server.overworld().getEntity(npcId);
               player.teleportTo(-4, 102, -2);
               player.setYRot(-45);
               player.setXRot(8);
               npc.setPos(0.5, 101, 2.5);
               npc.setYRot(0);
               npc.yBodyRot = 0;
               npc.yHeadRot = 0;
               SasakiKojiroCombatService.playTsubamePresentation(npc);
            });
         }
         if (ticks == 339) {
            require(net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient.afterimageCount(npcId) == 2,
               "NPC did not receive its own Tsubame afterimage presentation");
            capture(mc, "sasaki-npc-tsubame-forward");
         }
         if (ticks >= 375) {
            require(verified, "Server checks incomplete");
            done = true;
            TYPE_MOON_WORLD.LOGGER.info("SASAKI_CLIENT_SMOKE_PASS animations=11 tsubame=450 mana=20 npc=humanoid afterimages=2_per_caster expires=true vfx=rendered");
            mc.stop();
         }
      } catch (Throwable error) {
         done = true;
         TYPE_MOON_WORLD.LOGGER.error("SASAKI_CLIENT_SMOKE_FAIL ticks=" + ticks, error);
         mc.stop();
      }
   }

   private void setup(Minecraft mc) {
      var server = mc.getSingleplayerServer();
      var level = server.overworld();
      var player = server.getPlayerList().getPlayer(mc.player.getUUID());
      player.setGameMode(GameType.SURVIVAL);
      var state = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      if (state.transformed()) ServantCardTransformManager.release(player, false);
      state.setTransformCooldown(0);
      state.setReleaseCooldown(0);
      level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
      level.setDayTime(6000);
      var previous = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
      for (var entity : level.getAllEntities()) if (entity instanceof net.minecraft.world.entity.Mob) previous.add(entity);
      previous.forEach(net.minecraft.world.entity.Entity::discard);
      for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
         level.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
         for (int y = 101; y < 110; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
      }
      player.teleportTo(0.5, 101, 0.5);
      player.setYRot(0);
      player.setXRot(0);
      require(ServantCardTransformManager.transform(player, "sasaki_kojiro"), "Transform failed");
      state.setMana(200);
      var victim = EntityType.HUSK.create(level);
      victim.setPos(0.5, 101, 2.5);
      victim.setNoAi(true);
      victim.setNoGravity(true);
      victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
      victim.getAttribute(Attributes.ARMOR).setBaseValue(0);
      victim.setHealth(1000);
      level.addFreshEntity(victim);
      require(ServantCardTransformManager.triggerAction(player, 9), "Real player Tsubame failed");
      require(Math.abs(victim.getHealth() - 550) < 0.02, "Real player Tsubame did not deal 450");
      require(state.mana() == 180 && state.npCooldown() == 0, "Real player Tsubame resources differ");
      var npc = ModEntities.SASAKI_KOJIRO.get().create(level);
      npc.setNoAi(true);
      npc.setPos(5.5, 101, 3.5);
      npc.setYRot(0);
      level.addFreshEntity(npc);
      npcId = npc.getUUID();
      SasakiKojiroActionAnimationService.play(npc, SasakiKojiroAction.TSUBAME_GAESHI);
      verified = true;
   }

   private void server(Minecraft mc, Runnable work) {
      mc.getSingleplayerServer().execute(() -> {
         try { work.run(); } catch (Throwable error) { failure = error.toString(); }
      });
   }

   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
   private static void capture(Minecraft mc, String name) {
      File directory = new File(mc.gameDirectory, "qa");
      directory.mkdirs();
      Screenshot.grab(directory, name + ".png", mc.getMainRenderTarget(), ignored -> { });
   }
}
