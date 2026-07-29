package net.xxxjk.TYPE_MOON_WORLD.servant.arash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ArashBowItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashAimHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardArashSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import org.junit.jupiter.api.Test;

class ArashRulesTest {
   @Test
   void ranksAndStoutProduceApprovedStats() {
      ServantParams params = ServantParams.of("A", false, "B", false, "B", true, "E", false, "D", false);
      assertEquals(500.0, params.maxHealth());
      assertEquals(600.0, params.maxHealth() + 100.0);
      assertEquals(20.0, params.attackDamage());
      assertEquals(0.48, params.movementSpeed());
      assertEquals(200.0, params.manaPool());
      assertEquals(4.0, params.critRatePercent());
      assertEquals(0.70F, ArashCombatRules.STOUT_DAMAGE_MULTIPLIER);
      assertEquals(70.0F, 100.0F * ArashCombatRules.STOUT_DAMAGE_MULTIPLIER);
      assertEquals(2.0, ArashCombatRules.DEFENSE_RECOVERY_MULTIPLIER);
      assertEquals(2.0, ArashCombatRules.POISE_RECOVERY_MULTIPLIER);
      assertEquals(32.0, ArashCombatRules.boostedDefenseRecovery(16.0));
      assertEquals(26.0, ArashCombatRules.boostedPoiseRecovery(13.0));
      assertEquals(0.0, ArashCombatRules.boostedDefenseRecovery(-1.0));
      assertEquals(0.0, ArashCombatRules.boostedPoiseRecovery(-1.0));
   }

   @Test
   void arrowCostsDamageAndCadenceAreExact() {
      assertEquals(500, ArashCombatRules.INITIAL_ARROW_COUNT);
      assertEquals(5000, ArashCombatRules.MAX_ARROW_COUNT);
      assertEquals(1000, ArashCombatRules.ARROW_CREATION_THRESHOLD);
      assertEquals(10, ArashCombatRules.ARROW_CREATION_AMOUNT);
      assertEquals(1.0, ArashCombatRules.ARROW_CREATION_MANA_COST);
      assertEquals(1, ArashCombatRules.NORMAL_ARROW_COST);
      assertEquals(1, ArashCombatRules.ENERGY_ARROW_COST);
      assertEquals(100, ArashCombatRules.ARROW_RAIN_COST);
      assertEquals(500, ArashCombatRules.CROUCH_ARROW_RAIN_COST);
      assertEquals(1, ArashCombatRules.STELLA_ARROW_COST);
      assertEquals(10.0F, ArashCombatRules.NORMAL_ARROW_DAMAGE);
      assertEquals(5, ArashCombatRules.NORMAL_ARROW_INTERVAL);
      assertEquals(100, ArashCombatRules.RAIN_ARROW_COUNT);
      assertEquals(500, ArashCombatRules.CROUCH_RAIN_ARROW_COUNT);
      assertEquals(6.0, ArashCombatRules.RAIN_SPREAD_RADIUS);
      assertEquals(30.0, ArashCombatRules.CROUCH_RAIN_SPREAD_RADIUS);
      assertEquals(36.0, ArashCombatRules.CROUCH_RAIN_ASSIST_RADIUS);
      assertEquals(13.2F, ArashCombatRules.RAIN_ARROW_DAMAGE);
      assertEquals(30.0F, ArashCombatRules.SMALL_ENERGY_DAMAGE);
      assertEquals(60.0F, ArashCombatRules.LARGE_ENERGY_DAMAGE);
      assertEquals(2, ArashCombatRules.SMALL_ENERGY_TERRAIN_RADIUS);
      assertEquals(4, ArashCombatRules.LARGE_ENERGY_TERRAIN_RADIUS);
      assertEquals(0, ArashCombatRules.terrainDestructionRadius(0));
      assertEquals(0, ArashCombatRules.terrainDestructionRadius(1));
      assertEquals(2, ArashCombatRules.terrainDestructionRadius(2));
      assertEquals(4, ArashCombatRules.terrainDestructionRadius(3));
      assertEquals(40, ArashBowItem.CHARGED_ARROW_TICKS);
      assertEquals(80, ArashBowItem.HEAVY_CHARGED_ARROW_TICKS);
   }

