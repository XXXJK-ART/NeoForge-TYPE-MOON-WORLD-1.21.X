package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.minecraft.util.Mth;

/** Pure combat policy decisions kept separate from entity/world mutation. */
public final class NpcCombatDecision {
   private NpcCombatDecision() {
   }

   public static boolean shouldRetreat(double healthRatio, double configuredThreshold,
                                       boolean strongTarget, int enemyCount) {
      double threshold = configuredThreshold;
      if (strongTarget) {
         threshold += 0.07D;
      }
      if (enemyCount >= 3) {
         threshold += 0.08D;
      }
      threshold = Mth.clamp(threshold, 0.16D, 0.58D);
      return healthRatio <= threshold;
   }
}
