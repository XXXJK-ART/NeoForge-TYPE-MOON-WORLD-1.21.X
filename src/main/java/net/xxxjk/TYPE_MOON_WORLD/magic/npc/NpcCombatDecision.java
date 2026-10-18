package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;

/** Target and melee decisions kept separate from action execution. */
public final class NpcCombatDecision {
   private static final String TAG_CAP_HAS_RANGED = "TypeMoonNpcCapHasRanged";
   private static final String TAG_CAP_HAS_MELEE_BURST = "TypeMoonNpcCapHasMeleeBurst";
   private static final String TAG_CAP_ONLY_MELEE = "TypeMoonNpcCapOnlyMelee";
   private NpcCombatDecision() {
   }

   public static boolean shouldUseMelee(MysticMagicianEntity npc) {
      if (npc == null) return true;
      var data = npc.getPersistentData();
      boolean hasRanged = data.getBoolean(TAG_CAP_HAS_RANGED);
      boolean hasMeleeBurst = data.getBoolean(TAG_CAP_HAS_MELEE_BURST);
      boolean onlyMelee = data.getBoolean(TAG_CAP_ONLY_MELEE);
      NpcCombatTemperament temperament = npc.getCombatTemperament();
      LivingEntity target = npc.getTarget();
      if (target == null || !target.isAlive()) return !hasRanged;
      double distanceSqr = npc.distanceToSqr(target);
      if (onlyMelee) return distanceSqr <= 121.0D;
      if (hasRanged && hasMeleeBurst) {
         return distanceSqr <= switch (temperament) {
            case TIMID -> 20.25D;
            case BOLD -> 42.25D;
            default -> 30.25D;
         };
      }
      if (hasRanged) {
         return distanceSqr <= switch (temperament) {
            case TIMID -> 2.25D;
            case BOLD -> 6.25D;
            default -> 4.0D;
         };
      }
      return distanceSqr <= 25.0D;
   }

   public static boolean isTargetCandidate(MysticMagicianEntity npc, LivingEntity target) {
      return npc != null && NpcMagicCastBridge.isNpcTargetCandidate(npc, target);
   }

   public static boolean shouldRetreat(double healthRatio, double threshold, boolean strongTarget, int enemyCount) {
      double adjusted = threshold;
      if (strongTarget) adjusted += 0.07D;
      if (enemyCount >= 3) adjusted += 0.08D;
      adjusted = Mth.clamp(adjusted, 0.16D, 0.58D);
      return healthRatio <= adjusted;
   }
}
