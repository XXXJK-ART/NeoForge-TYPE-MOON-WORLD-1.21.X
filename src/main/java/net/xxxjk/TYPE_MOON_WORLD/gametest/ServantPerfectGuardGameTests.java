package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreat;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreatService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTacticalController;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmDamageClassifier;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.BuiltinServantEntityFactory;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

@GameTestHolder("typemoonworld_guard")
@PrefixGameTestTemplate(false)
public final class ServantPerfectGuardGameTests {
   private static final String[] RESET_SERVANTS = {
      "oda_nobunaga", "cu_chulainn", "sasaki_kojiro", "heracles", "diarmuid_ua_duibhne"
   };

   private ServantPerfectGuardGameTests() { }

   private static ServantEntity defender(GameTestHelper h, String id) {
      var defender = BuiltinServantEntityFactory.create(h.getLevel(), ResourceLocation.fromNamespaceAndPath("typemoonworld", id));
      defender.readAdditionalSaveData(defender.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
      defender.setCurrentMp(defender.getDefinition().parameters().manaPool());
      defender.setHealth(defender.getMaxHealth());
      defender.setPos(h.absolutePos(new BlockPos(4, 3, 4)).getCenter());
      defender.setNoAi(true);
      defender.setNoGravity(true);
      h.getLevel().addFreshEntity(defender);
      ServantCombatDefenseService.initialize(defender);
      defender.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES, 0);
      defender.getPersistentData().putLong(ServantCombatDefenseService.AUTO_RESTORE, Long.MAX_VALUE);
      chooseRoll(defender, 0.90, 1, 0, 0.05);
      return defender;
   }

   private static LivingEntity attacker(GameTestHelper h) {
      var attacker = h.spawn(EntityType.HUSK, new BlockPos(4, 3, 6));
      attacker.setNoAi(true);
      attacker.setNoGravity(true);
      return attacker;
   }

   private static void assertPerfect(GameTestHelper h, ServantEntity defender, DamageSource source, float damage) {
      double stamina = defender.getPersistentData().getDouble(ServantCombatDefenseService.TAG_STAMINA);
      double mana = defender.getCurrentMp();
      var event = new LivingIncomingDamageEvent(defender, new DamageContainer(source, damage));
      resolveHit(defender, event);
      h.assertTrue(event.isCanceled(), defender.getServantId() + " failed to cancel a parried hit");
      h.assertTrue(defender.getPersistentData().getInt("TypeMoonDefenseParryCount") == 1,
         defender.getServantId() + " dodged or blocked instead of perfect guarding");
      h.assertTrue(ServantCombatDefenseService.RESULT_PARRY.equals(
         defender.getPersistentData().getString(ServantCombatDefenseService.TAG_LAST_RESULT)), "wrong defense feedback");
      h.assertTrue(defender.getPersistentData().getDouble(ServantCombatDefenseService.TAG_STAMINA) == stamina,
         "perfect guard spent stamina");
      h.assertTrue(defender.getCurrentMp() == mana, "perfect guard spent dodge MP");
   }

