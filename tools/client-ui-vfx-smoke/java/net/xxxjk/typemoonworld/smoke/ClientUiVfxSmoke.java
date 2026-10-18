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
            mc.options.simulationDistance().set(4);
            mc.options.guiScale().set(2);
            mc.getWindow().setWindowed(1280, 720);
            mc.getLanguageManager().setSelected("zh_cn");
            mc.options.languageCode = "zh_cn";
            mc.reloadResourcePacks().thenRun(() -> mc.execute(() -> openWorld(mc)));
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
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
         if (ticks == 180) VFXPostProcessManager.add(List.of(new VFXScreenEffectDefinition("white_flash", 0.15F, 3, 0)));
         if (ticks == 190) capture(mc, "sky-screen-effect", true);
         if (ticks == 220) VFXEnvironmentManager.add(new VFXEnvironmentDefinition("world_desaturation", -1, 0, 3, 0, 0, 0.7F, 128), 0, 105, 13);
         if (ticks == 230) capture(mc, "sky-desaturation", true);
         if (ticks == 260) VFXRenderManager.clear();
         if (ticks == 280) {
            String[] effects = {"ea_charge", "ea_beam", "servant_exchange_clash", "gilgamesh_ea_tree"};
            for (int i = 0; i < effects.length; i++) VFXClientRuntime.spawn(effects[i], -3 + i * 2, 106, 8, -1, 42);
         }
         if (ticks == 290) capture(mc, "sky-materials", true);
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
            TYPE_MOON_WORLD.LOGGER.info("CLIENT_UI_VFX_SMOKE_PASS sky=bloom,resized,screen,desaturation,materials ui=small,scroll,large profiles=pass");
            mc.stop();
         }
      } catch (Throwable error) {
         done = true;
         TYPE_MOON_WORLD.LOGGER.error("CLIENT_UI_VFX_SMOKE_FAIL ticks=" + ticks, error);
         mc.stop();
      }
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

   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
