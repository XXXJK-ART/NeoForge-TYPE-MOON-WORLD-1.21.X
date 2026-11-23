package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;

@Mod(value="typemoonworld_heracles_smoke",dist=Dist.CLIENT)
public final class HeraclesPressureClientSmoke {
   private static final String WORLD = "Heracles-Pressure-QA";
   private boolean opened, done;
   private int ticks;
   private UUID npcId;
   private volatile String failure;
   private volatile boolean serverVerified;
   private float maxImpactPitch;

   public HeraclesPressureClientSmoke() { NeoForge.EVENT_BUS.register(this); }

   @SubscribeEvent public void tick(ClientTickEvent.Post event) {
      var mc = Minecraft.getInstance();
      if (done || mc.getOverlay()!=null) return;
      try {
         if (failure!=null) throw new AssertionError(failure);
         if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardingAccessibilityFinished();mc.setScreen(new TitleScreen());
         }
         if (mc.screen instanceof TitleScreen && !opened) {
            opened=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(4);
            mc.getWindow().setWindowed(1280,720);
            if (new File(mc.gameDirectory,"saves/"+WORLD+"/level.dat").isFile())
               mc.createWorldOpenFlows().openWorld(WORLD,()->{ throw new IllegalStateException("QA world open failed"); });
            else mc.createWorldOpenFlows().createFreshLevel(WORLD,
               new LevelSettings(WORLD,GameType.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
               new WorldOptions(42,false,false),registry->registry.registryOrThrow(Registries.WORLD_PRESET)
                  .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
         }
         if (mc.player==null || mc.getSingleplayerServer()==null) return;
         ticks++;
         if (ticks>=70 && ticks<=86 && npcId!=null) {
            net.minecraft.world.entity.Entity viewed=null;
            for(var entity:mc.level.entitiesForRendering()) if(entity instanceof HeraclesEntity) { viewed=entity;break; }
            if (viewed instanceof HeraclesEntity heracles) {
               var renderer=(net.xxxjk.TYPE_MOON_WORLD.client.renderer.HeraclesRenderer)mc.getEntityRenderDispatcher().getRenderer(heracles);
               var body=renderer.getGeoModel().getAnimationProcessor().getBone("upper_body");
               var arm=renderer.getGeoModel().getAnimationProcessor().getBone("right arm");
               TYPE_MOON_WORLD.LOGGER.info("HERACLES_PRESSURE_SAMPLE tick={} bodyPresent={} armPresent={}",ticks,body!=null,arm!=null);
               if(body!=null && arm!=null) {
                  TYPE_MOON_WORLD.LOGGER.info("HERACLES_PRESSURE_POSE tick={} body={} arm={}",ticks,body.getRotX(),arm.getRotX());
                  maxImpactPitch=Math.max(maxImpactPitch,Math.abs(body.getRotX()));
               }
            }
         }
         if (ticks==10) {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            mc.player.setYRot(0);mc.player.setXRot(0);
            for (String name : new String[]{"heracles_revive","heracles_adapt","heracles_impact"})
               require(PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld",name))!=null,"player animation missing: "+name);
            server(mc,()->setup(mc));
         }
         if (ticks==40) capture(mc,"01-heavy-idle");
         if (ticks==50) server(mc,()->npc(mc).triggerNamedActionAnimation("frenzy_walk"));
         if (ticks==56) capture(mc,"02-approach");
         if (ticks==70) server(mc,()->{ HeraclesAbilityService.cancel(npc(mc));require(HeraclesAbilityService.start(npc(mc),1),"NPC slam refused"); });
         if (ticks==74) capture(mc,"03-slam-windup");
         if (ticks==78) capture(mc,"04-slam-impact");
         if (ticks==100) server(mc,()->{
            var npc=npc(mc);int lives=GodHandService.read(npc).lives();
            require(GodHandService.revive(npc,npc.damageSources().generic(),AttackOrigin.skill("qa","lethal")),"NPC revival refused");
            require(GodHandService.read(npc).lives()==lives-1 && npc.invulnerableTime==0,"revival changed combat rules");
         });
         if (ticks==104) capture(mc,"05-npc-recovery");
         if (ticks==120) server(mc,()->{
            var npc=npc(mc);
            GodHandService.write(npc,new GodHandState(GodHandService.read(npc).lives(),java.util.Set.of(),"qa:lethal",1,java.util.Set.of()));
            GodHandService.tick(npc);
            require(GodHandService.read(npc).adapted().contains("qa:lethal"),"adaptation not completed");
         });
         if (ticks==123) capture(mc,"06-adaptation");
         if (ticks==140) {
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            mc.player.setYRot(180);mc.player.setXRot(12);
            server(mc,()->{
               var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
               p.setYRot(180);p.setXRot(12);
               require(ServantCardTransformManager.transform(p,"heracles"),"player transform refused");
               require(GodHandService.revive(p,p.damageSources().generic(),AttackOrigin.skill("qa","player_lethal")),"player revival refused");
            });
         }
         if (ticks==143) {
            require(reactionActive(mc),"player revival animation did not become active");
            capture(mc,"07-player-recovery");
         }
         if (ticks==160) server(mc,()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            HeraclesPresentationService.adapted(p);
         });
         if (ticks==162) { require(reactionActive(mc),"player adaptation animation inactive");capture(mc,"08-player-adaptation"); }
         if (ticks==180) server(mc,()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            HeraclesAbilityService.clearCooldowns(p);require(HeraclesAbilityService.start(p,1),"player slam refused");serverVerified=true;
         });
         if (ticks==188) capture(mc,"09-player-impact");
         if (ticks>=220) {
            require(maxImpactPitch>.5F,"rendered slam never reached its weight-bearing pose");
            require(serverVerified,"server checks unfinished");done=true;
            TYPE_MOON_WORLD.LOGGER.info("HERACLES_PRESSURE_CLIENT_PASS actions=loaded npc=slam_revival_adaptation player=reactions_and_impact");mc.stop();
         }
      } catch (Throwable error) {
         done=true;TYPE_MOON_WORLD.LOGGER.error("HERACLES_PRESSURE_CLIENT_FAIL ticks="+ticks,error);mc.stop();
      }
   }

