package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
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
import net.xxxjk.TYPE_MOON_WORLD.client.DiarmuidActionAnimationClient;
import net.xxxjk.TYPE_MOON_WORLD.client.DiarmuidVisualsClient;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction;

@Mod(value = "typemoonworld_diarmuid_smoke", dist = Dist.CLIENT)
public final class DiarmuidThirdPersonSmoke {
   private static final String WORLD = "Diarmuid-Third-Person-QA";
   private static final DiarmuidAction[] ACTIONS = {
      DiarmuidAction.ADVANCING_THRUST, DiarmuidAction.YELLOW_DOUBLE, DiarmuidAction.CROSS_THRUST,
      DiarmuidAction.RED_SWEEP, DiarmuidAction.LOW_SPIN, DiarmuidAction.LANDING_SWEEP,
      DiarmuidAction.RISING_THRUST, DiarmuidAction.RETREATING_THRUST, DiarmuidAction.ROSE_DANCE
   };
   private boolean opened, done;
   private int ticks;
   private volatile String failure;
   private ArmorStand camera;
   private Vec3 walkStart;

   public DiarmuidThirdPersonSmoke() { NeoForge.EVENT_BUS.register(this); }

   private static void require(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }

   private void server(Minecraft mc, Runnable task) {
      mc.getSingleplayerServer().execute(() -> {
         try { task.run(); } catch (Throwable error) { failure = error.toString(); }
      });
   }

