package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.Collections;
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
      java.util.List<ServantTraitTag> attackerTraits = attackerDef != null ? attackerDef.traits() : Collections.emptyList();

      double baseDamage = ServantDamageCalculator.computeBaseDamage(attackerAttack, normalAttackMultiplier);

      double zoneB = ServantDamageCalculator.computeCritMultiplier(isCrit, attacker.getCritRate() * 0.01);

      double factionMultiplier = ServantDamageCalculator.computeFactionMultiplier(attackerFaction, getDefenderFaction(defender));
      java.util.List<ServantTraitTag> specialConditions = npSpecialConditions != null ? npSpecialConditions : attackerTraits;
      java.util.List<ServantTraitTag> defenderTraitList = getDefenderTraits(defender);
      double traitMultiplier = ServantDamageCalculator.computeTraitMultiplier(specialConditions, defenderTraitList);
      double zoneC = factionMultiplier * traitMultiplier;

      double defenderArmor = defender.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
      return ServantDamageCalculator.computeFinalDamage(baseDamage, zoneABuffMultiplier, zoneB, zoneC, defenderArmor);
   }

   @Nullable
   private static ServantFaction getDefenderFaction(LivingEntity defender) {
      if (defender instanceof ServantEntity servantDefender) {
         ServantDefinition def = servantDefender.getDefinition();
         return def != null ? def.faction() : null;
      }
      return null;
   }

   private static java.util.List<ServantTraitTag> getDefenderTraits(LivingEntity defender) {
      if (defender instanceof ServantEntity servantDefender) {
         ServantDefinition def = servantDefender.getDefinition();
         return def != null ? def.traits() : Collections.emptyList();
      }
      return Collections.emptyList();
   }
}
