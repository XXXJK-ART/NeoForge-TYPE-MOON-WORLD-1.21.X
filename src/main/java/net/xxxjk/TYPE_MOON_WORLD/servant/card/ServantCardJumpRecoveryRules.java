package net.xxxjk.TYPE_MOON_WORLD.servant.card;

final class ServantCardJumpRecoveryRules {
   static final int DEFAULT_JUMP_CHARGES = 4;
   static final int EMPTY_RECOVERY_TICKS = 100;
   static final int PARTIAL_RECOVERY_TICKS = 20;

   private ServantCardJumpRecoveryRules() {
   }

   static State normalize(int charges, int recoveryTicks, long recoveryEnd, boolean eightBoatActive, long now) {
      int maxCharges = ServantCardUshiwakamaruRules.jumpLimit(eightBoatActive);
      int clampedCharges = ServantCardUshiwakamaruRules.clampJumpCharges(charges, eightBoatActive);
      boolean changed = clampedCharges != charges;
      charges = clampedCharges;
      if (charges >= maxCharges) {
         if (recoveryTicks != 0 || recoveryEnd != 0L) {
            recoveryTicks = 0;
            recoveryEnd = 0L;
            changed = true;
         }
         return new State(charges, recoveryTicks, recoveryEnd, changed);
      }
      int clampedRecovery = clampTicks(recoveryTicks);
      if (clampedRecovery != recoveryTicks) {
         recoveryTicks = clampedRecovery;
         changed = true;
      }
      if (recoveryEnd > 0L) {
         long remaining = recoveryEnd - now;
         if (remaining > EMPTY_RECOVERY_TICKS) {
            recoveryTicks = recoveryTicksFor(charges);
            recoveryEnd = now + recoveryTicks;
            changed = true;
         }
      }
      return new State(charges, recoveryTicks, recoveryEnd, changed);
   }

   static int recoveryTicksFor(int jumpCharges) {
      return jumpCharges <= 0 ? EMPTY_RECOVERY_TICKS : PARTIAL_RECOVERY_TICKS;
   }

   static int clampTicks(int ticks) {
      return Math.max(0, Math.min(EMPTY_RECOVERY_TICKS, ticks));
   }

   record State(int charges, int recoveryTicks, long recoveryEnd, boolean changed) {
   }
}