   @SubscribeEvent public void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (failure != null) throw new AssertionError(failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished(); mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(4); mc.options.simulationDistance().set(5);
            mc.getWindow().setWindowed(1280, 900);
            if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile())
               mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA save failed"); });
            else mc.createWorldOpenFlows().createFreshLevel(WORLD,
               new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
               new WorldOptions(42, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                  .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         if (ticks == 10) {
            validatePoses(mc);
            server(mc, () -> setup(mc));
         }
         if (ticks == 30) {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            camera = new ArmorStand(mc.level, 4.2, -58.2, 5.0);
            camera.setYRot(140); camera.yRotO = 140;
            camera.yHeadRot = 140; camera.yHeadRotO = 140;
            camera.setXRot(15); camera.xRotO = 15;
            camera.xo = camera.getX(); camera.yo = camera.getY(); camera.zo = camera.getZ();
            mc.setCameraEntity(camera);
         }
         if (ticks == 40) { validateSpearPaths(mc); validateStance(mc); validateRecovery(mc); }
         int index = (ticks - 60) / 55;
         int frame = (ticks - 60) % 55;
         if (ticks >= 60 && index < ACTIONS.length) {
            if (frame == 0) server(mc, () -> cast(mc, ACTIONS[index]));
            if (frame >= 2 && frame <= 16 && frame % 2 == 0)
               capture(mc, ACTIONS[index].path() + "-frame-" + frame);
         }
         if (ticks == 570) {
            server(mc, () -> cast(mc, DiarmuidAction.FIGURE_EIGHT));
         }
         if (ticks == 578) {
            mc.setCameraEntity(mc.player);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            walkStart = mc.player.position();
            mc.options.keyUp.setDown(true);
         }
         if (ticks >= 578 && ticks < 618) {
            mc.options.keyUp.setDown(true);
            mc.player.setYRot(15); mc.player.setXRot(-18);
         }
         if (ticks >= 582 && ticks <= 610 && ticks % 4 == 2) capture(mc, "walking-dance-" + ticks);
         if (ticks == 618) {
            mc.options.keyUp.setDown(false);
            require(mc.player.position().distanceTo(walkStart) > 2, "held dance prevented walking");
            Object raw = PlayerAnimationAccess.getPlayerAssociatedData(mc.player).get(id("diarmuid_action"));
            require(raw instanceof ModifierLayer<?> layer && layer.getAnimation() != null && layer.getAnimation().isActive(),
               "walking cancelled held dance");
            server(mc, () -> {
               var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
               ServantCardTransformManager.handleHoldAction(player, 8, false);
            });
         }
         if (ticks == 635) {
            server(mc, () -> {
               var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
               player.teleportTo(0.5, -59, 0.5); player.setYRot(0); player.setXRot(0);
               DiarmuidAbilityService.cancel(player);
               player.getPersistentData().remove("DiarmuidAction:double_spear_stance");
               require(DiarmuidAbilityService.start(player, DiarmuidAction.DOUBLE_SPEAR_STANCE), "stance activation failed");
            });
            mc.setCameraEntity(camera);
         }
         if (ticks == 650 || ticks == 690) positionCamera(mc, false);
         if (ticks == 660 || ticks == 700) positionCamera(mc, true);
         if (ticks == 655 || ticks == 665 || ticks == 695 || ticks == 705)
            capture(mc, "drag-stance-" + ticks);
         if (ticks == 675) server(mc, () -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            var main = player.getMainHandItem().copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, player.getOffhandItem().copy());
            player.setItemInHand(InteractionHand.OFF_HAND, main);
         });
         if (ticks == 715) {
            mc.setCameraEntity(mc.player); mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            walkStart = mc.player.position(); mc.options.keyUp.setDown(true);
         }
         if (ticks >= 716 && ticks <= 745) {
            require(DiarmuidActionAnimationClient.stanceIdle(mc.player), "walking/running cancelled stance");
            if (ticks == 730) { mc.options.keySprint.setDown(true); mc.player.setSprinting(true); }
            if (ticks % 5 == 0) capture(mc, "drag-walk-run-" + ticks);
         }
         if (ticks == 746) {
            mc.options.keyUp.setDown(false); mc.options.keySprint.setDown(false);
            require(mc.player.position().distanceTo(walkStart) > 2, "stance blocked locomotion");
            mc.options.keyJump.setDown(true);
         }
         if (ticks == 749) {
            mc.options.keyJump.setDown(false);
            require(!mc.player.onGround() && !DiarmuidActionAnimationClient.stanceIdle(mc.player), "jump retained drag stance");
            capture(mc, "drag-jump-release");
         }
         if (ticks == 775) {
            DiarmuidActionAnimationClient.play(mc.player.getUUID(), DiarmuidAction.RED_BASIC.networkId(), InteractionHand.OFF_HAND.ordinal());
            validateIndependentHands(mc);
            capture(mc, "drag-single-hand");
         }
         if (ticks == 790) {
            DiarmuidActionAnimationClient.play(mc.player.getUUID(), DiarmuidAction.YELLOW_BASIC.networkId(), InteractionHand.MAIN_HAND.ordinal());
            DiarmuidActionAnimationClient.play(mc.player.getUUID(), DiarmuidAction.RED_BASIC.networkId(), InteractionHand.OFF_HAND.ordinal());
         }
         if (ticks == 791) capture(mc, "drag-both-hands");
         if (ticks == 810) {
            TYPE_MOON_WORLD.LOGGER.info("DIARMUID_THIRD_PERSON_SMOKE_OK: 24 animations decoded; directional cuts; actual spear rotation; natural finish and interruption; continuous hand links; close back stance; walking/running/jumping");
            done = true; mc.stop();
         }
      } catch (Throwable error) {
         done = true; mc.options.keyUp.setDown(false);
         TYPE_MOON_WORLD.LOGGER.error("DIARMUID_THIRD_PERSON_SMOKE_FAILED", error); mc.stop();
      }
   }

   private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", path); }

   private void positionCamera(Minecraft mc, boolean back) {
      camera.setPos(mc.player.getX() + 2.2, mc.player.getY() + 0.8, mc.player.getZ() + (back ? -2.9 : 2.9));
      camera.setYRot(back ? 37 : 143); camera.yRotO = camera.getYRot();
      camera.yHeadRot = camera.getYRot(); camera.yHeadRotO = camera.getYRot();
      camera.setXRot(8); camera.xRotO = 8;
      camera.xo = camera.getX(); camera.yo = camera.getY(); camera.zo = camera.getZ();
   }

   private static void validateStance(Minecraft mc) {
      var player = mc.player;
      var main = player.getMainHandItem().copy(); var off = player.getOffhandItem().copy();
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int mode = vars.servant_card_action_mode;
      boolean ground = player.onGround();
      try {
         vars.servant_card_action_mode = 1; player.setOnGround(true);
         for (int swap = 0; swap < 2; swap++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, swap == 0 ? main : off);
            player.setItemInHand(InteractionHand.OFF_HAND, swap == 0 ? off : main);
            InteractionHand redHand = swap == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            Vec3 red = DiarmuidActionAnimationClient.spearTip(player, DiarmuidAction.DOUBLE_SPEAR_STANCE, 6, redHand);
            Vec3 yellow = DiarmuidActionAnimationClient.spearTip(player, DiarmuidAction.DOUBLE_SPEAR_STANCE, 6,
               redHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            TYPE_MOON_WORLD.LOGGER.info("Diarmuid drag stance swap={}: red={}, yellow={}", swap, red, yellow);
            Vec3 redTail = DiarmuidActionAnimationClient.spearTail(player, DiarmuidAction.DOUBLE_SPEAR_STANCE, 6, redHand);
            Vec3 yellowTail = DiarmuidActionAnimationClient.spearTail(player, DiarmuidAction.DOUBLE_SPEAR_STANCE, 6,
               redHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            TYPE_MOON_WORLD.LOGGER.info("Diarmuid stance tails swap={}: red={}, yellow={}", swap, redTail, yellowTail);
            require(red != null && yellow != null && redTail != null && yellowTail != null, "missing stance endpoints");
            require(redTail.z > 0.3 && yellowTail.z > 0.3 && redTail.y > red.y && yellowTail.y > yellow.y,
               "tails must rise behind the back while tips point down");
            require(swap == 0 ? red.x > 0 && yellow.x < 0 && redTail.x < 0 && yellowTail.x > 0
               : red.x < 0 && yellow.x > 0 && redTail.x > 0 && yellowTail.x < 0,
               "only the tail portions must cross behind the back");
            for (Vec3 shaft : new Vec3[]{red.subtract(redTail), yellow.subtract(yellowTail)}) {
               double elevation = Math.toDegrees(Math.atan2(shaft.y, Math.sqrt(shaft.x * shaft.x + shaft.z * shaft.z)));
               require(Math.abs(elevation + 45) < 2, "shaft did not reverse then lower 45 degrees: " + elevation);
            }
            var model = new PlayerModel<LivingEntity>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            model.head.xRot = 0.3F; model.rightLeg.xRot = 0.6F; model.body.yRot = 0.2F;
            DiarmuidActionAnimationClient.applyIdlePose(player, model);
            require(model.head.xRot == 0.3F && model.rightLeg.xRot == 0.6F && model.body.yRot == 0.2F, "stance changed vanilla body/look/legs");
            require(model.rightArm.xRot > 0 && model.leftArm.xRot > 0, "stance raised the arms");
            require(swap == 0 ? model.rightArm.xRot > model.leftArm.xRot : model.rightArm.xRot < model.leftArm.xRot,
               "stance did not follow actual spear type");
            Vec3 start = DiarmuidActionAnimationClient.spearTip(player, DiarmuidAction.RED_BASIC, 0, redHand);
            Vec3 ready = DiarmuidActionAnimationClient.spearTip(player, DiarmuidAction.RED_BASIC, 1, redHand);
            Vec3 strike = DiarmuidActionAnimationClient.spearTip(player, DiarmuidAction.RED_BASIC, 2, redHand);
            Vec3 end = DiarmuidActionAnimationClient.spearTip(player, DiarmuidAction.RED_BASIC, DiarmuidAction.RED_BASIC.visualDuration(), redHand);
            require(start.distanceTo(red) < 0.01 && end.distanceTo(red) < 0.01 && ready.z < -1 && ready.z - strike.z > 0.35,
               "attack must return to ready before striking, then recover behind the back");
         }
         player.setOnGround(false);
         require(!DiarmuidActionAnimationClient.stanceIdle(player), "airborne stance remains active");
         player.setOnGround(true); vars.servant_card_action_mode = 0;
         require(!DiarmuidActionAnimationClient.stanceIdle(player), "ordinary idle uses drag stance");
      } finally {
         player.setItemInHand(InteractionHand.MAIN_HAND, main); player.setItemInHand(InteractionHand.OFF_HAND, off);
         vars.servant_card_action_mode = mode; player.setOnGround(ground);
      }
   }

   private static void validateIndependentHands(Minecraft mc) {
      var model = new PlayerModel<LivingEntity>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
      require(DiarmuidActionAnimationClient.stanceIdle(mc.player), "independent attack cleared whole stance");
      DiarmuidActionAnimationClient.applyIdlePose(mc.player, model);
      float idle = model.rightArm.xRot;
      DiarmuidActionAnimationClient.applyHandPose(mc.player, model, 1.5F);
      require(model.rightArm.xRot == idle && model.leftArm.xRot < 0, "single attack moved resting arm / failed swapped red attack");
      DiarmuidActionAnimationClient.play(mc.player.getUUID(), DiarmuidAction.ADVANCING_THRUST.networkId());
      require(!DiarmuidActionAnimationClient.stanceIdle(mc.player), "whole-body attack retained stance");
      DiarmuidActionAnimationClient.play(mc.player.getUUID(), -1);
   }

   private static void validateRecovery(Minecraft mc) {
      var player = mc.player;
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int mode = vars.servant_card_action_mode;
      boolean ground = player.onGround();
      vars.servant_card_action_mode = 1; player.setOnGround(true);
      try {
         for (DiarmuidAction action : new DiarmuidAction[]{DiarmuidAction.RED_BASIC, DiarmuidAction.YELLOW_BASIC}) {
            InteractionHand hand = action == DiarmuidAction.RED_BASIC ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            DiarmuidVisualsClient.stop(player.getUUID());
            DiarmuidVisualsClient.beginHand(player, action, hand, mc.level.getGameTime());
            double total = 0; Vec3 previous = null;
            for (float tick = 3; tick <= action.visualDuration() - 2; tick += 0.05F) {
               Vec3 tip = DiarmuidActionAnimationClient.spearTip(player, action, tick, hand);
               Vec3 tail = DiarmuidActionAnimationClient.spearTail(player, action, tick, hand);
               Vec3 shaft = tip.subtract(tail).normalize();
               if (previous != null) total += Math.acos(Math.max(-1, Math.min(1, shaft.dot(previous))));
               previous = shaft;
            }
            TYPE_MOON_WORLD.LOGGER.info("Diarmuid recovery {} actual shaft travel={} degrees", action, Math.toDegrees(total));
            require(total > Math.PI * 1.8, "recovery did not complete an actual spear rotation: " + action);
            DiarmuidActionAnimationClient.play(player.getUUID(), net.xxxjk.TYPE_MOON_WORLD.network.DiarmuidActionAnimationMessage.FINISH, hand.ordinal());
            require(DiarmuidVisualsClient.handAction(player.getUUID(), hand) == action, "natural finish truncated recovery");
            long start = mc.level.getGameTime() - 5;
            DiarmuidVisualsClient.stop(player.getUUID());
            DiarmuidVisualsClient.beginHand(player, action, hand, start);
            Vec3 before = DiarmuidActionAnimationClient.spearTip(player, action, 5, hand);
            DiarmuidVisualsClient.beginHand(player, action, hand, mc.level.getGameTime());
            Vec3 after = DiarmuidActionAnimationClient.spearTip(player, action, 0, hand);
            require(before.distanceTo(after) < 0.03, "successive attacks jumped at the blend boundary: " + before.distanceTo(after));
            DiarmuidActionAnimationClient.play(player.getUUID(), -1, hand.ordinal());
            require(DiarmuidVisualsClient.handAction(player.getUUID(), hand) == null, "interruption retained recovery");
            java.util.Set<String> paths = new java.util.HashSet<>();
            for (int variant = 0; variant < 4; variant++) {
               DiarmuidVisualsClient.beginHand(player, action, hand, mc.level.getGameTime() - 1);
               String path = DiarmuidVisualsClient.animationPath(player.getUUID(), action, hand);
               require(PlayerAnimationRegistry.getAnimation(id(path)) instanceof KeyframeAnimation, "missing directional variant: " + path);
               paths.add(path);
               Vec3 contact = DiarmuidActionAnimationClient.spearTip(player, action, 2, hand);
               require(contact.z < -1 && Math.abs(contact.x) < 0.65 && contact.y > 0.6, "cut misses the forward contact: " + path + " " + contact);
               DiarmuidVisualsClient.stop(player.getUUID());
            }
            require(paths.size() == 4, "basic attacks did not vary the striking plane");
         }
      } finally {
         DiarmuidVisualsClient.stop(player.getUUID());
         vars.servant_card_action_mode = mode; player.setOnGround(ground);
      }
   }

   private static void validatePoses(Minecraft mc) {
      var model = new PlayerModel<LivingEntity>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
      for (var action : DiarmuidAction.values()) {
         var animation = PlayerAnimationRegistry.getAnimation(id(action.animationPath()));
         require(animation instanceof KeyframeAnimation, "undecodable animation: " + action);
         var data = (KeyframeAnimation)animation;
         var sample = new KeyframeAnimationPlayer(data, Math.min(6, action.duration() - 1));
         Vec3f vanilla = new Vec3f(0.31F, -0.47F, 0.12F);
         for (String bone : new String[]{"head", "rightLeg", "leftLeg"}) {
            Vec3f result = sample.get3DTransform(bone, TransformType.ROTATION, 0, vanilla);
            require(Math.abs(result.getX() - vanilla.getX()) < 0.0001 && Math.abs(result.getY() - vanilla.getY()) < 0.0001,
               action + " overrides vanilla " + bone);
         }
         model.head.setRotation(0.31F, -0.47F, 0.12F);
         model.rightLeg.setRotation(0.65F, 0.05F, 0);
         model.leftLeg.setRotation(-0.65F, -0.05F, 0);
         DiarmuidActionAnimationClient.applyImportedPose(model, action, Math.min(6, action.duration() - 1));
         require(model.head.xRot == 0.31F && model.head.yRot == -0.47F && model.rightLeg.xRot == 0.65F
            && model.leftLeg.xRot == -0.65F, action + " resets NPC look/walk pose");
      }
   }

   private static void setup(Minecraft mc) {
      var level = mc.getSingleplayerServer().overworld();
      var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
      for (int x = -12; x <= 12; x++) for (int z = -8; z <= 20; z++) {
         level.setBlock(new BlockPos(x, -60, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
         for (int y = -59; y <= -53; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
      }
      level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, mc.getSingleplayerServer());
      level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, mc.getSingleplayerServer());
      level.setDayTime(6000);
      if (player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().transformed())
         ServantCardTransformManager.release(player, false);
      require(ServantCardTransformManager.transform(player, "diarmuid_ua_duibhne"), "card transformation failed");
      player.teleportTo(0.5, -59, 0.5); player.setYRot(0); player.setXRot(0);
   }

   private static void validateSpearPaths(Minecraft mc) {
      for (var action : new DiarmuidAction[]{DiarmuidAction.ADVANCING_THRUST, DiarmuidAction.YELLOW_DOUBLE,
         DiarmuidAction.CROSS_THRUST, DiarmuidAction.RETREATING_THRUST, DiarmuidAction.ROSE_DANCE}) {
         for (var hit : action.hits()) {
            InteractionHand hand = hit.red() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            Vec3 tip = DiarmuidActionAnimationClient.spearTip(mc.player, action, hit.tick(), hand);
            Vec3 drawn = DiarmuidActionAnimationClient.spearTip(mc.player, action, hit.tick() - 2, hand);
            require(tip != null && drawn != null, "missing real spear geometry: " + action);
            TYPE_MOON_WORLD.LOGGER.info("Diarmuid third-person thrust {} tick {}: tip={}, drawn={}", action, hit.tick(), tip, drawn);
            require(tip.z < -1 && Math.abs(tip.x) < 0.6 && tip.y > 0.5 && tip.y < 2,
               "thrust misses forward attack line: " + action + " " + tip);
            if (!hit.red() && action != DiarmuidAction.ROSE_DANCE) require(tip.y < 1.3, "yellow stab lost low lane: " + action);
            if (action != DiarmuidAction.ROSE_DANCE) require(drawn.y > 0.65, "thrust preparation dips toward the floor: " + action);
            require(drawn.z - tip.z > 0.35, "thrust lacks forward extension: " + action);
         }
      }
      for (var action : new DiarmuidAction[]{DiarmuidAction.RED_SWEEP, DiarmuidAction.LOW_SPIN, DiarmuidAction.LANDING_SWEEP}) {
         if (action != DiarmuidAction.RED_SWEEP) {
            Vec3 crossedRight = DiarmuidActionAnimationClient.spearTip(mc.player, action, 2, InteractionHand.MAIN_HAND);
            Vec3 crossedLeft = DiarmuidActionAnimationClient.spearTip(mc.player, action, 2, InteractionHand.OFF_HAND);
            require(crossedRight.x > 0 && crossedLeft.x < 0, "paired sweep windup crossed the chest: " + action);
            for (float tick = 4; tick <= 10; tick += 0.125F) {
               Vec3 right = DiarmuidActionAnimationClient.spearTip(mc.player, action, tick, InteractionHand.MAIN_HAND);
               Vec3 left = DiarmuidActionAnimationClient.spearTip(mc.player, action, tick, InteractionHand.OFF_HAND);
               require(right != null && left != null && right.x > 0 && left.x < 0,
                  "paired spear tips crossed their outward sectors: " + action + " tick=" + tick);
            }
            continue;
         }
         for (var hit : action.hits()) {
            double previous = 0, total = 0;
            for (float tick = hit.tick() - 3; tick <= hit.tick() + 3; tick += 0.25F) {
               Vec3 tip = DiarmuidActionAnimationClient.spearTip(mc.player, action, tick,
                  hit.red() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
               double angle = Math.atan2(tip.x, -tip.z);
               if (tick > hit.tick() - 3) total += Math.atan2(Math.sin(angle - previous), Math.cos(angle - previous));
               previous = angle;
            }
            TYPE_MOON_WORLD.LOGGER.info("Diarmuid third-person sweep {} {}: actual tip arc {} degrees", action, hit.red(), Math.toDegrees(total));
            require(Math.abs(total) > Math.PI, "actual spear-tip sweep remains narrow: " + action);
         }
      }
   }

   private static void cast(Minecraft mc, DiarmuidAction action) {
      var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
      DiarmuidAbilityService.cancel(player);
      player.teleportTo(0.5, -59, 0.5); player.setYRot(0); player.setXRot(0);
      player.setDeltaMovement(Vec3.ZERO);
      player.getPersistentData().remove("DiarmuidAction:" + action.path());
      player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(200);
      if (action == DiarmuidAction.LANDING_SWEEP)
         net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork.sendToPlayer(player,
            new net.xxxjk.TYPE_MOON_WORLD.network.DiarmuidActionAnimationMessage(player.getUUID(), action.networkId()));
      else require(DiarmuidAbilityService.start(player, action), "cast failed: " + action);
   }

   private static void capture(Minecraft mc, String name) {
      Screenshot.grab(mc.gameDirectory, "third-person-" + name + ".png", mc.getMainRenderTarget(),
         message -> TYPE_MOON_WORLD.LOGGER.info("Diarmuid third-person screenshot: {}", message.getString()));
   }
}
