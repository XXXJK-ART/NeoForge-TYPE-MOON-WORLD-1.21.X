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
import net.xxxjk.TYPE_MOON_WORLD.client.ArtoriaSwordTrailClient;
import net.xxxjk.TYPE_MOON_WORLD.client.SasakiKojiroActionAnimationClient;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SwordActionAnimationService;

@Mod(value = "typemoonworld_okita_smoke", dist = Dist.CLIENT)
public final class OkitaClientSmoke {
   private static final String WORLD = "Okita-QA";
   private static final SasakiKojiroAction[] ACTIONS = {
      SasakiKojiroAction.PRESENCE_CONCEALMENT, SasakiKojiroAction.VERTICAL_SLASH,
      SasakiKojiroAction.SPINNING_SLASH, SasakiKojiroAction.OKITA_THRUST,
      SasakiKojiroAction.DOWNWARD_SLASH, SasakiKojiroAction.TRANSPARENCY,
      SasakiKojiroAction.COUNTER_STANCE, SasakiKojiroAction.TSUBAME_WINDUP,
      SasakiKojiroAction.OKITA_MUMYOUDAN_ZUKI
   };
   private boolean opened, done;
   private int ticks, samples;
   private volatile UUID npcId;
   private volatile UUID targetId;
   private volatile String failure;
   private int impactSounds;

   public OkitaClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent
   public void sound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
      if (event.getSound() != null && event.getSound().getLocation().equals(
         ResourceLocation.fromNamespaceAndPath("minecraft", "item.trident.hit"))) impactSounds++;
   }

   @SubscribeEvent
   public void tick(ClientTickEvent.Post event) {
      var mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         require(failure == null, failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished(); mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true; mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(4); mc.options.simulationDistance().set(4);
            mc.getWindow().setWindowed(1280, 900);
            if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile())
               mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA world failed to load"); });
            else mc.createWorldOpenFlows().createFreshLevel(WORLD,
               new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
               new WorldOptions(42, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                  .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         if (ticks == 10) server(mc, () -> setup(mc));
         if (npcId == null) return;
         OkitaSoujiSaberEntity npc = null;
         for (var entity : mc.level.entitiesForRendering()) if (entity.getUUID().equals(npcId)) npc = (OkitaSoujiSaberEntity)entity;
         if (npc == null) { require(ticks < 100, "Okita NPC failed to synchronize"); return; }
         if (ticks == 50) {
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            require(ArtoriaSwordTrailClient.isOkita(npc) && ArtoriaSwordTrailClient.isOkita(mc.player), "Asagi trail owner detection failed");
            for (var action : ACTIONS) require(PlayerAnimationRegistry.getAnimation(
               ResourceLocation.fromNamespaceAndPath("typemoonworld", action.playerAnimationPath())) instanceof KeyframeAnimation,
               "Animation resource missing: " + action);
         }
         if (ticks == 285) {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            mc.options.hideGui = true;
            server(mc, () -> {
               var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
               player.setGameMode(GameType.SPECTATOR);
               player.connection.teleport(-4.5, 102.0, 6.5, -128.0F, 10.0F);
            });
         }
         if (ticks >= 60 && (ticks - 60) % 30 == 0) {
            int index = (ticks - 60) / 30;
            if (index < ACTIONS.length) {
               var action = ACTIONS[index];
               server(mc, () -> {
                  var level = mc.getSingleplayerServer().overworld();
                  var owner = (OkitaSoujiSaberEntity)level.getEntity(npcId);
                  var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                  if (action == SasakiKojiroAction.OKITA_MUMYOUDAN_ZUKI) {
                     var target = (net.minecraft.world.entity.LivingEntity)level.getEntity(targetId);
                     require(net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberTechniqueService.beginMumyoudan(owner, target, () -> { }), "NPC NP failed");
                  } else {
                     SwordActionAnimationService.play(owner, action); SwordActionAnimationService.play(player, action);
                  }
               });
            }
         }
         if (ticks >= 65 && ticks <= 275 && (ticks - 65) % 30 == 0 || ticks == 315) {
            int index = ticks == 315 ? 8 : (ticks - 65) / 30;
            if (index < ACTIONS.length) {
               var action = ACTIONS[index];
               var renderer = mc.getEntityRenderDispatcher().getRenderer(npc);
               require(renderer instanceof net.xxxjk.TYPE_MOON_WORLD.client.renderer.HumanoidServantRenderer<?>, "NPC did not use humanoid animation renderer");
               var model = ((net.xxxjk.TYPE_MOON_WORLD.client.renderer.HumanoidServantRenderer<?>)renderer).getModel();
               require(Math.abs(model.rightArm.xRot) + Math.abs(model.rightArm.yRot) + Math.abs(model.rightArm.zRot) > .05F,
                  "NPC arm stayed idle: " + action);
               var tip = SasakiKojiroActionAnimationClient.bladeTip(npc, action, 5);
               require(tip != null && Double.isFinite(tip.lengthSqr()), "Invalid sword-tip pose: " + action);
               samples++;
               capture(mc, "okita-" + action.animationName());
            }
         }
         if (ticks == 309 || ticks == 317 || ticks == 320) capture(mc, "okita-three-thrust-" + ticks);
         if (ticks >= 335) {
            require(samples == ACTIONS.length, "Missing action samples: " + samples);
            require(impactSounds == 3, "Client did not receive three distinct contact sounds: " + impactSounds);
            TYPE_MOON_WORLD.LOGGER.info("OKITA_CLIENT_SMOKE_PASS animations={} player=true npc=true sword_tip=finite trail=asagi impact_sounds={}", samples, impactSounds);
            done = true; mc.stop();
         }
      } catch (Throwable error) {
         done = true; TYPE_MOON_WORLD.LOGGER.error("OKITA_CLIENT_SMOKE_FAIL ticks=" + ticks, error); mc.stop();
      }
   }

   private void setup(Minecraft mc) {
      var server = mc.getSingleplayerServer(); var level = server.overworld();
      var player = server.getPlayerList().getPlayer(mc.player.getUUID());
      level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
      level.setDayTime(6000);
      for (int x = -8; x <= 8; x++) for (int z = -8; z <= 10; z++) level.setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.SMOOTH_STONE.defaultBlockState());
      for (var entity : level.getEntitiesOfClass(OkitaSoujiSaberEntity.class, player.getBoundingBox().inflate(200))) entity.discard();
      player.setGameMode(GameType.CREATIVE); player.connection.teleport(.5, 101, -1.5, 0, 8);
      require(ServantCardTransformManager.transform(player, "okita_souji_saber"), "Card transform failed");
      var npc = ModEntities.OKITA_SOUJI_SABER.get().create(level);
      npc.setNoAi(true); npc.moveTo(.5, 101, 2.5, 180, 0); npc.yBodyRot = 180; npc.yHeadRot = 180;
      level.addFreshEntity(npc); npc.ensureDefaultNpcLoadout(false); npcId = npc.getUUID();
      for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, npc.getBoundingBox().inflate(30))) entity.discard();
      var target = net.minecraft.world.entity.EntityType.HUSK.create(level);
      target.moveTo(.5, 101, .5, 0, 0); target.setNoAi(true); target.setNoGravity(true);
      target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
      target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
      target.setHealth(1000); level.addFreshEntity(target); targetId = target.getUUID();
   }

   private void server(Minecraft mc, Runnable action) {
      mc.getSingleplayerServer().execute(() -> { try { action.run(); } catch (Throwable error) { failure = error.toString(); } });
   }

   private static void capture(Minecraft mc, String name) {
      Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), message -> { });
   }

   private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
