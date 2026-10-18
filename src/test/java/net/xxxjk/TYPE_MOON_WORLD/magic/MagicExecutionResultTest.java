package net.xxxjk.TYPE_MOON_WORLD.magic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import org.junit.jupiter.api.Test;

class MagicExecutionResultTest {
   @Test
   void continuousTransitionsAreAcceptedWithoutGenericCooldown() {
      assertTrue(MagicExecutionResult.STARTED.accepted());
      assertTrue(MagicExecutionResult.STARTED.startsContinuous());
      assertFalse(MagicExecutionResult.STARTED.shouldApplyCooldown());
      assertTrue(MagicExecutionResult.STOPPED.accepted());
      assertTrue(MagicExecutionResult.STOPPED.stopsContinuous());
      assertFalse(MagicExecutionResult.STOPPED.shouldApplyCooldown());
   }

   @Test
   void legacySuccessStillUsesDefaultCooldownSemantics() {
      assertTrue(MagicExecutionResult.SUCCESS.accepted());
      assertTrue(MagicExecutionResult.SUCCESS.shouldApplyCooldown());
      assertFalse(MagicExecutionResult.REJECTED.accepted());
   }
}
