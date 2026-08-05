package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ServantCombatContactRulesTest {
   @Test
   void basicReachScalesWithBodiesButDoesNotUseTheOldUniversalMaximum() {
      assertEquals(3.19, ServantCombatTempoService.basicAttackReach(0.6, 0.6), 1.0E-6);
      assertEquals(4.35, ServantCombatTempoService.basicAttackReach(2.0, 2.0), 1.0E-6);
      assertFalse(ServantCombatTempoService.basicAttackGeometry(3.8, 0.0, true, 0.6, 0.6));
      assertTrue(ServantCombatTempoService.basicAttackGeometry(3.1, 0.0, true, 0.6, 0.6));
   }

   @Test
   void basicAttackRejectsBadHeightAndBlockedSight() {
      assertFalse(ServantCombatTempoService.basicAttackGeometry(3.0, 3.0, true, 0.6, 0.6));
      assertFalse(ServantCombatTempoService.basicAttackGeometry(2.2, 0.0, false, 0.6, 0.6));
      assertTrue(ServantCombatTempoService.basicAttackGeometry(1.7, 0.0, false, 0.6, 0.6));
   }

   @Test
   void highSpeedSweepDetectsAContactAlongTheTravelSegment() {
      AABB attacker = new AABB(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3);
      AABB target = new AABB(1.35, 0.0, -0.3, 1.95, 1.8, 0.3);

      assertTrue(ServantCombatTempoService.sweptContact(attacker, new Vec3(1.2, 0.0, 0.0), target));
      assertFalse(ServantCombatTempoService.sweptContact(attacker, new Vec3(-1.2, 0.0, 0.0), target));
   }
}
