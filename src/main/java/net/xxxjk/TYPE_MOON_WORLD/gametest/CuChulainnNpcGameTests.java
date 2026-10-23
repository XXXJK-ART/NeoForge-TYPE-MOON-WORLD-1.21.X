package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import net.xxxjk.TYPE_MOON_WORLD.entity.CuSpearEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.cu.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity;

@GameTestHolder("typemoonworld_cu")
@PrefixGameTestTemplate(false)
public final class CuChulainnNpcGameTests {
   private static CuChulainnEntity npc(GameTestHelper h) {
      for(int x=1;x<=13;x++)for(int z=1;z<=13;z++) {
         h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
         for(int y=2;y<=9;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
      }
      var e=ModEntities.CU_CHULAINN.get().create(h.getLevel());
      e.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,2,3))));e.setNoAi(true);e.setNoGravity(true);e.setYRot(0);e.setXRot(0);
      h.getLevel().addFreshEntity(e);e.ensureDefaultNpcLoadout(false);e.setCurrentMp(600);return e;
   }
   private static LivingEntity target(GameTestHelper h,CuChulainnEntity owner,Vec3 offset) {
      var t=EntityType.HUSK.create(h.getLevel());t.setPos(owner.position().add(offset));t.setNoAi(true);t.setNoGravity(true);
      t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);t.getAttribute(Attributes.ARMOR).setBaseValue(0);
      t.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);t.setHealth(1000);h.getLevel().addFreshEntity(t);return t;
   }
   private static CuSpearEntity spear(GameTestHelper h,CuChulainnEntity owner) {
      var entry=CuSpearLedger.entry(owner);return entry==null || entry.entity==null?null:(CuSpearEntity)h.getLevel().getEntity(entry.entity);
   }
   private static int interruptionTick(CuChulainnEntity owner) {
      return (int)(NoblePhantasmCastService.voiceEnd(owner)-owner.level().getGameTime()-NoblePhantasmCastRules.interruptWindowStartTicks()+1);
   }
   private static void interrupt(GameTestHelper h,CuChulainnEntity owner) {
      h.assertTrue(NoblePhantasmCastService.inWindow(owner),"damage was outside interruption window");
      owner.hurt(owner.damageSources().genericKill(),1);
      h.assertTrue(NoblePhantasmCastService.interruptedByDamage(owner),"actual damage did not interrupt cast");
   }
   @GameTest(template="ancient_temple",timeoutTicks=30)
   public static void npcSelectsLungeAndHitsAtTheAnimationFrame(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,new Vec3(0,0,3.5));e.tickCount=10;
      CuChulainnCombatAi.tick(e,t);
      h.assertTrue(e.getPersistentData().getInt("CuAction")==-1,"NPC did not select lunge");
      h.assertTrue(e.getDeltaMovement().horizontalDistance()>1,"lunge had no real movement");
      h.assertTrue(t.getHealth()==1000,"lunge hit before its animation");
      h.runAfterDelay(7,()->{h.assertTrue(Math.abs(t.getHealth()-976)<.05,"lunge did not deal its single shared 24 damage");h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=65)
   public static void npcAimsOrdinaryThrowBeforePayingForOneRecall(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,new Vec3(4,0,9));e.setCurrentMp(100);e.tickCount=10;
      CuChulainnCombatAi.tick(e,t);
      h.assertTrue(e.getPersistentData().getInt("CuAction")==11,"AI did not select ordinary throw without NP mana");
      h.assertTrue(e.getCurrentMp()==100,"ordinary throw charged extra mana");
      h.runAfterDelay(6,()->{
         var s=spear(h,e);h.assertTrue(s!=null,"ordinary projectile missing");
         h.assertTrue(s.state()==0,"NPC recalled spear before outbound flight");
      });
      // Continue only retrieval decisions so a second attack cannot obscure the result.
      for(int tick=6;tick<=50;tick++)h.runAfterDelay(tick,()->CuChulainnCombatAi.tick(e,null));
      h.runAfterDelay(52,()->{
         h.assertTrue(t.getHealth()<1000,"off-axis target was missed");
         h.assertTrue(!CuSpearLedger.outstanding(e),"NPC did not retrieve its spear");
         h.assertTrue(e.getMainHandItem().is(ModItems.GAE_BULG.get()) && e.getMainHandItem().getCount()==1,"original spear missing or duplicated");
         h.assertTrue(CuChulainnAbilityService.cooldown(e,7)>h.getLevel().getGameTime(),"recall did not use its shared cooldown");h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=90)
   public static void interruptedFlightChainsIntoOrdinaryThrowAndRecall(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,new Vec3(0,0,9));e.setOnGround(true);
      h.assertTrue(CuChulainnWeaponService.noble(e,true,40),"flight start rejected");
      int hit=interruptionTick(e);double[] mana={0};
      h.runAfterDelay(hit,()->{
         mana[0]=e.getCurrentMp();interrupt(h,e);
         h.assertTrue(Math.abs(e.getCurrentMp()-mana[0]-150)<.05,"existing half-MP interruption refund changed");
         h.assertTrue(CuChulainnWeaponService.followupPending(e),"interrupted flight did not queue fallback");
         long end=e.getPersistentData().getLong(CuChulainnAbilityService.NP_END);
         h.assertTrue(end-h.getLevel().getGameTime()==600,"existing interrupted NP cooldown changed");
      });
      h.runAfterDelay(hit+8,()->{
         var s=spear(h,e);h.assertTrue(s!=null,"interrupted flight did not launch ordinary spear");
         h.assertTrue(!s.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getBoolean("Army"),"fallback retained NP army damage");
         h.assertTrue(!CuChulainnWeaponService.followupPending(e),"fallback was not consumed");
      });
      for(int tick=24;tick<=65;tick++)h.runAfterDelay(tick,()->CuChulainnCombatAi.tick(e,null));
      h.runAfterDelay(70,()->{
         h.assertTrue(t.getHealth()<1000 && t.getHealth()>900,"fallback did not use ordinary damage");
         h.assertTrue(!CuSpearLedger.outstanding(e) && e.getMainHandItem().is(ModItems.GAE_BULG.get()),"fallback spear was not retrieved");
         h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=70)
   public static void lostFallbackTargetDoesNotThrowOrDuplicateTheSpear(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,new Vec3(0,0,9));e.setOnGround(true);
      h.assertTrue(CuChulainnWeaponService.noble(e,true,40),"flight start rejected");int hit=interruptionTick(e);
      h.runAfterDelay(hit,()->{interrupt(h,e);t.discard();});
      h.runAfterDelay(hit+15,()->{
         h.assertTrue(!CuChulainnWeaponService.followupPending(e),"lost target retained fallback");
         h.assertTrue(!CuSpearLedger.outstanding(e),"lost target spawned an attack");
         h.assertTrue(e.getMainHandItem().is(ModItems.GAE_BULG.get()) && e.getMainHandItem().getCount()==1,"lost target consumed or duplicated spear");h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=70)
   public static void environmentalFlightFailureDoesNotCreateAnInterruptionFollowup(GameTestHelper h) {
      var e=npc(h);target(h,e,new Vec3(0,0,9));e.setOnGround(true);
      h.assertTrue(CuChulainnWeaponService.noble(e,true,40),"flight start rejected");
      h.getLevel().setBlockAndUpdate(BlockPos.containing(e.position().add(0,2,0)),Blocks.STONE.defaultBlockState());
      int release=(int)(NoblePhantasmCastService.releaseAt(e)-h.getLevel().getGameTime());
      h.runAfterDelay(release+15,()->{
         h.assertTrue(NoblePhantasmCastService.cancelled(e),"blocked leap did not cancel");
         h.assertTrue(!NoblePhantasmCastService.interruptedByDamage(e),"environmental failure counted as damage");
         h.assertTrue(!CuChulainnWeaponService.followupPending(e) && !CuSpearLedger.outstanding(e),"environmental failure created fallback");h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=35)
   public static void meleeFlourishesDealOnlyOneOrdinaryHitAndRespectRecovery(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,new Vec3(0,0,2));
      h.assertTrue(CuChulainnCombatAi.basicAttack(e,t),"ordinary melee rejected");
      int visual=e.getPersistentData().getInt("CuAction");
      h.assertTrue(visual>=CuChulainnRules.NPC_DANCE && visual<=CuChulainnRules.NPC_SLASH,"missing melee visual");
      h.assertTrue(!CuChulainnAbilityService.start(e,8) && !CuChulainnWeaponService.ordinaryThrow(e,t),"skill overwrote melee recovery");
      float[] after={1000};
      h.runAfterDelay(8,()->{after[0]=t.getHealth();h.assertTrue(after[0]<1000,"ordinary melee never hit");});
      h.runAfterDelay(14,()->h.assertTrue(!CuChulainnCombatAi.basicAttack(e,t),"spear flourish bypassed normal attack cadence"));
      h.runAfterDelay(19,()->{h.assertTrue(t.getHealth()==after[0],"visual flourishes added extra hits");h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=25)
   public static void ordinaryThrowRechecksTargetAndControlAtRelease(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,new Vec3(0,0,8));
      h.assertTrue(CuChulainnWeaponService.ordinaryThrow(e,t),"ordinary throw rejected");
      e.getPersistentData().putLong(ServantCombatSystem.TAG_STUN_UNTIL,h.getLevel().getGameTime()+10);
      h.runAfterDelay(8,()->{
         h.assertTrue(!CuSpearLedger.outstanding(e),"stunned NPC released delayed throw");
         h.assertTrue(e.getMainHandItem().is(ModItems.GAE_BULG.get()),"stun removed original");h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=80)
   public static void npcThornShowsItsStrikeBeforeAnyDamage(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,new Vec3(0,0,3));
      h.assertTrue(CuChulainnWeaponService.noble(e,false,20),"thorn rejected");
      h.assertTrue(CuChulainnVoiceService.locked(e),"thorn did not acquire its chant voice");
      h.assertTrue(e.getPersistentData().getInt("CuAction")==10,"thorn lacked charge animation");
      int release=(int)(NoblePhantasmCastService.releaseAt(e)-h.getLevel().getGameTime());float[] health={1000};
      h.runAfterDelay(release+3,()->{
         h.assertTrue(e.getPersistentData().getInt("CuAction")==CuChulainnRules.THORN_STRIKE,"thorn did not enter explicit thrust animation");
         h.assertTrue(t.getHealth()==1000,"thorn killed target before the thrust frame");
      });
      h.runAfterDelay(release+10,()->{health[0]=t.getHealth();h.assertTrue(health[0]<1000,"thorn missed its shared hit frame");});
      h.runAfterDelay(release+24,()->{h.assertTrue(t.getHealth()==health[0],"thorn recovery added another damage packet");h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=85)
   public static void playerUsesTheSameVisibleThornStrike(GameTestHelper h) {
      var anchor=npc(h);Vec3 pos=anchor.position();anchor.discard();
      var p=h.makeMockServerPlayerInLevel();p.teleportTo(pos.x,pos.y,pos.z);p.setYRot(0);p.setXRot(0);
      p.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES).is_magus=true;
      h.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(p,"cu_chulainn"),"transform rejected");
      p.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(600);
      var t=EntityType.HUSK.create(h.getLevel());t.setPos(p.position().add(0,0,3));t.setNoAi(true);t.setNoGravity(true);
      t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);t.setHealth(1000);h.getLevel().addFreshEntity(t);
      h.assertTrue(CuChulainnWeaponService.noble(p,false,20),"player thorn rejected");
      int release=(int)(NoblePhantasmCastService.releaseAt(p)-h.getLevel().getGameTime());
      h.runAfterDelay(release+3,()->{
         h.assertTrue(p.getPersistentData().getInt("CuAction")==CuChulainnRules.THORN_STRIKE,"player missing shared thrust action");
         h.assertTrue(t.getHealth()==1000,"player thorn hit before visible drive");
      });
      h.runAfterDelay(release+12,()->{h.assertTrue(t.getHealth()<1000,"player thrust never hit");h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=85)
   public static void committedFlightStillThrowsWhenTargetVanishesAndCasterIsStunned(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,new Vec3(0,0,9));e.setOnGround(true);
      h.assertTrue(CuChulainnWeaponService.noble(e,true,40),"flight rejected");
      int release=(int)(NoblePhantasmCastService.releaseAt(e)-h.getLevel().getGameTime());
      h.runAfterDelay(release+3,()->{
         t.discard();e.setYRot(180);e.setDeltaMovement(Vec3.ZERO);
         e.getPersistentData().putLong(ServantCombatSystem.TAG_STUN_UNTIL,h.getLevel().getGameTime()+40);
      });
      h.runAfterDelay(release+17,()->{
         var s=spear(h,e);h.assertTrue(s!=null,"committed jump silently lost its throw");
         var saved=s.saveWithoutId(new net.minecraft.nbt.CompoundTag());
         h.assertTrue(saved.getBoolean("Army") && Math.abs(saved.getFloat("Damage")-500)<.05,"committed flight lost paid NP damage");
         h.assertTrue(s.getZ()>e.getZ()+1,"flight ignored last aim and threw behind caster");
         h.assertTrue(!NoblePhantasmCastService.cancelled(e),"released flight was retroactively cancelled");h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=75)
   public static void deadTargetBeforeTakeoffStillRejectsFlight(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,new Vec3(0,0,9));e.setOnGround(true);
      h.assertTrue(CuChulainnWeaponService.noble(e,true,40),"flight rejected");
      int release=(int)(NoblePhantasmCastService.releaseAt(e)-h.getLevel().getGameTime());
      h.runAfterDelay(release-2,t::discard);
      h.runAfterDelay(release+18,()->{
         h.assertTrue(NoblePhantasmCastService.cancelled(e),"invalid pre-takeoff target was accepted");
         h.assertTrue(!CuSpearLedger.outstanding(e) && !CuChulainnWeaponService.followupPending(e),"pre-takeoff failure generated a phantom throw");h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=45)
   public static void actualSpearReturnTriggersOneCatchFlourish(GameTestHelper h) {
      var e=npc(h);h.assertTrue(CuSpearLedger.launch(e,false,24,null),"throw rejected");spear(h,e).setPos(e.position().add(0,1,5));
      h.assertTrue(CuChulainnAbilityService.start(e,7),"paid recall rejected");
      h.assertTrue(e.getPersistentData().getInt("CuAction")!=CuChulainnRules.RECALL_CATCH,"catch played before spear arrived");
      h.runAfterDelay(25,()->{
         h.assertTrue(!CuSpearLedger.outstanding(e) && e.getMainHandItem().is(ModItems.GAE_BULG.get()),"spear never reached hand");
         var data=e.getPersistentData();h.assertTrue(data.getInt("CuAction")==CuChulainnRules.RECALL_CATCH,"missing return flourish");
         long start=data.getLong("CuActionStart");h.assertTrue(data.getLong("CuActionUntil")-start==12,"catch duration changed");
         CuSpearLedger.deliverPending(e);h.assertTrue(data.getLong("CuActionStart")==start,"return flourish replayed without another return");h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=75)
   public static void interruptedThornDoesNotLeaveAnInvisibleDelayedHit(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,new Vec3(0,0,3));
      h.assertTrue(CuChulainnWeaponService.noble(e,false,20),"thorn rejected");int hit=interruptionTick(e);
      h.runAfterDelay(hit,()->interrupt(h,e));
      h.runAfterDelay(hit+35,()->{
         h.assertTrue(t.getHealth()==1000,"cancelled thorn left a delayed killing hit");
         h.assertTrue(!CuChulainnWeaponService.followupPending(e),"thorn incorrectly used flight's fallback");
         h.assertTrue(!CuChulainnVoiceService.locked(e),"interrupted chant left voice lock");h.succeed();
      });
   }
}
