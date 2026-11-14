package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ZhaoYunRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.zhaoyun.ZhaoYunSpearDanceService;

@GameTestHolder("typemoonworld_zhao_yun_dance")
@PrefixGameTestTemplate(false)
public final class ZhaoYunSpearDanceGameTests {
   private ZhaoYunSpearDanceGameTests() { }
   private static Vec3 origin(GameTestHelper h) {
      for (int x=2;x<=11;x++) for(int z=2;z<=13;z++) for(int y=2;y<=6;y++) h.setBlock(new BlockPos(x,y,z), Blocks.AIR);
      return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,2,5)));
   }
   private static ZhaoYunRiderEntity npc(GameTestHelper h) {
      var npc=ModEntities.ZHAO_YUN_RIDER.get().create(h.getLevel());
      npc.setPos(origin(h)); npc.setNoAi(true); npc.setNoGravity(true); npc.setCurrentMp(200);
      npc.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.YAJIAO_QIANG.get()));
      h.getLevel().addFreshEntity(npc); return npc;
   }
   private static LivingEntity target(GameTestHelper h, LivingEntity owner, double x, double z) {
      var target=EntityType.HUSK.create(h.getLevel());
      target.setPos(owner.position().add(x,0,z)); target.setNoAi(true); target.setNoGravity(true);
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000);
      target.getAttribute(Attributes.ARMOR).setBaseValue(0);
      target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
      target.setHealth(2000); h.getLevel().addFreshEntity(target); return target;
   }
   private static ServerPlayer card(GameTestHelper h) {
      var player=h.makeMockServerPlayerInLevel(); var pos=origin(h);
      player.teleportTo(pos.x,pos.y,pos.z);
      h.assertTrue(ServantCardTransformManager.transform(player,"zhao_yun_rider"),"transform failed");
      player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.YAJIAO_QIANG.get()));
      player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(200);
      return player;
   }
   @GameTest(template="ancient_temple",timeoutTicks=45)
   public static void repeatedHitsStopImmediatelyOnRelease(GameTestHelper h) {
      var owner=npc(h); var target=target(h,owner,0,2);
      h.assertTrue(ZhaoYunSpearDanceService.start(owner),"dance rejected");
      h.assertTrue(owner.getCurrentMp()==172,"NPC did not pay exactly 28 MP");
      h.runAfterDelay(5,()->h.assertTrue(target.getHealth()==2000,"hit before windup"));
      h.runAfterDelay(16,()->{
         h.assertTrue(target.getHealth()<1980,"continuous dance did not hit at least three times");
         ZhaoYunSpearDanceService.stop(owner,true); float health=target.getHealth();
         h.runAfterDelay(12,()->{
            h.assertTrue(target.getHealth()==health,"damage survived release");
            h.assertTrue(!ZhaoYunSpearDanceService.start(owner),"release reset cooldown"); h.succeed();
         });
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=35)
   public static void cardHoldReleaseAndDuplicatePressUseOneTransaction(GameTestHelper h) {
      var player=card(h); var target=target(h,player,0,2);
      ServantCardTransformManager.handleHoldAction(player,6,true);
      h.assertTrue(ZhaoYunSpearDanceService.active(player),"hold press did not start skill 6");
      double paid=player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().mana();
      h.assertTrue(paid==172,"card did not pay 28 MP");
      ServantCardTransformManager.handleHoldAction(player,6,true);
      h.assertTrue(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().mana()==paid,"duplicate press charged twice");
      h.runAfterDelay(12,()->{
         h.assertTrue(target.getHealth()<2000,"card had no continuous hits");
         ServantCardTransformManager.handleHoldAction(player,6,false); float health=target.getHealth();
         h.runAfterDelay(10,()->{
            h.assertTrue(!ZhaoYunSpearDanceService.active(player),"release retained skill");
            h.assertTrue(target.getHealth()==health,"release retained damage");
            h.assertTrue(ServantCardTransformManager.getSkillCooldown(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES),6)>0,"release removed cooldown");
            ServantCardTransformManager.release(player,false); h.succeed();
         });
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=35)
   public static void weaponLossCancelsWithoutDelayedHits(GameTestHelper h) {
      var owner=npc(h); var target=target(h,owner,0,2);
      h.assertTrue(ZhaoYunSpearDanceService.start(owner),"dance rejected");
      h.runAfterDelay(12,()->{
         owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY); float health=target.getHealth();
         h.runAfterDelay(10,()->{
            h.assertTrue(!ZhaoYunSpearDanceService.active(owner),"weapon loss retained skill");
            h.assertTrue(target.getHealth()==health,"weapon loss retained damage"); h.succeed();
         });
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=30)
   public static void danceHitsBehindButCannotHitThroughWallsOrExceedThreeTargets(GameTestHelper h) {
      var owner=npc(h); owner.setYRot(0);
      var first=target(h,owner,0,-1.5); var second=target(h,owner,-1.5,0); var third=target(h,owner,1.5,0);
      var fourth=target(h,owner,0,-3.6);
      var wallTarget=target(h,owner,0,2.8);
      var base=BlockPos.containing(owner.position());
      for(int x=-1;x<=1;x++) for(int y=0;y<=2;y++) h.getLevel().setBlock(base.offset(x,y,2),Blocks.STONE.defaultBlockState(),2);
      h.assertTrue(ZhaoYunSpearDanceService.start(owner),"dance rejected");
      h.runAfterDelay(12,()->{
         h.assertTrue(first.getHealth()<2000 && second.getHealth()<2000 && third.getHealth()<2000,"360-degree dance missed clear targets");
         h.assertTrue(fourth.getHealth()==2000,"dance exceeded three targets per pulse");
         h.assertTrue(wallTarget.getHealth()==2000,"dance hit through a wall");
         ZhaoYunSpearDanceService.stop(owner,false); h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=325)
   public static void maximumDurationEndsTheDance(GameTestHelper h) {
      var owner=npc(h);
      h.assertTrue(ZhaoYunSpearDanceService.start(owner),"dance rejected");
      h.runAfterDelay(299,()->h.assertTrue(ZhaoYunSpearDanceService.active(owner),"dance ended before maximum"));
      h.runAfterDelay(302,()->{ h.assertTrue(!ZhaoYunSpearDanceService.active(owner),"dance exceeded 15 seconds"); h.succeed(); });
   }

   @GameTest(template="ancient_temple",timeoutTicks=35)
   public static void suppressionAndTransformationReleaseCancelDamage(GameTestHelper h) {
      var player=card(h); var target=target(h,player,0,2);
      ServantCardTransformManager.handleHoldAction(player,6,true);
      h.assertTrue(ZhaoYunSpearDanceService.active(player),"dance rejected");
      h.runAfterDelay(8,()->{
         player.getPersistentData().putLong("TypeMoonCombatSuppressedUntil",h.getLevel().getGameTime()+100);
         float health=target.getHealth();
         h.runAfterDelay(8,()->{
            h.assertTrue(!ZhaoYunSpearDanceService.active(player),"suppression retained dance");
            h.assertTrue(target.getHealth()==health,"suppression retained damage");
            player.getPersistentData().remove("TypeMoonCombatSuppressedUntil");
            h.assertTrue(ZhaoYunSpearDanceService.start(player),"presentation restart rejected");
            ServantCardTransformManager.release(player,false);
            h.assertTrue(!ZhaoYunSpearDanceService.active(player),"transformation release retained dance");
            h.runAfterDelay(8,()->{h.assertTrue(target.getHealth()==health,"transformation release retained damage");h.succeed();});
         });
      });
   }

   @GameTest(template="ancient_temple",timeoutTicks=20)
   public static void invalidWeaponDoesNotConsumeManaOrCooldown(GameTestHelper h) {
      var player=card(h); player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
      h.assertTrue(!ServantCardTransformManager.triggerAction(player,6),"empty hand accepted dance");
      var vars=player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      h.assertTrue(vars.servantCardState().mana()==200,"failed cast consumed MP");
      h.assertTrue(ServantCardTransformManager.getSkillCooldown(vars,6)==0,"failed cast consumed cooldown");
      ServantCardTransformManager.release(player,false); h.succeed();
   }
}
