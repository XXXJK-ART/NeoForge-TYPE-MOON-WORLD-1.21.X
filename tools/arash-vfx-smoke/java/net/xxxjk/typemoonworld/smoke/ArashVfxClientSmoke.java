package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashTacticalSkillService;

@Mod(value = "typemoonworld_arash_vfx_smoke", dist = Dist.CLIENT)
public final class ArashVfxClientSmoke {
   private static final String WORLD = "Arash-VFX-QA-" + System.currentTimeMillis();
   private boolean opened, done;
   private int ticks;
   private volatile String failure;
   private Vec3 retreatStart;

   public ArashVfxClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   private void server(Minecraft mc, Runnable action) {
      mc.getSingleplayerServer().execute(() -> {
         try { action.run(); } catch (Throwable error) { failure = error.toString(); }
      });
   }

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
            opened = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(5);
            mc.getWindow().setWindowed(1280, 900);
            if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile()) {
               mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA world failed"); });
            } else {
               mc.createWorldOpenFlows().createFreshLevel(WORLD,
                  new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                     new GameRules(), WorldDataConfiguration.DEFAULT),
                  new WorldOptions(42, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                     .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
            }
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         if (ticks == 15) server(mc, () -> setup(mc));
         if (ticks == 50) capture(mc, "arash-night-flight-stages.png");
         if (ticks == 55) server(mc, () -> {
            for (var arrow : mc.getSingleplayerServer().overworld()
               .getEntitiesOfClass(ArashParticleArrowEntity.class,
                  new net.minecraft.world.phys.AABB(-20, -65, -20, 20, -40, 20))) arrow.discard();
            var arrow = new ArashParticleArrowEntity(mc.getSingleplayerServer().overworld(), player(mc),
               ArashParticleArrowEntity.NORMAL, 10.0F, 100);
            arrow.setPos(-3.5, -57.4, 3.5);
            arrow.setDeltaMovement(0.6, 0.0, 0.0);
            mc.getSingleplayerServer().overworld().addFreshEntity(arrow);
         });
         if (ticks == 61) capture(mc, "arash-moving-blue-arrow.png");
         if (ticks == 65) {
            mc.options.keyUse.setDown(true);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         }
         if (ticks == 150) {
            if (!mc.player.isUsingItem() || mc.player.getTicksUsingItem() < 40) throw new AssertionError("Real right-click did not sustain bow charge");
            capture(mc, "arash-first-person-charge.png");
         }
         if (ticks == 165) {
            mc.options.keyUse.setDown(false);
         }
         if (ticks == 185) {
            retreatStart = mc.player.position();
            server(mc, () -> ArashTacticalSkillService.performRetreat(player(mc)));
         }
         if (ticks == 203) {
            double moved = mc.player.position().subtract(retreatStart).horizontalDistance();
            if (moved < 3.0) throw new AssertionError("Real client retreat moved only " + moved);
            TYPE_MOON_WORLD.LOGGER.info("ARASH_RETREAT_CLIENT_DISTANCE: {}", moved);
            capture(mc, "arash-after-retreat.png");
         }
         if (ticks == 210) server(mc, () -> {
            for (var entity : mc.getSingleplayerServer().overworld().getAllEntities()) {
               if (entity instanceof ArashParticleArrowEntity arrow && arrow.getOwner() == player(mc)) arrow.discard();
            }
            var vars = player(mc).getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            vars.servantCardState().setArashArrowStock(5000);
            vars.syncServantCardRuntime(player(mc));
         });
         if (ticks == 230) {
            TypeMoonWorldModKeyMappings.SERVANT_CARD_CAST.setDown(true);
            KeyMapping.click(TypeMoonWorldModKeyMappings.SERVANT_CARD_CAST.getKey());
         }
         if (ticks == 275) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
         if (ticks == 280) {
            if (mc.options.keyUse.isDown()) throw new AssertionError("Rain charge changed the physical use key");
            if (!mc.player.isUsingItem() || mc.player.getTicksUsingItem() < 35
               || !mc.player.getMainHandItem().is(ModItems.ARASH_BOW.get())) {
               throw new AssertionError("Rain skill did not sustain a visible main-hand bow draw without right-click: using="
                  + mc.player.isUsingItem() + ", useTicks=" + mc.player.getTicksUsingItem());
            }
            capture(mc, "arash-rain-third-person-charge.png");
            server(mc, () -> assertRainOnly(mc, false));
         }
         if (ticks == 282) mc.options.setCameraType(CameraType.FIRST_PERSON);
         if (ticks == 284) capture(mc, "arash-rain-first-person-charge.png");
         if (ticks == 290) TypeMoonWorldModKeyMappings.SERVANT_CARD_CAST.setDown(false);
         if (ticks == 293) capture(mc, "arash-rain-release.png");
         if (ticks == 300) server(mc, () -> assertRainOnly(mc, true));
         if (ticks == 310) server(mc, () -> {
            for (var entity : mc.getSingleplayerServer().overworld().getAllEntities()) {
               if (entity instanceof ArashParticleArrowEntity arrow && arrow.getOwner() == player(mc)) arrow.discard();
            }
         });
         if (ticks == 315) {
            TypeMoonWorldModKeyMappings.SERVANT_CARD_CAST.setDown(true);
            KeyMapping.click(TypeMoonWorldModKeyMappings.SERVANT_CARD_CAST.getKey());
         }
         if (ticks == 335) server(mc, () -> player(mc).hurt(player(mc).damageSources().genericKill(), 1.0F));
         if (ticks == 345) {
            if (mc.player.isUsingItem()) throw new AssertionError("Damage did not interrupt real-client rain charge");
            mc.options.keyUse.setDown(true);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         }
         if (ticks == 360) mc.options.keyUse.setDown(false);
         if (ticks == 365) {
            if (mc.player.isUsingItem()) throw new AssertionError("Interrupted rain kept ownership of the next normal bow draw");
            server(mc, () -> {
               int stock = player(mc).getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().arashArrowStock();
               if (stock != 4899) throw new AssertionError("Rain interruption changed the next-shot cost: " + stock);
               TYPE_MOON_WORLD.LOGGER.info("ARASH_RAIN_INTERRUPTION_CLIENT_OK: next normal bow shot cost one arrow");
            });
            TypeMoonWorldModKeyMappings.SERVANT_CARD_CAST.setDown(false);
         }
         if (ticks == 385) {
            done = true;
            TYPE_MOON_WORLD.LOGGER.info("ARASH_VFX_CLIENT_SMOKE_OK: flight stages, bow charge, retreat, skill-key rain charge/release without basic arrows");
            mc.stop();
         }
      } catch (Throwable error) {
         done = true;
         TYPE_MOON_WORLD.LOGGER.error("ARASH_VFX_CLIENT_SMOKE_FAILED", error);
         mc.stop();
      }
   }

   private void assertRainOnly(Minecraft mc, boolean released) {
      int normal = 0, rain = 0;
      var player = player(mc);
      for (var entity : mc.getSingleplayerServer().overworld().getAllEntities()) {
         if (entity instanceof ArashParticleArrowEntity arrow && arrow.getOwner() == player) {
            if (arrow.getVariant() == ArashParticleArrowEntity.NORMAL) normal++;
            if (arrow.getVariant() == ArashParticleArrowEntity.RAIN) rain++;
         }
      }
      int stock = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().arashArrowStock();
      if (normal != 0 || released && rain != 100 || !released && rain != 0 || stock != (released ? 4900 : 5000)) {
         throw new AssertionError("Rain input leaked shots or costs: released=" + released
            + ", normal=" + normal + ", rain=" + rain + ", stock=" + stock);
      }
      TYPE_MOON_WORLD.LOGGER.info("ARASH_RAIN_CLIENT_CHECK: released={}, normal={}, rain={}, stock={}", released, normal, rain, stock);
   }

   private net.minecraft.server.level.ServerPlayer player(Minecraft mc) {
      return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
   }

   private void setup(Minecraft mc) {
      var level = mc.getSingleplayerServer().overworld();
      var player = player(mc);
      for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
         new net.minecraft.world.phys.AABB(-20, -65, -20, 20, -40, 20), entity -> !(entity instanceof Player))) entity.discard();
      for (var arrow : level.getEntitiesOfClass(ArashParticleArrowEntity.class,
         new net.minecraft.world.phys.AABB(-20, -65, -20, 20, -40, 20))) arrow.discard();
      for (int x = -12; x <= 12; x++) for (int z = -18; z <= 16; z++) {
         level.setBlock(new BlockPos(x, -60, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
         for (int y = -59; y <= -50; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
      }
      player.teleportTo(0.5, -59.0, -6.5);
      player.setYRot(0.0F); player.setXRot(0.0F);
      if (player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().transformed()) {
         ServantCardTransformManager.release(player, false);
      }
      ServantCardTransformManager.transform(player, "arash");
      level.setDayTime(18000L);
      level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, mc.getSingleplayerServer());
      for (int i = 0; i < 3; i++) {
         var arrow = new ArashParticleArrowEntity(level, player, ArashParticleArrowEntity.SMALL_ENERGY, 10.0F, 100);
         var data = new CompoundTag();
         arrow.addAdditionalSaveData(data);
         data.putInt("FlightTicks", i == 0 ? 0 : i == 1 ? 85 : 165);
         arrow.readAdditionalSaveData(data);
         arrow.setPos(-3.5 + i * 4.0, -57.4, 3.5);
         // Side, oblique and steep views expose flat sprites during visual QA.
         arrow.setYRot(i == 0 ? 90.0F : 35.0F);
         arrow.yRotO = arrow.getYRot();
         arrow.setXRot(i == 2 ? 65.0F : 0.0F);
         arrow.xRotO = arrow.getXRot();
         arrow.setDeltaMovement(Vec3.ZERO);
         level.addFreshEntity(arrow);
      }
      var npc = ModEntities.ARASH.get().create(level);
      npc.setPos(6.5, -59.0, 0.5); npc.setNoAi(true); npc.setYRot(180.0F);
      npc.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ARASH_BOW.get()));
      level.addFreshEntity(npc);
      npc.startUsingItem(InteractionHand.MAIN_HAND);
   }

   private void capture(Minecraft mc, String name) {
      Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), ignored -> { });
   }
}
