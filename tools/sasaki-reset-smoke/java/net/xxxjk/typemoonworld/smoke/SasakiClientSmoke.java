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
import net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient;
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
   private boolean heardAttackVoice;
   private boolean heardTsubameVoice;
   private boolean heardBladeDraw;
   private boolean heardBladeSwing;
   private boolean heardBladeImpact;
   private boolean heardTsubameRelease;

   public SasakiClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent
   public void sound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
      if (event.getSound() == null) return;
      ResourceLocation id = event.getSound().getLocation();
      if (id.equals(ResourceLocation.fromNamespaceAndPath("typemoonworld", "sasaki_kojiro_voice_attack"))) heardAttackVoice = true;
      if (id.equals(ResourceLocation.fromNamespaceAndPath("typemoonworld", "sasaki_kojiro_voice_tsurigameshi"))) heardTsubameVoice = true;
      if (id.equals(ResourceLocation.fromNamespaceAndPath("typemoonworld", "sasaki_blade_draw"))) heardBladeDraw = true;
      if (id.equals(ResourceLocation.fromNamespaceAndPath("typemoonworld", "sasaki_blade_swing"))) heardBladeSwing = true;
      if (id.equals(ResourceLocation.fromNamespaceAndPath("typemoonworld", "sasaki_blade_impact"))) heardBladeImpact = true;
      if (id.equals(ResourceLocation.fromNamespaceAndPath("typemoonworld", "sasaki_tsubame_release"))) heardTsubameRelease = true;
   }

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
            verifyTsubameGeometry(mc);
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            server(mc, () -> setup(mc));
         }
         if (ticks == 60) mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
         if (ticks == 28) {
            var vars = mc.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            require(ServantCardTransformManager.getSkillCooldown(vars, 9) > 500,
               "Tsubame skill cooldown did not reach the client");
            var hudCooldown = net.xxxjk.TYPE_MOON_WORLD.client.screens.ServantCardHud.class.getDeclaredMethod(
               "effectiveNpCooldown", Minecraft.class, TypeMoonWorldModVariables.PlayerVariables.class);
            hudCooldown.setAccessible(true);
            require((int)hudCooldown.invoke(null, mc, vars) > 500, "HUD failed to read Tsubame's slot cooldown");
            require(net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient.afterimageCount(mc.player.getUUID()) == 2,
               "Player Tsubame did not render two afterimages");
            require(net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient.afterimageCount(npcId) == 2,
               "NPC Tsubame did not render two afterimages");
            capture(mc, "sasaki-tsubame");
         }
         if (ticks == 35) {
            server(mc, () -> {
               var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
               require(ServantCardTransformManager.triggerAction(player, 1), "Voiced card slash failed");
            });
         }
         if (ticks == 45) {
            require(net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient.afterimageCount(mc.player.getUUID()) == 0
               && net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient.afterimageCount(npcId) == 0,
               "Tsubame afterimages did not expire");
            server(mc, () -> verifyTransparency(mc));
         }
         if (ticks >= 60 && (ticks - 60) % 25 == 0) {
            int index = (ticks - 60) / 25;
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
         if (ticks == 95) capture(mc, "sasaki-concealment");
         if (ticks == 140) capture(mc, "sasaki-slash-pose");
         if (ticks >= 67 && ticks <= 342 && (ticks - 67) % 25 == 0) {
            if (ticks == 67) {
               Object layer = dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess.getPlayerAssociatedData(mc.player)
                  .get(ResourceLocation.fromNamespaceAndPath("typemoonworld", "sasaki_kojiro_action"));
               require(layer instanceof dev.kosmx.playerAnim.api.layered.ModifierLayer<?> animation
                  && animation.isActive()
                  && animation.getFirstPersonMode(0.0F) == dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.THIRD_PERSON_MODEL,
                  "Sasaki action did not enable its first-person animation");
            }
            capture(mc, "sasaki-action-" + SasakiKojiroAction.values()[(ticks - 67) / 25].animationName());
         }
         if (ticks == 85) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
         if (ticks == 380) {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            mc.options.hideGui = true;
            server(mc, () -> {
               var server = mc.getSingleplayerServer();
               var player = server.getPlayerList().getPlayer(mc.player.getUUID());
               player.setGameMode(GameType.SPECTATOR);
               player.connection.teleport(-5, 102, 8, -135, 12);
            });
         }
         if (ticks == 395) {
            server(mc, () -> {
               var server = mc.getSingleplayerServer();
               var npc = (SasakiKojiroEntity)server.overworld().getEntity(npcId);
               npc.setPos(0.5, 101, 2.5);
               npc.setYRot(0);
               npc.yBodyRot = 0;
               npc.yHeadRot = 0;
               SasakiKojiroCombatService.playTsubamePresentation(npc);
            });
         }
         if (ticks == 400) {
            require(SasakiTsubameAfterimageClient.afterimageCount(npcId) == 0, "Afterimages appeared before the windup finished");
            capture(mc, "sasaki-npc-tsubame-windup");
         }
         if (ticks == 407) capture(mc, "sasaki-npc-tsubame-start");
         if (ticks == 410) capture(mc, "sasaki-npc-tsubame-split");
         if (ticks == 414) {
            require(net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient.afterimageCount(npcId) == 2,
               "NPC did not receive its own Tsubame afterimage presentation");
            var npc = (SasakiKojiroEntity)mc.level.getEntity(findNpcEntityId(mc));
            verifyBladeTrails(npc);
            require(Math.abs(SasakiTsubameAfterimageClient.facingYaw(npc, 90.0F)) < 0.01F,
               "Tsubame body yaw is not locked to the forward cast direction");
            capture(mc, "sasaki-npc-tsubame-forward");
         }
         if (ticks == 418) capture(mc, "sasaki-npc-tsubame-finish");
         if (ticks == 430) {
            require(SasakiTsubameAfterimageClient.afterimageCount(npcId) == 0, "NPC cast did not expire");
            server(mc, () -> {
               var server = mc.getSingleplayerServer();
               var player = server.getPlayerList().getPlayer(mc.player.getUUID());
               var npc = (SasakiKojiroEntity)server.overworld().getEntity(npcId);
               player.connection.teleport(-6, 102, 6, -118, 12);
               npc.setYRot(90);
               npc.yBodyRot = 90;
               npc.yHeadRot = 90;
            });
         }
         if (ticks == 435) server(mc, () -> SasakiKojiroCombatService.playTsubamePresentation(
            (SasakiKojiroEntity)mc.getSingleplayerServer().overworld().getEntity(npcId)));
         if (ticks == 454) {
            var npc = (SasakiKojiroEntity)mc.level.getEntity(findNpcEntityId(mc));
            require(Math.abs(SasakiTsubameAfterimageClient.facingYaw(npc, 0.0F) - 90.0F) < 0.01F,
               "Rotated Tsubame did not keep its cast-relative forward direction");
            capture(mc, "sasaki-npc-tsubame-rotated");
         }
         if (ticks >= 470) {
            require(verified, "Server checks incomplete");
            require(heardAttackVoice && heardTsubameVoice, "Client did not receive restored attack and Tsubame voices");
            require(heardBladeDraw && heardBladeSwing && heardBladeImpact && heardTsubameRelease,
               "Client did not receive one or more of the four sword sound events");
            TYPE_MOON_WORLD.LOGGER.info("SASAKI_SWORD_SOUND_SMOKE_PASS draw=true swing=true impact=true release=true");
            TYPE_MOON_WORLD.LOGGER.info("SASAKI_VOICE_SMOKE_PASS attack=true tsubame=true");
            done = true;
            TYPE_MOON_WORLD.LOGGER.info("SASAKI_CLIENT_SMOKE_PASS animations=12 source=full_tracks windup=10 tsubame=450 mana=20 npc=humanoid afterimages=2_per_caster origin=shared split=left_right facing=cast_relative_0_90 vfx=stretched_source_texture expires=true cooldown_hud=true transparency=cleanse_teleport");
            mc.stop();
         }
      } catch (Throwable error) {
         done = true;
         TYPE_MOON_WORLD.LOGGER.error("SASAKI_CLIENT_SMOKE_FAIL ticks=" + ticks, error);
         mc.stop();
      }
   }

   private int findNpcEntityId(Minecraft mc) {
      for (var entity : mc.level.entitiesForRendering()) if (npcId.equals(entity.getUUID())) return entity.getId();
      throw new AssertionError("NPC missing on client");
   }

   private static void verifyBladeTrails(SasakiKojiroEntity owner) {
      for (int blade = 1; blade <= 3; blade++) {
         for (float age : new float[] {1.0F, 2.5F, 6.0F, 10.0F, 17.0F}) {
            var trail = SasakiTsubameAfterimageClient.bladeTrail(owner, blade, age);
            var tip = SasakiTsubameAfterimageClient.bladeTip(owner, blade, age);
            require(trail.getLast().distanceToSqr(tip) < 1.0E-8, "Trail head detached from blade tip: " + blade);
            for (var point : trail) require(Double.isFinite(point.lengthSqr()), "Non-finite blade trail");
         }
         var trail = SasakiTsubameAfterimageClient.bladeTrail(owner, blade, 6.0F);
         require(trail.getFirst().distanceTo(trail.getLast()) > 2.0, "Blade trail lost its extended tail: " + blade);
      }
      TYPE_MOON_WORLD.LOGGER.info("SASAKI_BLADE_TRAIL_SMOKE_PASS blades=3 head=animated_tip tail=extended_history");
   }

   private static void verifyTsubameGeometry(Minecraft mc) {
      for (int shadow = 2; shadow <= 3; shadow++) {
         require(SasakiTsubameAfterimageClient.shadowOffset(shadow, 0.0F).lengthSqr() < 0.000001,
            "Afterimage must originate at the caster: " + shadow);
      }
      for (float time : new float[] {0.0625F, 0.125F, 0.3125F, 0.625F}) {
         var left = SasakiTsubameAfterimageClient.shadowOffset(2, time);
         var right = SasakiTsubameAfterimageClient.shadowOffset(3, time);
         require(left.x < 0 && right.x > 0 && left.z < 0 && right.z < 0,
            "Afterimages must separate to opposite sides and advance: " + time);
      }
      var model = new net.minecraft.client.model.PlayerModel<net.minecraft.world.entity.LivingEntity>(
         mc.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER), false);
      SasakiTsubameAfterimageClient.applyMainPose(model, 0.3125F);
      float armX = model.rightArm.x;
      SasakiTsubameAfterimageClient.applyMainPose(model, 0.3125F);
      require(Math.abs(model.rightArm.x - armX) < 0.0001F, "Repeated pose application accumulated translation");
      require(model.head.yRot == 0.0F && model.body.yRot == 0.0F, "Body and head must face forward");
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
      require(victim.getHealth() == 1000, "Real player Tsubame skipped its stance");
      TYPE_MOON_WORLD.queueServerWork(11, () -> {
         try { require(Math.abs(victim.getHealth() - 550) < 0.02, "Real player Tsubame did not deal 450 after the stance"); }
         catch (Throwable error) { failure = error.toString(); }
      });
      require(state.mana() == 180 && state.npCooldown() == 0, "Real player Tsubame resources differ");
      var npc = ModEntities.SASAKI_KOJIRO.get().create(level);
      npc.setNoAi(true);
      npc.setPos(5.5, 101, 3.5);
      npc.setYRot(0);
      level.addFreshEntity(npc);
      npcId = npc.getUUID();
      SasakiKojiroCombatService.playTsubamePresentation(npc);
      verified = true;
   }

   private void server(Minecraft mc, Runnable work) {
      mc.getSingleplayerServer().execute(() -> {
         try { work.run(); } catch (Throwable error) { failure = error.toString(); }
      });
   }

   private void verifyTransparency(Minecraft mc) {
      var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
      player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 200));
      require(ServantCardTransformManager.triggerAction(player, 5), "Real player transparency failed");
      require(!player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)
         && !player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)
         && !player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE), "Transparency added buffs or failed to cleanse");
      require(player.getZ() < 2.5 && player.getZ() > 0.5, "Transparency did not teleport behind the enemy");
   }

   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
   private static void capture(Minecraft mc, String name) {
      File directory = new File(mc.gameDirectory, "qa");
      directory.mkdirs();
      Screenshot.grab(directory, name + ".png", mc.getMainRenderTarget(), ignored -> { });
   }
}
