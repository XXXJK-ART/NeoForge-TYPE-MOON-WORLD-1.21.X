package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServantCardJumpRecoveryTest {
   @Test
   void fullJumpChargesClearStaleRecoveryState() {
      ServantCardJumpRecoveryRules.State state = ServantCardJumpRecoveryRules.normalize(4, 83, 2000L, false, 1000L);

      assertTrue(state.changed());
      assertEquals(4, state.charges());
      assertEquals(0, state.recoveryTicks());
      assertEquals(0L, state.recoveryEnd());
   }

   @Test
   void farFutureRecoveryEndIsClampedToNormalWindow() {
      ServantCardJumpRecoveryRules.State state = ServantCardJumpRecoveryRules.normalize(2, 20, 5000L, false, 1000L);

      assertTrue(state.changed());
      assertEquals(2, state.charges());
      assertEquals(20, state.recoveryTicks());
      assertEquals(1020L, state.recoveryEnd());
   }

   @Test
   void recoveryWindowsStayBounded() {
      assertEquals(100, ServantCardJumpRecoveryRules.recoveryTicksFor(0));
      assertEquals(20, ServantCardJumpRecoveryRules.recoveryTicksFor(1));
      assertEquals(20, ServantCardJumpRecoveryRules.recoveryTicksFor(3));
   }
}
