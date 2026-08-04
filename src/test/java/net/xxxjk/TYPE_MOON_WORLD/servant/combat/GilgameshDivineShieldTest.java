package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GilgameshDivineShieldTest {
   @Test
   void constantsMatchThePersistentTwoThousandHpDesign() {
      assertEquals(600, GilgameshDivineShield.COOLDOWN_TICKS);
      assertEquals(2000.0F, GilgameshDivineShield.MAX_HP);
      assertEquals(0.80F, GilgameshDivineShield.NON_PROJECTILE_ABSORPTION);
   }

   @Test
   void projectilesAreFullyAbsorbedWhileCapacityRemains() {
      var hit = GilgameshDivineShield.absorb(2000.0F, 250.0F, true);
      assertEquals(1750.0F, hit.remainingShieldHp());
      assertEquals(0.0F, hit.remainingDamage());
      assertEquals(250.0F, hit.absorbedDamage());
      assertTrue(hit.projectile());
   }

   @Test
   void otherDamageIsReducedByEightyPercent() {
      var hit = GilgameshDivineShield.absorb(2000.0F, 100.0F, false);
      assertEquals(1920.0F, hit.remainingShieldHp());
      assertEquals(20.0F, hit.remainingDamage(), 0.0001F);
      assertEquals(80.0F, hit.absorbedDamage());
   }

   @Test
   void damageOverflowsWhenTheShieldBreaks() {
      var projectile = GilgameshDivineShield.absorb(50.0F, 100.0F, true);
      assertEquals(50.0F, projectile.remainingDamage());
      assertTrue(projectile.broken());

      var melee = GilgameshDivineShield.absorb(50.0F, 100.0F, false);
      assertEquals(50.0F, melee.remainingDamage());
      assertTrue(melee.broken());
   }
}
