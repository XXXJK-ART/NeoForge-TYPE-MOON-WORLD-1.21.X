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
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantDefenseTraits;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.DiarmuidUaDuibhneEntity;

@GameTestHolder("typemoonworld_diarmuid")
@PrefixGameTestTemplate(false)
public final class DiarmuidGameTests {
   private static final String ARENA = "ancient_temple";
   private DiarmuidGameTests() { }

   private static Vec3 origin(GameTestHelper h) {
      for (int x = 2; x <= 11; x++) for (int z = 2; z <= 13; z++) for (int y = 2; y <= 6; y++) h.setBlock(new BlockPos(x,y,z), Blocks.AIR);
      return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,2,4)));
   }

   private static DiarmuidUaDuibhneEntity npc(GameTestHelper h) {
      var owner = ModEntities.DIARMUID_UA_DUIBHNE.get().create(h.getLevel());
      owner.setPos(origin(h)); owner.setYRot(0); owner.setXRot(0);
      owner.setNoAi(true); owner.setNoGravity(true); owner.setCurrentMp(200);
      owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GAE_DEARG.get()));
      owner.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.GAE_BUIDHE.get()));
      h.getLevel().addFreshEntity(owner);
      return owner;
   }

   private static ServerPlayer card(GameTestHelper h) {
      var owner = h.makeMockServerPlayerInLevel();
      Vec3 pos = origin(h); owner.teleportTo(pos.x, pos.y, pos.z); owner.setYRot(0); owner.setXRot(0);
      h.assertTrue(ServantCardTransformManager.transform(owner, "diarmuid_ua_duibhne"), "card transform failed");
      owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(200);
      return owner;
   }

   @GameTest(template = ARENA, timeoutTicks = 40)
   public static void globalNoCooldownBypassesSkillTimerAndKeepsManaCost(GameTestHelper h) {
      var owner=card(h);var data=owner.getPersistentData();var vars=owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      data.putLong("DiarmuidAction:advancing_thrust",owner.level().getGameTime()+1000);
      data.putBoolean("TypeMoonNoCooldown",true);
      double mana=vars.servantCardState().mana();
      check(h,DiarmuidAbilityService.start(owner,DiarmuidAction.ADVANCING_THRUST),"global no-cooldown did not bypass skill timer");
      check(h,Math.abs(vars.servantCardState().mana()-(mana-DiarmuidAction.ADVANCING_THRUST.mana()))<.02,"no-cooldown removed mana cost");
      check(h,!data.contains("DiarmuidAction:advancing_thrust"),"custom Diarmuid timer remained");
      check(h,ServantCardTransformManager.getSkillCooldown(vars,0)==0,"card HUD retained skill timer");h.succeed();
   }

   private static LivingEntity target(GameTestHelper h, LivingEntity owner, double distance) {
      var target = EntityType.HUSK.create(h.getLevel());
      target.setPos(owner.position().add(0,0,distance)); target.setNoAi(true); target.setNoGravity(true);
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
      target.getAttribute(Attributes.ARMOR).setBaseValue(0);
      target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
      target.setHealth(200); h.getLevel().addFreshEntity(target);
      return target;
   }

   private static void check(GameTestHelper h, boolean condition, String message) { h.assertTrue(condition, message); }

   @GameTest(template = ARENA, timeoutTicks = 55)
   public static void stanceStartsDisabledAndTogglesWithoutManaOrWeaponRepair(GameTestHelper h) {
      var owner=card(h);
      check(h,!DiarmuidAbilityService.isDoubleSpearStance(owner),"transformation enabled stance automatically");
      check(h,ServantCardTransformManager.triggerAction(owner,7),"stance activation rejected");
      check(h,DiarmuidAbilityService.isDoubleSpearStance(owner),"stance did not activate");
      check(h,owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().mana()==200,"stance charged mana");
      h.runAfterDelay(22,()->{
         owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
         check(h,ServantCardTransformManager.triggerAction(owner,7),"exit required missing weapon");
         check(h,!DiarmuidAbilityService.isDoubleSpearStance(owner) && owner.getOffhandItem().isEmpty(),"exit restored spear or kept stance");
         ServantCardTransformManager.release(owner,false); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 60)
   public static void singleHandDanceHasThreeHitsAndOnlyDeflectsWeakProjectilesInWindow(GameTestHelper h) {
      var owner=npc(h); var target=target(h,owner,2.4);
      check(h,DiarmuidAbilityService.start(owner,DiarmuidAction.DOUBLE_SPEAR_STANCE),"stance rejected");
      h.runAfterDelay(14,()->{
         check(h,DiarmuidAbilityService.start(owner,DiarmuidAction.RED_DANCE),"single-hand dance rejected");
         var weak=EntityType.ARROW.create(h.getLevel()); weak.setBaseDamage(2); weak.setDeltaMovement(0,0,-1);
         check(h,DiarmuidAbilityService.deflectLowDamageProjectile(owner,weak),"weak arrow not deflected");
         check(h,weak.getDeltaMovement().z>0 && weak.getOwner()==owner,"deflection did not reverse flight or attribution");
         var strong=EntityType.ARROW.create(h.getLevel()); strong.setBaseDamage(20); strong.setDeltaMovement(0,0,-1);
         check(h,!DiarmuidAbilityService.deflectLowDamageProjectile(owner,strong),"strong arrow deflected");
         var snowball=EntityType.SNOWBALL.create(h.getLevel()); snowball.setDeltaMovement(0,0,-1);
         check(h,DiarmuidAbilityService.deflectLowDamageProjectile(owner,snowball),"snowball not deflected");
      });
      h.runAfterDelay(27,()->{
         check(h,Math.abs(target.getHealth()-182)<0.01,"dance damage not three six-point hits: "+target.getHealth());
         check(h,DiarmuidCombatHelper.redDurability(owner)==197 && DiarmuidCombatHelper.yellowDurability(owner)==200,"dance used wrong spear or duplicated effects");
         var snowball=EntityType.SNOWBALL.create(h.getLevel()); snowball.setDeltaMovement(0,0,-1);
         check(h,!DiarmuidAbilityService.deflectLowDamageProjectile(owner,snowball),"deflection outlasted dance");
         owner.discard(); target.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 55)
   public static void heldSkillEightStopsOnRelease(GameTestHelper h) {
      var owner=card(h); var target=target(h,owner,2.4);
      check(h,ServantCardTransformManager.triggerAction(owner,7),"stance rejected");
      h.runAfterDelay(14,()->{
         ServantCardTransformManager.handleHoldAction(owner,8,true);
         check(h,DiarmuidAbilityService.active(owner),"held skill eight did not start");
         check(h,owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().mana()==176,"held skill charged the wrong MP");
         h.runAfterDelay(14,()->{
            double health=target.getHealth();
            check(h,health<200,"held skill did not strike continuously");
            ServantCardTransformManager.handleHoldAction(owner,8,false);
            check(h,!DiarmuidAbilityService.active(owner),"release did not stop the dance");
            h.runAfterDelay(10,()->{
               check(h,Math.abs(target.getHealth()-health)<0.01,"dance kept hitting after release");
               ServantCardTransformManager.release(owner,false); target.discard(); h.succeed();
            });
         });
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 30)
   public static void distanceUsesEyeToNearestBoxAndWalls(GameTestHelper h) {
      var owner = npc(h); var target = target(h, owner, 3.30);
      check(h, DiarmuidAbilityService.canHit(owner,target,3), "yellow rejected nearest-box boundary");
      target.setPos(owner.position().add(0,0,3.32));
      check(h, !DiarmuidAbilityService.canHit(owner,target,3), "yellow exceeded 3 blocks");
      check(h, DiarmuidAbilityService.canHit(owner,target,5), "red failed beyond yellow reach");
      h.setBlock(new BlockPos(5,3,5), Blocks.STONE);
      check(h, !DiarmuidAbilityService.canHit(owner,target,5), "spear struck through block");
      owner.discard(); target.discard(); h.succeed();
   }

   @GameTest(template = ARENA, timeoutTicks = 70)
   public static void npcFinisherDeals92WithExactlyThreeEffectsPerSpear(GameTestHelper h) {
      var owner = npc(h); var target = target(h, owner, 2.4);
      check(h, DiarmuidAbilityService.start(owner,DiarmuidAction.ROSE_DANCE), "finisher rejected");
      check(h, owner.getCurrentMp() == 164, "NPC mana cost drifted");
      h.runAfterDelay(33, () -> {
         check(h, Math.abs(target.getHealth() - 108) < 0.01, "finisher damage != 92: " + target.getHealth());
         check(h, DiarmuidCombatHelper.stacks(target) == 3, "finisher yellow stacks != 3");
         check(h, DiarmuidCombatHelper.redDurability(owner) == 197 && DiarmuidCombatHelper.yellowDurability(owner) == 197, "effect wear duplicated");
         check(h, owner.getMainHandItem().getDamageValue() == 3 && owner.getOffhandItem().getDamageValue() == 3, "item wear duplicated");
         owner.discard(); target.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 65)
   public static void cardFinisherUsesOrdinarySkillCooldownAndSameDamage(GameTestHelper h) {
      var owner = card(h); var target = target(h, owner, 2.4);
      check(h, ServantCardTransformManager.triggerAction(owner,9), "card finisher rejected");
      var state = owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      check(h, state.mana() == 164, "card did not spend 36MP");
      check(h, state.npCooldown() == 0, "ordinary finisher acquired NP cooldown");
      check(h, ServantCardTransformManager.getSkillCooldown(owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES),9) == 400, "slot 9 cooldown != 400");
      h.runAfterDelay(33, () -> {
         check(h, Math.abs(target.getHealth()-108) < 0.01, "card finisher damage != 92: " + target.getHealth());
         check(h, DiarmuidCombatHelper.stacks(target) == 3, "card curse count drifted");
         ServantCardTransformManager.release(owner, false); target.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 65)
   public static void yellowSegmentsMissWhenLockedTargetLeavesThreeBlocks(GameTestHelper h) {
      var owner = npc(h); var target = target(h, owner, 2.4);
      check(h, DiarmuidAbilityService.start(owner,DiarmuidAction.ROSE_DANCE), "finisher rejected");
      h.runAfterDelay(9, () -> target.setPos(owner.position().add(0,0,4.8)));
      h.runAfterDelay(33, () -> {
         check(h, DiarmuidCombatHelper.stacks(target) == 0, "yellow hit outside 3 blocks");
         check(h, DiarmuidCombatHelper.redDurability(owner) == 197, "red did not retain longer reach");
         check(h, DiarmuidCombatHelper.yellowDurability(owner) == 200, "miss consumed yellow durability");
         owner.discard(); target.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 65)
   public static void firstMissNeverRetargetsLaterSegments(GameTestHelper h) {
      var owner = npc(h);
      check(h, DiarmuidAbilityService.start(owner,DiarmuidAction.ROSE_DANCE), "empty cast rejected");
      h.runAfterDelay(10, () -> target(h,owner,2.4));
      h.runAfterDelay(33, () -> {
         check(h, DiarmuidCombatHelper.redDurability(owner)==200 && DiarmuidCombatHelper.yellowDurability(owner)==200, "empty first segment retargeted");
         owner.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 45)
   public static void twoYellowHitsConsumeOnlyTwoDurability(GameTestHelper h) {
      var owner = npc(h); var target = target(h,owner,2.4);
      check(h, DiarmuidAbilityService.start(owner,DiarmuidAction.YELLOW_DOUBLE), "double rejected");
      h.runAfterDelay(10, () -> {
         check(h, Math.abs(target.getHealth()-180)<0.01, "two 10-point hits suppressed by i-frames");
         check(h, DiarmuidCombatHelper.stacks(target)==2 && owner.getOffhandItem().getDamageValue()==2, "yellow hit duplicated");
         owner.discard(); target.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 35)
   public static void independentHandCooldownsAllowSimultaneousAndOverlappingAttacks(GameTestHelper h) {
      var owner = npc(h);
      var target = target(h, owner, 2.4);
      check(h, DiarmuidAbilityService.start(owner,DiarmuidAction.DOUBLE_SPEAR_STANCE),"stance rejected");
      owner.getRandom().setSeed(42L);
      h.runAfterDelay(14,()->{
         owner.getRandom().setSeed(7L);
         check(h, DiarmuidAbilityService.basic(owner,InteractionHand.MAIN_HAND), "red basic rejected");
         check(h, DiarmuidAbilityService.basic(owner,InteractionHand.OFF_HAND), "same-tick yellow basic rejected");
         check(h, DiarmuidAbilityService.handActionActive(owner,InteractionHand.MAIN_HAND)
            && DiarmuidAbilityService.handActionActive(owner,InteractionHand.OFF_HAND),"two hands did not retain independent timelines");
         h.runAfterDelay(5, () -> {
            check(h, !DiarmuidAbilityService.basic(owner,InteractionHand.MAIN_HAND), "red cooldown ended early");
            check(h, !DiarmuidAbilityService.basic(owner,InteractionHand.OFF_HAND), "yellow cooldown ended early");
         });
         h.runAfterDelay(10, () -> {
            check(h, Math.abs(target.getHealth()-165)<0.01, "both same-tick spear hits did not land once each: "+target.getHealth());
            check(h, DiarmuidCombatHelper.redDurability(owner)==199 && DiarmuidCombatHelper.yellowDurability(owner)==199,"overlapping attacks damaged the wrong weapons");
            owner.discard(); target.discard(); h.succeed();
         });
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 45)
   public static void brokenSpearCancelsItsSegmentsWithoutRepairOrRefund(GameTestHelper h) {
      var owner = npc(h); var target = target(h,owner,2.4);
      owner.getPersistentData().putInt(DiarmuidCombatHelper.YELLOW_SPEAR_DURABILITY_TAG,1);
      owner.getOffhandItem().setDamageValue(199);
      check(h, DiarmuidAbilityService.start(owner,DiarmuidAction.YELLOW_DOUBLE), "last-use cast rejected");
      h.runAfterDelay(16, () -> {
         check(h, owner.getOffhandItem().isEmpty(), "broken spear remained: " + owner.getOffhandItem() + "/" + owner.getOffhandItem().getDamageValue() + "/" + owner.getOffhandItem().getMaxDamage());
         check(h, DiarmuidCombatHelper.yellowDurability(owner)==0, "broken durability reset");
         check(h, Math.abs(target.getHealth()-190)<0.01, "broken second segment dealt damage");
         check(h, DiarmuidCombatHelper.stacks(target)==0, "curse remained after break");
         check(h, !DiarmuidAbilityService.start(owner,DiarmuidAction.YELLOW_DOUBLE), "missing spear accepted");
         owner.discard(); target.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 45)
   public static void hardControlCancelsFutureHitsWithoutRefund(GameTestHelper h) {
      var owner = npc(h); var target = target(h,owner,2.4);
      check(h, DiarmuidAbilityService.start(owner,DiarmuidAction.ROSE_DANCE), "finisher rejected");
      check(h, owner.getCurrentMp()==164, "accepted cast did not charge 36MP");
      owner.getPersistentData().putLong("TypeMoonCombatStunUntil",h.getLevel().getGameTime()+12);
      h.runAfterDelay(34, () -> {
         check(h, target.getHealth()==200 && !DiarmuidAbilityService.active(owner), "hard control failed to cancel hits");
         check(h, owner.getCurrentMp() < 200 && DiarmuidAbilityService.cooldown(owner,DiarmuidAction.ROSE_DANCE)>h.getLevel().getGameTime(), "interruption refunded costs");
         owner.discard(); target.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 35)
   public static void noManaAndMissingWeaponsFailBeforePayment(GameTestHelper h) {
      var owner = npc(h); owner.setCurrentMp(5);
      check(h, !DiarmuidAbilityService.start(owner,DiarmuidAction.YELLOW_DOUBLE), "insufficient mana accepted");
      check(h, owner.getCurrentMp()==5 && DiarmuidAbilityService.cooldown(owner,DiarmuidAction.YELLOW_DOUBLE)==0, "failed cast paid costs");
      owner.setCurrentMp(200); owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
      check(h, !DiarmuidAbilityService.start(owner,DiarmuidAction.ROSE_DANCE), "missing spear accepted");
      check(h, owner.getCurrentMp()==200 && owner.getOffhandItem().isEmpty(), "missing spear restored or charged");
      owner.discard(); h.succeed();
   }

   @GameTest(template = ARENA, timeoutTicks = 40)
   public static void guardCounterRequiresActualGuardAndWaitsThreeTicks(GameTestHelper h) {
      var owner = npc(h); var target = target(h,owner,2.4);
      check(h, DiarmuidAbilityService.start(owner,DiarmuidAction.CROSS_GUARD), "guard rejected");
      h.runAfterDelay(4, () -> {
         DiarmuidAbilityService.tick(owner);
         check(h, ServantCombatDefenseService.setGuarding(owner,true), "guard resources unavailable");
         var reduced = ServantCombatDefenseService.applyGuardHit(owner,target.damageSources().mobAttack(target),10);
         check(h, reduced != null && reduced < 10, "shared guard transaction missing");
         check(h, target.getHealth()==200, "counter fired immediately");
      });
      h.runAfterDelay(9, () -> {
         check(h, Math.abs(target.getHealth()-174)<0.01, "counter not 26 damage after 3 ticks: " + target.getHealth());
         check(h, DiarmuidCombatHelper.redDurability(owner)==199, "counter effects duplicated");
         owner.discard(); target.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 470)
   public static void mindEyeHasTwoChargesAndRestoresWholeGroupAfter400Ticks(GameTestHelper h) {
      var owner = npc(h);
      check(h, ServantDefenseTraits.resolve(owner).autoDodges()==2, "Mind's Eye B != 2 charges");
      check(h, ServantCombatDefenseService.trySkillAutoDodge(owner), "first dodge failed");
      check(h, ServantCombatDefenseService.trySkillAutoDodge(owner), "second dodge failed");
      check(h, !ServantCombatDefenseService.trySkillAutoDodge(owner), "extra dodge granted");
      h.runAfterDelay(398, () -> check(h, !ServantCombatDefenseService.trySkillAutoDodge(owner), "restored before 400 ticks"));
      h.runAfterDelay(402, () -> {
         ServantCombatDefenseService.tick(owner);
         check(h, owner.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES)==2, "group did not restore");
         owner.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 30)
   public static void handSwapPreservesRedAndYellowDistanceLimits(GameTestHelper h) {
      var owner=npc(h); var target=target(h,owner,4.2);
      var red=owner.getMainHandItem(); owner.setItemInHand(InteractionHand.MAIN_HAND,owner.getOffhandItem());
      owner.setItemInHand(InteractionHand.OFF_HAND,red);
      check(h, DiarmuidAbilityService.basic(owner,InteractionHand.MAIN_HAND), "swapped yellow attack rejected");
      h.runAfterDelay(5, () -> {
         check(h,target.getHealth()==200 && DiarmuidCombatHelper.yellowDurability(owner)==200,"swapped yellow exceeded 3 blocks");
         check(h,DiarmuidAbilityService.basicSpear(owner,true),"red not located in offhand");
      });
      h.runAfterDelay(9, () -> {
         check(h,Math.abs(target.getHealth()-182)<0.01 && DiarmuidCombatHelper.redDurability(owner)==199,"swapped red did not hit at long range");
         owner.discard(); target.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 35)
   public static void areaStrikeSelectsNearestThreeLegalEnemies(GameTestHelper h) {
      var owner=npc(h); var a=target(h,owner,1.5); var b=target(h,owner,2.5); var c=target(h,owner,3.5); var d=target(h,owner,4.8);
      check(h,DiarmuidAbilityService.start(owner,DiarmuidAction.RED_SWEEP),"sweep rejected");
      h.runAfterDelay(10, () -> {
         check(h,Math.abs(a.getHealth()-178)<0.01 && Math.abs(b.getHealth()-178)<0.01 && Math.abs(c.getHealth()-178)<0.01,"sweep did not hit nearest three");
         check(h,d.getHealth()==200 && DiarmuidCombatHelper.redDurability(owner)==197,"sweep exceeded three enemies");
         for (var entity : java.util.List.of(owner,a,b,c,d)) entity.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 60)
   public static void releasingCardCancelsAllRemainingSegments(GameTestHelper h) {
      var owner=card(h); var target=target(h,owner,2.4);
      check(h,ServantCardTransformManager.triggerAction(owner,9),"finisher rejected");
      check(h,ServantCardTransformManager.release(owner,false),"release failed");
      h.runAfterDelay(34, () -> {
         check(h,target.getHealth()==200 && !DiarmuidAbilityService.active(owner),"released card retained delayed hits");
         target.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 60)
   public static void unloadingOwnerCancelsAllRemainingSegments(GameTestHelper h) {
      var owner=npc(h); var target=target(h,owner,2.4);
      check(h,DiarmuidAbilityService.start(owner,DiarmuidAction.ROSE_DANCE),"finisher rejected");
      owner.discard();
      h.runAfterDelay(34, () -> {
         check(h,target.getHealth()==200 && !DiarmuidAbilityService.active(owner),"unloaded owner retained delayed hits");
         target.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 45)
   public static void yellowVoiceTriggersOnHitAndDoesNotRepeatWithinCombo(GameTestHelper h) {
      var owner=npc(h); var target=target(h,owner,2.4); var stamp=new java.util.concurrent.atomic.AtomicLong();
      check(h,DiarmuidAbilityService.start(owner,DiarmuidAction.YELLOW_DOUBLE),"yellow double rejected");
      check(h,!owner.getPersistentData().contains(net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidVoiceService.YELLOW_VOICE_READY),"voice played during windup");
      h.runAfterDelay(4, () -> {
         stamp.set(owner.getPersistentData().getLong(net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidVoiceService.YELLOW_VOICE_READY));
         check(h,stamp.get()>h.getLevel().getGameTime(),"effective yellow hit did not play callout");
      });
      h.runAfterDelay(10, () -> {
         check(h,stamp.get()==owner.getPersistentData().getLong(net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidVoiceService.YELLOW_VOICE_READY),"second yellow hit repeated callout");
         owner.discard(); target.discard(); h.succeed();
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 90)
   public static void servantScaleLeapLandsIntoADualSpearSweep(GameTestHelper h) {
      var owner=npc(h); Vec3 start=owner.position();
      owner.setNoAi(false);
      owner.setNoGravity(false);
      for (int x=2;x<=11;x++) for (int z=2;z<=17;z++) h.setBlock(new BlockPos(x,1,z),Blocks.SMOOTH_STONE);
      owner.setOnGround(true);
      Vec3 velocity=DiarmuidAbilityService.leapVelocity(owner);
      check(h,velocity.y>=1.05 && velocity.horizontalDistance()>0.5,"servant big-jump impulse is missing");
      check(h,DiarmuidAbilityService.start(owner,DiarmuidAction.MANA_LEAP),"legal leap rejected");
      h.runAfterDelay(12, () -> check(h,owner.getY()-start.y>3.0,"servant-scale leap height="+(owner.getY()-start.y)+", velocity="+owner.getDeltaMovement()+", onGround="+owner.onGround()+", action="+DiarmuidAbilityService.currentAction(owner)));
      awaitLandingSweep(h,owner,0);
   }

   private static void awaitLandingSweep(GameTestHelper h, LivingEntity owner, int elapsed) {
      if (DiarmuidAbilityService.currentAction(owner)==DiarmuidAction.LANDING_SWEEP) {
         var target=target(h,owner,2.0);
         h.runAfterDelay(20,()->{
            check(h,Math.abs(target.getHealth()-178)<0.01,"landing sweep did not hit with both spears: "+target.getHealth());
            check(h,!DiarmuidAbilityService.protectFall(owner),"landing left fall protection active");
            check(h,DiarmuidAbilityService.currentAction(owner)==null,"landing sweep did not recover");
            if (owner instanceof ServerPlayer player) ServantCardTransformManager.release(player,false);
            else owner.discard();
            target.discard(); h.succeed();
         });
         return;
      }
      check(h,elapsed<65,"big jump never transitioned into its landing sweep");
      if(elapsed<65) h.runAfterDelay(1,()->awaitLandingSweep(h,owner,elapsed+1));
   }

   @GameTest(template = ARENA, timeoutTicks = 40)
   public static void cardLeapPreservesMomentumAndLeavesFlightToPlayerMovement(GameTestHelper h) {
      var owner=card(h);
      owner.setOnGround(true);
      owner.setDeltaMovement(0.3,0,0.1);
      Vec3 launch=DiarmuidAbilityService.leapVelocity(owner);
      check(h,ServantCardTransformManager.triggerAction(owner,6),"card leap rejected");
      h.runAfterDelay(5,()->{
         check(h,!owner.isNoGravity(),"leap disabled vanilla gravity");
         check(h,owner.getDeltaMovement().x>0.15,"leap erased lateral momentum");
         check(h,owner.getDeltaMovement().z>launch.z*0.5,"leap erased forward momentum");
         check(h,owner.getDeltaMovement().y>0.7,"takeoff impulse missing");
         Vec3 steered=new Vec3(-0.4,-0.2,0.12);
         owner.setDeltaMovement(steered);
         owner.setYRot(90);
         h.runAfterDelay(5,()->{
            check(h,owner.getDeltaMovement().distanceToSqr(steered)<0.0001,"skill overwrote player steering");
            check(h,DiarmuidAbilityService.currentAction(owner)==DiarmuidAction.MANA_LEAP,"sweep started before landing");
            owner.setOnGround(true);
            h.runAfterDelay(2,()->{
               check(h,DiarmuidAbilityService.currentAction(owner)==DiarmuidAction.LANDING_SWEEP,"actual landing did not start sweep");
               ServantCardTransformManager.release(owner,false); h.succeed();
            });
         });
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 25)
   public static void thrustImpulseNeverDragsPlayerOrErasesTheirSteering(GameTestHelper h) {
      var owner=card(h);
      owner.setDeltaMovement(0.25,0,0);
      check(h,ServantCardTransformManager.triggerAction(owner,0),"advancing thrust rejected");
      h.runAfterDelay(2,()->{
         check(h,owner.getDeltaMovement().x>0.15,"thrust erased sideways momentum");
         check(h,owner.getDeltaMovement().z>0.3,"thrust did not apply its impulse");
         owner.setDeltaMovement(-0.3,0,-0.2);
         owner.setYRot(90);
         h.runAfterDelay(4,()->{
            check(h,owner.getDeltaMovement().distanceToSqr(new Vec3(-0.3,0,-0.2))<0.0001,"thrust reapplied movement after steering");
            ServantCardTransformManager.release(owner,false); h.succeed();
         });
      });
   }

   @GameTest(template = ARENA, timeoutTicks = 30)
   public static void blockedLeapHasNoPaymentOrCooldown(GameTestHelper h) {
      var owner=npc(h);
      for (int x=3;x<=8;x++) for (int z=3;z<=12;z++) { h.setBlock(new BlockPos(x,1,z),Blocks.SMOOTH_STONE); h.setBlock(new BlockPos(x,4,z),Blocks.STONE); }
      owner.setOnGround(true);
      check(h,!DiarmuidAbilityService.start(owner,DiarmuidAction.MANA_LEAP),"low-ceiling leap accepted");
      check(h,owner.getCurrentMp()==200 && DiarmuidAbilityService.cooldown(owner,DiarmuidAction.MANA_LEAP)==0,"invalid leap consumed resources");
      owner.discard(); h.succeed();
   }

   @GameTest(template = ARENA, timeoutTicks = 35)
   public static void autoDodgeInGuardWindowDoesNotTriggerCounter(GameTestHelper h) {
      var owner=npc(h); var target=target(h,owner,2.4);
      check(h,DiarmuidAbilityService.start(owner,DiarmuidAction.CROSS_GUARD),"guard rejected");
      h.runAfterDelay(4, () -> {
         owner.hurt(target.damageSources().mobAttack(target),10);
      });
      h.runAfterDelay(15, () -> {
         check(h,target.getHealth()==200 && DiarmuidCombatHelper.redDurability(owner)==200,"automatic dodge triggered guard counter");
         owner.discard(); target.discard(); h.succeed();
      });
   }
}
