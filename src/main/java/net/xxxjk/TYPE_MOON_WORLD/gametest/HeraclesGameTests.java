package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaCombatEvents;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.*;

@GameTestHolder("typemoonworld_heracles")
@PrefixGameTestTemplate(false)
public final class HeraclesGameTests {
   private HeraclesGameTests() {}
   private static HeraclesEntity owner(GameTestHelper h) {
      var at=h.absolutePos(new BlockPos(6,6,6));
      for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=-1;y<=36;y++)h.getLevel().setBlock(at.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
      var e=ModEntities.HERACLES.get().create(h.getLevel());
      e.readAdditionalSaveData(e.saveWithoutId(new CompoundTag()));
      e.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(6,6,6))));e.setYRot(0);e.setNoAi(true);e.setNoGravity(true);
      for(var slot:EquipmentSlot.values())e.setItemSlot(slot,ItemStack.EMPTY);
      e.getAttribute(Attributes.ARMOR).setBaseValue(0);e.setHealth(e.getMaxHealth());e.setCurrentMp(1000);
      e.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES,0);
      e.getPersistentData().putLong(ServantCombatDefenseService.AUTO_RESTORE,Long.MAX_VALUE);
      h.getLevel().addFreshEntity(e);return e;
   }
   private static LivingEntity target(GameTestHelper h,Vec3 pos) {
      var e=EntityType.HUSK.create(h.getLevel());e.setPos(pos);e.setNoAi(true);e.setNoGravity(true);
      e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);e.getAttribute(Attributes.ARMOR).setBaseValue(0);e.setHealth(1000);
      h.getLevel().addFreshEntity(e);return e;
   }
   private static void close(GameTestHelper h,double actual,double expected,String reason) { h.assertTrue(Math.abs(actual-expected)<.02,reason+": "+actual+" expected "+expected); }
   private static void ticks(LivingEntity e,int count) { for(int i=0;i<count;i++)HeraclesAbilityService.tick(new EntityTickEvent.Post(e)); }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_attributes")
   public static void attributesAreSharedAndRefreshNeverHeals(GameTestHelper h) {
      var e=owner(h);h.assertTrue("A".equals(e.getDefinition().parameters().noblePhantasmRank()),"NP parameter panel rank");close(h,e.getMaxHealth(),680,"NPC maximum health");close(h,e.getAttributeValue(Attributes.ATTACK_DAMAGE),72,"NPC basic attack");
      close(h,e.getAttributeValue(Attributes.MOVEMENT_SPEED),e.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)*1.13,"NPC additive speed");
      e.setHealth(321);CommonSkillService.reconcile(e);CommonSkillService.reconcile(e);close(h,e.getHealth(),321,"refresh healed");
      var p=h.makeMockServerPlayerInLevel();h.assertTrue(ServantCardTransformManager.transform(p,"heracles"),"transform failed");
      close(h,p.getMaxHealth(),680,"player maximum health");
      p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
      close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),72,"player basic attack");
      p.setHealth(222);CommonSkillService.reconcile(p);close(h,p.getHealth(),222,"player refresh healed");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_threshold")
   public static void thresholdIsBeforeArmorAndBeforeDodge(GameTestHelper h) {
      var e=owner(h);e.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES,2);
      float health=e.getHealth();e.hurt(e.damageSources().generic(),14.99F);
      close(h,e.getHealth(),health,"below-threshold damage");h.assertTrue(e.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES)==2,"immune hit consumed dodge");
      e.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES,0);e.hurt(e.damageSources().generic(),15);
      close(h,health-e.getHealth(),12,"15-point threshold");
      e.invulnerableTime=0;e.getAttribute(Attributes.ARMOR).setBaseValue(30);e.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(20);e.setHealth(40);var attacker=target(h,e.position().add(0,0,2));e.hurt(e.damageSources().mobAttack(attacker),100);
      h.assertTrue(e.getHealth()<40&&e.getPersistentData().getInt("GodHandLives")==11,"armor was applied before threshold or revival");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_revival")
   public static void revivesInPlaceWithoutRefillingResourcesOrLockingNextHit(GameTestHelper h) {
      var e=owner(h);var id=e.getUUID();e.setCurrentMp(123);e.getPersistentData().putLong("HeraclesCooldown:3",999999);
      var source=new CombatDamageSource(e.damageSources().generic(),AttackOrigin.skill("test","lethal"));
      e.hurt(source,10000);e.hurt(source,10000);
      h.assertTrue(e.isAlive()&&e.getUUID().equals(id)&&e.getPersistentData().getInt("GodHandLives")==9,"consecutive independent lethals did not consume once each");
      close(h,e.getHealth(),680,"full revival");close(h,e.getCurrentMp(),123,"revival restored mana");
      h.assertTrue(e.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES)==0&&e.getPersistentData().getLong("HeraclesCooldown:3")==999999,"revival refreshed resources");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_adaptation")
   public static void adaptationPersistsAndCompletesAt100Ticks(GameTestHelper h) {
      var e=owner(h);var source=new CombatDamageSource(e.damageSources().generic(),AttackOrigin.skill("test","sword"));
      e.hurt(source,10000);
      for(int i=0;i<99;i++)GodHandService.tick(e);
      h.assertTrue(GodHandService.read(e).adapted().isEmpty(),"adapted at tick99");GodHandService.tick(e);
      h.assertTrue(GodHandService.read(e).adapted().contains(AttackOrigin.of(source).key()),"not adapted at tick100");
      var loaded=ModEntities.HERACLES.get().create(h.getLevel());loaded.load(e.saveWithoutId(new CompoundTag()));
      h.assertTrue(GodHandService.read(loaded).equals(GodHandService.read(e)),"NBT lost current round");
      close(h,loaded.getHealth(),e.getHealth(),"NBT changed health");
      e.invulnerableTime=0;e.hurt(source,100);close(h,680-e.getHealth(),20,"adapted multiplier");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_oda")
   public static void odaSplitRetainsEraAndOnlyExemptsDivineIncrement(GameTestHelper h) {
      var e=owner(h);var oda=ModEntities.ODA_NOBUNAGA.get().create(h.getLevel());
      var source=oda.damageSources().mobAttack(oda);
      var incoming=new LivingIncomingDamageEvent(e,new DamageContainer(source,10));OdaCombatEvents.outgoing(incoming);
      GodHandService.beforeDefense(incoming);close(h,incoming.getAmount(),82,"unadapted Oda split");
      GodHandService.write(e,new GodHandState(11,java.util.Set.of(AttackOrigin.of(source).key()),"",0,java.util.Set.of()));
      incoming=new LivingIncomingDamageEvent(e,new DamageContainer(source,10));OdaCombatEvents.outgoing(incoming);GodHandService.beforeDefense(incoming);
      close(h,incoming.getAmount(),58,"adapted Oda split");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_poison")
   public static void poisonIgnoresAbsorptionAndDoesNotAdaptButWitherUsesNormalRules(GameTestHelper h) {
      var e=owner(h);e.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(30);e.setAbsorptionAmount(30);e.getAttribute(Attributes.ARMOR).setBaseValue(30);
      e.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES,2);
      e.hurt(e.damageSources().source(NeoForgeMod.POISON_DAMAGE),10);close(h,e.getHealth(),670,"poison health");close(h,e.getAbsorptionAmount(),30,"poison consumed absorption");
      h.assertTrue(e.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES)==2,"poison consumed dodge");
      e.hurt(e.damageSources().wither(),10);close(h,e.getHealth(),670,"wither bypassed threshold");
      e.hurt(e.damageSources().source(NeoForgeMod.POISON_DAMAGE),10000);
      h.assertTrue(GodHandService.read(e).lives()==10&&GodHandService.read(e).remainingTicks()==0,"poison adaptation or duplicate revival");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_concepts")
   public static void specialImmunitiesArePerSkillAndCausalityWins(GameTestHelper h) {
      var e=owner(h);
      h.assertTrue(GodHandService.special(e,"medusa:cybele"),"first special failed");GodHandService.special(e,"medusa:cybele");
      h.assertTrue(GodHandService.read(e).lives()==10,"repeated concept consumed lives");
      GodHandService.special(e,"li_shuwen:wu_er_da");h.assertTrue(GodHandService.read(e).lives()==9,"different concept shared immunity");
      e.getPersistentData().putBoolean("CausalSevered",true);h.assertTrue(!GodHandService.special(e,"test:concept"),"causal cut allowed revival");
      e.hurt(e.damageSources().source(NeoForgeMod.POISON_DAMAGE),10000);h.assertTrue(!e.isAlive(),"causal cut allowed continuation");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_continuation")
   public static void continuationWaitsForAbsorptionAndGodHandExhaustion(GameTestHelper h) {
      var e=owner(h);GodHandService.write(e,new GodHandState(0,java.util.Set.of(),"",0,java.util.Set.of()));e.setHealth(10);e.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(30);e.setAbsorptionAmount(30);
      e.hurt(e.damageSources().generic(),20);close(h,e.getHealth(),10,"absorption triggered continuation");
      h.assertTrue(!e.getPersistentData().contains("CommonContinuationReady"),"absorption started cooldown");
      e.invulnerableTime=0;e.hurt(e.damageSources().generic(),200);close(h,e.getHealth(),136,"A continuation health");
      close(h,e.getPersistentData().getLong("CommonContinuationReady")-e.level().getGameTime(),12000,"A continuation cooldown");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_origins")
   public static void weaponsAndNamedSkillsHaveStableDistinctOrigins(GameTestHelper h) {
      var p=h.makeMockServerPlayerInLevel();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
      var sword=AttackOrigin.of(p.damageSources().playerAttack(p));p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));
      h.assertTrue(!sword.equals(AttackOrigin.of(p.damageSources().playerAttack(p))),"sword and axe shared origin");
      var q=h.makeMockServerPlayerInLevel();q.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
      h.assertTrue(sword.equals(AttackOrigin.of(q.damageSources().playerAttack(q))),"wielder changed origin");
      h.assertTrue(!AttackOrigin.skill("one","fire").equals(AttackOrigin.skill("two","fire")),"different character skills shared origin");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_flurry")
   public static void flurryUsesThreeFixedHitsAndPaysOnlyOnce(GameTestHelper h) {
      var e=owner(h);var t=target(h,e.position().add(0,0,2));e.setTarget(t);double mp=e.getCurrentMp();
      h.assertTrue(HeraclesAbilityService.start(e,7),"flurry start: target="+e.getTarget()+" distance="+e.distanceTo(t)+" visible="+e.hasLineOfSight(t)+" enemy="+HeraclesAbilityService.enemy(e,t)+" control="+HeraclesAbilityService.controlled(e)+" suppressed="+ServantCombatSystem.skillsSuppressed(e));h.assertTrue(!HeraclesAbilityService.start(e,3),"overlapping action started");
      ticks(e,14);close(h,1000-t.getHealth(),66,"flurry fixed damage");close(h,mp-e.getCurrentMp(),30,"flurry resource payment");
      HeraclesAbilityService.cancel(e);h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_validation")
   public static void failedValidationAndHardControlCannotSpendMana(GameTestHelper h) {
      var e=owner(h);double mp=e.getCurrentMp();h.assertTrue(!HeraclesAbilityService.start(e,5),"throw started without target");close(h,e.getCurrentMp(),mp,"invalid action charged mana");
      var t=target(h,e.position().add(0,0,2));e.setTarget(t);e.setCurrentMp(1);h.assertTrue(!HeraclesAbilityService.start(e,3),"insufficient mana accepted");
      e.setCurrentMp(1000);h.assertTrue(HeraclesAbilityService.start(e,3),"uppercut start: target="+e.getTarget()+" distance="+e.distanceTo(t)+" visible="+e.hasLineOfSight(t)+" enemy="+HeraclesAbilityService.enemy(e,t)+" control="+HeraclesAbilityService.controlled(e)+" suppressed="+ServantCombatSystem.skillsSuppressed(e));e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.PETRIFIED,20));
      ticks(e,8);h.assertTrue(!HeraclesAbilityService.busy(e)&&t.getHealth()==1000,"controlled windup still hit");close(h,e.getCurrentMp(),980,"interruption refunded mana");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=40,batch="heracles_terrain")
   public static void delayedWallAndGroundJobsRespectTheFootingAndOneLayer(GameTestHelper h) {
      var e=owner(h);var origin=e.blockPosition();
      for(int x=-3;x<=3;x++)for(int z=-3;z<=5;z++) {
         h.getLevel().setBlock(origin.offset(x,-2,z),Blocks.STONE.defaultBlockState(),3);
         CombatTerrainProtection.canRemove(h.getLevel(),origin.offset(x,-2,z));
         h.getLevel().setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
      }
      for(int x=-1;x<=1;x++)for(int y=0;y<3;y++)h.getLevel().setBlock(origin.offset(x,y,2),Blocks.STONE.defaultBlockState(),3);
      TerrainImpactService.impactWallTunnel(h.getLevel(),e,e.position(),new Vec3(0,-1,1),TerrainImpactProfile.of(TerrainImpactProfile.Tier.MEDIUM),3,3,3);
      h.runAfterDelay(10,()->{
         h.assertTrue(h.getLevel().getBlockState(origin.below()).is(Blocks.STONE),"horizontal passage removed support");
         h.assertTrue(h.getLevel().getBlockState(origin.offset(0,1,2)).isAir(),"horizontal passage did not break the wall");
         TerrainImpactService.impactGroundLayer(h.getLevel(),e,e.position(),new TerrainImpactProfile(TerrainImpactProfile.Tier.MEDIUM,3.2,6,12,12),TerrainImpactService.Permission.PLAYER);
         h.runAfterDelay(10,()->{h.assertTrue(h.getLevel().getBlockState(origin.below()).isAir(),"ground layer not removed");h.assertTrue(h.getLevel().getBlockState(origin.below(2)).is(Blocks.STONE),"ground job dug a second layer");h.succeed();});
      });
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_three_strikes")
   public static void uppercutSweepAndStompUseFixedDamage(GameTestHelper h) {
      var e=owner(h);var t=target(h,e.position().add(0,0,2));e.setTarget(t);
      int[] slots={3,4,6};float[] damage={36,40,24};
      for(int i=0;i<slots.length;i++) {
         t.setHealth(1000);t.invulnerableTime=0;e.setTarget(t);
         h.assertTrue(HeraclesAbilityService.start(e,slots[i]),"action did not start "+slots[i]);
         ticks(e,16);close(h,1000-t.getHealth(),damage[i],"fixed strike "+slots[i]);HeraclesAbilityService.cancel(e);
      }
      h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_shoulder")
   public static void shoulderMovesAtMostSixBlocksAndHitsEachEnemyOnce(GameTestHelper h) {
      var e=owner(h);var start=e.position();var t=target(h,start.add(0,0,2.5));e.setTarget(t);
      h.assertTrue(HeraclesAbilityService.start(e,2),"shoulder start failed");ticks(e,14);
      close(h,1000-t.getHealth(),32,"shoulder repeated contact");h.assertTrue(e.position().distanceTo(start)<=6.01,"shoulder exceeded six blocks");
      HeraclesAbilityService.cancel(e);h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_big_jump")
   public static void bigJumpOwnsTheLandingAndPreventsDuplicatePassiveImpact(GameTestHelper h) {
      var e=owner(h);var start=e.position();var t=target(h,start.add(0,0,2));e.setOnGround(true);
      h.assertTrue(HeraclesAbilityService.start(e,0),"jump start failed");ticks(e,4);
      close(h,e.getDeltaMovement().horizontalDistance(),1.05,"jump horizontal speed");close(h,e.getDeltaMovement().y,1.52,"jump vertical speed");
      e.setOnGround(false);e.setPos(start.add(0,6,0));ticks(e,8);e.setPos(start);e.setOnGround(true);ticks(e,1);
      close(h,1000-t.getHealth(),26,"jump landing damage");ticks(e,15);close(h,1000-t.getHealth(),26,"duplicate passive landing");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_pounce")
   public static void pounceLocksItsDestinationAndResolvesOnlyOnce(GameTestHelper h) {
      var e=owner(h);var t=target(h,e.position().add(0,0,8));e.setTarget(t);Vec3 destination=t.position();
      h.assertTrue(HeraclesAbilityService.start(e,8),"pounce start failed");ticks(e,40);
      close(h,1000-t.getHealth(),46,"pounce repeated impact");h.assertTrue(e.position().distanceTo(destination)<.1,"pounce destination drifted");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_throw")
   public static void throwConstrainsBothActorsAndCollidesOnlyOnce(GameTestHelper h) {
      var e=owner(h);var t=target(h,e.position().add(0,0,4));e.setTarget(t);var floor=e.blockPosition().below();
      for(int x=-4;x<=4;x++)for(int z=-4;z<=8;z++)h.getLevel().setBlock(floor.offset(x,0,z),Blocks.STONE.defaultBlockState(),2);
      h.assertTrue(HeraclesAbilityService.start(e,5),"throw start failed");ticks(e,8);
      h.assertTrue(HeraclesAbilityService.held(t)&&HeraclesAbilityService.motionLocked(e),"grip did not constrain both actors");
      ticks(e,10);h.assertTrue(!HeraclesAbilityService.held(t),"throw left grab control");close(h,1000-t.getHealth(),28,"grab and release damage");
      t.horizontalCollision=true;HeraclesAbilityService.tick(new EntityTickEvent.Post(t));HeraclesAbilityService.tick(new EntityTickEvent.Post(t));
      close(h,1000-t.getHealth(),50,"throw collision did not settle exactly once");HeraclesAbilityService.cancel(e);h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_slam")
   public static void slamUsesHorizontalRadiusAndVerticalInterval(GameTestHelper h) {
      var e=owner(h);var near=target(h,e.position().add(5.3,0,0));var far=target(h,e.position().add(5.5,0,0));var high=target(h,e.position().add(0,3,1));
      h.assertTrue(HeraclesAbilityService.start(e,1),"slam failed");ticks(e,15);
      close(h,1000-near.getHealth(),58,"slam distance falloff");close(h,far.getHealth(),1000,"slam exceeded radius");close(h,high.getHealth(),1000,"slam exceeded vertical interval");
      HeraclesAbilityService.cancel(e);h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_concept_dedup")
   public static void aConceptWithNumericalDamageConsumesOnlyOneLife(GameTestHelper h) {
      var e=owner(h);e.setHealth(1);
      GodHandService.conceptHit(e,e.damageSources().generic(),100,"test:concept",true);
      h.assertTrue(GodHandService.read(e).lives()==10&&GodHandService.immune(e,"test:concept"),"one impact consumed twice");
      GodHandService.conceptHit(e,e.damageSources().generic(),100,"test:concept",true);
      h.assertTrue(GodHandService.read(e).lives()==10,"immune concept consumed again");close(h,e.getHealth(),600,"immune concept erased ordinary damage");h.succeed();
   }
   @GameTest(template="ancient_temple",timeoutTicks=420,batch="heracles_dodge_group")
   public static void exactlyTwoDodgesRestoreAsAGroupAfter400Ticks(GameTestHelper h) {
      var e=owner(h);e.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES,2);var attacker=target(h,e.position().add(0,0,2));
      var source=e.damageSources().mobAttack(attacker);e.hurt(source,20);e.hurt(source,20);
      h.assertTrue(e.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES)==0,"did not use exactly two charges");
      float before=e.getHealth();e.hurt(source,20);h.assertTrue(e.getHealth()<before,"extra random dodge remained");
      long restore=e.getPersistentData().getLong(ServantCombatDefenseService.AUTO_RESTORE);
      close(h,restore-e.level().getGameTime(),400,"dodge group duration");
      h.runAfterDelay(399,()->{ServantCombatDefenseService.tick(e);h.assertTrue(e.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES)==0,"restored at 399 ticks");});
      h.runAfterDelay(400,()->{ServantCombatDefenseService.tick(e);h.assertTrue(e.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES)==2,"not restored at 400 ticks");h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=30,batch="heracles_half_slab")
   public static void horizontalPassageProtectsTheActualHalfSlabSupport(GameTestHelper h) {
      var e=owner(h);var slab=e.blockPosition();
      h.getLevel().setBlock(slab,Blocks.STONE_SLAB.defaultBlockState(),2);e.setPos(e.position().add(0,.5,0));
      for(int y=0;y<4;y++)h.getLevel().setBlock(slab.offset(0,y,2),Blocks.STONE.defaultBlockState(),2);
      TerrainImpactService.impactWallTunnel(h.getLevel(),e,e.position(),new Vec3(0,-10,1),TerrainImpactProfile.of(TerrainImpactProfile.Tier.MEDIUM),3,3,3);
      h.runAfterDelay(12,()->{h.assertTrue(h.getLevel().getBlockState(slab).is(Blocks.STONE_SLAB),"half slab support was removed");h.assertTrue(h.getLevel().getBlockState(slab.offset(0,1,2)).isAir(),"passage ignored horizontal direction");h.succeed();});
   }
   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_player_adapter")
   public static void playerCardUsesTheSameExecutorWithoutAClientAnimationConnection(GameTestHelper h) {
      var e=owner(h);var player=h.makeMockServerPlayerInLevel();player.teleportTo(e.getX()+4,e.getY(),e.getZ());player.setYRot(0);player.setXRot(0);
      h.assertTrue(ServantCardTransformManager.transform(player,"heracles"),"player transform failed");
      player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
      var t=target(h,player.position().add(0,0,2));
      h.assertTrue(HeraclesAbilityService.start(player,7),"player flurry failed");ticks(player,14);close(h,1000-t.getHealth(),66,"player and NPC fixed damage diverged");
      HeraclesAbilityService.cancel(player);h.succeed();
   }

   @GameTest(template="ancient_temple",timeoutTicks=45,batch="heracles_shallow_crater")
   public static void heavyImpactsBreakTwoCentralLayersWithoutDiggingFurtherOnRepeat(GameTestHelper h) {
      var e=owner(h);var origin=e.blockPosition();
      for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-4;y<=-1;y++)
         h.getLevel().setBlock(origin.offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
      var profile=new TerrainImpactProfile(TerrainImpactProfile.Tier.HEAVY,3.2,6,48,64);
      TerrainImpactService.impactHeraclesGround(h.getLevel(),e,e.position(),profile,TerrainImpactService.Permission.PLAYER,2);
      h.runAfterDelay(12,()->{
         h.assertTrue(h.getLevel().getBlockState(origin.below()).isAir(),"surface did not break");
         h.assertTrue(h.getLevel().getBlockState(origin.below(2)).isAir(),"central second layer did not break");
         h.assertTrue(h.getLevel().getBlockState(origin.below(3)).is(Blocks.STONE),"impact exceeded two layers");
         TerrainImpactService.impactHeraclesGround(h.getLevel(),e,e.position().add(0,-2,0),profile,TerrainImpactService.Permission.PLAYER,2);
         h.runAfterDelay(12,()->{
            h.assertTrue(h.getLevel().getBlockState(origin.below(3)).is(Blocks.STONE),"repeat impact dug below original surface limit");
            h.succeed();
         });
      });
   }

   @GameTest(template="ancient_temple",timeoutTicks=25,batch="heracles_sprint_geometry")
   public static void sprintIgnoresFootingAndEmptySpaceButDetectsARealFrontWall(GameTestHelper h) {
      var e=owner(h);var origin=e.blockPosition();
      for(int x=-3;x<=3;x++)for(int z=-3;z<=4;z++)h.getLevel().setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
      var forward=new Vec3(0,0,1);
      h.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantSprintCollisionHelper.hasHeraclesForwardObstacle(h.getLevel(),e,forward),"flat floor counted as a front collision");
      h.assertTrue(!TerrainImpactService.impactWallTunnel(h.getLevel(),e,e.position(),forward,TerrainImpactProfile.of(TerrainImpactProfile.Tier.MEDIUM),3,3,3),"empty wall job reported an impact");
      h.getLevel().setBlock(origin.offset(0,0,1),Blocks.STONE_SLAB.defaultBlockState(),2);
      h.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantSprintCollisionHelper.hasHeraclesForwardObstacle(h.getLevel(),e,forward),"step-height slab counted as a wall");
      h.getLevel().setBlock(origin.offset(0,1,1),Blocks.STONE.defaultBlockState(),2);
      h.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantSprintCollisionHelper.hasHeraclesForwardObstacle(h.getLevel(),e,forward),"real front wall was ignored");
      h.assertTrue(TerrainImpactService.impactWallTunnel(h.getLevel(),e,e.position(),forward,TerrainImpactProfile.of(TerrainImpactProfile.Tier.MEDIUM),3,3,3),"real wall was not queued");
      h.runAfterDelay(10,()->{
         h.assertTrue(h.getLevel().getBlockState(origin.offset(0,1,1)).isAir(),"front wall was not broken");
         h.assertTrue(h.getLevel().getBlockState(origin.below()).is(Blocks.STONE),"sprint removed footing");h.succeed();
      });
   }

   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_phase_free")
   public static void berserkerHasNoPhaseTransitionsAndAttacksWithoutMana(GameTestHelper h) {
      var e=owner(h);var t=target(h,e.position().add(0,0,2));e.setTarget(t);e.setCurrentMp(0);
      for(float health:new float[]{680,300,50,680}) {
         e.setHealth(health);
         var update=net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantPhaseService.tick(e,t,null,h.getLevel().getGameTime());
         h.assertTrue(!update.changed()&&update.phase()==net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantCombatPhase.DECISIVE,"Berserker entered a health/time phase");
      }
      h.assertTrue(HeraclesAbilityService.npcCombat(e),"low mana fell back to generic duel AI");
      h.assertTrue(t.getHealth()<1000,"low mana prevented basic melee");
      t.setPos(t.position().add(0,0,8));
      h.assertTrue(HeraclesAbilityService.npcCombat(e)&&e.isSprinting(),"cooldown/mana gap did not pursue");h.succeed();
   }

   @GameTest(template="ancient_temple",timeoutTicks=20,batch="heracles_input_buffer")
   public static void recoveryBuffersOnePlayerInputAndRevalidatesItBeforeStarting(GameTestHelper h) {
      var e=owner(h);var player=h.makeMockServerPlayerInLevel();player.teleportTo(e.getX()+4,e.getY(),e.getZ());player.setYRot(0);player.setXRot(0);
      h.assertTrue(ServantCardTransformManager.transform(player,"heracles"),"player transform failed");
      var t=target(h,player.position().add(0,0,2));
      h.assertTrue(HeraclesAbilityService.start(player,6),"stomp start failed");
      h.assertTrue(!ServantCardTransformManager.triggerAction(player,3),"windup unexpectedly allowed a second action");
      ticks(player,1);
      h.assertTrue(ServantCardTransformManager.triggerAction(player,3),"recovery input was discarded");
      h.assertTrue(!player.getPersistentData().contains("HeraclesCooldown:3"),"queued input paid before execution");
      ticks(player,4);
      h.assertTrue(player.getPersistentData().contains("HeraclesCooldown:3"),"buffered action did not start at recovery end");
      ticks(player,2);h.assertTrue(t.getHealth()<976,"buffered uppercut did not hit on its second tick");
      HeraclesAbilityService.cancel(player);h.succeed();
   }
}