   @Test
   void arashUsesContinuousMobileRangedTactics() {
      assertEquals(12, ArashCombatRules.TACTICAL_REPATH_INTERVAL);
      assertEquals(32.0, ArashCombatRules.PREFERRED_COMBAT_RANGE);
      assertEquals(56.0, ArashCombatRules.APPROACH_THRESHOLD);
      assertEquals(7.0, ArashCombatRules.CROSSOVER_TRIGGER_RANGE);
      assertEquals(60, ArashCombatRules.CROSSOVER_COOLDOWN);
   }

   @Test
   void autoAimUsesSmallConeAndBoundedMovementPrediction() {
      Vec3 forward = new Vec3(1.0, 0.0, 0.0);
      Vec3 inside = new Vec3(Math.cos(Math.toRadians(7.9)), 0.0, Math.sin(Math.toRadians(7.9)));
      Vec3 outside = new Vec3(Math.cos(Math.toRadians(8.1)), 0.0, Math.sin(Math.toRadians(8.1)));
      assertEquals(200.0, ArashAimHelper.AUTO_AIM_RANGE);
      assertEquals(8.0, ArashAimHelper.AUTO_AIM_ANGLE_DEGREES);
      assertEquals(12.0, ArashAimHelper.ARROW_RAIN_ASSIST_RADIUS);
      assertTrue(ArashAimHelper.isWithinAimCone(forward, inside, ArashAimHelper.AUTO_AIM_ANGLE_DEGREES));
      assertFalse(ArashAimHelper.isWithinAimCone(forward, outside, ArashAimHelper.AUTO_AIM_ANGLE_DEGREES));
      Vec3 clamped = ArashAimHelper.clampDirectionToCone(forward, outside, ArashAimHelper.AUTO_AIM_ANGLE_DEGREES);
      assertTrue(ArashAimHelper.isWithinAimCone(forward, clamped, ArashAimHelper.AUTO_AIM_ANGLE_DEGREES));
      assertTrue(ArashAimHelper.estimateFlightTicks(100.0, 3.4) > 100.0 / 3.4,
         "prediction must account for projectile drag");
      Vec3 predicted = ArashAimHelper.predictionOffset(new Vec3(0.2, 0.0, 0.4), 40.0);
      assertTrue(predicted.x > 0.0 && predicted.z > 0.0, "prediction did not lead horizontal movement");
      assertTrue(predicted.horizontalDistance() <= 32.0001, "prediction exceeded its safe lead distance");
   }

   @Test
   void stellaUsesOnePointFiveSpeedAndFiftyBlockEndSphere() {
      assertEquals(700, ArashCombatRules.STELLA_CHANT_TICKS);
      assertEquals(777, ArashCombatRules.STELLA_VOICE_TICKS);
      assertEquals(200, ArashCombatRules.STELLA_SACRIFICE_TICKS);
      assertEquals(20, ArashCombatRules.STELLA_SACRIFICE_DAMAGE_INTERVAL);
      assertEquals(0.10F, ArashCombatRules.STELLA_SACRIFICE_DAMAGE_FRACTION);
      assertEquals(600.0F, ArashCombatRules.stellaRemainingHealth(600.0F, 0));
      assertEquals(600.0F, ArashCombatRules.stellaRemainingHealth(600.0F, 19));
      assertEquals(540.0F, ArashCombatRules.stellaRemainingHealth(600.0F, 20));
      assertEquals(300.0F, ArashCombatRules.stellaRemainingHealth(600.0F, 100));
      assertEquals(0.0F, ArashCombatRules.stellaRemainingHealth(600.0F, 200));
      assertEquals(540.0F, ArashCombatRules.applyStellaSacrificePulse(600.0F, 600.0F, false));
      assertEquals(1.0F, ArashCombatRules.applyStellaSacrificePulse(40.0F, 600.0F, false));
      assertEquals(0.0F, ArashCombatRules.applyStellaSacrificePulse(40.0F, 600.0F, true));
      assertEquals(2500.0, ArashCombatRules.STELLA_LENGTH);
      assertEquals(2400, ArashCombatRules.STELLA_FLIGHT_TICKS);
      assertEquals(1250.0, ArashCombatRules.stellaDistanceAtTick(1200));
      assertEquals(2500.0, ArashCombatRules.stellaDistanceAtTick(2400));
      assertEquals(10, ArashCombatRules.STELLA_TERRAIN_RADIUS);
      assertEquals(18, ArashCombatRules.STELLA_SCAR_RADIUS);
      assertEquals(1.25, ArashCombatRules.STELLA_CORE_RADIUS);
      assertEquals(5.0, ArashCombatRules.STELLA_OUTER_RADIUS);
      assertEquals(50.0, ArashCombatRules.STELLA_END_RADIUS);
      assertEquals(25.0, ArashCombatRules.stellaExplosionRadiusAtTick(50));
      assertEquals(50.0, ArashCombatRules.stellaExplosionRadiusAtTick(100));
   }

