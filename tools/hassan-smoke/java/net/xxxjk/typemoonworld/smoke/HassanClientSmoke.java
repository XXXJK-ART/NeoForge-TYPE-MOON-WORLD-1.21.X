package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
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
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HassanZabaniyaService;

@Mod(value = "typemoonworld_hassan_smoke", dist = Dist.CLIENT)
public final class HassanClientSmoke {
   private static final String WORLD = "Hassan-QA";
   private boolean opened, done;
   private int ticks;
   private volatile String failure;

   public HassanClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent public void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         if (failure != null) throw new AssertionError(failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished(); mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true; mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(3); mc.options.simulationDistance().set(3);
            mc.getWindow().setWindowed(1100, 760);
            if (new File(mc.gameDirectory, "saves/" + WORLD + "/level.dat").isFile())
               mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA save failed"); });
            else mc.createWorldOpenFlows().createFreshLevel(WORLD,
               new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
               new WorldOptions(42, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                  .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         if (ticks == 10) server(mc, () -> {
            var server = mc.getSingleplayerServer(); var level = server.overworld();
            var player = server.getPlayerList().getPlayer(mc.player.getUUID());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server); level.setDayTime(6000);
            for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
               level.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
               for (int y = 101; y < 108; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
            player.teleportTo(0.5, 101, -3.5); player.setYRot(0); player.setXRot(4);
            require(ServantCardTransformManager.transform(player, "cursed_arm_hassan"), "transform failed");
            var npc = ModEntities.CURSED_ARM_HASSAN.get().create(level); npc.setNoAi(true); npc.moveTo(0.5, 101, 0.5, 180, 0); level.addFreshEntity(npc);
            var cow = EntityType.COW.create(level); cow.setNoAi(true); cow.setPos(5.5, 101, 2.5); level.addFreshEntity(cow);
            var golem = EntityType.IRON_GOLEM.create(level); golem.setNoAi(true); golem.setPos(-5.5, 101, 2.5); level.addFreshEntity(golem);
            require(HassanZabaniyaService.extract(player, golem), "player core extraction failed");
            require(HassanZabaniyaService.extract(npc, cow), "NPC heart extraction failed");
            npc.getPersistentData().putLong("HassanZabaniyaCrushAt", level.getGameTime() + 130);
         });
         if (ticks == 50) {
            for (var item : java.util.List.of(ModItems.ZABANIYA_HEART.get(), ModItems.ZABANIYA_MECHANICAL_CORE.get(), ModItems.ZABANIYA_DIVINE_CLAY.get()))
               require(mc.getItemRenderer().getModel(new ItemStack(item), mc.level, mc.player, 0) != mc.getModelManager().getMissingModel(), "missing item model");
            for (String name : java.util.List.of("attack1", "attack2", "fail1", "fail2", "victory1", "victory2", "zabaniya"))
               require(mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("typemoonworld", "sounds/voice/cursed_arm_hassan/" + name + ".ogg")).isPresent(), "missing voice " + name);
            capture(mc, "hassan-held-heart-and-core");
         }
         if (ticks >= 60 && ticks <= 180 && ticks % 20 == 0) {
            SoundEvent voice = switch ((ticks - 60) / 20) {
               case 0 -> ModSounds.CURSED_ARM_HASSAN_VOICE_ATTACK.get();
               case 1 -> ModSounds.CURSED_ARM_HASSAN_VOICE_FAIL.get();
               case 2 -> ModSounds.CURSED_ARM_HASSAN_VOICE_VICTORY.get();
               case 3 -> ModSounds.CURSED_ARM_HASSAN_VOICE_ZABANIYA.get();
               default -> ModSounds.CURSED_ARM_HASSAN_VOICE_ZABANIYA_SHORT.get();
            };
            require(mc.getSoundManager().getSoundEvent(voice.getLocation()) != null, "unresolved voice event");
            mc.getSoundManager().play(SimpleSoundInstance.forUI(voice, 1.0F));
         }
         if (ticks == 155) capture(mc, "hassan-after-crush");
         if (ticks == 210) {
            TYPE_MOON_WORLD.LOGGER.info("HASSAN_CLIENT_SMOKE_PASS models=3 voices=7 held_items=true");
            done = true; mc.stop();
         }
      } catch (Throwable error) {
         done = true; TYPE_MOON_WORLD.LOGGER.error("HASSAN_CLIENT_SMOKE_FAIL ticks=" + ticks, error); mc.stop();
      }
   }

   private void server(Minecraft mc, Runnable work) {
      mc.getSingleplayerServer().execute(() -> { try { work.run(); } catch (Throwable error) { failure = error.toString(); } });
   }
   private static void capture(Minecraft mc, String name) {
      File directory = new File(mc.gameDirectory, "qa"); directory.mkdirs();
      Screenshot.grab(directory, name + ".png", mc.getMainRenderTarget(), ignored -> {});
   }
   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
