package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;

public final class ServantCombatService {
   private ServantCombatService() {
   }

   public static double calculateDamage(
      ServantEntity attacker,
      LivingEntity defender,
      double normalAttackMultiplier,
      boolean isCrit,
      @Nullable java.util.List<ServantTraitTag> npSpecialConditions,
      double zoneABuffMultiplier
   ) {
      ServantDefinition attackerDef = attacker.getDefinition();
      double attackerAttack = attackerDef != null ? attackerDef.parameters().attackDamage() : 5.0;
      ServantFaction attackerFaction = attackerDef != null ? attackerDef.faction() : null;
      java.util.List<ServantTraitTag> attackerTraits = attackerDef != null ? attackerDef.traits() : java.util.Collections.emptyList();

      double baseDamage = ServantDamageCalculator.computeBaseDamage(attackerAttack, normalAttackMultiplier);

      double critRate = attacker.getCritRate();
      if (attacker.getPersistentData().getLong("CasterGilgameshCritBuffUntil") > attacker.level().getGameTime()) {
         critRate += 80.0;
      }
      double zoneB = ServantDamageCalculator.computeCritMultiplier(isCrit, critRate * 0.01);

      double factionMultiplier = ServantDamageCalculator.computeFactionMultiplier(attackerFaction, getDefenderFaction(defender));
      java.util.List<ServantTraitTag> specialConditions = npSpecialConditions != null ? npSpecialConditions : attackerTraits;
      java.util.List<ServantTraitTag> defenderTraitList = ServantIdentityHelper.traitsOf(defender);
      double traitMultiplier = ServantDamageCalculator.computeTraitMultiplier(specialConditions, defenderTraitList);
      double zoneC = factionMultiplier * traitMultiplier;

      double defenderArmor = defender.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
      return ServantDamageCalculator.computeFinalDamage(baseDamage, zoneABuffMultiplier, zoneB, zoneC, defenderArmor);
   }

   @Nullable
   private static ServantFaction getDefenderFaction(LivingEntity defender) {
      return ServantIdentityHelper.factionOf(defender);
   }
}
