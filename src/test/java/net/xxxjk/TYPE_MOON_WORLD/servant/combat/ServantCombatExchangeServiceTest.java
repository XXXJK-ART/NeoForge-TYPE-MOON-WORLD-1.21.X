package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantManeuverService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatExchangeService.State.*;

class ServantCombatExchangeServiceTest {
   @Test void recoveryAndPursuitTakePrecedenceOverSpacing() {
      assertEquals(RECOVERY, ServantCombatExchangeService.selectState(true, true, 10, true));
      assertEquals(PURSUIT, ServantCombatExchangeService.selectState(false, true, 10, true));
      assertEquals(BREAK, ServantCombatExchangeService.selectState(false, false, 2, true));
      assertEquals(CLASH, ServantCombatExchangeService.selectState(false, false, 2, false));
      assertEquals(APPROACH, ServantCombatExchangeService.selectState(false, false, 12, false));
   }

   @Test void pairSidesStayComplementaryAcrossEveryAlternation() {
      UUID a = new UUID(1, 5), b = new UUID(9, 8);
      int last = ServantCombatExchangeService.side(a, b, 0), changes = 0, lastChange = 0;
      for (int tick = 0; tick < 240; tick++) {
         int side = ServantCombatExchangeService.side(a, b, tick);
         assertEquals(-side, ServantCombatExchangeService.side(b, a, tick));
         if (side != last) {
            assertTrue(tick - lastChange >= 12 && tick - lastChange <= 18);
            changes++; lastChange = tick; last = side;
         }
      }
      assertTrue(changes >= 12);
   }

   @Test void everyDashAngleKeepsBothForwardAndLateralMotion() {
      Vec3 forward = new Vec3(0.6, 0, 0.8);
      for (int angle = 35; angle <= 55; angle++) for (int sign : new int[]{-1, 1}) {
         Vec3 direction = ServantManeuverService.sideForwardDirection(forward, sign * angle);
         assertTrue(direction.dot(forward) > 0.57);
         assertTrue(Math.abs(direction.x * forward.z - direction.z * forward.x) > 0.57);
         assertEquals(0, direction.y);
         assertEquals(1, direction.length(), 1.0E-6);
      }
   }

   @Test void scriptedPairIsSymmetricAndSpecific() {
      assertTrue(ServantCombatExchangeService.isScriptedPair("gilgamesh", "enkidu"));
      assertTrue(ServantCombatExchangeService.isScriptedPair("enkidu", "gilgamesh"));
      assertFalse(ServantCombatExchangeService.isScriptedPair("gilgamesh", "medea"));
      assertFalse(ServantCombatExchangeService.isScriptedPair("artoria_pendragon", "enkidu"));
   }
}
