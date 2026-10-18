package net.xxxjk.typemoonworld.smoke;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.File;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardKeybindConfig;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardSkillInputController;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.ServantCardKeybindScreen;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.vfx.client.*;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEnvironmentDefinition;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXScreenEffectDefinition;
import org.lwjgl.glfw.GLFW;

/** Real framebuffer checks; this test-only mod is never included in release artifacts. */
@Mod(value = "typemoonworld_ui_vfx_smoke", dist = Dist.CLIENT)
public final class ClientUiVfxSmoke {
   private static final String WORLD = "UI-VFX-QA";
   private boolean opened, done;
   private int ticks;
   private int[] skyBeforeFlash;
   private int[] groundBeforeFlash;
   private int[] skyBeforeTint;
   private int[] groundBeforeTint;
   private int[] stableScenePixels;
   private int checkedFrames;
   private int maximumSceneDelta;
   private boolean receivedCombatLightning;

   public ClientUiVfxSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent
   public void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished();
            mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(4);
            mc.options.simulationDistance().set(6);
            mc.options.guiScale().set(2);
            mc.getWindow().setWindowed(1280, 720);
            mc.getLanguageManager().setSelected("zh_cn");
            mc.options.languageCode = "zh_cn";
            mc.reloadResourcePacks().thenRun(() -> mc.execute(() -> openWorld(mc)));
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         // Window changes can leave the pause menu open in an unattended run.
         // The scene renderer skips VFX behind menus, so close it explicitly.
         if (ticks < 320 && mc.screen != null) mc.setScreen(null);
         if (ticks == 1) initialize(mc);
         if (ticks == 20) {
            require(!mc.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_transformed, "normal-form input test started transformed");
            mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.literal("SPIRITUALIZATION_INPUT_SENTINEL"), false);
            var spirit = com.captain.servant.client.SpiritualizationClient.SPIRITUALIZATION_KEY;
            KeyMapping.click(spirit.getKey());
         }
         if (ticks == 40) {
            var overlay = net.minecraft.client.gui.Gui.class.getDeclaredField("overlayMessageString");
            overlay.setAccessible(true);
            var message = (net.minecraft.network.chat.Component) overlay.get(mc.gui);
            require(message != null && "SPIRITUALIZATION_INPUT_SENTINEL".equals(message.getString()), "left Ctrl displayed a spiritualization warning in normal form");
            TYPE_MOON_WORLD.LOGGER.info("SPIRITUALIZATION_NORMAL_CTRL_SMOKE_PASS");
         }
         if (ticks == 80) capture(mc, "sky-before", true);
         if (ticks == 100) {
            Config.vfxBloom = true;
            VFXRenderManager.spawnTest(0, 106, 9);
         }
         if (ticks == 110) capture(mc, "sky-bloom", true);
         if (ticks == 120) mc.getWindow().setWindowed(960, 540);
         if (ticks == 130) capture(mc, "sky-bloom-resized", true);
         if (ticks == 160) {
            VFXRenderManager.clear();
            mc.getWindow().setWindowed(1280, 720);
         }
         if (ticks == 179) {
            require(!mc.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "night vision contaminates the scene baseline");
            skyBeforeFlash = sampleSky(mc);
            groundBeforeFlash = sampleGround(mc);
            stableScenePixels = sampleStableScene(mc);
         }
         if (ticks == 180) VFXPostProcessManager.add(List.of(new VFXScreenEffectDefinition("white_flash", 1.0F, 3, 0)));
         // Exercise the independent legacy duel payload, including repeated
         // 0.1-second requests that the old screen-effect-only check missed.
         if (ticks >= 192 && ticks <= 212 && ticks % 2 == 0) mc.getSingleplayerServer().execute(() -> {
            var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork.sendToPlayer(p,
               new net.xxxjk.TYPE_MOON_WORLD.network.DuelScreenFlashMessage(2, 1.0F));
         });
         if (ticks == 205) {
            for (String type : new String[]{"whiteout", "whiteout_wave", "chromatic_aberration", "color_split",
               "time_stop", "timestop", "timestop_border", "radial_blur", "impact_blur", "vignette",
               "glass_break", "glass_shatter", "portal_rift"}) {
               VFXPostProcessManager.add(List.of(new VFXScreenEffectDefinition(type, 1.0F, 2, 128)));
            }
         }
         if (ticks == 190) {
            requireSimilar(skyBeforeFlash, sampleSky(mc), 8, "white flash changed sky");
            requireSimilar(groundBeforeFlash, sampleGround(mc), 8, "white flash changed ground");
            capture(mc, "sky-screen-effect", true);
         }
         if (ticks == 220) {
            for (String type : new String[]{"world_desaturation", "world_darkness", "red_fog", "thunderstorm_hint",
               "rain_weather", "thunderstorm_weather"}) {
               VFXEnvironmentManager.add(new VFXEnvironmentDefinition(type, 0xFFFF0000, 0, 3, 0, 0, 1.0F, 128), 0, 105, 13);
            }
         }
         if (ticks == 230) {
            capture(mc, "sky-desaturation", true);
            skyBeforeTint = sampleSky(mc);
            groundBeforeTint = sampleGround(mc);
         }
         if (ticks == 235) {
            VFXEnvironmentManager.add(new VFXEnvironmentDefinition("screen_tint", 0xFFFF0000, 0, 1, 0.0F, 0.0F, 1.0F, 128), 0, 105, 13);
         }
         if (ticks == 240) {
            requireSimilar(skyBeforeTint, sampleSky(mc), 8, "screen tint changed sky");
            requireSimilar(groundBeforeTint, sampleGround(mc), 8, "screen tint changed ground");
            capture(mc, "screen-tint-disabled", true);
         }
         if (ticks == 242) mc.getSingleplayerServer().execute(() -> {
            var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(mc.getSingleplayerServer().overworld());
            require(bolt != null, "combat lightning was not created");
            bolt.moveTo(30, 105, 13);
            bolt.setNoGravity(true);
            bolt.setVisualOnly(true);
            mc.getSingleplayerServer().overworld().addFreshEntity(bolt);
         });
         if (ticks == 250) {
            require(net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXVanillaParticleDefinition.particleByName("flash")
               == net.minecraft.core.particles.ParticleTypes.END_ROD, "legacy flash still expands over the view");
            net.xxxjk.TYPE_MOON_WORLD.client.TimeAlterVisualClient.applyState(mc.player.getUUID(), true, 1, 12, 2.0);
         }
         if (ticks == 260) VFXRenderManager.clear();
         if (ticks == 265) {
            net.xxxjk.TYPE_MOON_WORLD.client.TimeAlterVisualClient.applyState(mc.player.getUUID(), false, 1, 0, 1.0);
            // Ordinary offscreen particles used to switch on bloom over the
            // entire framebuffer even though none of their geometry was visible.
            Config.vfxBloom = true;
            Config.vfxScreenEffects = true;
            VFXClientRuntime.spawn("ea_charge", 100, 106, 13, -1, 42);
         }
         if (ticks == 279) {
            require(checkedFrames >= 50, "too few consecutive render frames checked: " + checkedFrames);
            require(receivedCombatLightning, "combat lightning did not reach the client");
            require(VFXRenderManager.hasActiveEffects(), "ordinary world VFX were disabled");
            TYPE_MOON_WORLD.LOGGER.info("COMBAT_SCREEN_STABILITY_PASS frames={} maxChannelDelta={} duelPackets=11 screenTypes=14 environmentTypes=7 combatLightning=pass legacyConfig=true",
               checkedFrames, maximumSceneDelta);
         }
         if (ticks == 280) {
            VFXRenderManager.clear();
            String[] effects = {"ea_charge", "ea_beam", "servant_exchange_clash", "gilgamesh_ea_tree"};
            for (int i = 0; i < effects.length; i++) VFXClientRuntime.spawn(effects[i], -3 + i * 2, 106, 8, -1, 42);
         }
         if (ticks == 290) {
            require(VFXRenderManager.debugStats().matches(".*particles=[1-9][0-9]* .*"),
               "world-space material VFX did not render: " + VFXRenderManager.debugStats());
            capture(mc, "sky-materials", true);
         }
         if (ticks == 320) {
            VFXRenderManager.clear();
            mc.getSingleplayerServer().execute(() -> {
               var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
               p.setGameMode(GameType.SURVIVAL);
               p.setInvulnerable(true);
               var vars = p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
               vars.master_active = false;
               vars.master_card_active = false;
               ServantCardTransformManager.transform(p, "gilgamesh");
               vars.syncPlayerVariables(p);
            });
         }
         if (ticks == 345) {
            require(mc.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_transformed, "transformation not synchronized");
            checkProfiles();
            checkInputModes(mc);
            mc.options.guiScale().set(3);
            mc.resizeDisplay();
            mc.setScreen(new ServantCardKeybindScreen("gilgamesh"));
         }
         if (ticks == 355) { checkBounds(mc); capture(mc, "keybinds-small", false); }
         if (ticks == 365) {
            for (int row = 0; row < 10; row++) mc.screen.mouseScrolled(100, 100, 0, -1);
            checkBounds(mc);
            capture(mc, "keybinds-small-scrolled", false);
         }
         if (ticks == 375) {
            mc.setScreen(null);
            mc.getWindow().setWindowed(1600, 900);
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            mc.setScreen(new ServantCardKeybindScreen("gilgamesh"));
         }
         if (ticks == 390) {
            checkBounds(mc);
            capture(mc, "keybinds-large", false);
            Button bind = (Button) mc.screen.children().getFirst();
            bind.onPress();
            mc.screen.keyPressed(GLFW.GLFW_KEY_Z, 0, 0);
            require(bind.getMessage().getString().contains("按键"), "reserved key should keep capture open");
            capture(mc, "keybinds-reserved", false);
            mc.screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
            require(mc.screen != null, "Escape should cancel capture before closing");
         }
         if (ticks == 405) {
            mc.setScreen(null);
            capture(mc, "servant-mana", false);
         }
         if (ticks == 410) mc.getSingleplayerServer().execute(() -> {
            var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            ServantCardTransformManager.release(p, false);
            var vars = p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            vars.is_magus = true;
            vars.player_mana = 850;
            vars.player_max_mana = 1000;
            vars.syncPlayerVariables(p);
         });
         if (ticks == 420) capture(mc, "player-mana", false);
         if (ticks >= 425) {
            done = true;
            TYPE_MOON_WORLD.LOGGER.info("CLIENT_UI_VFX_SMOKE_PASS sky=bloom,resized,screen,desaturation,materials tint=off flash=off ui=small,scroll,large profiles=pass");
            mc.stop();
         }
      } catch (Throwable error) {
         done = true;
         TYPE_MOON_WORLD.LOGGER.error("CLIENT_UI_VFX_SMOKE_FAIL ticks=" + ticks, error);
         mc.stop();
      }
   }

   /** Check every completed render frame, including transient onset and expiry. */
   @SubscribeEvent
   public void checkSceneFrame(RenderFrameEvent.Post event) {
      if (done || ticks < 180 || ticks >= 279 || stableScenePixels == null) return;
      Minecraft mc = Minecraft.getInstance();
      try {
         require(mc.screen == null, "screen stability check ran behind a menu");
         int[] pixels = sampleStableScene(mc);
         require(pixels.length == stableScenePixels.length, "scene sample dimensions changed");
         for (int i = 0; i < pixels.length; i++) {
            for (int shift : new int[]{0, 8, 16}) {
               int delta = Math.abs(((pixels[i] >>> shift) & 255) - ((stableScenePixels[i] >>> shift) & 255));
               maximumSceneDelta = Math.max(maximumSceneDelta, delta);
               require(delta <= 4, "scene recolored at tick " + ticks + " sample " + i + " channel " + shift + " delta=" + delta
                  + " pos=" + mc.player.position() + " rot=" + mc.player.getYRot() + "," + mc.player.getXRot()
                  + " time=" + mc.level.getDayTime() + " effects=" + mc.player.getActiveEffects());
            }
         }
         require(mc.level.getRainLevel(1) == 0 && mc.level.getThunderLevel(1) == 0, "VFX changed world weather");
         require(mc.level.getSkyFlashTime() == 0, "combat lightning flashed the whole sky");
         for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof net.minecraft.world.entity.LightningBolt && entity.isNoGravity()) receivedCombatLightning = true;
         }
         checkedFrames++;
      } catch (Throwable error) {
         done = true;
         capture(mc, "combat-stability-failure", false);
         TYPE_MOON_WORLD.LOGGER.error("CLIENT_UI_VFX_SMOKE_FAIL frame ticks=" + ticks, error);
         mc.stop();
      }
   }

   private static int[] sampleStableScene(Minecraft mc) {
      java.util.ArrayList<Integer> samples = new java.util.ArrayList<>();
      try (var pixels = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         for (float[] region : new float[][]{{0.05F, 0.2F, 0.3F, 0.33F}, {0.05F, 0.65F, 0.25F, 0.82F}}) {
            for (int x = (int)(pixels.getWidth() * region[0]); x < pixels.getWidth() * region[2]; x += 12)
               for (int y = (int)(pixels.getHeight() * region[1]); y < pixels.getHeight() * region[3]; y += 8)
                  samples.add(pixels.getPixelRGBA(x, y));
         }
      }
      return samples.stream().mapToInt(Integer::intValue).toArray();
   }

   private void openWorld(Minecraft mc) {
      if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile()) {
         mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA world failed to open"); });
      } else {
         mc.createWorldOpenFlows().createFreshLevel(WORLD,
            new LevelSettings(WORLD, GameType.SPECTATOR, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(42L, false, false),
            registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
      }
   }

   private void initialize(Minecraft mc) {
      mc.getSingleplayerServer().execute(() -> {
         var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
         var level = p.serverLevel();
         ServantCardTransformManager.release(p, false);
         p.removeAllEffects();
         var vars = p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         vars.master_active = false;
         vars.master_card_active = false;
         vars.is_magus = false;
         vars.syncPlayerVariables(p);
         p.setGameMode(GameType.SPECTATOR);
         p.teleportTo(0.5, 105, 13.5);
         p.setYRot(180); p.setXRot(-10);
         level.setDayTime(6000);
         level.setWeatherParameters(99999, 0, false, false);
         level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
         for (var entity : level.getAllEntities()) if (entity instanceof net.minecraft.world.entity.Mob) entity.discard();
         for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) {
            level.setBlock(new net.minecraft.core.BlockPos(x, 100, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
         }
      });
   }

   private static void checkProfiles() {
      String id = "ui_vfx_test_only";
      var global = TypeMoonWorldModKeyMappings.SERVANT_CARD_SKILL_KEYS[0];
      var original = global.getKey();
      try {
         ServantCardKeybindConfig.reset(id);
         ServantCardKeybindConfig.keyFor(id, 0); // Cache the inherited profile before a global edit.
         global.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_J));
         ServantCardKeybindConfig.setKey(id, 1, InputConstants.Type.MOUSE.getOrCreate(4));
         require(ServantCardKeybindConfig.keyFor(id, 0).equals(global.getKey()), "editing one slot lost updated global keys");
         ServantCardKeybindConfig.setKey(id, 2, InputConstants.Type.MOUSE.getOrCreate(4));
         require(ServantCardKeybindConfig.keyFor(id, 1).equals(InputConstants.UNKNOWN), "duplicate skill key not cleared");
         require(ServantCardKeybindConfig.isReservedKey(TypeMoonWorldModKeyMappings.SERVANT_CARD_SKILL_SELECT.getKey()), "skill selector must be reserved");
         require(!TypeMoonWorldModKeyMappings.SERVANT_CARD_DODGE.getKeyConflictContext().conflicts(
            TypeMoonWorldModKeyMappings.MAGIC_WHEEL_SWITCH.getKeyConflictContext()), "exclusive forms must have separate contexts");
      } finally {
         global.setKey(original);
         KeyMapping.resetMapping();
         ServantCardKeybindConfig.reset(id);
      }
   }

   private static void checkBounds(Minecraft mc) {
      require(mc.screen instanceof ServantCardKeybindScreen, "keybind screen closed unexpectedly");
      for (var child : mc.screen.children()) if (child instanceof AbstractWidget widget) {
         require(widget.getX() >= 0 && widget.getY() >= 0 && widget.getRight() <= mc.screen.width && widget.getBottom() <= mc.screen.height, "widget outside viewport");
      }
   }

   private static void checkInputModes(Minecraft mc) {
      var vars = mc.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      var cast = TypeMoonWorldModKeyMappings.SERVANT_CARD_CAST;
      var info = TypeMoonWorldModKeyMappings.BASIC_INFORMATION_GUI;
      var selector = TypeMoonWorldModKeyMappings.SERVANT_CARD_SKILL_SELECT;
      try {
         require(com.captain.servant.client.SpiritualizationClient.SPIRITUALIZATION_KEY.getKeyConflictContext().isActive(), "spiritualization binding is inactive in servant form");
         KeyMapping.releaseAll();
         KeyMapping.click(cast.getKey());
         require(cast.consumeClick() && !info.consumeClick(), "R must only cast in servant form");
         selector.setDown(true);
         int before = ServantCardSkillInputController.selectedSlot(vars);
         require(ServantCardSkillInputController.handleScroll(mc.player, vars, 1), "Z must enable servant skill scrolling");
         require(ServantCardSkillInputController.selectedSlot(vars) != before, "skill selection did not change");
         selector.setDown(false);
         require(!ServantCardSkillInputController.handleScroll(mc.player, vars, 1), "normal scrolling must not select skills");
         vars.servant_card_transformed = false;
         KeyMapping.click(info.getKey());
         require(info.consumeClick() && !cast.consumeClick(), "R must only open information in normal form");
      } finally {
         vars.servant_card_transformed = true;
         KeyMapping.releaseAll();
      }
   }

   private static void capture(Minecraft mc, String name, boolean checkSky) {
      if (checkSky) try (var pixels = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         int dark = 0, total = 0;
         for (int x = pixels.getWidth() / 20; x < pixels.getWidth() * 3 / 10; x += 8)
            for (int y = pixels.getHeight() / 5; y < pixels.getHeight() / 3; y += 4) {
               int rgba = pixels.getPixelRGBA(x, y);
               if ((rgba & 255) < 8 && ((rgba >> 8) & 255) < 8 && ((rgba >> 16) & 255) < 8) dark++;
               total++;
            }
         require(dark < total / 100 + 1, name + ": unexpected black sky pixels " + dark + "/" + total);
      }
      File output = new File(mc.gameDirectory, "qa");
      output.mkdirs();
      Screenshot.grab(output, name + ".png", mc.getMainRenderTarget(), ignored -> { });
      TYPE_MOON_WORLD.LOGGER.info("CLIENT_UI_VFX_CAPTURE {}", name);
   }

   private static int[] sampleSky(Minecraft mc) {
      return sampleRegion(mc, 0.05F, 0.2F, 0.3F, 0.33F);
   }

   private static int[] sampleGround(Minecraft mc) {
      return sampleRegion(mc, 0.1F, 0.58F, 0.9F, 0.82F);
   }

   private static int[] sampleRegion(Minecraft mc, float left, float top, float right, float bottom) {
      try (var pixels = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         long red = 0, green = 0, blue = 0, count = 0;
         for (int x = (int)(pixels.getWidth() * left); x < pixels.getWidth() * right; x += 8) {
            for (int y = (int)(pixels.getHeight() * top); y < pixels.getHeight() * bottom; y += 4) {
               int rgba = pixels.getPixelRGBA(x, y);
               red += rgba & 255;
               green += (rgba >>> 8) & 255;
               blue += (rgba >>> 16) & 255;
               count++;
            }
         }
         return new int[]{(int)(red / count), (int)(green / count), (int)(blue / count)};
      }
   }

   private static void requireSimilar(int[] before, int[] after, int tolerance, String message) {
      for (int channel = 0; channel < before.length; channel++) {
         require(Math.abs(after[channel] - before[channel]) <= tolerance,
            message + " channel " + channel + " from " + before[channel] + " to " + after[channel]);
      }
   }

   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
