package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService;

/** Scores explicitly shared data-driven actions while legacy helpers remain compatible. */
public final class ServantActionPlanner {
   private ServantActionPlanner() { }

   @Nullable
   public static AiActionDescriptor select(ServantEntity entity, LivingEntity target, ServantCombatPhase phase,
                                           ServantActionProfile profile, AiBlackboard blackboard) {
      if (target == null || profile == null) return null;
      List<AiActionDescriptor> candidates = candidates(entity, target, phase, profile, blackboard);
      return candidates.isEmpty() ? null : candidates.getFirst();
   }

   public static List<AiActionDescriptor> candidates(ServantEntity entity, LivingEntity target,
                                                      ServantCombatPhase phase, ServantActionProfile profile,
                                                      AiBlackboard blackboard) {
      if (entity == null || target == null || profile == null) return List.of();
      double distance = entity.distanceTo(target);
      AiBlackboard.OpponentSnapshot memory = blackboard == null
         ? AiBlackboard.OpponentSnapshot.EMPTY
         : blackboard.opponent(target.getUUID());
      return profile.actions().stream()
         .filter(action -> ServantPlannedActionExecutor.canExecute(entity, target, action, entity.level().getGameTime()))
         .sorted(Comparator.comparingDouble((AiActionDescriptor action) ->
            utility(entity, target, phase, profile, action, distance, memory)).reversed())
         .toList();
   }

   private static double utility(ServantEntity entity, LivingEntity target, ServantCombatPhase phase,
                                 ServantActionProfile profile, AiActionDescriptor action, double distance,
                                 AiBlackboard.OpponentSnapshot memory) {
      double rangeCenter = (action.minimumRange() + action.maximumRange()) * 0.5;
      double score = 100.0 - Math.abs(distance - rangeCenter) * 5.0;
      double environmentComfort = entity.getPersistentData().getDouble("TypeMoonAiEnvironmentComfort");
      score += environmentComfort * 3.0;
      score -= action.manaCost() * 0.08 + action.staminaCost() * 0.12;
      score -= action.timing().windupTicks() * 0.25 + action.timing().recoveryTicks() * 0.18;
      score += action.threat().danger() * 4.0;
      if (memory.repeatCount() >= 2 && (action.tags().contains(AiActionDescriptor.Tag.INTERRUPT)
         || action.tags().contains(AiActionDescriptor.Tag.CONTROL)
         || action.tags().contains(AiActionDescriptor.Tag.GUARD)
         || action.tags().contains(AiActionDescriptor.Tag.EVADE))) {
         score += Math.min(32.0, memory.repeatCount() * 4.0);
      }
      if (memory.defendedActions() > 0 && !action.threat().blockable()) {
         score += Math.min(30.0, memory.defendedActions() * 5.0);
      }
      if (memory.observedDamage() >= entity.getMaxHealth() * 0.25F
         && (action.tags().contains(AiActionDescriptor.Tag.GUARD) || action.tags().contains(AiActionDescriptor.Tag.EVADE))) {
         score += 30.0;
      }
      if (ServantCombatMotionService.canPursue(entity, target)) {
         if (action.tags().contains(AiActionDescriptor.Tag.PURSUIT)
            || action.tags().contains(AiActionDescriptor.Tag.INTERCEPT)
            || action.tags().contains(AiActionDescriptor.Tag.ANTI_AIR)) score += 55.0;
         if (action.tags().contains(AiActionDescriptor.Tag.HEAL)
            || action.tags().contains(AiActionDescriptor.Tag.GUARD)) score -= 20.0;
      }
      if (ServantCombatMotionService.isRecovering(target)
         && (action.tags().contains(AiActionDescriptor.Tag.FINISHER)
            || action.tags().contains(AiActionDescriptor.Tag.LAUNCHER))) score -= 45.0;
      if (action.tags().contains(AiActionDescriptor.Tag.NOBLE_PHANTASM)) score += phase.ordinal() >= ServantCombatPhase.DECISIVE.ordinal() ? 45.0 : -80.0;
      if (action.tags().contains(AiActionDescriptor.Tag.HEAL)) score += entity.getHealth() < entity.getMaxHealth() * 0.45F ? 40.0 : -50.0;
      if (action.terrainTier().ordinal() >= net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.HEAVY.ordinal()
         && phase.ordinal() < ServantCombatPhase.DECISIVE.ordinal()) score -= 25.0;
      if (action.threat().collateralRadius() > 0.0) {
         double radius = action.threat().collateralRadius();
         long allies = entity.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius), entity::isAlliedTo).size();
         double caution = net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTacticalProfileResolver.resolve(entity).collateralCaution();
         double collateralMultiplier = 0.35 + caution * 1.3;
         score -= allies * 35.0 * collateralMultiplier;
      }
      if (!entity.level().isClientSide() && BattlefieldAreaService.isHostileArea(entity, target.position())) {
         if (action.tags().contains(AiActionDescriptor.Tag.CONTROL) || action.tags().contains(AiActionDescriptor.Tag.NOBLE_PHANTASM)) score += 28.0;
         else score -= 12.0;
      }
      if (target instanceof ServantEntity rival) {
         for (ServantActionProfile.RivalRule rule : profile.rivals()) {
            if (rule.opponentServant().equals(rival.getServantId()) && phase.ordinal() >= rule.minimumPhase().ordinal()) {
               score *= rule.actionWeights().getOrDefault(action.id().toString(), 1.0);
            }
         }
      }
      double matchup = CombatMatchupEvaluator.actionMultiplier(entity, target, action, memory);
      score += (matchup - 1.0) * 55.0;
      return score;
   }
}
