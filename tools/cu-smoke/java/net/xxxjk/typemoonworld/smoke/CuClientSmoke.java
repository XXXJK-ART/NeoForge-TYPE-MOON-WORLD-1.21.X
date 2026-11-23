package net.xxxjk.typemoonworld.smoke;

import java.io.File;
import java.util.UUID;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
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
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.cu.*;

@Mod(value="typemoonworld_cu_smoke",dist=Dist.CLIENT)
public final class CuClientSmoke {
   private boolean opened,done;private int ticks;private volatile String failure;private UUID npcId,flightTargetId;
   private static final String WORLD="Cu-Reset-QA";
   private final UUID[] thornTargets=new UUID[2];
   public CuClientSmoke(){NeoForge.EVENT_BUS.register(this);}
   private static void require(boolean test,String message){if(!test)throw new AssertionError(message);}
   private void server(Minecraft mc,Runnable task){mc.getSingleplayerServer().execute(()->{try{task.run();}catch(Throwable t){failure=t.toString();}});}
   @SubscribeEvent public void tick(ClientTickEvent.Post event){
      Minecraft mc=Minecraft.getInstance();if(done||mc.getOverlay()!=null)return;
      try{
         if(failure!=null)throw new AssertionError(failure);
         if(mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen){mc.options.onboardingAccessibilityFinished();mc.setScreen(new TitleScreen());}
         if(mc.screen instanceof TitleScreen&&!opened){
            opened=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getWindow().setWindowed(1280,900);
            if(new File(mc.gameDirectory,"saves/"+WORLD+"/level.dat").isFile())mc.createWorldOpenFlows().openWorld(WORLD,()->{throw new IllegalStateException("world open failed");});
            else mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(42,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
         }
         if(mc.player==null||mc.getSingleplayerServer()==null)return;ticks++;
         if(ticks==10){for(String animation:new String[]{"cu_raise","cu_throw","cu_sweep","cu_flurry","cu_rune","cu_thrust","cu_npc_sweep","cu_npc_flurry","cu_npc_dance","cu_npc_thrust","cu_npc_slash","cu_thorn"})require(PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld",animation)) instanceof KeyframeAnimation,"animation decode "+animation);server(mc,()->setup(mc));mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}
         if(ticks==40)server(mc,()->skill(mc,0));
         if(ticks==48)capture(mc,"cu-rune-half-arc");if(ticks==55)capture(mc,"cu-fireball-volley");
         if(ticks==67)server(mc,()->skill(mc,1));if(ticks==72)capture(mc,"cu-laguz-sigil");
         if(ticks==85)server(mc,()->skill(mc,8));if(ticks==92 || ticks==98)capture(mc,"cu-sweep-third-person-"+ticks);
         if(ticks==120){mc.options.setCameraType(CameraType.FIRST_PERSON);server(mc,()->skill(mc,9));}if(ticks==129)capture(mc,"cu-flurry-first-person");
         if(ticks==155){mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);server(mc,()->skill(mc,6));}if(ticks==166)capture(mc,"cu-barrier-six-faces");
         if(ticks==175)server(mc,()->{require(CuChulainnAbilityService.start(player(mc),6),"player barrier toggle");require(CuChulainnAbilityService.start(npc(mc),6),"NPC barrier toggle");});
         if(ticks==180)server(mc,()->{CuChulainnWeaponService.ordinaryThrow(player(mc));CuChulainnWeaponService.ordinaryThrow(npc(mc));});
         if(ticks==183)capture(mc,"cu-raised-throw");if(ticks==205)capture(mc,"cu-physical-spear");
         if(ticks==215)server(mc,()->{CuSpearLedger.recall(player(mc));CuSpearLedger.recall(npc(mc));});if(ticks==223)capture(mc,"cu-red-zigzag-recall");
         if(ticks==270)server(mc,()->{for(var owner:java.util.List.of(player(mc),npc(mc))){
            require(!CuSpearLedger.outstanding(owner),"recall did not finish");
            var below=BlockPos.containing(owner.getX(),-60,owner.getZ());owner.level().setBlock(below,Blocks.STONE.defaultBlockState(),3);owner.teleportTo(owner.getX(),-59,owner.getZ());
            if(owner instanceof net.minecraft.world.entity.Mob mob)mob.setNoAi(false);
         }});
         if(ticks==275)server(mc,()->{
            var level=mc.getSingleplayerServer().overworld();var target=EntityType.IRON_GOLEM.create(level);target.setPos(.5,-59,12.5);target.setNoAi(true);level.addFreshEntity(target);flightTargetId=target.getUUID();
         });
         if(ticks==285)server(mc,()->{for(var owner:java.util.List.of(player(mc),npc(mc))){
            if(owner instanceof net.minecraft.world.entity.Mob)owner.setOnGround(true);
            if(owner instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity cu)
               cu.faceToward(owner.level().getServer().overworld().getEntity(flightTargetId).position());
            CuChulainnAbilityService.npCooldown(owner,0);
            if(owner instanceof net.minecraft.server.level.ServerPlayer p)p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(600);else npc(mc).setCurrentMp(600);
            require(CuChulainnWeaponService.noble(owner,true,70),"flight "+owner.getClass().getSimpleName()+" ground="+owner.onGround()+" spear="+CuChulainnAbilityService.spearHand(owner)+" pending="+CuSpearLedger.outstanding(owner)+" busy="+CuChulainnWeaponService.busy(owner)+" active="+CuChulainnAbilityService.active(owner)+" canAct="+CuChulainnAbilityService.canAct(owner)+" mana="+CuChulainnAbilityService.mana(owner));
         }});
         if(ticks==303 || ticks==312)capture(mc,"cu-np-charge-"+ticks);
         if(ticks==320)server(mc,()->{for(var owner:java.util.List.of(player(mc),npc(mc))){owner.setOnGround(false);owner.setPos(owner.position().add(0,.2,0));}});
         if(ticks==327)capture(mc,"cu-jump-before-throw");if(ticks==345)capture(mc,"cu-airborne-release");
         if(ticks==430)server(mc,()->{CuSpearLedger.recall(player(mc));CuSpearLedger.recall(npc(mc));});
         if(ticks==490)server(mc,()->{
            var p=player(mc);require(!CuSpearLedger.outstanding(p),"post-flight return");p.teleportTo(.5,-59,.5);p.setYRot(0);p.setXRot(15);
            require(CuSpearLedger.launch(p,false,24,null),"nearby ground spear");
            var spear=(net.xxxjk.TYPE_MOON_WORLD.entity.CuSpearEntity)p.serverLevel().getEntity(CuSpearLedger.entry(p).entity);
            spear.setPos(.5,-58.7,2.5);spear.setDeltaMovement(0,-.5,0);
         });
         if(ticks==500)capture(mc,"cu-ground-spear-sized");
         if(ticks==510)server(mc,()->{
            var p=player(mc);var spear=(net.xxxjk.TYPE_MOON_WORLD.entity.CuSpearEntity)p.serverLevel().getEntity(CuSpearLedger.entry(p).entity);
            require(spear.state()==1,"ground spear did not embed");spear.interactAt(p,new Vec3(0,1,0),InteractionHand.MAIN_HAND);
            require(!CuSpearLedger.outstanding(p),"ground pickup");
            require(p.getPersistentData().getInt("CuAction")==CuChulainnRules.RECALL_CATCH,"pickup catch flourish");
            require(CuChulainnWeaponService.begin(p),"begin hold");p.startUsingItem(InteractionHand.MAIN_HAND);
         });
         if(ticks==540)capture(mc,"cu-held-thorn-charge");
         if(ticks==555)server(mc,()->{var p=player(mc);p.releaseUsingItem();});
         if(ticks==570){mc.options.keyUse.setDown(false);mc.gameMode.releaseUsingItem(mc.player);server(mc,()->{var p=player(mc);CuChulainnAbilityService.npCooldown(p,0);p.setYRot(0);p.setXRot(0);
            var target=EntityType.IRON_GOLEM.create(p.serverLevel());target.setNoAi(true);target.setPos(p.position().add(0,0,3));p.serverLevel().addFreshEntity(target);
         });}
         if(ticks==580){mc.player.setYRot(0);mc.player.setXRot(0);mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);}
         if(ticks==590)server(mc,()->require(player(mc).isUsingItem(),"server did not receive client use request"));
         if(ticks==610){require(mc.player.isUsingItem(),"client lost held use state");mc.options.keyUse.setDown(false);}
         if(ticks==616)server(mc,()->require(net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.chanting(player(mc)),"actual client button release did not start thorn"));
         if(ticks==670){mc.options.setCameraType(CameraType.FIRST_PERSON);server(mc,()->{
            var p=player(mc);p.setGameMode(GameType.SPECTATOR);p.teleportTo(.5,-58,-4.5);p.setYRot(0);p.setXRot(8);
            var n=npc(mc);CuChulainnAbilityService.clear(n);n.setNoAi(true);n.setTarget(null);n.teleportTo(.5,-59,1.5);n.setDeltaMovement(Vec3.ZERO);n.faceVector(new Vec3(0,0,-1));n.ensureDefaultNpcLoadout(false);
         });}
         for(int index=0;index<5;index++){
            int action=CuChulainnRules.NPC_SWEEP+index,start=690+index*30;
            if(ticks==start)server(mc,()->CuChulainnVisualService.action(npc(mc),action,action<=CuChulainnRules.NPC_FLURRY?20:12));
            if(ticks==start+6)capture(mc,"cu-npc-spear-technique-"+action);
         }
         if(ticks==875){mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);server(mc,()->prepareThorns(mc));}
         if(ticks==885)server(mc,()->{require(CuChulainnWeaponService.noble(player(mc),false,20),"player visible thorn");require(CuChulainnWeaponService.noble(npc(mc),false,20),"NPC visible thorn");});
         if(ticks==893){require(net.xxxjk.TYPE_MOON_WORLD.client.CuChulainnVoiceClient.active(mc.player.getUUID()),"player thorn voice not playing");require(net.xxxjk.TYPE_MOON_WORLD.client.CuChulainnVoiceClient.active(npcId),"NPC thorn voice not playing");capture(mc,"cu-np-dense-charge");}
         if(ticks==925)mc.options.setCameraType(CameraType.FIRST_PERSON);
         if(ticks==928)capture(mc,"cu-thorn-player-drive");
         if(ticks==930){mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);capture(mc,"cu-thorn-both-release");}
         if(ticks==1010)server(mc,()->prepareThorns(mc));
         if(ticks==1015)server(mc,()->{require(CuChulainnWeaponService.noble(player(mc),false,20),"player isolated voice cast");require(CuChulainnWeaponService.noble(npc(mc),false,20),"NPC isolated voice cast");});
         if(ticks==1034)server(mc,()->{
            var n=npc(mc);require(net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.inWindow(n),"voice stop outside interrupt window");n.hurt(n.damageSources().genericKill(),1);
         });
         if(ticks==1040){require(!net.xxxjk.TYPE_MOON_WORLD.client.CuChulainnVoiceClient.active(npcId),"interrupted NPC voice still playing");require(net.xxxjk.TYPE_MOON_WORLD.client.CuChulainnVoiceClient.active(mc.player.getUUID()),"NPC interruption silenced player's NP");}
         if(ticks>=1140){done=true;TYPE_MOON_WORLD.LOGGER.info("CU_CLIENT_SMOKE_OK twelve animations, pure NPC spear techniques, dense rune/NP effects, visible player/NPC thorn, per-owner voice isolation, catch flourish, real input, leap and recall");mc.stop();}
      }catch(Throwable t){done=true;TYPE_MOON_WORLD.LOGGER.error("CU_CLIENT_SMOKE_FAILED",t);mc.stop();}
   }
   private net.minecraft.server.level.ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
   private net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity npc(Minecraft mc){return (net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity)mc.getSingleplayerServer().overworld().getEntity(npcId);}
   private void prepareThorns(Minecraft mc) {
      var p=player(mc);var n=npc(mc);var level=p.serverLevel();
      p.setGameMode(GameType.SURVIVAL);p.teleportTo(-1.5,-59,.5);p.setYRot(0);p.setXRot(0);
      n.setNoAi(true);n.setTarget(null);n.teleportTo(2.5,-59,.5);n.setDeltaMovement(Vec3.ZERO);n.faceVector(new Vec3(0,0,1));
      var owners=java.util.List.<LivingEntity>of(p,n);
      for(int i=0;i<owners.size();i++) {
         var owner=owners.get(i);CuChulainnAbilityService.clear(owner);CuChulainnAbilityService.npCooldown(owner,0);
         if(thornTargets[i]!=null && level.getEntity(thornTargets[i])!=null)level.getEntity(thornTargets[i]).discard();
         if(owner==p)p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(600);else n.setCurrentMp(600);
         var target=EntityType.IRON_GOLEM.create(level);target.setNoAi(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(10000);target.setHealth(10000);
         target.setPos(owner.position().add(0,0,3));level.addFreshEntity(target);thornTargets[i]=target.getUUID();
         require(CuChulainnWeaponService.lock(owner,target),"thorn QA lock");
      }
   }
   private void setup(Minecraft mc){var level=mc.getSingleplayerServer().overworld();
      for(int x=-8;x<=12;x++)for(int z=-8;z<=54;z++){level.setBlock(new BlockPos(x,-60,z),Blocks.STONE.defaultBlockState(),3);for(int y=-59;y<=-39;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);}
      var p=player(mc);p.setGameMode(GameType.SURVIVAL);if(p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().transformed())ServantCardTransformManager.release(p,false);
      p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).is_magus=true;
      p.teleportTo(.5,-59,.5);p.setYRot(0);p.setXRot(0);require(ServantCardTransformManager.transform(p,"cu_chulainn"),"transform");
      for(var old:level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(-10,-65,-10,15,-35,58)))old.discard();
      var npc=ModEntities.CU_CHULAINN.get().create(level);npc.setPos(3.5,-59,1.5);npc.setNoAi(true);npc.setYRot(0);level.addFreshEntity(npc);npc.ensureDefaultNpcLoadout(false);npcId=npc.getUUID();
      var team=level.getScoreboard().getPlayerTeam("cu_qa");if(team==null)team=level.getScoreboard().addPlayerTeam("cu_qa");
      level.getScoreboard().addPlayerToTeam(p.getScoreboardName(),team);level.getScoreboard().addPlayerToTeam(npc.getScoreboardName(),team);
      level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,mc.getSingleplayerServer());
      level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,mc.getSingleplayerServer());
      mc.getSingleplayerServer().setDifficulty(Difficulty.PEACEFUL,true);
   }
   private void skill(Minecraft mc,int slot){for(var owner:java.util.List.of(player(mc),npc(mc))){
      owner.getPersistentData().remove("CuShared:cd:"+slot);if(owner instanceof net.minecraft.server.level.ServerPlayer p)p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(600);else npc(mc).setCurrentMp(600);
      require(CuChulainnAbilityService.start(owner,slot),"skill "+slot);
   }}
   private void capture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name+".png",mc.getMainRenderTarget(),c->TYPE_MOON_WORLD.LOGGER.info("CU_CAPTURE {}",name));}
}
