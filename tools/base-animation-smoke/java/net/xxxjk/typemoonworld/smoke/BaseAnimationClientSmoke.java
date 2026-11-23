package net.xxxjk.typemoonworld.smoke;

import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import java.io.File;
import net.minecraft.client.Screenshot;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
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
import net.xxxjk.TYPE_MOON_WORLD.client.ServantBaseAnimationClient;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;

@Mod(value = "typemoonworld_base_animation_smoke", dist = Dist.CLIENT)
public final class BaseAnimationClientSmoke {
   private static final String WORLD = "Base-Animation-QA";
   private static final ResourceLocation BASE_LAYER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_base_action");
   private boolean opened, done;
   private boolean comboStarted, comboRefreshed, comboStopped, singleStarted;
   private boolean sceneCaptured;
   private int ticks, singleStart, singleDuration;
   private int comboStartTick;
   private volatile String failure;
   private volatile UUID heraclesId;
   private PlayerModel<?> model;

   public BaseAnimationClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent
   public void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (failure != null) throw new AssertionError(failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished(); mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) openWorld(mc);
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         if (ticks > 220) throw new AssertionError("NPC did not synchronize before smoke timeout");
         if (ticks == 10) {
            verifyRegisteredClipsAndUpperBodyIsolation(mc);
            server(mc, () -> setup(mc));
         }
         if (heraclesId == null) return;
         HeraclesEntity heracles = findHeracles(mc);
         if (heracles == null) return;
         if (ticks < 35) server(mc, () -> driveWalkers(mc));
         if (!sceneCaptured && ticks >= 16) {
            var hassan = findHassan(mc);
            if (hassan != null) {
               capture(mc, "base-animation-heracles-cursed-arm"); sceneCaptured = true;
            }
         }
         if (ticks >= 20 && !comboStarted) {
            ServantBaseAnimationClient.play(heraclesId, ServantBaseAction.SWORD_COMBO);
            require(ServantBaseAnimationClient.applyNpcPose(heracles, model, 0), "combo did not start for remote NPC");
            comboStarted = true; comboStartTick = ticks;
         }
         if (comboStarted && ticks >= comboStartTick + 4 && !comboRefreshed) {
            ServantBaseAnimationClient.play(heraclesId, ServantBaseAction.SWORD_COMBO);
            require(ServantBaseAnimationClient.applyNpcPose(heracles, model, 0), "repeated combo event stopped continuous playback");
            comboRefreshed = true;
         }
         if (comboRefreshed && ticks >= comboStartTick + 6 && !comboStopped) {
            ServantBaseAnimationClient.stopAttack(heraclesId);
            require(!ServantBaseAnimationClient.applyNpcPose(heracles, model, 0), "stop did not immediately clear NPC combo");
            comboStopped = true;
         }
         if (comboStopped && ticks >= Math.max(35, comboStartTick + 15) && !singleStarted) {
            ServantBaseAnimationClient.play(heraclesId, ServantBaseAction.SWORD);
            singleStart = ticks;
            require(ServantBaseAnimationClient.applyNpcPose(heracles, model, 0), "single sword attack did not start");
            singleStarted = true;
         }
         if (singleStarted && ticks >= singleStart + singleDuration + 1) {
            ServantBaseAnimationClient.tick();
            require(!ServantBaseAnimationClient.applyNpcPose(heracles, model, 0), "single attack failed to expire without a loop event");
            Object layer = PlayerAnimationAccess.getPlayerAssociatedData(mc.player).get(BASE_LAYER);
            require(layer instanceof ModifierLayer<?>, "base player layer was not registered");
            ServantBaseAnimationClient.play(mc.player.getUUID(), ServantBaseAction.SWORD);
            require(((ModifierLayer<?>) layer).isActive(), "short attack did not activate player animation layer");
            ServantBaseAnimationClient.stopAttack(mc.player.getUUID());
            require(!((ModifierLayer<?>) layer).isActive(), "stop did not immediately clear player attack layer");
            TYPE_MOON_WORLD.LOGGER.info("BASE_ANIMATION_CLIENT_SMOKE_PASS resources=all head_legs=unchanged npc_combo=stopped_immediately single_attack=non_looping");
            done = true; mc.stop();
         }
      } catch (Throwable error) {
         done = true; TYPE_MOON_WORLD.LOGGER.error("BASE_ANIMATION_CLIENT_SMOKE_FAIL ticks=" + ticks, error); mc.stop();
      }
   }

   private void openWorld(Minecraft mc) {
      opened = true; mc.options.pauseOnLostFocus = false; mc.options.renderDistance().set(4);
      mc.options.simulationDistance().set(4); mc.getWindow().setWindowed(1280, 900);
      if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile()) {
         mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("base animation QA save failed to open"); });
      } else {
         mc.createWorldOpenFlows().createFreshLevel(WORLD,
            new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(42, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
               .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
      }
   }

   private void verifyRegisteredClipsAndUpperBodyIsolation(Minecraft mc) throws Exception {
      int[][] actions = {{ServantBaseAction.SWORD, 0}, {ServantBaseAction.FIST, 0}, {ServantBaseAction.FIST, 1},
         {2, 0}, {3, 0}, {4, 0}, {5, 0}, {6, 0}, {7, 0}, {8, 0},
         {ServantBaseAction.SWORD_COMBO, 0}, {ServantBaseAction.FIST_COMBO, 0}};
      model = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
      Method apply = ServantBaseAnimationClient.class.getDeclaredMethod("apply", HumanoidModel.class, KeyframeAnimationPlayer.class);
      apply.setAccessible(true);
      float[][] expected = new float[3][];
      var parts = new net.minecraft.client.model.geom.ModelPart[] {model.head, model.rightLeg, model.leftLeg};
      for (int i = 0; i < parts.length; i++) {
         parts[i].setPos(4 + i, 7 + i, 10 + i); parts[i].setRotation(0.2F + i, 0.4F + i, 0.6F + i);
         expected[i] = pose(parts[i]);
      }
      for (int[] entry : actions) {
         String path = ServantBaseAction.animationId(entry[0], entry[1]);
         require(path != null, "action map returned null for " + entry[0]);
         ResourceLocation id = ResourceLocation.fromNamespaceAndPath("typemoonworld", path);
         require(PlayerAnimationRegistry.getAnimation(id) instanceof KeyframeAnimation, "missing animation resource " + id);
         KeyframeAnimation clip = (KeyframeAnimation) PlayerAnimationRegistry.getAnimation(id);
         if (entry[0] == ServantBaseAction.SWORD) singleDuration = clip.endTick;
         int end = Math.max(0, clip.endTick);
         for (int sampleTick : new int[] {0, end / 2, end}) {
            KeyframeAnimationPlayer sample = new KeyframeAnimationPlayer(clip, sampleTick);
            sample.setupAnim(0.5F); apply.invoke(null, model, sample);
            for (int part = 0; part < parts.length; part++) require(same(expected[part], pose(parts[part])),
               path + " changed head/leg pose at tick " + sampleTick);
         }
      }
   }

   private void setup(Minecraft mc) {
      var server = mc.getSingleplayerServer(); var level = server.overworld();
      var player = server.getPlayerList().getPlayer(mc.player.getUUID()); player.setGameMode(GameType.CREATIVE);
      level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
      for (int x = -8; x <= 8; x++) for (int z = -8; z <= 16; z++) {
         level.setBlock(new BlockPos(x, 100, z), Blocks.BEDROCK.defaultBlockState(), 2);
         for (int y = 101; y <= 107; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
      }
      player.teleportTo(0.5, 101, 7.5); player.setYRot(180);
      HeraclesEntity heracles = ModEntities.HERACLES.get().create(level);
      heracles.setNoAi(true); heracles.setPos(0.5, 101, 0.5);
      heracles.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.TEMPLE_STONE_SWORD_AXE.get()));
      var target = net.minecraft.world.entity.EntityType.HUSK.create(level);
      target.setNoAi(true); target.setNoGravity(true); target.setPos(0.5, 101, 12.5);
      target.setInvisible(true); level.addFreshEntity(target);
      heracles.setTarget(target); level.addFreshEntity(heracles); heraclesId = heracles.getUUID();
      CursedArmHassanEntity hassan = ModEntities.CURSED_ARM_HASSAN.get().create(level);
      var hassanTarget = net.minecraft.world.entity.EntityType.HUSK.create(level);
      hassanTarget.setNoAi(true); hassanTarget.setNoGravity(true); hassanTarget.setPos(4.5, 101, 12.5);
      hassanTarget.setInvisible(true); level.addFreshEntity(hassanTarget);
      hassan.setNoAi(true); hassan.setPos(4.5, 101, 0.5);
      hassan.setTarget(hassanTarget); level.addFreshEntity(hassan);
   }

   private void driveWalkers(Minecraft mc) {
      var level = mc.getSingleplayerServer().overworld();
      HeraclesEntity heracles = (HeraclesEntity) level.getEntity(heraclesId);
      if (heracles != null) heracles.setDeltaMovement(0.12, 0, 0);
      for (var entity : level.getAllEntities()) if (entity instanceof CursedArmHassanEntity hassan) {
         hassan.setDeltaMovement(0.20, 0, 0); break;
      }
   }

   private HeraclesEntity findHeracles(Minecraft mc) {
      for (var entity : mc.level.entitiesForRendering()) {
         if (heraclesId.equals(entity.getUUID())) return (HeraclesEntity) entity;
      }
      return null;
   }

   private CursedArmHassanEntity findHassan(Minecraft mc) {
      for (var entity : mc.level.entitiesForRendering()) if (entity instanceof CursedArmHassanEntity hassan) return hassan;
      return null;
   }

   private void server(Minecraft mc, Runnable work) {
      mc.getSingleplayerServer().execute(() -> { try { work.run(); } catch (Throwable error) { failure = error.toString(); } });
   }

   private static float[] pose(net.minecraft.client.model.geom.ModelPart part) {
      return new float[] {part.x, part.y, part.z, part.xRot, part.yRot, part.zRot};
   }

   private static boolean same(float[] a, float[] b) {
      for (int i = 0; i < a.length; i++) if (Math.abs(a[i] - b[i]) > 0.0001F) return false;
      return true;
   }

   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }

   private static void capture(Minecraft mc, String name) {
      File directory = new File(mc.gameDirectory, "qa"); directory.mkdirs();
      Screenshot.grab(directory, name + ".png", mc.getMainRenderTarget(), ignored -> {});
   }
}
