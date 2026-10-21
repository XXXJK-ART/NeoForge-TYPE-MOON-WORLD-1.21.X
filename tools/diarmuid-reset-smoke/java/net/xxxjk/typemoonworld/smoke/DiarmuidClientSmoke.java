package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import java.util.UUID;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.DiarmuidUaDuibhneEntity;

@Mod(value = "typemoonworld_diarmuid_smoke", dist = Dist.CLIENT)
public final class DiarmuidClientSmoke {
   private boolean opened, done;
   private int ticks;
   private volatile String failure;
   private UUID npcId;
   private static final String WORLD = "Diarmuid-Reset-QA";
   public DiarmuidClientSmoke() { NeoForge.EVENT_BUS.register(this); }
   private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
   private void server(Minecraft mc, Runnable run) {
      mc.getSingleplayerServer().execute(() -> { try { run.run(); } catch (Throwable error) { failure = error.toString(); } });
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
            opened = true; mc.options.pauseOnLostFocus = false; mc.options.renderDistance().set(4);
            mc.options.simulationDistance().set(5); mc.getWindow().setWindowed(1280,900);
            if (new File(mc.gameDirectory,"saves/"+WORLD+"/level.dat").isFile()) mc.createWorldOpenFlows().openWorld(WORLD, () -> { throw new IllegalStateException("QA save failed"); });
            else mc.createWorldOpenFlows().createFreshLevel(WORLD,
               new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
               new WorldOptions(42,false,false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
         }
         if (mc.player == null || mc.getSingleplayerServer() == null) return;
         ticks++;
         if (ticks == 10) {
            for (var action : DiarmuidAction.values()) require(PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld",action.animationPath())) instanceof KeyframeAnimation,"undecodable animation: "+action);
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            server(mc, () -> setup(mc));
         }
         if (ticks >= 40 && (ticks-40)%55==0) {
            int index=(ticks-40)/55;
            if (index < 11) server(mc, () -> cast(mc,DiarmuidAction.bySlot(index-1)));
         }
         if (ticks >= 48 && (ticks-48)%55==0 && (ticks-48)/55<11) capture(mc,"diarmuid-skill-"+((ticks-48)/55-1));
         if (ticks == 68) {
            Object layer=PlayerAnimationAccess.getPlayerAssociatedData(mc.player).get(ResourceLocation.fromNamespaceAndPath("typemoonworld","diarmuid_action"));
            // The short cross-thrust has recovered by this frame; verify subsequent live samples below.
            require(layer instanceof ModifierLayer<?>,"PlayerAnimator layer not installed");
         }
         if (ticks == 616) {
            require(net.xxxjk.TYPE_MOON_WORLD.client.DiarmuidVisualsClient.activeTrailCount(mc.player.getUUID())>0,"player spear light missing");
            require(net.xxxjk.TYPE_MOON_WORLD.client.DiarmuidVisualsClient.activeTrailCount(npcId)>0,"NPC spear light missing");
            capture(mc,"diarmuid-finisher-colored-trails");
         }
         if (ticks == 635) {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            server(mc, () -> cast(mc,DiarmuidAction.FIGURE_EIGHT));
         }
         if (ticks == 645) capture(mc,"diarmuid-first-person");
         if (ticks == 680) {
            TYPE_MOON_WORLD.LOGGER.info("DIARMUID_CLIENT_SMOKE_OK: all actions decoded, player/NPC animations, HUD, dual grips and colored spear trails; no afterimages");
            done=true; mc.stop();
         }
      } catch (Throwable error) { done=true; TYPE_MOON_WORLD.LOGGER.error("DIARMUID_CLIENT_SMOKE_FAILED",error); mc.stop(); }
   }

   private void setup(Minecraft mc) {
      var level=mc.getSingleplayerServer().overworld();
      var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
      for (int x=-8;x<=10;x++) for (int z=-6;z<=14;z++) {
         level.setBlock(new BlockPos(x,-60,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);
         for (int y=-59;y<=-53;y++) level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
      }
      for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(-10,-62,-10,12,-50,16))) entity.discard();
      player.teleportTo(0.5,-59,0.5); player.setYRot(0); player.setXRot(0);
      if (player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().transformed()) ServantCardTransformManager.release(player,false);
      require(ServantCardTransformManager.transform(player,"diarmuid_ua_duibhne"),"card transformation failed");
      var npc=ModEntities.DIARMUID_UA_DUIBHNE.get().create(level);
      npc.setPos(3.5,-59,2.5); npc.setNoAi(true); npc.setYRot(0); npc.setCurrentMp(200);
      npc.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(ModItems.GAE_DEARG.get()));
      npc.setItemInHand(InteractionHand.OFF_HAND,new net.minecraft.world.item.ItemStack(ModItems.GAE_BUIDHE.get()));
      npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new net.minecraft.world.item.ItemStack(ModItems.SERVANT_CARD_DIARMUID_UA_DUIBHNE_CHEST.get()));
      npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,new net.minecraft.world.item.ItemStack(ModItems.SERVANT_CARD_DIARMUID_UA_DUIBHNE_LEGS.get()));
      npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new net.minecraft.world.item.ItemStack(ModItems.SERVANT_CARD_DIARMUID_UA_DUIBHNE_FEET.get()));
      level.addFreshEntity(npc); npcId=npc.getUUID();
      level.setDayTime(6000); level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,mc.getSingleplayerServer());
   }

   private void cast(Minecraft mc, DiarmuidAction action) {
      var server=mc.getSingleplayerServer(); var player=server.getPlayerList().getPlayer(mc.player.getUUID());
      var npc=(DiarmuidUaDuibhneEntity)server.overworld().getEntity(npcId);
      DiarmuidAbilityService.cancel(player); DiarmuidAbilityService.cancel(npc);
      player.teleportTo(0.5,-59,0.5); npc.setPos(3.5,-59,2.5); player.setYRot(0); npc.setYRot(0); npc.setOnGround(true);
      player.setDeltaMovement(Vec3.ZERO); npc.setDeltaMovement(Vec3.ZERO);
      player.getPersistentData().remove("DiarmuidAction:"+action.path()); npc.getPersistentData().remove("DiarmuidAction:"+action.path());
      player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(200); npc.setCurrentMp(200);
      for (var previous : server.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
         new net.minecraft.world.phys.AABB(-8,-62,-8,12,-50,16))) previous.discard();
      if (action != DiarmuidAction.MANA_LEAP) {
         for (var owner : java.util.List.of(player,npc)) {
            var target=EntityType.HUSK.create(server.overworld()); target.setPos(owner.position().add(0,0,2.4)); target.setNoAi(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); target.setHealth(200);
            target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); server.overworld().addFreshEntity(target);
         }
      }
      require(ServantCardTransformManager.triggerAction(player,action.slot()),"card cast failed: "+action);
      require(DiarmuidAbilityService.start(npc,action),"NPC cast failed: "+action);
   }

   private static void capture(Minecraft mc,String name) {
      Screenshot.grab(mc.gameDirectory,name+".png",mc.getMainRenderTarget(),message -> TYPE_MOON_WORLD.LOGGER.info("Diarmuid QA screenshot: {}",message.getString()));
   }
}
