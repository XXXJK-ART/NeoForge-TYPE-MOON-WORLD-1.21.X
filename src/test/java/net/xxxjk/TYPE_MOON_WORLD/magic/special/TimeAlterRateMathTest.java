package net.xxxjk.TYPE_MOON_WORLD.magic.special;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TimeAlterRateMathTest {
   @Test
   void accelerationUsesLinearActionsAndSquaredGravity() {
      double rate = TimeAlterRateMath.effectiveActionRate(MagicTimeAlter.MODE_ACCEL, 4.0);
      assertEquals(4.0, rate, 1.0E-9);
      assertEquals(3.0, TimeAlterRateMath.linearModifierAmount(rate), 1.0E-9);
   }

   @Test
   void actionRateCapsWithoutChangingTheConfiguredMultiplier() {
      assertEquals(10.0, TimeAlterRateMath.effectiveActionRate(MagicTimeAlter.MODE_ACCEL, 20.0), 1.0E-9);
   }

   @Test
   void stagnationSlowsActionsAndGravity() {
      double rate = TimeAlterRateMath.effectiveActionRate(MagicTimeAlter.MODE_STAGNATE, 0.25);
      assertEquals(0.25, rate, 1.0E-9);
      assertEquals(-0.75, TimeAlterRateMath.linearModifierAmount(rate), 1.0E-9);
   }

   @Test
   void acceleratedItemUseAdvancesMultipleTicksAtOnce() {
      TimeAlterRateMath.UseAdvance advance = TimeAlterRateMath.advanceItemUse(0.0, 4.0);
      assertEquals(4, advance.elapsedUseTicks());
      assertEquals(0.0, advance.remainingProgress(), 1.0E-9);
      assertEquals(29, TimeAlterRateMath.durationBeforeVanillaDecrement(32, advance.elapsedUseTicks()));
   }

   @Test
   void stagnatedItemUseAdvancesOneTickEveryFourWorldTicks() {
      double remainder = 0.0;
      int elapsed = 0;
      for (int tick = 0; tick < 8; tick++) {
         TimeAlterRateMath.UseAdvance advance = TimeAlterRateMath.advanceItemUse(remainder, 0.25);
         elapsed += advance.elapsedUseTicks();
         remainder = advance.remainingProgress();
      }
      assertEquals(2, elapsed);
      assertEquals(0.0, remainder, 1.0E-9);
   }
}