   /** Component fixtures supply damage already reduced by base defense; real-bus tests also cover armor. */
   private static void resolveHit(ServantEntity defender, LivingIncomingDamageEvent event) {
      ServantCombatSystem.handleIncomingDamage(defender, event);
      if (!event.isCanceled()) {
         ServantCombatDefenseService.resolveDefense(new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(defender, event.getContainer()));
         if (event.getAmount() <= 0) event.setCanceled(true);
      }
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void allFiveResetServantsParryNativeMartialSkillsWithoutTelegraphs(GameTestHelper h) {
      LivingEntity attacker = attacker(h);
      for (String id : RESET_SERVANTS) {
         var defender = defender(h, id);
         var source = new CombatDamageSource(attacker.damageSources().mobAttack(attacker),
            AttackOrigin.skill("diarmuid_ua_duibhne", "advancing_thrust"));
         assertPerfect(h, defender, source, 80.0F);
         defender.discard();
      }
      attacker.discard();
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void allFiveResetServantsParryLightProjectiles(GameTestHelper h) {
      LivingEntity attacker = attacker(h);
      var arrow = EntityType.ARROW.create(h.getLevel());
      arrow.setOwner(attacker);
      for (String id : RESET_SERVANTS) {
         var defender = defender(h, id);
         assertPerfect(h, defender, attacker.damageSources().arrow(arrow, attacker), 30.0F);
         defender.discard();
      }
      attacker.discard();
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void allFiveResetServantsParryLightMagicBoltsThatOnlyBypassArmor(GameTestHelper h) {
      LivingEntity attacker = attacker(h);
      var projectile = EntityType.SNOWBALL.create(h.getLevel()); projectile.setOwner(attacker);
      var source = attacker.damageSources().indirectMagic(projectile, attacker);
      h.assertTrue(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR), "fixture must bypass armor");
      for (String id : RESET_SERVANTS) {
         var defender = defender(h, id);
         assertPerfect(h, defender, source, 30.0F);
         defender.discard();
      }
      attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void allFiveResetServantsParryHitscanDamageThroughTheRealEventBus(GameTestHelper h) {
      LivingEntity attacker = attacker(h);
      attacker.setPos(h.absolutePos(new BlockPos(4, 3, 14)).getCenter());
      for (String id : RESET_SERVANTS) {
         var defender = defender(h, id);
         var source = new net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaDamageSource(
            attacker.damageSources().mobAttack(attacker), attacker, attacker,
            net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaDamageSource.Ability.WEAPON);
         float health = defender.getHealth();
         defender.hurt(source, 30.0F);
         h.assertTrue(defender.getPersistentData().getInt("TypeMoonDefenseParryCount") == 1,
            id + " never reached perfect guard on the real damage event bus");
         h.assertTrue(defender.getHealth() == health, id + " lost health on a perfect guard");
         defender.discard();
      }
      attacker.discard();
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void perfectGuardWinsOverOrdinaryDodgeAndOnlyOneHitIsFree(GameTestHelper h) {
      var defender = defender(h, "sasaki_kojiro");
      LivingEntity attacker = attacker(h);
      h.assertTrue(ServantCombatDefenseService.setGuarding(defender, true), "explicit guard rejected");
      assertPerfect(h, defender, attacker.damageSources().mobAttack(attacker), 30);
      var second = new LivingIncomingDamageEvent(defender, new DamageContainer(attacker.damageSources().mobAttack(attacker), 30));
      resolveHit(defender, second);
      h.assertTrue(!second.isCanceled() && Math.abs(second.getAmount() - 6) < 0.001,
         "second hit dodged or received another free parry");
      h.assertTrue(defender.getPersistentData().getInt("TypeMoonDefenseParryCount") == 1, "one guard parried multiple hits");
      ServantCombatDefenseService.setGuarding(defender, false);
      chooseRoll(defender, 0.90, 1, 0, 0.05);
      h.assertTrue(!ServantCombatDefenseService.prepareNpcGuard(defender, attacker.damageSources().mobAttack(attacker), 30),
         "ordinary hit after parry bypassed the release cooldown");
      defender.discard(); attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void countedSkillEvasionKeepsItsPriorityAndDoesNotTriggerGuardCounter(GameTestHelper h) {
      var defender = defender(h, "diarmuid_ua_duibhne");
      LivingEntity attacker = attacker(h);
      defender.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES, 1);
      h.assertTrue(ServantCombatDefenseService.setGuarding(defender, true), "guard rejected");
      var event = new LivingIncomingDamageEvent(defender, new DamageContainer(attacker.damageSources().mobAttack(attacker), 30));
      resolveHit(defender, event);
      h.assertTrue(event.isCanceled() && defender.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES) == 0,
         "guard displaced counted skill evasion");
      h.assertTrue(defender.getPersistentData().getInt("TypeMoonDefenseParryCount") == 0, "skill evasion also triggered perfect guard");
      defender.discard(); attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void unusedGuardStartsReleaseCooldown(GameTestHelper h) {
      var defender = defender(h, "diarmuid_ua_duibhne");
      LivingEntity attacker = attacker(h);
      h.assertTrue(ServantCombatDefenseService.setGuarding(defender, true), "first guard rejected");
      ServantCombatDefenseService.setGuarding(defender, false);
      h.assertTrue(!ServantCombatDefenseService.setGuarding(defender, true), "unused guard could be spammed to refresh the parry window");
      defender.discard(); attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void perfectGuardCanRestartImmediatelyAfterRelease(GameTestHelper h) {
      var defender = defender(h, "heracles");
      LivingEntity attacker = attacker(h);
      var source = attacker.damageSources().mobAttack(attacker);
      assertPerfect(h, defender, source, 30);
      ServantCombatDefenseService.setGuarding(defender, false);
       h.assertTrue(ServantCombatDefenseService.setGuarding(defender, true), "perfect guard inherited a release cooldown");
       h.assertTrue(ServantCombatDefenseService.applyGuardHit(defender, source, 30) == 0, "fresh guard failed to parry");
       h.assertTrue(defender.getPersistentData().getInt("TypeMoonDefenseParryCount") == 2, "second parry not recorded");
      defender.discard(); attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void guardCleanupDoesNotEraseAnUnrelatedDodgePose(GameTestHelper h) {
      var defender = defender(h, "cu_chulainn");
      defender.setCombatPose(1, 6);
      ServantCombatDefenseService.tick(defender);
      h.assertTrue(defender.getCombatPose() == 1, "idle guard tick erased dodge pose");
      ServantCombatDefenseService.setGuarding(defender, false);
      h.assertTrue(defender.getCombatPose() == 1, "guard release erased dodge pose");
      defender.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void reactiveGuardHonorsControlBreakAndUnblockableAttacks(GameTestHelper h) {
      var defender = defender(h, "oda_nobunaga");
      LivingEntity attacker = attacker(h);
      var source = attacker.damageSources().mobAttack(attacker);
      long now = h.getLevel().getGameTime();
      defender.getPersistentData().putLong(ServantCombatSystem.TAG_STUN_UNTIL, now + 20);
      h.assertTrue(!ServantCombatDefenseService.prepareNpcGuard(defender, source, 30), "stunned NPC parried");
      defender.getPersistentData().remove(ServantCombatSystem.TAG_STUN_UNTIL);
      defender.getPersistentData().putLong(ServantCombatDefenseService.TAG_GUARD_BROKEN, Long.MAX_VALUE);
      h.assertTrue(!ServantCombatDefenseService.prepareNpcGuard(defender, source, 30), "broken guard parried");
      defender.getPersistentData().remove(ServantCombatDefenseService.TAG_GUARD_BROKEN);
      var tsubame = new CombatDamageSource(source, AttackOrigin.skill("sasaki_kojiro", "tsubame_gaeshi"));
      h.assertTrue(!ServantCombatDefenseService.prepareNpcGuard(defender, tsubame, 30), "unblockable Tsubame parried");
      h.assertTrue(!ServantCombatDefenseService.prepareNpcGuard(defender,
         NoblePhantasmDamageClassifier.withNoblePhantasm(source, "cu_gae_bolg"), 30), "NP selected a reactive parry");
      var arrow = EntityType.ARROW.create(h.getLevel()); arrow.setOwner(attacker);
      h.assertTrue(!ServantCombatDefenseService.prepareNpcGuard(defender, attacker.damageSources().arrow(arrow, attacker), 100),
         "heavy projectile selected a light-projectile parry");
      defender.discard(); attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 35)
   public static void telegraphedGuardSurvivesTacticalArbitrationThenExpires(GameTestHelper h) {
      var defender = defender(h, "cu_chulainn");
      LivingEntity attacker = attacker(h);
      defender.setTarget(attacker);
      long now = h.getLevel().getGameTime();
      CombatThreatService.publish(h.getLevel(), new CombatThreat(ResourceLocation.fromNamespaceAndPath("typemoonworld", "test/martial"),
         attacker.getUUID(), defender.getUUID(), attacker.getEyePosition(), defender.position().subtract(attacker.position()),
         CombatThreat.Shape.SPHERE, 4, 4, 2, now, now + 4, now + 5, true, true, true));
      ServantCombatDefenseService.tick(defender);
      h.assertTrue(ServantCombatDefenseService.isGuarding(defender), "telegraph did not arm guard");
      ServantTacticalController.tick(defender);
      h.assertTrue(ServantCombatDefenseService.isGuarding(defender), "tactical AI replaced guard with dodge");
      h.assertTrue(defender.getPersistentData().getLong(ServantCombatDefenseService.TAG_DODGE_COOLDOWN) <= now,
         "tactical AI spent a dodge during guard");
      CombatThreatService.clearForSource(h.getLevel(), attacker.getUUID());
      h.runAfterDelay(16, () -> {
         ServantCombatDefenseService.tick(defender);
         h.assertTrue(!ServantCombatDefenseService.isGuarding(defender), "guard remained active without an incoming threat");
         defender.discard(); attacker.discard(); h.succeed();
      });
   }

   private static void chooseRoll(ServantEntity defender, double dodgeLower, double dodgeUpper,
                                   double guardLower, double guardUpper) {
      var random = net.minecraft.util.RandomSource.create();
      for (long seed = 0; seed < 100000; seed++) {
         random.setSeed(seed);
         double dodgeRoll = random.nextDouble(), guardRoll = random.nextDouble();
         if (dodgeRoll >= dodgeLower && dodgeRoll < dodgeUpper && guardRoll >= guardLower && guardRoll < guardUpper) {
            defender.getRandom().setSeed(seed); return;
         }
      }
      throw new AssertionError("no deterministic reaction seed");
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void allFiveChooseGuardDodgeOrAnOffensiveOpening(GameTestHelper h) {
      var attacker = attacker(h);
      var source = attacker.damageSources().mobAttack(attacker);
      for (String id : RESET_SERVANTS) {
         for (int choice = 0; choice < 3; choice++) {
            var defender = defender(h, id);
            var params = defender.getDefinition().parameters();
            var chances = net.xxxjk.TYPE_MOON_WORLD.servant.combat.DefenseRules.npcReactionChances(
               net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatFormulas.defenseStep(params),
               net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatFormulas.agilityStep(params),
               30, defender.getMaxHealth(), 1, true, true);
            if (choice == 0) chooseRoll(defender, chances.dodge(), 1, 0, chances.guard());
            else if (choice == 1) chooseRoll(defender, 0, chances.dodge(), 0, chances.guard());
            else chooseRoll(defender, chances.dodge(), 1, chances.guard(), 1);
            double stamina = defender.getPersistentData().getDouble(ServantCombatDefenseService.TAG_STAMINA);
            double mana = defender.getCurrentMp();
            var event = new LivingIncomingDamageEvent(defender, new DamageContainer(source, 30));
            resolveHit(defender, event);
            if (choice == 0) {
               h.assertTrue(event.isCanceled() && defender.getPersistentData().getInt("TypeMoonDefenseParryCount") == 1, id + " never chose guard");
            } else if (choice == 1) {
               h.assertTrue(event.isCanceled() && defender.getCurrentMp() < mana, id + " never chose a paid dodge");
               h.assertTrue(defender.getPersistentData().getInt("TypeMoonDefenseGuardCount") == 0, id + " also guarded during dodge");
               h.assertTrue(defender.getCombatPose() == 1, id + " did not show dodge pose");
            } else {
               h.assertTrue(!event.isCanceled() && event.getAmount() == 15, id + " lost passive defense while attacking");
               h.assertTrue(!ServantCombatDefenseService.isGuarding(defender), id + " guarded on the offensive branch");
               double passiveCost = net.xxxjk.TYPE_MOON_WORLD.servant.combat.DefenseRules.guardCost(
                  30.0,
                  net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantDefenseTraits.resolve(defender).guardBonus(), false, false);
               h.assertTrue(defender.getCurrentMp() == mana && Math.abs(defender.getPersistentData().getDouble(
                  ServantCombatDefenseService.TAG_STAMINA) - Math.max(0, stamina - passiveCost)) < 0.001,
                  id + " passive defense did not charge exactly once");
               h.assertTrue(!ServantCombatSystem.tickBeforeAi(defender), id + " passive defense suppressed attacks");
            }
            defender.discard();
         }
      }
      attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void strongHitsShiftTheSameRollFromGuardToDodge(GameTestHelper h) {
      var attacker = attacker(h);
      var source = attacker.damageSources().mobAttack(attacker);
      for (String id : RESET_SERVANTS) {
         var light = defender(h, id); var heavy = defender(h, id);
         // Isolate absolute attack severity from a freshly spawned entity's small health pool.
         light.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
         heavy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
         chooseRoll(light, 0.48, 0.49, 0, 0.05); chooseRoll(heavy, 0.48, 0.49, 0, 0.05);
         var lightReaction = ServantCombatDefenseService.reactToIncomingAttack(light, source, 30, true, true);
         var heavyReaction = ServantCombatDefenseService.reactToIncomingAttack(heavy, source, 200, true, true);
         h.assertTrue(lightReaction == net.xxxjk.TYPE_MOON_WORLD.servant.combat.DefenseRules.NpcReaction.GUARD,
            id + " light hit did not guard: " + lightReaction);
         h.assertTrue(heavyReaction == net.xxxjk.TYPE_MOON_WORLD.servant.combat.DefenseRules.NpcReaction.DODGE, id + " heavy hit did not favor dodge");
         light.discard(); heavy.discard();
      }
      attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void failedDodgeFallsBackToOneProbabilisticGuardForAllFive(GameTestHelper h) {
      var attacker = attacker(h);
      var source = attacker.damageSources().mobAttack(attacker);
      for (String id : RESET_SERVANTS) {
         for (int unavailable = 0; unavailable < 2; unavailable++) {
            for (boolean guardSucceeds : new boolean[]{true, false}) {
               var defender = defender(h, id);
               if (unavailable == 0) defender.getPersistentData().putLong(ServantCombatDefenseService.TAG_DODGE_COOLDOWN,
                  h.getLevel().getGameTime() + 100);
               else defender.setCurrentMp(0);
               chooseRoll(defender, 0, 0.05, guardSucceeds ? 0 : 0.90, guardSucceeds ? 0.05 : 1);
               double mana = defender.getCurrentMp();
               var event = new LivingIncomingDamageEvent(defender, new DamageContainer(source, 30));
               resolveHit(defender, event);
               h.assertTrue(defender.getCurrentMp() == mana, id + " spent MP on an unavailable dodge");
               h.assertTrue(event.isCanceled() == guardSucceeds, id + " failed probabilistic guard fallback");
               h.assertTrue(ServantCombatDefenseService.isGuarding(defender) == guardSucceeds, id + " wrong fallback pose");
               if (!guardSucceeds) h.assertTrue(event.getAmount() == 15, id + " fallback lost passive defense");
               defender.discard();
            }
         }
      }
      attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void passiveDefenseWorksUntilStaminaIsExhaustedAndHonorsUnblockableHits(GameTestHelper h) {
      var attacker = attacker(h);
      var source = attacker.damageSources().mobAttack(attacker);
      for (String id : RESET_SERVANTS) {
         var defender = defender(h, id);
         defender.getPersistentData().putDouble(ServantCombatDefenseService.TAG_STAMINA, 1);
         h.assertTrue(ServantCombatDefenseService.applyGuardHit(defender, source, 30) == 15, id + " lost last-stamina reduction");
         h.assertTrue(ServantCombatDefenseService.applyGuardHit(defender, source, 30) == null, id + " guarded with no stamina");
         defender.getPersistentData().putDouble(ServantCombatDefenseService.TAG_STAMINA, 100);
         defender.getPersistentData().putLong(ServantCombatDefenseService.TAG_GUARD_BROKEN, Long.MAX_VALUE);
         h.assertTrue(ServantCombatDefenseService.applyGuardHit(defender, source, 30) == null, id + " guarded while broken");
         defender.getPersistentData().remove(ServantCombatDefenseService.TAG_GUARD_BROKEN);
         var tsubame = new CombatDamageSource(source, AttackOrigin.skill("sasaki_kojiro", "tsubame_gaeshi"));
         h.assertTrue(ServantCombatDefenseService.applyGuardHit(defender, tsubame, 30) == null, id + " passively blocked Tsubame");
         h.assertTrue(!ServantCombatDefenseService.isGuarding(defender) && defender.getCombatPose() != 2,
            id + " passive defense entered an active guard pose");
         defender.discard();
      }
      attacker.discard(); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 45)
   public static void sustainedTelegraphCannotRenewGuardOrRerollTheDecision(GameTestHelper h) {
      var defender = defender(h, "cu_chulainn"); var attacker = attacker(h);
      long now = h.getLevel().getGameTime();
      var threat = new CombatThreat(ResourceLocation.fromNamespaceAndPath("typemoonworld", "test/sustained"),
         attacker.getUUID(), defender.getUUID(), attacker.getEyePosition(), defender.position().subtract(attacker.position()),
         CombatThreat.Shape.SPHERE, 4, 4, 1, now, now + 4, now + 35, true, true, true);
      CombatThreatService.publish(h.getLevel(), threat);
      ServantCombatDefenseService.tick(defender);
      h.assertTrue(ServantCombatDefenseService.isGuarding(defender), "initial guard was not chosen");
      h.runAfterDelay(11, () -> {
         ServantCombatDefenseService.tick(defender);
         h.assertTrue(!ServantCombatDefenseService.isGuarding(defender), "sustained threat renewed guard");
         h.assertTrue(!ServantCombatSystem.tickBeforeAi(defender), "expired guard still suppressed attacks");
      });
      h.runAfterDelay(20, () -> {
         chooseRoll(defender, 0.90, 1, 0, 0.05);
         ServantCombatDefenseService.tick(defender);
         h.assertTrue(!ServantCombatDefenseService.isGuarding(defender), "same threat rerolled after reaction interval");
         CombatThreatService.clearForSource(h.getLevel(), attacker.getUUID());
         defender.discard(); attacker.discard(); h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 35)
   public static void offensiveChoiceSurvivesRepeatedSensorAndDamageChecks(GameTestHelper h) {
      var defender = defender(h, "sasaki_kojiro"); var attacker = attacker(h);
      long now = h.getLevel().getGameTime();
      var threat = new CombatThreat(ResourceLocation.fromNamespaceAndPath("typemoonworld", "test/continue_attacking"),
         attacker.getUUID(), defender.getUUID(), attacker.getEyePosition(), defender.position().subtract(attacker.position()),
         CombatThreat.Shape.SPHERE, 4, 4, 1, now, now + 4, now + 30, true, true, true);
      CombatThreatService.publish(h.getLevel(), threat);
      chooseRoll(defender, 0.90, 1, 0.90, 1); ServantCombatDefenseService.tick(defender);
      h.assertTrue(!ServantCombatDefenseService.isGuarding(defender), "offensive choice armed guard");
      h.runAfterDelay(20, () -> {
         chooseRoll(defender, 0.90, 1, 0, 0.05);
         ServantCombatDefenseService.tick(defender);
         var event = new LivingIncomingDamageEvent(defender, new DamageContainer(attacker.damageSources().mobAttack(attacker), 30));
         resolveHit(defender, event);
         h.assertTrue(!event.isCanceled() && event.getAmount() == 15, "offensive decision lost passive defense or rerolled");
         CombatThreatService.clearForSource(h.getLevel(), attacker.getUUID());
         defender.discard(); attacker.discard(); h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void projectileSensorsKeepTheOffensiveDecisionUntilImpact(GameTestHelper h) {
      var defender = defender(h, "diarmuid_ua_duibhne"); var attacker = attacker(h);
      var arrow = EntityType.ARROW.create(h.getLevel()); arrow.setOwner(attacker);
      chooseRoll(defender, 0.90, 1, 0.90, 1);
      h.assertTrue(!ServantCombatDefenseService.reactToProjectile(defender, arrow, 30, java.util.Set.of()), "offensive projectile choice defended");
      chooseRoll(defender, 0.90, 1, 0, 0.05);
      var event = new LivingIncomingDamageEvent(defender, new DamageContainer(attacker.damageSources().arrow(arrow, attacker), 30));
      resolveHit(defender, event);
      h.assertTrue(!event.isCanceled() && event.getAmount() == 15, "projectile lost passive defense or rerolled sensor decision");
      defender.discard(); attacker.discard(); h.succeed();
   }
}