   private void setup(Minecraft mc) {
      var server=mc.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayer(mc.player.getUUID());
      var vars=player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed) ServantCardTransformManager.release(player,false);
      vars.servantCardState().setTransformCooldown(0);vars.servantCardState().setReleaseCooldown(0);
      player.setGameMode(GameType.CREATIVE);level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
      var previous=new java.util.ArrayList<net.minecraft.world.entity.Entity>();
      for (var entity:level.getAllEntities()) if(entity instanceof net.minecraft.world.entity.Mob) previous.add(entity);
      previous.forEach(net.minecraft.world.entity.Entity::discard);
      for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++) {
         for(int y=98;y<=100;y++)level.setBlock(new BlockPos(x,y,z),Blocks.BEDROCK.defaultBlockState(),2);
         for(int y=101;y<=110;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
      }
      player.teleportTo(.5,101,-7.5);player.setYRot(0);player.setXRot(0);
      var npc=ModEntities.HERACLES.get().create(level);npc.readAdditionalSaveData(npc.saveWithoutId(new CompoundTag()));
      npc.setNoAi(true);npc.setPos(.5,101,.5);npc.setYRot(180);npc.setYHeadRot(180);npc.yBodyRot=180;
      npc.setCurrentMp(1000);level.addFreshEntity(npc);npcId=npc.getUUID();
      for(String key:new String[]{"revive","adapt","slam_windup","frenzy_walk","frenzy_impact","martial_8"})
         require(npc.hasActionAnimation(key),"NPC action mapping absent: "+key);
   }
   private HeraclesEntity npc(Minecraft mc) { return (HeraclesEntity)mc.getSingleplayerServer().overworld().getEntity(npcId); }
   private static boolean reactionActive(Minecraft mc) {
      return dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess.getPlayerAssociatedData(mc.player)
         .get(ResourceLocation.fromNamespaceAndPath("typemoonworld","heracles_reaction")) instanceof dev.kosmx.playerAnim.api.layered.ModifierLayer<?> layer && layer.isActive();
   }
   private void server(Minecraft mc,Runnable work) {
      mc.getSingleplayerServer().execute(()->{try {work.run();}catch(Throwable error){failure=error.toString();}});
   }
   private static void capture(Minecraft mc,String name) {
      File directory=new File(mc.gameDirectory,"qa");directory.mkdirs();Screenshot.grab(directory,name+".png",mc.getMainRenderTarget(),ignored->{});
   }
   private static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