   @Test
   void playerStellaScalesFromTenSecondsToFullCharge() {
      assertEquals(200, ArashCombatRules.PLAYER_STELLA_MIN_CHARGE_TICKS);
      assertEquals(720, ArashCombatRules.PLAYER_STELLA_AUTO_RELEASE_TICKS);
      assertEquals(720, ArashCombatRules.PLAYER_STELLA_FULL_CHARGE_TICKS);
      assertEquals(660, ArashCombatRules.PLAYER_STELLA_LONG_VOICE_CUTOFF_TICKS);
      assertFalse(ArashCombatRules.canReleasePlayerStella(199));
      assertTrue(ArashCombatRules.canReleasePlayerStella(200));
      assertFalse(ArashCombatRules.shouldFinishLongStellaVoice(659));
      assertTrue(ArashCombatRules.shouldFinishLongStellaVoice(660));

      ArashCombatRules.StellaProfile minimum = ArashCombatRules.playerStellaProfile(200);
      assertEquals(500.0, minimum.length());
      assertEquals(1000.0F, minimum.coreDamage());
      assertEquals(2000.0F, minimum.coreDamage() * 2.0F,
         "a core target hit by both the line and final blast must take 2000 total damage");
      assertEquals(10.0, minimum.endRadius());

      ArashCombatRules.StellaProfile full = ArashCombatRules.playerStellaProfile(760);
      assertEquals(2500.0, full.length());
      assertEquals(2000.0F, full.coreDamage());
      assertEquals(50.0, full.endRadius());
      assertEquals(ArashCombatRules.fullStellaProfile(), full);

      ArashCombatRules.StellaProfile clamped = ArashCombatRules.playerStellaProfile(1000);
      assertEquals(full, clamped);
      assertTrue(ArashCombatRules.playerStellaProfile(480).length() > minimum.length());
      assertTrue(ArashCombatRules.playerStellaProfile(480).length() < full.length());
   }

   @Test
   void servantCardSlotsAndChantToleranceAreExact() {
      assertEquals(0.0, ServantCardArashSkills.CHANT_MOVE_RADIUS);
      assertEquals(0, ServantCardArashSkills.STELLA_LOCKED_HOTBAR_SLOT);
      assertEquals(3.0F, ServantCardArashSkills.CHANT_LOOK_TOLERANCE);
      assertEquals(0.10F, ServantCardArashSkills.BASE_DODGE_CHANCE);
      assertAction(0, "arash_arrow_rain", 8.0, 160);
      assertAction(1, "arash_energy_small", 8.0, 80);
      assertAction(2, "arash_energy_large", 20.0, 240);
      assertAction(3, "arash_arrow_creation", 1.0, 0);
      for (int slot = 4; slot <= 8; slot++) assertNull(ServantCardSkillLayout.actionFor("arash", slot, false));
      assertAction(9, "arash_stella", 100.0, 3600);
   }

   private static void assertAction(int slot, String effect, double mana, int cooldown) {
      ServantCardSkillAction action = ServantCardSkillLayout.actionFor("arash", slot, false);
      assertNotNull(action);
      assertEquals(effect, action.effectId());
      assertEquals(mana, action.mpCost());
      assertEquals(cooldown, action.cooldownTicks());
   }
}
