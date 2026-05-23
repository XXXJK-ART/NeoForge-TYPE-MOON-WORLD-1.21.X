package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;

public final class ServantDamageCalculator {
   private ServantDamageCalculator() {
   }

   public static double computeFinalDamage(
      double baseDamage,
      double zoneABuffMultiplier,
      double zoneBCritMultiplier,
      double zoneCTraitMultiplier,
      double defenderArmor
   ) {
      double afterZones = baseDamage * zoneABuffMultiplier * zoneBCritMultiplier * zoneCTraitMultiplier;
      double armorReduction = defenderArmor / (defenderArmor + 100.0);
      return afterZones * (1.0 - armorReduction);
   }

   public static double computeBaseDamage(double attackerAttack, double normalAttackMultiplier) {
      return attackerAttack * normalAttackMultiplier;
   }

   public static double computeNpBaseDamage(double attackerAttack, double npDamageMultiplier) {
      return attackerAttack * npDamageMultiplier;
   }

   public static double computeCritMultiplier(boolean isCrit, double luckBonus) {
      return isCrit ? (2.0 + luckBonus) : 1.0;
   }

   public static double computeFactionMultiplier(
      @Nullable ServantFaction attackerFaction,
      @Nullable ServantFaction defenderFaction
   ) {
      return ServantFactionAdvantage.computeMultiplier(attackerFaction, defenderFaction);
   }

   public static double computeTraitMultiplier(
      List<ServantTraitTag> specialAttackConditions,
      List<ServantTraitTag> defenderTraits
   ) {
      return ServantTraitAttackEvaluator.computeMultiplier(specialAttackConditions, defenderTraits);
   }

   public static double computeArmorReduction(double defenderArmor) {
      return defenderArmor / (defenderArmor + 100.0);
   }
}
