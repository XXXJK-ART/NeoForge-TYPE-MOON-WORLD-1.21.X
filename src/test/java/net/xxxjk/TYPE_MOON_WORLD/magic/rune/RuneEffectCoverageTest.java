package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** Regression matrix for all 24 rune ids and their four executable positions. */
class RuneEffectCoverageTest {
   @Test
   void allNinetySixActionsHaveAnExecutableHandler() {
      assertEquals(24, RuneRegistry.all().size());
      for (RuneDefinition definition : RuneRegistry.all()) {
         for (RunePosition position : RunePosition.values()) {
            String action = RuneRegistry.actionKey(definition.idPath(), position);
            assertFalse(action.isBlank(), definition.idPath() + ":" + position);
            assertTrue(RuneEffectDispatcher.isBuiltInAction(position, action), definition.idPath() + ":" + position + "=" + action);
         }
      }
   }

   @Test
   void splitCreatesRealProjectileCountsAndDamageScaling() {
      assertEquals(1, RuneEffectDispatcher.projectileCountForSplit(0));
      assertEquals(2, RuneEffectDispatcher.projectileCountForSplit(1));
      assertEquals(4, RuneEffectDispatcher.projectileCountForSplit(2));
      assertEquals(8, RuneEffectDispatcher.projectileCountForSplit(3));
      assertEquals(8, RuneEffectDispatcher.projectileCountForSplit(8));
      assertEquals(80.0D, RuneEffectDispatcher.projectileDamageForSplit(100.0D, 1), 0.001D);
      assertEquals(64.0D, RuneEffectDispatcher.projectileDamageForSplit(100.0D, 2), 0.001D);
   }
}
