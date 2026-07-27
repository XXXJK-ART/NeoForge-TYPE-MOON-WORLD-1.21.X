package net.xxxjk.TYPE_MOON_WORLD.servant.arash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.xxxjk.TYPE_MOON_WORLD.item.custom.ArashBowItem;
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
   }

   @Test
   void arrowCostsDamageAndCadenceAreExact() {
      assertEquals(10, ArashCombatRules.ARROW_CAPACITY);
      assertEquals(5.0, ArashCombatRules.ARROW_REFILL_MANA);
      assertEquals(10.0F, ArashCombatRules.NORMAL_ARROW_DAMAGE);
      assertEquals(4, ArashCombatRules.NORMAL_ARROW_INTERVAL);
      assertEquals(50, ArashCombatRules.RAIN_ARROW_COUNT);
      assertEquals(13.2F, ArashCombatRules.RAIN_ARROW_DAMAGE);
      assertEquals(30.0F, ArashCombatRules.SMALL_ENERGY_DAMAGE);
      assertEquals(60.0F, ArashCombatRules.LARGE_ENERGY_DAMAGE);
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
   void stellaUsesOnePointFiveSpeedAndFiftyBlockEndSphere() {
      assertEquals(700, ArashCombatRules.STELLA_CHANT_TICKS);
      assertEquals(200, ArashCombatRules.STELLA_SACRIFICE_TICKS);
      assertEquals(600.0F, ArashCombatRules.stellaRemainingHealth(600.0F, 0));
      assertEquals(300.0F, ArashCombatRules.stellaRemainingHealth(600.0F, 100));
      assertEquals(0.0F, ArashCombatRules.stellaRemainingHealth(600.0F, 200));
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
   void servantCardSlotsAndChantToleranceAreExact() {
      assertEquals(0.0, ServantCardArashSkills.CHANT_MOVE_RADIUS);
      assertEquals(0, ServantCardArashSkills.STELLA_LOCKED_HOTBAR_SLOT);
      assertEquals(3.0F, ServantCardArashSkills.CHANT_LOOK_TOLERANCE);
      assertEquals(0.10F, ServantCardArashSkills.BASE_DODGE_CHANCE);
      assertAction(0, "arash_arrow_rain", 8.0, 160);
      assertAction(1, "arash_energy_small", 8.0, 80);
      assertAction(2, "arash_energy_large", 20.0, 240);
      for (int slot = 3; slot <= 8; slot++) assertNull(ServantCardSkillLayout.actionFor("arash", slot, false));
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
