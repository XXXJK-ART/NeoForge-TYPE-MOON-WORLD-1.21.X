package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.HeraclesRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.HumanoidServantRenderer;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;

@Mod(value = "typemoonworld_npc_animation_smoke", dist = Dist.CLIENT)
public final class NpcAnimationClientSmoke {
   private static final String WORLD = "Npc-Animation-QA";
   private boolean opened, done;
   private int ticks, movingSamples;
   private volatile String failure;
   private volatile UUID heraclesId, odaId;
   private float previousArm = Float.NaN, smallestArm = Float.POSITIVE_INFINITY, largestArm = Float.NEGATIVE_INFINITY;

   public NpcAnimationClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent
   public void tick(ClientTickEvent.Post event) {
      var mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (failure != null) throw new AssertionError(failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished(); mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true; mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(4); mc.options.simulationDistance().set(4);
            mc.options.entityDistanceScaling().set(1.0); mc.getWindow().setWindowed(1280, 900);
            if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile()) {
               mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA save failed to open"); });
            } else {
               mc.createWorldOpenFlows().createFreshLevel(WORLD,
                  new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                  new WorldOptions(42, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                     .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
            }
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         if (ticks == 10) server(mc, () -> setup(mc));
         if (heraclesId == null || odaId == null) return;
         HeraclesEntity heracles = null;
         OdaNobunagaEntity oda = null;
         for (var entity : mc.level.entitiesForRendering()) {
            if (entity.getUUID().equals(heraclesId)) heracles = (HeraclesEntity) entity;
            if (entity.getUUID().equals(odaId)) oda = (OdaNobunagaEntity) entity;
         }
         if (heracles == null || oda == null) {
            require(ticks < 50, "NPCs did not synchronize"); return;
         }
         if (ticks >= 35 && ticks < 220) sample(mc, heracles, oda);
         if (ticks == 48) {
            require(heracles.isFrenzyAnimationActive() && oda.isCombatReady(), "combat display state did not synchronize");
            capture(mc, "npc-frenzy-ready");
         }
         if (ticks == 50) server(mc, () -> {
            npc(mc).triggerBasicAttackAnimation(); oda(mc).triggerBasicAttackAnimation();
         });
         if (ticks >= 70 && ticks <= 230 && (ticks - 70) % 20 == 0) {
            int slot = (ticks - 70) / 20;
            server(mc, () -> {
               npc(mc).triggerNamedActionAnimation("martial_" + slot);
               oda(mc).triggerNamedActionAnimation(slot % 2 == 0 ? "attack_shot" : "skill_command");
            });
         }
         if (ticks == 132 || ticks == 214) capture(mc, "npc-skill-" + ticks);
         if (ticks == 248) server(mc, () -> npc(mc).triggerNamedActionAnimation("frenzy_impact"));
         if (ticks == 258) capture(mc, "npc-landing-slash");
         if (ticks == 265) server(mc, () -> {
            npc(mc).triggerNamedActionAnimation("martial_end");
            npc(mc).setTarget(null); oda(mc).setTarget(null);
         });
         if (ticks >= 300) {
            require(movingSamples >= 100 && largestArm - smallestArm > 1.0F, "rendered weapon arm did not sustain broad swings");
            require(!heracles.isFrenzyAnimationActive() && !oda.isCombatReady(), "target loss did not synchronize");
            var renderer = (HeraclesRenderer) mc.getEntityRenderDispatcher().getRenderer(heracles);
            var arm = renderer.getGeoModel().getAnimationProcessor().getBone("right arm");
            require(arm != null && Math.abs(arm.getRotX()) < .15F, "idle arm failed to recover after combat");
            capture(mc, "npc-idle-recovered"); done = true;
            TYPE_MOON_WORLD.LOGGER.info("NPC_ANIMATION_CLIENT_SMOKE_PASS skills=9 synchronized=true moving_samples={} arm_span={} idle=recovered sleeves=aligned", movingSamples, largestArm - smallestArm);
            mc.stop();
         }
      } catch (Throwable error) {
         done = true; TYPE_MOON_WORLD.LOGGER.error("NPC_ANIMATION_CLIENT_SMOKE_FAIL ticks=" + ticks, error); mc.stop();
      }
   }

   private void sample(Minecraft mc, HeraclesEntity heracles, OdaNobunagaEntity oda) {
      var renderer = (HeraclesRenderer) mc.getEntityRenderDispatcher().getRenderer(heracles);
      var arm = renderer.getGeoModel().getAnimationProcessor().getBone("right arm");
      if (arm != null) {
         float rotation = arm.getRotX();
         require(Float.isFinite(rotation), "non-finite weapon arm rotation");
         if (Float.isFinite(previousArm) && Math.abs(rotation - previousArm) > .025F) movingSamples++;
         previousArm = rotation; smallestArm = Math.min(smallestArm, rotation); largestArm = Math.max(largestArm, rotation);
      }
      if (mc.getEntityRenderDispatcher().getRenderer(oda) instanceof HumanoidServantRenderer<?> humanoid) {
         var model = humanoid.getModel();
         require(Math.abs(model.rightArm.xRot - model.rightSleeve.xRot) < .0001F, "Oda sleeve separated from animated arm");
         require(Math.abs(model.body.yRot - model.jacket.yRot) < .0001F, "Oda jacket separated from animated torso");
      }
   }

   private void setup(Minecraft mc) {
      var server = mc.getSingleplayerServer(); var level = server.overworld();
      var player = server.getPlayerList().getPlayer(mc.player.getUUID());
      player.setGameMode(GameType.CREATIVE);
      level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server); level.setDayTime(6000);
      var old = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
      for (var entity : level.getAllEntities()) if (entity instanceof net.minecraft.world.entity.Mob) old.add(entity);
      old.forEach(net.minecraft.world.entity.Entity::discard);
      for (int x = -10; x <= 10; x++) for (int z = -8; z <= 12; z++) {
         level.setBlock(new BlockPos(x, 100, z), Blocks.BEDROCK.defaultBlockState(), 2);
         for (int y = 101; y <= 108; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
      }
      player.teleportTo(.5, 101, 9.5); player.setYRot(180); player.setXRot(-3);
      var heracles = ModEntities.HERACLES.get().create(level);
      var oda = ModEntities.ODA_NOBUNAGA.get().create(level);
      heracles.readAdditionalSaveData(heracles.saveWithoutId(new CompoundTag()));
      oda.readAdditionalSaveData(oda.saveWithoutId(new CompoundTag()));
      heracles.setNoAi(true); heracles.setNoGravity(true); heracles.setPos(-1.5, 101, 2.5);
      oda.setNoAi(true); oda.setNoGravity(true); oda.setPos(2.5, 101, 2.5);
      heracles.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(ModItems.TEMPLE_STONE_SWORD_AXE.get()));
      level.addFreshEntity(heracles); level.addFreshEntity(oda);
      var target = EntityType.HUSK.create(level); target.setNoAi(true); target.setNoGravity(true);
      target.setPos(.5, 101, 5.5); target.setInvisible(true);
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000); target.setHealth(100000);
      level.addFreshEntity(target); heracles.setTarget(target); oda.setTarget(target);
      heraclesId = heracles.getUUID(); odaId = oda.getUUID();
   }

   private HeraclesEntity npc(Minecraft mc) {
      return (HeraclesEntity) mc.getSingleplayerServer().overworld().getEntity(heraclesId);
   }

   private OdaNobunagaEntity oda(Minecraft mc) {
      return (OdaNobunagaEntity) mc.getSingleplayerServer().overworld().getEntity(odaId);
   }

   private void server(Minecraft mc, Runnable work) {
      mc.getSingleplayerServer().execute(() -> { try { work.run(); } catch (Throwable error) { failure = error.toString(); } });
   }

   private static void capture(Minecraft mc, String name) {
      File directory = new File(mc.gameDirectory, "qa"); directory.mkdirs();
      Screenshot.grab(directory, name + ".png", mc.getMainRenderTarget(), ignored -> {});
   }

   private static void require(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }
}
