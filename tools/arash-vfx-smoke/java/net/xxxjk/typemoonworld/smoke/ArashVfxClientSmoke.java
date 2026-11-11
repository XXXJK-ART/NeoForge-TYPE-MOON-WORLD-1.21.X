package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import net.minecraft.client.Minecraft;
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
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashTacticalSkillService;

@Mod(value = "typemoonworld_arash_vfx_smoke", dist = Dist.CLIENT)
public final class ArashVfxClientSmoke {
   private static final String WORLD = "Arash-VFX-QA";
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
         });
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
         if (ticks == 220) {
            done = true;
            TYPE_MOON_WORLD.LOGGER.info("ARASH_VFX_CLIENT_SMOKE_OK: flight stages, bow charge and real-client retreat");
            mc.stop();
         }
      } catch (Throwable error) {
         done = true;
         TYPE_MOON_WORLD.LOGGER.error("ARASH_VFX_CLIENT_SMOKE_FAILED", error);
         mc.stop();
      }
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
         var arrow = new ArashParticleArrowEntity(level, player, ArashParticleArrowEntity.NORMAL, 10.0F, 100);
         var data = new CompoundTag();
         arrow.addAdditionalSaveData(data);
         data.putInt("FlightTicks", i == 0 ? 0 : i == 1 ? 85 : 165);
         arrow.readAdditionalSaveData(data);
         arrow.setPos(-3.5 + i * 4.0, -57.4, 3.5);
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
