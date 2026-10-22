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
   private boolean opened,done;private int ticks;private volatile String failure;private UUID npcId;
   private static final String WORLD="Cu-Reset-QA";
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
         if(ticks==10){for(String animation:new String[]{"cu_raise","cu_throw","cu_sweep","cu_flurry","cu_rune","cu_thrust"})require(PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld",animation)) instanceof KeyframeAnimation,"animation decode "+animation);server(mc,()->setup(mc));mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}
         if(ticks==40)server(mc,()->skill(mc,0));
         if(ticks==48)capture(mc,"cu-rune-half-arc");if(ticks==55)capture(mc,"cu-fireball-volley");
         if(ticks==85)server(mc,()->skill(mc,8));if(ticks==92 || ticks==98)capture(mc,"cu-sweep-third-person-"+ticks);
         if(ticks==120){mc.options.setCameraType(CameraType.FIRST_PERSON);server(mc,()->skill(mc,9));}if(ticks==129)capture(mc,"cu-flurry-first-person");
         if(ticks==155){mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);server(mc,()->skill(mc,6));}if(ticks==166)capture(mc,"cu-barrier-six-faces");
         if(ticks==180)server(mc,()->{var p=player(mc);CuChulainnAbilityService.clearBarrier(p);CuChulainnAbilityService.clearBarrier(npc(mc));CuChulainnWeaponService.ordinaryThrow(p);CuChulainnWeaponService.ordinaryThrow(npc(mc));});
         if(ticks==183)capture(mc,"cu-raised-throw");if(ticks==205)capture(mc,"cu-physical-spear");
         if(ticks==215)server(mc,()->{CuSpearLedger.recall(player(mc));CuSpearLedger.recall(npc(mc));});if(ticks==223)capture(mc,"cu-red-zigzag-recall");
         if(ticks==270)server(mc,()->{for(var owner:java.util.List.of(player(mc),npc(mc))){
            require(!CuSpearLedger.outstanding(owner),"recall did not finish");
            var below=BlockPos.containing(owner.getX(),-60,owner.getZ());owner.level().setBlock(below,Blocks.STONE.defaultBlockState(),3);owner.teleportTo(owner.getX(),-59,owner.getZ());
            if(owner instanceof net.minecraft.world.entity.Mob mob)mob.setNoAi(false);
         }});
         if(ticks==285)server(mc,()->{for(var owner:java.util.List.of(player(mc),npc(mc))){
            if(owner instanceof net.minecraft.world.entity.Mob)owner.setOnGround(true);
            CuChulainnAbilityService.npCooldown(owner,0);
            if(owner instanceof net.minecraft.server.level.ServerPlayer p)p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(600);else npc(mc).setCurrentMp(600);
            require(CuChulainnWeaponService.noble(owner,true,70),"flight "+owner.getClass().getSimpleName()+" ground="+owner.onGround()+" spear="+CuChulainnAbilityService.spearHand(owner)+" pending="+CuSpearLedger.outstanding(owner)+" busy="+CuChulainnWeaponService.busy(owner)+" active="+CuChulainnAbilityService.active(owner)+" canAct="+CuChulainnAbilityService.canAct(owner)+" mana="+CuChulainnAbilityService.mana(owner));
         }});
         if(ticks==320)server(mc,()->npc(mc).setOnGround(true));
         if(ticks==327)capture(mc,"cu-jump-before-throw");if(ticks==345)capture(mc,"cu-airborne-release");
         if(ticks>=430){done=true;TYPE_MOON_WORLD.LOGGER.info("CU_CLIENT_SMOKE_OK six animations, player and NPC skills, transparent box, physical spear, leap and recall");mc.stop();}
      }catch(Throwable t){done=true;TYPE_MOON_WORLD.LOGGER.error("CU_CLIENT_SMOKE_FAILED",t);mc.stop();}
   }
   private net.minecraft.server.level.ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
   private net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity npc(Minecraft mc){return (net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity)mc.getSingleplayerServer().overworld().getEntity(npcId);}
   private void setup(Minecraft mc){var level=mc.getSingleplayerServer().overworld();
      for(int x=-8;x<=12;x++)for(int z=-8;z<=54;z++){level.setBlock(new BlockPos(x,-60,z),Blocks.STONE.defaultBlockState(),3);for(int y=-59;y<=-39;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);}
      var p=player(mc);if(p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().transformed())ServantCardTransformManager.release(p,false);
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
