package net.xxxjk.TYPE_MOON_WORLD.servant.ai.module;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.MoralAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.PrincipleAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.SpecialTargetPrinciple;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class HostileTargetingModule implements ServantAiModule {
   private static final String LAST_TARGET_SCAN_TICK = "ServantLastTargetScanTick";
   private static final int TARGET_SCAN_INTERVAL_TICKS = 5;
   private static final int AGGRESSION_MEMORY_TICKS = 200;
   private static final double MAX_TARGET_SCAN_RANGE = 32.0;
   private static final double MAX_LAGUZ_TARGET_SCAN_RANGE = 48.0;
   private static final int MAX_TARGET_CANDIDATES_PER_SCAN = 96;

   @Override
   public void tick(ServantEntity entity, ServantAiContext context) {
      boolean laguzActive = CuChulainnCombatHelper.isLaguzActive(entity);
      double profileRange = Math.max(16.0, context.behaviorProfile().aggressionRange());
      boolean emiyaArcher = EmiyaArcherEntity.SERVANT_KEY.equals(entity.getServantId());
      boolean clairvoyanceActive = emiyaArcher && entity.getPersistentData().getBoolean("ClairvoyanceActive");
      double cap = laguzActive ? MAX_LAGUZ_TARGET_SCAN_RANGE : MAX_TARGET_SCAN_RANGE;
      double scaledRange = laguzActive ? profileRange * 1.5 : profileRange;
      if (emiyaArcher) {
         double multiplier = clairvoyanceActive ? 4.0 : 2.0;
         cap = MAX_TARGET_SCAN_RANGE * multiplier;
         scaledRange = profileRange * multiplier;
      }
      double aggressionRange = Math.min(cap, scaledRange);
      MoralAxis morality = entity.getMoralAxis();
      PrincipleAxis principle = entity.getPrincipleAxis();
      LivingEntity currentTarget = entity.getTarget();
      ServantFaction faction = entity.getDefinition() != null
         ? entity.getDefinition().faction()
         : ServantFaction.HUMAN;
      if (isValidCurrentTarget(entity, currentTarget, faction, aggressionRange, morality, principle)) {
         return;
      }

      long gameTick = context.gameTick();
      long lastScanTick = entity.getPersistentData().getLong(LAST_TARGET_SCAN_TICK);
      if (gameTick - lastScanTick < TARGET_SCAN_INTERVAL_TICKS) {
         return;
      }
      entity.getPersistentData().putLong(LAST_TARGET_SCAN_TICK, gameTick);

      LivingEntity bestTarget = null;
      double bestScore = Double.NEGATIVE_INFINITY;
      TargetScan scan = createTargetScan(entity, aggressionRange);
      int checked = 0;

      for (LivingEntity le : scan.nearbyLiving()) {
         if (++checked > MAX_TARGET_CANDIDATES_PER_SCAN) {
            break;
         }
         if (!isHostileTo(le, faction, entity, morality, principle, scan)) {
            continue;
         }

         double score = scoreTarget(entity, le, aggressionRange, morality, principle, scan);
         if (score > bestScore) {
            bestScore = score;
            bestTarget = le;
         }
      }

      if (bestTarget != null) {
         entity.setTarget(bestTarget);
      } else if (currentTarget != null) {
         entity.setTarget(null);
      }
   }

   private static boolean isValidCurrentTarget(
      ServantEntity entity,
      LivingEntity target,
      ServantFaction faction,
      double aggressionRange,
      MoralAxis morality,
      PrincipleAxis principle
   ) {
      if (target == null || target.isDeadOrDying() || !target.isAlive()) {
         return false;
      }
      if (EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      double maxDistanceSqr = aggressionRange * aggressionRange * 1.35;
      double targetDistanceSqr = entity.distanceToSqr(target);
      if (targetDistanceSqr > maxDistanceSqr) {
         return false;
      }

      if (!entity.getSensing().hasLineOfSight(target) && targetDistanceSqr >= 16.0) {
         return false;
      }

      if (target.isAlliedTo(entity)
         || entity.isAlliedTo(target)
         || CursedArmHassanCombatHelper.shouldAvoidPassiveFellowHassanTarget(entity, target)
         || CursedArmHassanCombatHelper.refusesToHarm(entity, target)
         || isForbiddenByPrinciple(entity, target)) {
         return false;
      }
      if (isImmediateThreat(entity, target)) {
         return true;
      }
      if (target instanceof ServantEntity otherServant) {
         ServantFaction otherFaction = otherServant.getDefinition() != null ? otherServant.getDefinition().faction() : ServantFaction.HUMAN;
         return isFactionEnemy(faction, otherFaction)
            || morality == MoralAxis.EVIL
            || principle == PrincipleAxis.CHAOTIC
            || otherServant.getMoralAxis() == MoralAxis.EVIL && morality == MoralAxis.GOOD;
      }
      if (target instanceof Player) {
         return morality == MoralAxis.EVIL || principle == PrincipleAxis.CHAOTIC || principle == PrincipleAxis.NEUTRAL;
      }
      if (isVanillaHostile(target)) {
         return morality != MoralAxis.EVIL || principle != PrincipleAxis.ORDERLY || isImmediateThreat(entity, target);
      }
      return isFriendlyCreature(target) && morality == MoralAxis.EVIL;
   }

   private static double scoreTarget(
      ServantEntity entity,
      LivingEntity target,
      double aggressionRange,
      MoralAxis morality,
      PrincipleAxis principle,
      TargetScan scan
   ) {
      double distanceSqr = entity.distanceToSqr(target);
      double score = aggressionRange * aggressionRange - distanceSqr;

      if (isImmediateThreat(entity, target)) score += 220.0;
      if (hasAttackedProtectedEntity(entity, target, morality, scan)) score += 180.0;
      if (entity.getSensing().hasLineOfSight(target)) {
         score += 40.0;
      }
      if (isVanillaHostile(target)) {
         score += morality == MoralAxis.GOOD ? 55.0 : 20.0;
      }
      if (isFriendlyCreature(target)) {
         score += morality == MoralAxis.EVIL ? 60.0 : -40.0;
      }
      if (target instanceof Player) {
         score += 25.0;
      }
      if (target instanceof ServantEntity otherServant) {
         score += otherServant.getMoralAxis() == MoralAxis.EVIL && morality == MoralAxis.GOOD ? 20.0 : 0.0;
         score += principle == PrincipleAxis.CHAOTIC ? entity.getRandom().nextDouble() * 20.0 : 0.0;
      }
      if (target.getHealth() < target.getMaxHealth() * 0.35F && principle != PrincipleAxis.ORDERLY) {
         score += 15.0;
      }

      return score;
   }

   private static boolean isHostileTo(
      LivingEntity other,
      ServantFaction myFaction,
      ServantEntity self,
      MoralAxis morality,
      PrincipleAxis principle,
      TargetScan scan
   ) {
      if (other == null
         || !other.isAlive()
         || other == self
         || EntityUtils.isImmunePlayerTarget(other)
         || other.isAlliedTo(self)
         || self.isAlliedTo(other)
         || CursedArmHassanCombatHelper.shouldAvoidPassiveFellowHassanTarget(self, other)
         || CursedArmHassanCombatHelper.refusesToHarm(self, other)
         || isForbiddenByPrinciple(self, other)) {
         return false;
      }

      if (principle == PrincipleAxis.CHAOTIC && morality == MoralAxis.EVIL) {
         return true;
      }
      if (principle == PrincipleAxis.CHAOTIC && morality == MoralAxis.NEUTRAL) {
         return true;
      }

      if (isImmediateThreat(self, other)) {
         return true;
      }

      if (hasAttackedProtectedEntity(self, other, morality, scan)) {
         return true;
      }

      return switch (morality) {
         case GOOD -> isGoodHostile(self, other, principle);
         case NEUTRAL -> isNeutralHostile(self, other, principle, scan);
         case EVIL -> isEvilHostile(self, other, principle, myFaction, scan);
      };
   }

   private static boolean isGoodHostile(ServantEntity self, LivingEntity other, PrincipleAxis principle) {
      if (isVanillaHostile(other)) {
         return true;
      }
      if (other instanceof ServantEntity otherServant) {
         return otherServant.getMoralAxis() == MoralAxis.EVIL && principle != PrincipleAxis.ORDERLY && isLowHealth(self);
      }
      return false;
   }

   private static boolean isNeutralHostile(ServantEntity self, LivingEntity other, PrincipleAxis principle, TargetScan scan) {
      if (principle == PrincipleAxis.NEUTRAL && isLowHealth(self)) {
         return isVanillaHostile(other) || other instanceof ServantEntity || other instanceof Player;
      }
      if (principle == PrincipleAxis.CHAOTIC) {
         return true;
      }
      return hasAttackedNearbyServant(other, scan);
   }

   private static boolean isEvilHostile(
      ServantEntity self,
      LivingEntity other,
      PrincipleAxis principle,
      ServantFaction myFaction,
      TargetScan scan
   ) {
      if (isFriendlyCreature(other)) {
         return true;
      }
      if (isVanillaHostile(other)) {
         return principle != PrincipleAxis.ORDERLY || isImmediateThreat(self, other);
      }
      if (other instanceof Player) {
         return principle == PrincipleAxis.CHAOTIC || principle == PrincipleAxis.NEUTRAL && hasAttackedEnemyMob(other, self, scan);
      }
      if (other instanceof ServantEntity otherServant) {
         if (principle == PrincipleAxis.ORDERLY) {
            ServantFaction otherFaction = otherServant.getDefinition() != null ? otherServant.getDefinition().faction() : ServantFaction.HUMAN;
            return isFactionEnemy(myFaction, otherFaction) || otherServant.getMoralAxis() == MoralAxis.GOOD;
         }
         return true;
      }
      return false;
   }

   private static boolean isImmediateThreat(ServantEntity self, LivingEntity candidate) {
      if (self.getLastHurtByMob() == candidate && self.tickCount - self.getLastHurtByMobTimestamp() <= AGGRESSION_MEMORY_TICKS) {
         return true;
      }
      return candidate instanceof Mob mob && mob.getTarget() == self;
   }

   private static boolean hasAttackedProtectedEntity(ServantEntity self, LivingEntity candidate, MoralAxis morality, TargetScan scan) {
      if (hasAttackedNearbyServant(candidate, scan)) {
         return true;
      }
      if (morality != MoralAxis.GOOD) {
         return false;
      }
      for (LivingEntity nearby : scan.nearbyLiving()) {
         if (nearby == self || nearby == candidate || !isFriendlyCreature(nearby)) {
            continue;
         }
         if (wasRecentlyAttackedBy(nearby, candidate)) {
            return true;
         }
      }
      return false;
   }

   private static boolean hasAttackedNearbyServant(LivingEntity candidate, TargetScan scan) {
      for (ServantEntity nearbyServant : scan.nearbyServants()) {
         if (wasRecentlyAttackedBy(nearbyServant, candidate)) {
            return true;
         }
      }
      return false;
   }

   private static boolean hasAttackedEnemyMob(LivingEntity candidate, ServantEntity self, TargetScan scan) {
      for (LivingEntity nearby : scan.nearbyLiving()) {
         if (nearby == self || nearby == candidate || !isVanillaHostile(nearby)) {
            continue;
         }
         if (wasRecentlyAttackedBy(nearby, candidate)) {
            return true;
         }
      }
      return false;
   }

   private static boolean wasRecentlyAttackedBy(LivingEntity victim, LivingEntity attacker) {
      return victim.getLastHurtByMob() == attacker && victim.tickCount - victim.getLastHurtByMobTimestamp() <= AGGRESSION_MEMORY_TICKS;
   }

   private static TargetScan createTargetScan(ServantEntity self, double aggressionRange) {
      List<LivingEntity> nearbyLiving = self.level().getEntitiesOfClass(
         LivingEntity.class,
         self.getBoundingBox().inflate(aggressionRange),
         target -> target != self && target.isAlive()
      );
      List<ServantEntity> nearbyServants = new ArrayList<>();
      for (LivingEntity living : nearbyLiving) {
         if (living instanceof ServantEntity servant) {
            nearbyServants.add(servant);
         }
      }
      return new TargetScan(nearbyLiving, nearbyServants);
   }

   private record TargetScan(List<LivingEntity> nearbyLiving, List<ServantEntity> nearbyServants) {
   }

   private static boolean isFriendlyCreature(LivingEntity other) {
      return other instanceof AbstractVillager
         || other instanceof WanderingTrader
         || other instanceof Animal
         || other instanceof TamableAnimal;
   }

   private static boolean isVanillaHostile(LivingEntity other) {
      return other instanceof Enemy;
   }

   private static boolean isLowHealth(ServantEntity self) {
      return self.getHealth() / Math.max(1.0F, self.getMaxHealth()) <= 0.35F;
   }

   private static boolean isForbiddenByPrinciple(ServantEntity self, LivingEntity target) {
      if (self.hasSpecialTargetPrinciple(SpecialTargetPrinciple.SPARE_CANINES) && isCanine(target)) {
         return true;
      }
      return self.hasSpecialTargetPrinciple(SpecialTargetPrinciple.SPARE_PIGS) && isPigLike(target);
   }

   private static boolean isCanine(LivingEntity target) {
      if (target instanceof Wolf) {
         return true;
      }
      ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
      String path = key == null ? "" : key.getPath();
      return path.contains("wolf") || path.contains("dog") || path.contains("canine");
   }

   private static boolean isPigLike(LivingEntity target) {
      if (target instanceof Pig || target instanceof Hoglin || target instanceof Zoglin) {
         return true;
      }
      ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
      String path = key == null ? "" : key.getPath();
      return path.contains("pig") || path.contains("hog");
   }

   private static boolean isFactionEnemy(ServantFaction a, ServantFaction b) {
      if (a == b) return false;
      return switch (a) {
         case HEAVEN -> b == ServantFaction.BEAST;
         case EARTH -> b == ServantFaction.BEAST;
         case HUMAN -> b == ServantFaction.BEAST;
         case STAR -> b == ServantFaction.BEAST || b == ServantFaction.HEAVEN;
         case BEAST -> true;
      };
   }
}
