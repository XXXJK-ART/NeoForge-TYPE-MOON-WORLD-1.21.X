package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.List;
import javax.annotation.Nullable;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;

public final class ServantTraitAttackEvaluator {
   private ServantTraitAttackEvaluator() {
   }

   public static double computeMultiplier(List<ServantTraitTag> attackerConditions, List<ServantTraitTag> defenderTraits) {
      if (attackerConditions == null || attackerConditions.isEmpty()
          || defenderTraits == null || defenderTraits.isEmpty()) {
         return 1.0;
      }

      double multiplier = 1.0;
      for (ServantTraitTag condition : attackerConditions) {
         if (defenderTraits.contains(condition)) {
            multiplier *= 1.5;
         }
      }

      return multiplier;
   }

   public static boolean hasTrait(@Nullable net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag trait,
                                  List<ServantTraitTag> defenderTraits) {
      return trait != null && defenderTraits != null && defenderTraits.contains(trait);
   }
}
