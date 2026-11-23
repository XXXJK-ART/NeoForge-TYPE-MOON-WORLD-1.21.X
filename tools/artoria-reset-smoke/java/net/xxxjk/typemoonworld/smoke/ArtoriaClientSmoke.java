package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
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
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaStateService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;

@Mod(value = "typemoonworld_artoria_smoke", dist = Dist.CLIENT)
public final class ArtoriaClientSmoke {
   private boolean opened, done;
   private int ticks;
   private UUID npcId;
   private volatile String failure;
   private volatile boolean verified;
   private ArtoriaExcaliburBeamEntity beam;
   public ArtoriaClientSmoke() { NeoForge.EVENT_BUS.register(this); }
   @SubscribeEvent public void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (done || mc.getOverlay() != null) return;
      try {
         require(failure == null, failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished(); mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened = true; mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(4); mc.options.simulationDistance().set(5);
            mc.getWindow().setWindowed(1280, 720);
            mc.createWorldOpenFlows().createFreshLevel("Artoria-QA-" + System.currentTimeMillis(),
               new LevelSettings("Artoria QA", GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
               new WorldOptions(42, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                  .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         if (ticks == 20) server(mc, () -> setup(mc));
         if (ticks == 65) {
            require(mc.player.getMainHandItem().is(ModItems.EXCALIBUR.get()), "Sword did not synchronize");
            var data = mc.player.getMainHandItem().get(DataComponents.CUSTOM_DATA);
            require(data != null && data.copyTag().getBoolean("ServantCardArtoriaWindVeiled"), "Player wind veil missing");
            net.minecraft.world.entity.Entity npc = null;
            for (var candidate : mc.level.entitiesForRendering()) if (candidate.getUUID().equals(npcId)) { npc = candidate; break; }
            require(npc != null && npc instanceof net.minecraft.world.entity.LivingEntity living
               && living.getMainHandItem().is(ModItems.EXCALIBUR.get()), "NPC lost its veiled real weapon");
            capture(mc, "artoria-wind-first-person");
         }
         if (ticks == 85) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
         if (ticks == 100) capture(mc, "artoria-wind-third-person");
         if (ticks == 115) server(mc, () -> {
            var owner = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            require(ServantCardTransformManager.triggerAction(owner, 1), "Wind hammer rejected");
            require(ServantCardTransformManager.triggerAction(owner, 0), "Burst rejected");
         });
         if (ticks == 128) server(mc, () -> {
            var server = mc.getSingleplayerServer();
            var player = server.getPlayerList().getPlayer(mc.player.getUUID());
            ArtoriaStateService.autoReleaseWindBarrier(player);
            var npc = (net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity)server.overworld().getEntity(npcId);
            net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem.forcePhaseAtLeast(npc,
               net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase.DECISIVE);
            ArtoriaStateService.startBurst(npc);
            ArtoriaStateService.autoReleaseWindBarrier(npc);
         });
         if (ticks == 140) {
            var playerSwordData = mc.player.getMainHandItem().get(DataComponents.CUSTOM_DATA);
            require(playerSwordData != null && playerSwordData.copyTag().getBoolean("ArtoriaManaBurstActive")
               && !playerSwordData.copyTag().getBoolean("ServantCardArtoriaWindVeiled"),
               "Player Mana Burst state did not reach Excalibur renderer");
            net.minecraft.world.entity.Entity npcEntity = null;
            for (var candidate : mc.level.entitiesForRendering()) if (candidate.getUUID().equals(npcId)) { npcEntity = candidate; break; }
            require(npcEntity instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity,
               "NPC was missing during exposed-blade smoke capture");
            var npcSwordData = ((net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity)npcEntity)
               .getMainHandItem().get(DataComponents.CUSTOM_DATA);
            require(npcSwordData != null && npcSwordData.copyTag().getBoolean("ArtoriaManaBurstActive")
               && !npcSwordData.copyTag().getBoolean("ServantCardArtoriaWindVeiled"),
               "NPC Mana Burst sword state did not synchronize after phase three");
            capture(mc, "artoria-mana-burst-full-blade");
         }
         if (ticks == 145) mc.options.setCameraType(CameraType.FIRST_PERSON);
         if (ticks == 150) capture(mc, "artoria-mana-burst-first-person");
         if (ticks == 170) { mc.options.setCameraType(CameraType.FIRST_PERSON); mc.player.setXRot(0); }
         if (ticks == 179) mc.options.keyShift.setDown(true);
         if (ticks == 180) server(mc, () -> {
            var owner = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            owner.startUsingItem(InteractionHand.MAIN_HAND);
            require(net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper.startExcaliburCharge(owner),
               "Server did not accept the full Excalibur charge");
         });
         if (ticks == 350) {
            server(mc, () -> {
               var owner = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
               require(owner.getPersistentData().hasUUID(ArtoriaStateService.NP_BEAM), "Held right click did not emit Excalibur at shared release point");
               beam = (ArtoriaExcaliburBeamEntity)owner.serverLevel().getEntity(owner.getPersistentData().getUUID(ArtoriaStateService.NP_BEAM));
            });
            mc.player.setXRot(-80);
         }
         if (ticks == 365) capture(mc, "artoria-excalibur-sky");
         if (ticks == 380) mc.player.setXRot(80);
         if (ticks == 395) capture(mc, "artoria-excalibur-ground");
         if (ticks == 410) { mc.player.setXRot(0); mc.player.setYRot(mc.player.getYRot() + 180); }
         if (ticks == 430) capture(mc, "artoria-excalibur-turn");
         if (ticks == 495) {
            mc.options.keyUse.setDown(false);
            mc.gameMode.releaseUsingItem(mc.player);
            mc.options.keyShift.setDown(false);
            server(mc, () -> {
               require(beam != null && beam.isRemoved() && beam.emittedPulses() == 15, "Client cast did not finish with 15 pulses");
               var owner = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
               require(!owner.getPersistentData().hasUUID(ArtoriaStateService.NP_BEAM), "Completed cast left a beam token");
               verified = true;
            });
         }
         if (ticks >= 520) {
            require(verified, "Server verification did not finish");
            TYPE_MOON_WORLD.LOGGER.info("ARTORIA CLIENT SMOKE PASS: first/third person veil, NPC weapon, revealed sword, held cast, sky/ground/turn, fifteen pulses.");
            done = true; mc.stop();
         }
      } catch (Throwable error) {
         failure = error.toString(); done = true; mc.options.keyUse.setDown(false);
         TYPE_MOON_WORLD.LOGGER.error("ARTORIA CLIENT SMOKE FAILED", error); mc.stop();
      }
   }
   private void setup(Minecraft mc) {
      var level = mc.getSingleplayerServer().overworld();
      var owner = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
      level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, mc.getSingleplayerServer());
      for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
         level.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
         for (int y = 101; y <= 107; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
      }
      owner.teleportTo(.5, 101, .5); owner.setYRot(0); owner.setXRot(0);
      require(ServantCardTransformManager.transform(owner, "artoria_pendragon"), "Saber transform failed");
      owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(1000);
      var npc = ModEntities.ARTORIA_PENDRAGON.get().create(level);
      npc.setNoAi(true); npc.setPos(3.5, 101, 4.5); npc.setYRot(180); level.addFreshEntity(npc);
      npc.ensureDefaultNpcLoadout(false); npcId = npc.getUUID();
   }
   private void server(Minecraft mc, Runnable action) {
      mc.getSingleplayerServer().execute(() -> { try { action.run(); } catch (Throwable error) { failure = error.toString(); } });
   }
   private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
   private static void capture(Minecraft mc, String name) {
      File directory = new File(mc.gameDirectory, "qa"); directory.mkdirs();
      Screenshot.grab(directory, name + ".png", mc.getMainRenderTarget(), ignored -> { });
   }
}
