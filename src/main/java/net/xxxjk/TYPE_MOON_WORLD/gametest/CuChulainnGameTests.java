package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import net.xxxjk.TYPE_MOON_WORLD.entity.CuSpearEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.cu.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity;

@GameTestHolder("typemoonworld_cu")
@PrefixGameTestTemplate(false)
public final class CuChulainnGameTests {
   private CuChulainnGameTests() { }
   private static Vec3 arena(GameTestHelper h) {
      for(int x=1;x<=13;x++)for(int z=1;z<=13;z++){
         h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
         for(int y=2;y<=9;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
      }
      return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,2,4)));
   }
   private static CuChulainnEntity npc(GameTestHelper h) {
      var e=ModEntities.CU_CHULAINN.get().create(h.getLevel());e.setPos(arena(h));e.setYRot(0);e.setXRot(0);e.setNoAi(true);e.setNoGravity(true);
      h.getLevel().addFreshEntity(e);e.ensureDefaultNpcLoadout(false);e.setCurrentMp(600);return e;
   }
   private static ServerPlayer card(GameTestHelper h) {
      var e=h.makeMockServerPlayerInLevel();Vec3 p=arena(h);e.teleportTo(p.x,p.y,p.z);e.setYRot(0);e.setXRot(0);
      h.assertTrue(ServantCardTransformManager.transform(e,"cu_chulainn"),"transform failed");
      e.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(600);return e;
   }
   private static LivingEntity target(GameTestHelper h,LivingEntity owner,double distance) {
      var t=EntityType.HUSK.create(h.getLevel());t.setPos(owner.position().add(0,0,distance));t.setNoAi(true);t.setNoGravity(true);
      t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);t.getAttribute(Attributes.ARMOR).setBaseValue(0);t.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
      t.setHealth(1000);h.getLevel().addFreshEntity(t);return t;
   }
   private static void close(GameTestHelper h,double actual,double expected,String why){h.assertTrue(Math.abs(actual-expected)<.05,why+": "+actual+" expected "+expected);}
   private static CuSpearEntity spear(GameTestHelper h,LivingEntity owner){var e=CuSpearLedger.entry(owner);return e==null||e.entity==null?null:(CuSpearEntity)h.getLevel().getEntity(e.entity);}
   @GameTest(template="ancient_temple",timeoutTicks=40)
   public static void playerAndNpcShareCostsAndIndependentCooldowns(GameTestHelper h) {
      var player=card(h);var npc=npc(h);npc.setPos(player.position().add(5,0,0));
      for(var e:java.util.List.of(player,npc)){
         h.assertTrue(CuChulainnAbilityService.start(e,1),"Laguz rejected");close(h,CuChulainnAbilityService.mana(e),580,"Laguz mana");
         h.assertTrue(e.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED).getAmplifier()==2,"Laguz not speed III");
         h.assertTrue(CuChulainnAbilityService.cooldown(e,-1)==0,"thrust shared a cooldown");
         h.assertTrue(CuChulainnAbilityService.cooldown(e,6)==0,"barrier shared a cooldown");
      }
      h.runAfterDelay(15,()->{for(var e:java.util.List.of(player,npc))h.assertTrue(CuChulainnAbilityService.start(e,-1),"independent thrust rejected");h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=205)
   public static void berkanaHasExactlyNineSixPointTicksAndImmediateTen(GameTestHelper h) {
      var e=npc(h);e.setHealth(50);
      h.assertTrue(CuChulainnAbilityService.start(e,4),"Berkana rejected");e.removeEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
      close(h,e.getHealth(),60,"immediate heal");
      h.runAfterDelay(19,()->close(h,e.getHealth(),60,"early heal"));
      h.runAfterDelay(182,()->close(h,e.getHealth(),114,"nine ticks"));
      h.runAfterDelay(199,()->{close(h,e.getHealth(),114,"extra tick");h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=25)
   public static void actualDamageAloneRestartsRecoveryAndDoesNotClearCooldown(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,3);e.setTarget(t);e.setHealth(50);
      long now=h.getLevel().getGameTime();e.getPersistentData().putLong(CuChulainnAbilityService.LAST_DAMAGE,now-1199);
      CuChulainnAbilityService.cooldown(e,5,1000);CuChulainnAbilityService.npCooldown(e,600);
      h.assertTrue(CuChulainnAbilityService.start(e,3),"Algiz rejected");
      long before=e.getPersistentData().getLong(CuChulainnAbilityService.LAST_DAMAGE);
      e.hurt(e.damageSources().mobAttack(t),10);
      h.assertTrue(e.getPersistentData().getLong(CuChulainnAbilityService.LAST_DAMAGE)==before,"absorbed damage reset timer");
      e.getPersistentData().putLong(CuChulainnAbilityService.LAST_DAMAGE,now-1200);CuChulainnAbilityService.tick(e);
      close(h,e.getHealth(),e.getMaxHealth(),"target blocked recovery");close(h,CuChulainnAbilityService.mana(e),e.getMaxMp(),"mana recovery");
      h.assertTrue(CuChulainnAbilityService.cooldown(e,5)==now+1000 && e.getPersistentData().getLong(CuChulainnAbilityService.NP_END)==now+600,"recovery refreshed cooldown");
      CuChulainnAbilityService.hit(e,t,10,"test",false);
      h.assertTrue(e.getPersistentData().getLong(CuChulainnAbilityService.LAST_DAMAGE)==now,"outgoing actual damage not recorded");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=35)
   public static void flurryLocksOneTargetAndHitsThreeTimes(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,3);var other=target(h,e,4);h.assertTrue(CuChulainnAbilityService.start(e,9),"flurry rejected");
      h.runAfterDelay(22,()->{close(h,t.getHealth(),970,"flurry total");close(h,other.getHealth(),1000,"flurry switched targets");h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=35)
   public static void sweepCapsNearestThreeAndFiltersAllies(GameTestHelper h) {
      var e=npc(h);var team=h.getLevel().getScoreboard().addPlayerTeam("cu"+e.getId());h.getLevel().getScoreboard().addPlayerToTeam(e.getScoreboardName(),team);
      var ally=target(h,e,1);h.getLevel().getScoreboard().addPlayerToTeam(ally.getScoreboardName(),team);
      var a=target(h,e,1.5);var b=target(h,e,2);var c=target(h,e,2.5);var d=target(h,e,3.2);
      h.assertTrue(CuChulainnAbilityService.start(e,8),"sweep rejected");
      h.runAfterDelay(22,()->{for(var t:java.util.List.of(a,b,c))close(h,t.getHealth(),976,"two sweep stages");close(h,d.getHealth(),1000,"fourth target");close(h,ally.getHealth(),1000,"allied target");h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=30)
   public static void spearKeepsComponentsAndReturnsOnceWithOccupiedHand(GameTestHelper h) {
      var e=card(h);var item=e.getMainHandItem();item.set(DataComponents.CUSTOM_NAME,Component.literal("Original Gae Bulg"));
      h.assertTrue(CuSpearLedger.launch(e,false,24,null),"throw rejected");h.assertTrue(e.getMainHandItem().isEmpty(),"throw did not remove original");
      h.assertTrue(!CuSpearLedger.launch(e,false,24,null),"second spear generated");
      var spear=spear(h,e);e.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE));
      CuSpearLedger.deliver(spear,e);CuSpearLedger.deliverPending(e);
      h.assertTrue(e.getMainHandItem().is(Items.STONE),"occupied hand overwritten");
      long count=e.getInventory().items.stream().filter(s->s.is(ModItems.GAE_BULG.get())).count();close(h,count,1,"return duplicated");
      h.assertTrue(e.getInventory().items.stream().anyMatch(s->s.is(ModItems.GAE_BULG.get())&&"Original Gae Bulg".equals(s.getHoverName().getString())),"components lost");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=35)
   public static void otherPlayerInteractionReturnsWithoutManaOrRepeatedDamage(GameTestHelper h) {
      var e=card(h);var outsider=h.makeMockServerPlayerInLevel();
      h.assertTrue(CuSpearLedger.launch(e,false,24,null),"throw rejected");var spear=spear(h,e);spear.setPos(e.position().add(0,1,8));
      double mana=CuChulainnAbilityService.mana(e);spear.interact(outsider,InteractionHand.MAIN_HAND);spear.interact(outsider,InteractionHand.MAIN_HAND);
      h.assertTrue(spear.state()==2,"other interaction did not recall");close(h,CuChulainnAbilityService.mana(e),mana,"interaction charged MP");
      h.runAfterDelay(24,()->{h.assertTrue(!CuSpearLedger.outstanding(e),"return did not complete");h.assertTrue(e.getMainHandItem().is(ModItems.GAE_BULG.get()),"owner did not receive original");h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=25)
   public static void fullInventoryStaysPendingAndInvalidatesOldEntity(GameTestHelper h) {
      var e=card(h);h.assertTrue(CuSpearLedger.launch(e,false,24,null),"throw rejected");var spear=spear(h,e);
      e.getAbilities().instabuild=true;
      for(int slot=0;slot<36;slot++)e.getInventory().setItem(slot,new ItemStack(Items.STONE,64));
      CuSpearLedger.deliver(spear,e);h.assertTrue(CuSpearLedger.entry(e).state==CuSpearLedger.State.PENDING,"full inventory lost record");
      h.assertTrue(!CuSpearLedger.valid(spear),"old entity remained valid");e.getInventory().setItem(8,ItemStack.EMPTY);CuSpearLedger.deliverPending(e);CuSpearLedger.deliverPending(e);
      h.assertTrue(e.getInventory().getItem(8).is(ModItems.GAE_BULG.get()),"pending delivery failed");h.assertTrue(!CuSpearLedger.outstanding(e),"pending delivered repeatedly");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=25)
   public static void reloadPreservesUniqueOriginalAndInvalidatesOldToken(GameTestHelper h) {
      var e=npc(h);h.assertTrue(CuSpearLedger.launch(e,false,24,null),"throw rejected");var ledger=CuSpearLedger.get(h.getLevel());
      var tag=ledger.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
      var loaded=CuSpearLedger.load(tag,h.getLevel().registryAccess());var saved=loaded.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
      h.assertTrue(saved.getList("Spears",10).stream().map(t->(net.minecraft.nbt.CompoundTag)t).anyMatch(t->t.getUUID("Owner").equals(e.getUUID())&&t.getString("State").equals("PENDING")&&!t.hasUUID("Entity")),"reload did not retire old entity");
      CuSpearLedger.ownerLeft(e);CuSpearLedger.deliverPending(e);h.assertTrue(!CuSpearLedger.outstanding(e),"pending restoration failed");h.succeed();
   }

   @GameTest(template="ancient_temple",timeoutTicks=25)
   public static void lockExpiresAndFailedRetargetDoesNotRefreshIt(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,4);
      h.assertTrue(CuChulainnWeaponService.lock(e,t),"four-block lock rejected");
      long end=e.getPersistentData().getLong(CuChulainnAbilityService.LOCK_END);
      close(h,end-h.getLevel().getGameTime(),600,"lock lifetime");
      t.setPos(e.position().add(0,0,6));
      h.assertTrue(!CuChulainnWeaponService.lock(e,t),"distant target accepted");
      h.assertTrue(e.getPersistentData().getLong(CuChulainnAbilityService.LOCK_END)==end,"failed lock refreshed lifetime");
      h.assertTrue(CuChulainnWeaponService.locked(e)==t,"retained lock lost outside acquisition range");
      h.assertTrue(!CuChulainnWeaponService.noble(e,false,20),"retained lock bypassed thorn range");
      close(h,e.getCurrentMp(),600,"rejected thorn charged mana");
      e.getPersistentData().putLong(CuChulainnAbilityService.LOCK_END,h.getLevel().getGameTime());
      h.assertTrue(CuChulainnWeaponService.locked(e)==null,"expired lock remained");h.succeed();
   }

   @GameTest(template="ancient_temple",timeoutTicks=25)
   public static void shieldsConsumeOnlyTheirCapacityAndBranchesApplyOnce(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,3);e.setHealth(e.getMaxHealth()/2);
      CuChulainnAbilityService.hit(e,t,100,"branches_test",false);
      close(h,t.getHealth(),885,"four branches applied more than once");
      e.getPersistentData().putFloat(net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper.ALGIZ_SHIELD_TAG,100);
      close(h,CuChulainnAbilityService.absorb(e,140),40,"Algiz overflow");
      close(h,e.getPersistentData().getFloat(net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper.ALGIZ_SHIELD_TAG),0,"Algiz not removed");
      h.assertTrue(CuChulainnAbilityService.start(e,6),"barrier rejected");
      close(h,CuChulainnAbilityService.absorb(e,2025),25,"barrier overflow");
      close(h,e.getPersistentData().getFloat(net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper.RUNE_BARRIER_HP_TAG),0,"barrier not removed");h.succeed();
   }

   @GameTest(template="ancient_temple",timeoutTicks=40)
   public static void recallSweepsTargetsOnceAndPreservesProtectedBlocks(GameTestHelper h) {
      var e=npc(h);var t=target(h,e,5);
      t.setPos(t.position().add(1.2,0,0));
      h.assertTrue(CuSpearLedger.launch(e,false,24,null),"throw rejected");var spear=spear(h,e);
      spear.setPos(e.position().add(0,1,10));
      BlockPos bedrock=BlockPos.containing(e.position().add(0,1,7));h.getLevel().setBlockAndUpdate(bedrock,Blocks.BEDROCK.defaultBlockState());
      spear.beginReturn();
      h.runAfterDelay(20,()->{
         close(h,t.getHealth(),976,"recall did not hit exactly once");
         h.assertTrue(h.getLevel().getBlockState(bedrock).is(Blocks.BEDROCK),"recall broke protected terrain");
         h.assertTrue(!CuSpearLedger.outstanding(e),"protected terrain blocked return");h.succeed();
      });
   }

   @GameTest(template="ancient_temple",timeoutTicks=220)
   public static void failedLeapKeepsSpearAndUsesInterruptionRefund(GameTestHelper h) {
      var e=npc(h);e.setNoGravity(false);e.setOnGround(true);
      h.assertTrue(CuChulainnWeaponService.noble(e,true,40),"flight rejected");
      close(h,e.getCurrentMp(),300,"flight payment");
      long release=net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.releaseAt(e);
      // Lose the ground during the voice window, before the effect is allowed to jump.
      e.setNoGravity(true);e.setOnGround(false);
      double[] before={0};
      h.runAfterDelay((int)(release-h.getLevel().getGameTime())-1,()->before[0]=e.getCurrentMp());
      h.runAfterDelay((int)(release-h.getLevel().getGameTime())+3,()->{
         double restored=e.getCurrentMp()-before[0];
         double regeneration=net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService.regenPerSecondFor(e.getServantId())/20;
         h.assertTrue(restored>=150 && restored<=150+regeneration*5+.05,"failed leap refund: "+restored);
         h.assertTrue(e.getMainHandItem().is(ModItems.GAE_BULG.get()),"failed leap removed original");
         h.assertTrue(!CuSpearLedger.outstanding(e),"failed leap generated a projectile");
         h.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.cancelled(e),"failed effect remained live");h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=60)
   public static void recallReturnsAcrossTheFullThrowRange(GameTestHelper h) {
      var e=npc(h);h.assertTrue(CuSpearLedger.launch(e,false,24,null),"throw rejected");var spear=spear(h,e);
      spear.setPos(e.position().add(0,1,48));
      h.getLevel().getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED,new net.minecraft.world.level.ChunkPos(spear.blockPosition()),2,new net.minecraft.world.level.ChunkPos(spear.blockPosition()));
      spear.beginReturn();
      h.runAfterDelay(45,()->{h.assertTrue(!CuSpearLedger.outstanding(e),"full-range return failed state="+(CuSpearLedger.entry(e)==null?null:CuSpearLedger.entry(e).state)+" pos="+spear.position());h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=45)
   public static void ordinaryThrowStopsAtRangeAndEmbeds(GameTestHelper h) {
      var e=npc(h);Vec3 origin=e.position();
      for(int x=-1;x<=1;x++)for(int z=46;z<=49;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(origin.add(x,-1,z)),Blocks.STONE.defaultBlockState());
      var chunk=new net.minecraft.world.level.ChunkPos(BlockPos.containing(origin.add(0,0,48)));
      h.getLevel().getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED,chunk,2,chunk);
      h.assertTrue(CuSpearLedger.launch(e,false,24,null),"throw rejected");var spear=spear(h,e);
      h.runAfterDelay(38,()->{
         h.assertTrue(spear.position().subtract(origin).horizontalDistance()<=48.01,"throw exceeded range");
         h.assertTrue(spear.state()==1,"throw failed to fall and embed");h.succeed();
      });
   }
}
