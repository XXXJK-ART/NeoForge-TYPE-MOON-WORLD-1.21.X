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
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardUnlimitedMode;
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
   @GameTest(template="ancient_temple",timeoutTicks=30)
   public static void servantCardItemAppliesPassivesAndCanTransformAgain(GameTestHelper h) {
      var player=h.makeMockServerPlayerInLevel();
      player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).is_magus=true;
      for(int attempt=0;attempt<2;attempt++) {
         player.setItemInHand(InteractionHand.MAIN_HAND,ModItems.SERVANT_CARD_CU_CHULAINN.get().getDefaultInstance());
         player.getMainHandItem().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
         var state=player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
         h.assertTrue(state.transformed() && "cu_chulainn".equals(state.id()),"card item did not transform");
         var data=player.getPersistentData();
         close(h,data.getFloat("MagicResistanceDebuffResist"),.10,"card debuff resistance");
         h.assertTrue(player.getMainHandItem().is(ModItems.GAE_BULG.get()),"transformation did not equip spear");
         ServantCardTransformManager.release(player,false);
         h.assertTrue(!state.transformed(),"release did not restore player");
      }
      h.succeed();
   }
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
      t.setPos(t.position().add(2.2,0,0));
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
      target(h,e,8);
      h.assertTrue(CuChulainnWeaponService.noble(e,true,40),"flight rejected");
      close(h,e.getCurrentMp(),300,"flight payment");
      long release=net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.releaseAt(e);
      // Block the takeoff after payment without removing the original spear.
      e.setNoGravity(true);e.setOnGround(false);
      h.getLevel().setBlockAndUpdate(BlockPos.containing(e.position().add(0,2,0)),Blocks.STONE.defaultBlockState());
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
   public static void ordinaryThrowEmbedsAtSolidTerrain(GameTestHelper h) {
      var e=npc(h);Vec3 origin=e.position();
      // Ordinary throws now use view distance; test collision independently of that limit.
      for(int x=-1;x<=1;x++)for(int y=0;y<=3;y++)h.getLevel().setBlockAndUpdate(BlockPos.containing(origin.add(x,y,8)),Blocks.STONE.defaultBlockState());
      h.assertTrue(CuSpearLedger.launch(e,false,24,null),"throw rejected");var spear=spear(h,e);
      h.runAfterDelay(38,()->{
         h.assertTrue(spear.position().subtract(origin).horizontalDistance()<=8.01,"throw passed solid terrain");
         h.assertTrue(spear.state()==1,"throw failed to embed in the wall");h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=30)
   public static void barrierToggleBypassesCooldownAndDoesNotChargeAgain(GameTestHelper h) {
      var e=card(h);
      h.assertTrue(CuChulainnAbilityService.start(e,6),"barrier start rejected");
      double mana=CuChulainnAbilityService.mana(e);long cooldown=CuChulainnAbilityService.cooldown(e,6);
      h.assertTrue(CuChulainnAbilityService.start(e,6),"active barrier could not toggle off");
      close(h,CuChulainnAbilityService.mana(e),mana,"toggle charged mana");
      h.assertTrue(CuChulainnAbilityService.cooldown(e,6)==cooldown,"toggle refreshed cooldown");
      h.assertTrue(!CuChulainnAbilityService.active(e),"toggle retained cast");
      close(h,e.getPersistentData().getFloat(net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper.RUNE_BARRIER_HP_TAG),0,"barrier retained");
      h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=45)
   public static void embeddedSpearHasReachableHitboxAndOwnerCanPickItUp(GameTestHelper h) {
      var e=card(h);h.assertTrue(CuSpearLedger.launch(e,false,24,null),"throw rejected");var spear=spear(h,e);
      spear.setPos(e.position().add(0,.3,1));spear.setDeltaMovement(0,-.5,0);
      h.runAfterDelay(5,()->{
         h.assertTrue(spear.state()==1,"spear not embedded");
         h.assertTrue(spear.getBoundingBox().getYsize()>=2,"embedded hitbox too short");
         spear.interactAt(e,new Vec3(0,1,0),InteractionHand.MAIN_HAND);
         h.assertTrue(!CuSpearLedger.outstanding(e),"owner pickup failed");
         h.assertTrue(e.getMainHandItem().is(ModItems.GAE_BULG.get()),"original missing");h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=85)
   public static void unlockedThornFindsVisibleAimedTarget(GameTestHelper h) {
      var e=npc(h);e.setYRot(0);var t=target(h,e,4.8);
      h.assertTrue(CuChulainnWeaponService.locked(e)==null,"unexpected lock");
      h.assertTrue(CuChulainnWeaponService.noble(e,false,20),"unlocked thorn rejected");
      close(h,e.getCurrentMp(),580,"thorn payment");
      long release=net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.releaseAt(e);
      // No-AI fixtures do not integrate movement, so complete the final approach explicitly.
      h.runAfterDelay((int)(release-h.getLevel().getGameTime())+1,()->e.setPos(t.position().add(0,0,-3)));
      h.runAfterDelay(55,()->{h.assertTrue(t.getHealth()<1000,"unlocked target not hit");h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=25)
   public static void unlockedNoblesRejectOutOfRangeOrOccludedTargetsWithoutPayment(GameTestHelper h) {
      var e=npc(h);e.setOnGround(true);var t=target(h,e,49);
      var chunk=new net.minecraft.world.level.ChunkPos(t.blockPosition());
      h.getLevel().getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED,chunk,2,chunk);
      h.assertTrue(!CuChulainnWeaponService.noble(e,false,20),"out-of-range thorn accepted");
      h.assertTrue(!CuChulainnWeaponService.noble(e,true,40),"out-of-range flight accepted");
      t.setPos(e.position().add(0,0,3));
      for(int x=-1;x<=1;x++)for(int y=0;y<=3;y++)h.getLevel().setBlockAndUpdate(BlockPos.containing(e.position().add(x,y,1)),Blocks.STONE.defaultBlockState());
      h.assertTrue(!CuChulainnWeaponService.noble(e,false,20),"thorn selected through wall");
      h.assertTrue(!CuChulainnWeaponService.noble(e,true,40),"flight selected through wall");
      close(h,e.getCurrentMp(),600,"failed selection charged mana");
      h.assertTrue(e.getPersistentData().getLong(CuChulainnAbilityService.NP_END)==0,"failed selection started cooldown");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=25)
   public static void unlockedThornRequiresAimAndFlightAllowsMoreTolerance(GameTestHelper h) {
      var e=npc(h);e.setYRot(0);var t=target(h,e,4);
      t.setPos(t.position().add(.9,0,0));
      h.assertTrue(CuChulainnAbilityService.aimedTargets(e,CuChulainnRules.THORN_RANGE,.35).isEmpty(),"thorn aim accepted a miss");
      h.assertTrue(CuChulainnAbilityService.aimedTargets(e,CuChulainnRules.THROW_RANGE,1.25).contains(t),"flight aim tolerance was not wider");
      h.assertTrue(!CuChulainnWeaponService.noble(e,false,20),"unlocked thorn ignored aim");
      close(h,e.getCurrentMp(),600,"missed thorn charged MP");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=25)
   public static void noCooldownCommandClearsAndBypassesCuTimersWithoutFreeMana(GameTestHelper h) {
      var p=card(h);CuChulainnAbilityService.cooldown(p,1,600);CuChulainnAbilityService.npCooldown(p,600);
      p.getPersistentData().putBoolean("TypeMoonNoCooldown",true);
      ServantCardUnlimitedMode.clearServantCardCooldowns(p);
      var vars=p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);double mana=CuChulainnAbilityService.mana(p);
      h.assertTrue(CuChulainnAbilityService.start(p,1),"global no-cooldown did not bypass rune cooldown");
      close(h,CuChulainnAbilityService.mana(p),mana-20,"no-cooldown also removed mana cost");
      h.assertTrue(!p.getPersistentData().contains("CuShared:cd:1"),"custom rune timer remained");
      CuChulainnAbilityService.npCooldown(p,600);
      h.assertTrue(!p.getPersistentData().contains(CuChulainnAbilityService.NP_END),"NP timer was written in no-cooldown mode");
      h.assertTrue(vars.servantCardState().npCooldown()==0,"card NP timer remained");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=450)
   public static void ordinaryThrowUsesGravityAndReturnsAtViewDistance(GameTestHelper h) {
      var p=card(h);p.setXRot(-90);p.setYRot(0);
      var chunk=new net.minecraft.world.level.ChunkPos(p.blockPosition());
      h.getLevel().getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED,chunk,2,chunk);
      h.assertTrue(CuSpearLedger.launch(p,false,24,null),"ordinary throw rejected");var thrown=spear(h,p);
      h.assertTrue(thrown!=null,"ordinary spear missing");
      int clientChunks=p.clientInformation().viewDistance();
      int viewChunks=Math.max(1,clientChunks>0?Math.min(h.getLevel().getServer().getPlayerList().getViewDistance(),clientChunks)
         :h.getLevel().getServer().getPlayerList().getViewDistance());
      double maxDistance=viewChunks*16.0, predictedDistance=0, predictedSpeed=thrown.getDeltaMovement().y;
      // The structure has overhead blocks. Keep this vertical flight fixture unobstructed.
      for(int y=1;y<=Math.ceil(maxDistance)+4;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
         h.getLevel().setBlockAndUpdate(BlockPos.containing(p.position().add(x,y,z)),Blocks.AIR.defaultBlockState());
      int flightTicks=0;
      while(predictedDistance<maxDistance && flightTicks<1000) {
         predictedSpeed-=.005;predictedDistance+=predictedSpeed;flightTicks++;
      }
      h.assertTrue(flightTicks<1000,"view range exceeded test flight bound");
      double initialVerticalSpeed=thrown.getDeltaMovement().y;
      int returnCheckTick=flightTicks+3;
      h.runAfterDelay(2,()->h.assertTrue(thrown.getDeltaMovement().y<initialVerticalSpeed,"ordinary spear ignored gravity"));
      h.runAfterDelay(returnCheckTick,()->{
         h.assertTrue(thrown.state()==2,"spear did not return at view-distance limit: "+thrown.state());
         h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=105)
   public static void flightSurvivesLeavingGroundDuringChant(GameTestHelper h) {
      var e=npc(h);e.setOnGround(true);var t=target(h,e,8);
      h.assertTrue(CuChulainnWeaponService.noble(e,true,40),"flight rejected");
      e.setOnGround(false);e.setPos(e.position().add(0,.3,0));
      long release=net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.releaseAt(e);
      h.runAfterDelay((int)(release-h.getLevel().getGameTime())+24,()->{
         h.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.cancelled(e),"airborne chant cancelled");
         h.assertTrue(CuSpearLedger.outstanding(e),"spear not launched");h.succeed();
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=80)
   public static void returnCrossesBothSidesOfWideZigzag(GameTestHelper h) {
      var e=npc(h);h.assertTrue(CuSpearLedger.launch(e,false,24,null),"throw rejected");var spear=spear(h,e);
      spear.setPos(e.position().add(0,1,40));
      var chunk=new net.minecraft.world.level.ChunkPos(spear.blockPosition());
      h.getLevel().getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED,chunk,2,chunk);
      spear.beginReturn();double[] bounds={0,0};
      for(int tick=1;tick<=45;tick++)h.runAfterDelay(tick,()->{double x=spear.getX()-e.getX();bounds[0]=Math.min(bounds[0],x);bounds[1]=Math.max(bounds[1],x);});
      h.runAfterDelay(55,()->{
         h.assertTrue(bounds[0]<=-5.5 && bounds[1]>=5.5,"return did not span both sides: "+java.util.Arrays.toString(bounds));
         h.assertTrue(!CuSpearLedger.outstanding(e),"wide return did not complete");h.succeed();
      });
   }
}
