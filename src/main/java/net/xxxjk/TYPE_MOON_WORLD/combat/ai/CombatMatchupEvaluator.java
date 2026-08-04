package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactType;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;

/** Bounded matchup scoring. It changes preferences but never executes or fabricates abilities. */
public final class CombatMatchupEvaluator {
   public static final double MIN_ACTION_MULTIPLIER = 0.25;
   public static final double MAX_ACTION_MULTIPLIER = 1.75;

   private CombatMatchupEvaluator() { }

   public static double actionMultiplier(LivingEntity self, LivingEntity target, AiActionDescriptor action,
                                         AiBlackboard.OpponentSnapshot knowledge) {
      if (action == null || target == null) return 1.0;
      CombatCapabilitySnapshot own = ServantCapabilityResolver.resolve(self);
      CombatCapabilitySnapshot visibleTarget = ServantCapabilityResolver.resolveVisible(target);
      boolean targetMobile = ServantCapabilityResolver.isMobile(target);
      double multiplier = learnedDefenseMultiplier(action, knowledge, targetMobile);

      if (action.tags().contains(AiActionDescriptor.Tag.MELEE)) {
         multiplier *= 1.0 + own.strength(FactType.MELEE_PRESSURE) * 0.12;
      }
      if (action.tags().contains(AiActionDescriptor.Tag.PROJECTILE)) {
         multiplier *= 1.0 + own.strength(FactType.PROJECTILE_PRESSURE) * 0.12;
      }
      if (action.tags().contains(AiActionDescriptor.Tag.CONTROL)) {
         multiplier *= 1.0 + own.strength(FactType.CONTROL) * 0.15;
      }
      if (action.tags().contains(AiActionDescriptor.Tag.PURSUIT)
         || action.tags().contains(AiActionDescriptor.Tag.GAP_CLOSER)
         || action.tags().contains(AiActionDescriptor.Tag.INTERCEPT)) {
         multiplier *= 1.0 + Math.max(own.strength(FactType.PURSUIT), own.strength(FactType.GAP_CLOSE)) * 0.12;
      }
      if (visibleTarget.has(FactType.PROJECTILE_PRESSURE)
         && (action.tags().contains(AiActionDescriptor.Tag.GAP_CLOSER)
            || action.tags().contains(AiActionDescriptor.Tag.INTERCEPT))) multiplier *= 1.12;
      return clampMultiplier(multiplier);
   }

   static double learnedDefenseMultiplier(AiActionDescriptor action, AiBlackboard.OpponentSnapshot knowledge,
                                          boolean targetMobile) {
      if (knowledge == null) return 1.0;
      double result = 1.0;
      double projectileNegation = knowledge.knownFactStrength(FactType.PROJECTILE_NEGATION);
      if (projectileNegation > 0.0) {
         if (action.tags().contains(AiActionDescriptor.Tag.PROJECTILE)) {
            result *= targetMobile ? 1.0 - projectileNegation * 0.7 : 1.05;
         }
         if (targetMobile && (action.tags().contains(AiActionDescriptor.Tag.CONTROL)
            || action.tags().contains(AiActionDescriptor.Tag.AREA))) result *= 1.0 + projectileNegation * 0.35;
         if (targetMobile && (action.tags().contains(AiActionDescriptor.Tag.MELEE)
            || action.tags().contains(AiActionDescriptor.Tag.GAP_CLOSER))) result *= 1.0 + projectileNegation * 0.2;
      }
      if (knowledge.knows(FactType.MAGIC_RESISTANCE) && action.tags().contains(AiActionDescriptor.Tag.AREA)) result *= 0.85;
      if ((knowledge.knows(FactType.SHIELD) || knowledge.knows(FactType.DAMAGE_THRESHOLD))
         && action.tags().contains(AiActionDescriptor.Tag.INTERRUPT)) result *= 1.2;
      if ((knowledge.knows(FactType.REVIVE) || knowledge.knows(FactType.REGENERATION))
         && action.tags().contains(AiActionDescriptor.Tag.FINISHER)) result *= 1.2;
      return clampMultiplier(result);
   }

   public static double targetAdjustment(LivingEntity self, LivingEntity target,
                                         AiBlackboard.OpponentSnapshot knowledge) {
      CombatCapabilitySnapshot own = ServantCapabilityResolver.resolve(self);
      CombatCapabilitySnapshot visibleTarget = ServantCapabilityResolver.resolveVisible(target);
      double score = 0.0;
      score += visibleTarget.strength(FactType.PROJECTILE_PRESSURE)
         * Math.max(own.strength(FactType.GAP_CLOSE), own.strength(FactType.PURSUIT)) * 12.0;
      if (knowledge != null && knowledge.knows(FactType.PROJECTILE_NEGATION)) {
         score += own.strength(FactType.CONTROL) * 18.0 + own.strength(FactType.MELEE_PRESSURE) * 10.0;
         score -= own.strength(FactType.PROJECTILE_PRESSURE) * 12.0;
      }
      if (knowledge != null && knowledge.knows(FactType.MAGIC_RESISTANCE)) {
         score -= own.strength(FactType.MAGIC_PRESSURE) * 10.0;
      }
      return Math.max(-32.0, Math.min(32.0, score));
   }

   public static boolean canIgnoreProjectile(LivingEntity defender, ProjectileThreatSensor.IncomingProjectile threat) {
      return defender != null && threat != null
         && CuChulainnCombatHelper.hasActiveProtectionFromArrows(defender)
         && !ProjectileThreatClassifier.bypassesProjectileNegation(threat.bypasses());
   }

   public static boolean negatesProjectileDamage(LivingEntity defender, net.minecraft.world.damagesource.DamageSource source) {
      return defender != null && source != null
         && source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile
         && projectile.getOwner() != defender
         && CuChulainnCombatHelper.hasActiveProtectionFromArrows(defender)
         && !ProjectileThreatClassifier.bypassesProjectileNegation(ProjectileThreatClassifier.classify(source));
   }

   public static double clampMultiplier(double value) {
      return Math.max(MIN_ACTION_MULTIPLIER, Math.min(MAX_ACTION_MULTIPLIER, value));
   }
}
