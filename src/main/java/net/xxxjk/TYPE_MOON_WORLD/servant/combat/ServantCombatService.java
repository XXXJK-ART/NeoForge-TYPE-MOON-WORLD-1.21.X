package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanPersonaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;

public final class ServantCombatService {
   public static final String SHARED_HIT_TAG = "TypeMoonSharedDamageHit";
   private ServantCombatService() {
   }

   public static void beginSharedHit(LivingEntity attacker) {
      if (attacker != null) attacker.getPersistentData().putBoolean(SHARED_HIT_TAG, true);
   }

   public static void endSharedHit(LivingEntity attacker) {
      if (attacker != null) attacker.getPersistentData().remove(SHARED_HIT_TAG);
   }

   public static double calculateDamage(
      ServantEntity attacker,
      LivingEntity defender,
      double normalAttackMultiplier,
      boolean isCrit,
      @Nullable java.util.List<ServantTraitTag> npSpecialConditions,
      double zoneABuffMultiplier
   ) {
      defender = net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterDamageRouter.recipient(defender);
      ServantDefinition attackerDef = attacker.getDefinition();
      double attackerAttack = attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
      ServantFaction attackerFaction = attackerDef != null ? attackerDef.faction() : null;

      double baseDamage = ServantDamageCalculator.computeBaseDamage(attackerAttack, normalAttackMultiplier);

      double critRate = attacker.getCritRate();
      if (attacker.getPersistentData().getLong("CasterGilgameshCritBuffUntil") > attacker.level().getGameTime()) {
         critRate += 80.0;
      }
      double zoneB = ServantDamageCalculator.computeCritMultiplier(isCrit, critRate * 0.01);

      double factionMultiplier = ServantDamageCalculator.computeFactionMultiplier(attackerFaction, getDefenderFaction(defender));
      // Identity tags describe the wielder, not the targets an attack is strong against.
      java.util.List<ServantTraitTag> specialConditions = npSpecialConditions != null ? npSpecialConditions : List.of();
      java.util.List<ServantTraitTag> defenderTraitList = ServantIdentityHelper.traitsOf(defender);
      double traitMultiplier = attackerDef != null && "oda_nobunaga".equals(attackerDef.id()) ? 1 : ServantDamageCalculator.computeTraitMultiplier(specialConditions, defenderTraitList);
      double zoneC = factionMultiplier * traitMultiplier;

      double defenderArmor = net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper.hasGodHand(defender) ? 0 : defender.getAttributeValue(Attributes.ARMOR);
      return ServantDamageCalculator.computeFinalDamage(baseDamage, zoneABuffMultiplier, zoneB, zoneC, defenderArmor);
   }

   /** Shared damage contract for manifested servants and transformed players. */
   public static double calculateDamage(
      LivingEntity attacker,
      LivingEntity defender,
      double normalAttackMultiplier,
      boolean isCrit,
      @Nullable java.util.List<ServantTraitTag> specialConditions,
      double zoneABuffMultiplier
   ) {
      defender = net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterDamageRouter.recipient(defender);
      if (attacker instanceof ServantEntity servant) {
         return calculateDamage(servant, defender, normalAttackMultiplier, isCrit, specialConditions, zoneABuffMultiplier);
      }
      ServantDefinition definition = ServantIdentityHelper.definitionOf(attacker);
      double attack = attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
      if (definition != null && attack <= 0.0) attack = definition.parameters().attackDamage();
      double base = ServantDamageCalculator.computeBaseDamage(Math.max(0.0, attack), normalAttackMultiplier);
      double critRate = attacker instanceof Player ? 0.0 : 0.0;
      double critMultiplier = ServantDamageCalculator.computeCritMultiplier(isCrit, critRate * 0.01);
      double factionMultiplier = ServantDamageCalculator.computeFactionMultiplier(
         definition == null ? null : definition.faction(), getDefenderFaction(defender));
      List<ServantTraitTag> traits = specialConditions != null ? specialConditions : List.of();
      double traitMultiplier = definition != null && "oda_nobunaga".equals(definition.id()) ? 1 : ServantDamageCalculator.computeTraitMultiplier(traits, ServantIdentityHelper.traitsOf(defender));
      double armor = net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper.hasGodHand(defender) ? 0 : defender.getAttributeValue(Attributes.ARMOR);
      return ServantDamageCalculator.computeFinalDamage(base, zoneABuffMultiplier, critMultiplier,
         factionMultiplier * traitMultiplier, armor);
   }

   @Nullable
   private static ServantFaction getDefenderFaction(LivingEntity defender) {
      return ServantIdentityHelper.factionOf(defender);
   }
}
