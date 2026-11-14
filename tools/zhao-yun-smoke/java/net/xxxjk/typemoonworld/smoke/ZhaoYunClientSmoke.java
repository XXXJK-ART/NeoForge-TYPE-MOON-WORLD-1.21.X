package net.xxxjk.typemoonworld.smoke;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import java.io.File;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
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
import net.xxxjk.TYPE_MOON_WORLD.client.ZhaoYunSpearTrailClient;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ZhaoYunRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;

@Mod(value = "typemoonworld_zhao_yun_smoke", dist = Dist.CLIENT)
public final class ZhaoYunClientSmoke {
   private boolean opened, done, checked;
   private int ticks;
   private static final String WORLD = "Zhao-Yun-Presentation-QA";
   public ZhaoYunClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent public void tick(ClientTickEvent.Post event) {
      var mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished(); mc.setScreen(new TitleScreen());
         }
         if (!opened && mc.screen instanceof TitleScreen) open(mc);
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         mc.player.setYRot(180); mc.player.setXRot(8);
         ticks++;
         if (ticks == 5) setup(mc);
         if (ticks == 25) {
            for (String name : new String[]{"thrust", "sweep", "dance", "combo", "held_dance", "recovery"}) {
               require(PlayerAnimationRegistry.getAnimation(id("zhao_yun_spear_" + name)) instanceof KeyframeAnimation,
                  "clip did not load: " + name);
            }
            for (String name : new String[]{"attack", "fail", "victory", "np"}) {
               require(mc.getSoundManager().getSoundEvent(id("zhao_yun_voice_" + name)) != null, "voice missing: " + name);
            }
         }
         if (ticks >= 30 && ticks < 85 && ticks % 25 == 5) {
            int index = 0;
            for (var entity : mc.level.entitiesForRendering()) if (entity instanceof ZhaoYunRiderEntity zhao) {
               ServantBaseAnimationClient.play(zhao.getUUID(), 11 + index++ % 3);
            }
            ServantBaseAnimationClient.play(mc.player.getUUID(), ServantBaseAction.ZHAO_YUN_DANCE);
         }
         if (ticks == 42 || ticks == 72) {
            var field = ZhaoYunSpearTrailClient.class.getDeclaredField("TRAILS"); field.setAccessible(true);
            require(!((Map<?, ?>)field.get(null)).isEmpty(), "rendered weapon never recorded its tip");
            Screenshot.grab(mc.gameDirectory, "zhao-yun-spear-" + ticks + ".png", mc.getMainRenderTarget(), ignored -> { });
            checked = true;
         }
         if (ticks >= 90 && ticks < 135 && (ticks - 90) % 8 == 0) playAll(mc, ServantBaseAction.ZHAO_YUN_HELD_DANCE);
         if (ticks == 105) {
            ServantBaseAnimationClient.stopAttack(mc.player.getUUID());
            require(ServantBaseAnimationClient.activeAction(mc.player, 0).action() == ServantBaseAction.ZHAO_YUN_HELD_DANCE,
               "releasing the basic attack interrupted held skill 6");
         }
         if (ticks == 130) {
            var active = ServantBaseAnimationClient.activeAction(mc.player, 0);
            require(active != null && active.action() == ServantBaseAction.ZHAO_YUN_HELD_DANCE && active.age() >= 35,
               "held dance restarted or stopped across refreshes");
            Screenshot.grab(mc.gameDirectory, "zhao-yun-held-dance.png", mc.getMainRenderTarget(), ignored -> { });
         }
         if (ticks == 135) playAll(mc, ServantBaseAction.ZHAO_YUN_RECOVERY);
         if (ticks == 140) playAll(mc, ServantBaseAction.ZHAO_YUN_RECOVERY);
         if (ticks == 141) {
            require(ServantBaseAnimationClient.activeAction(mc.player, 0).action() == ServantBaseAction.ZHAO_YUN_RECOVERY,
               "release did not enter recovery");
            require(ServantBaseAnimationClient.activeAction(mc.player, 0).age() >= 5,
               "the server echo restarted a predicted recovery");
            Screenshot.grab(mc.gameDirectory, "zhao-yun-recovery.png", mc.getMainRenderTarget(), ignored -> { });
         }
         if (ticks == 165) {
            require(checked, "scene was not checked");
            require(ServantBaseAnimationClient.activeAction(mc.player, 0) == null, "recovery did not finish");
            var field = ZhaoYunSpearTrailClient.class.getDeclaredField("TRAILS"); field.setAccessible(true);
            require(((Map<?, ?>)field.get(null)).isEmpty(), "weapon light survived its recovery");
            TYPE_MOON_WORLD.LOGGER.info("ZHAO_YUN_CLIENT_SMOKE_PASS clips=6 voices=4 rendered_tip=true held_refresh=true recovery=true cleanup=true");
            done = true; mc.stop();
         }
      } catch (Throwable error) {
         TYPE_MOON_WORLD.LOGGER.error("ZHAO_YUN_CLIENT_SMOKE_FAIL tick=" + ticks, error);
         done = true; mc.stop();
      }
   }

   private void playAll(Minecraft mc, int action) {
      for (var entity : mc.level.entitiesForRendering()) if (entity instanceof ZhaoYunRiderEntity zhao) {
         ServantBaseAnimationClient.play(zhao.getUUID(), action);
      }
      ServantBaseAnimationClient.play(mc.player.getUUID(), action);
   }

   private void setup(Minecraft mc) {
      mc.getSingleplayerServer().execute(() -> {
         var server = mc.getSingleplayerServer(); var level = server.overworld();
         var old = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
         for (var entity : level.getAllEntities()) if (entity instanceof ZhaoYunRiderEntity) old.add(entity);
         old.forEach(net.minecraft.world.entity.Entity::discard);
         level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
         level.setDayTime(6000);
         for (int x = -10; x <= 10; x++) for (int z = -8; z <= 12; z++) {
            level.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
            for (int y = 101; y <= 106; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
         }
         var player = server.getPlayerList().getPlayer(mc.player.getUUID());
         player.connection.teleport(0.5, 101, 7.5, 180, 8);
         player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.YAJIAO_QIANG.get()));
         for (int index = 0; index < 3; index++) {
            var zhao = ModEntities.ZHAO_YUN_RIDER.get().create(level);
            zhao.setNoAi(true); zhao.setPos(-3.5 + 4 * index, 101, 0.5);
            zhao.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.YAJIAO_QIANG.get()));
            level.addFreshEntity(zhao);
         }
      });
   }

   private void open(Minecraft mc) {
      opened = true; mc.options.pauseOnLostFocus = false;
      mc.options.hideGui = true;
      mc.options.renderDistance().set(4); mc.options.simulationDistance().set(4);
      mc.getWindow().setWindowed(1280, 900);
      if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile()) {
         mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA save failed"); });
      } else {
         mc.createWorldOpenFlows().createFreshLevel(WORLD,
            new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(42, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
               .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
      }
   }

   private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", path); }
   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
