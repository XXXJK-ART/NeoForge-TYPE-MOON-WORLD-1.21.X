package net.xxxjk.TYPE_MOON_WORLD.servant.fanatic;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.FanaticAssassinEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.FanaticAssassinJinnEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import org.joml.Vector3f;

public final class FanaticAssassinCombatHelper {
   public static final TagKey<MobEffect> MENTAL_EFFECTS = TagKey.create(
      Registries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "fanatic_mental_effects"));
   private static final TagKey<net.minecraft.world.entity.EntityType<?>> DEAD_APOSTLES = TagKey.create(
      Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "dead_apostles"));
   private static final DustParticleOptions SENSE_PURPLE = dust(0.45F, 0.30F, 1.0F, 1.0F);
   private static final DustParticleOptions SENSE_BLUE = dust(0.35F, 0.65F, 1.0F, 0.8F);
   private static final DustParticleOptions HAIR_BLACK = dust(0.025F, 0.025F, 0.035F, 0.75F);
   private static final DustParticleOptions HAIR_SILVER = dust(0.88F, 0.90F, 0.94F, 0.65F);
   private static final DustParticleOptions MARROW_PURPLE = dust(0.92F, 0.24F, 0.92F, 1.0F);
   private static final DustParticleOptions MARROW_DARK = dust(0.35F, 0.08F, 0.42F, 0.75F);
   private static final DustParticleOptions COMPUTER_RED = dust(0.85F, 0.035F, 0.015F, 1.1F);
   private static final DustParticleOptions COMPUTER_DARK = dust(0.32F, 0.008F, 0.005F, 0.8F);
   private static final DustParticleOptions HEART_PURPLE = dust(0.38F, 0.05F, 0.42F, 1.0F);
   private static final DustParticleOptions HEART_BRIGHT = dust(0.85F, 0.20F, 0.72F, 0.9F);
   private static final DustParticleOptions HEART_RED = dust(0.65F, 0.015F, 0.03F, 1.1F);
   private static final DustParticleOptions CRYSTAL = dust(0.82F, 0.92F, 1.0F, 1.0F);
   private static final DustParticleOptions STEEL = dust(0.48F, 0.53F, 0.60F, 0.75F);
   private static final DustParticleOptions TOXIN = dust(0.58F, 0.16F, 0.67F, 1.0F);
   private static final DustParticleOptions TOXIN_BRIGHT = dust(0.82F, 0.38F, 0.88F, 0.75F);
   private static final DustParticleOptions JINN_GRAY = dust(0.48F, 0.48F, 0.53F, 1.1F);
   private static final DustParticleOptions JINN_PURPLE = dust(0.44F, 0.32F, 0.67F, 0.8F);
   private static final String BUSY_UNTIL = "FanaticBusyUntil";
   private static final String LAST_BASIC = "FanaticLastBasicAttack";
   private static final String RETREATING = "FanaticRetreating";
   private static final String COMBO_COUNT = "FanaticComboCount";
   private static final String COMBO_PREVIOUS = "FanaticComboPrevious";

   private FanaticAssassinCombatHelper() {
   }

   public static void tickTargeting(FanaticAssassinEntity entity, ServantAiContext context) {
      long now = context.gameTick();
      LivingEntity current = entity.getTarget();
      if (current != null && !current.isAlive()) {
         ServantVoiceHelper.tryPlayVictory(entity, current);
         entity.setTarget(null);
         current = null;
      }
      if (current != null && isValidTarget(entity, current)) return;
      if (now - entity.getPersistentData().getLong("FanaticLastTargetScan") < 10L) return;
      entity.getPersistentData().putLong("FanaticLastTargetScan", now);

      double radius = entity.isNervesActive() ? 40.0 : 32.0;
      LivingEntity best = entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius),
         candidate -> isValidTarget(entity, candidate)
            && (entity.isNervesActive() || entity.distanceToSqr(candidate) <= 16.0 || entity.getSensing().hasLineOfSight(candidate)))
         .stream().max(Comparator.comparingDouble(candidate -> targetScore(entity, candidate))).orElse(null);
      // A scan can legitimately miss a target for a few ticks behind terrain.
      // Keep the combat memory until it is actually dead or stale instead of
      // dropping the target and restarting the encounter.
      if (best != null) {
         entity.setTarget(best);
      } else if (current != null && current.isAlive()
         && now - entity.getPersistentData().getLong("FanaticLastCombatTick") <= 240L) {
         entity.setTarget(current);
      }
   }

   public static void tick(FanaticAssassinEntity entity, ServantAiContext context) {
      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive() || !isValidTarget(entity, target)) {
         entity.setTarget(null);
         entity.getPersistentData().remove(RETREATING);
         clearCombo(entity);
         return;
      }
      long now = context.gameTick();
      entity.getPersistentData().putLong("FanaticLastCombatTick", now);
      entity.getLookControl().setLookAt(target, 45.0F, 45.0F);

      // MP only gates techniques.  The assassin must keep closing and using its
      // poison blade/basic attack when empty instead of entering a retreat loop.
      entity.getPersistentData().remove(RETREATING);
      if (now < entity.getPersistentData().getLong(BUSY_UNTIL)) return;
      if (tryConcealedApproach(entity, target, now)) return;
      if (continueCombo(entity, target, now)) return;

      double distance = entity.distanceTo(target);
      boolean lineOfSight = entity.getSensing().hasLineOfSight(target) || entity.isNervesActive();
      List<LivingEntity> nearbyEnemies = enemiesAround(entity, FanaticAssassinRules.MARROW_RADIUS);
      FanaticAssassinRules.TechniqueDecision decision = FanaticAssassinRules.chooseTechnique(
         distance <= FanaticAssassinRules.COMPUTER_RANGE
            && canCast(entity, "Computer", FanaticAssassinRules.COMPUTER_MP, FanaticAssassinRules.COMPUTER_COOLDOWN, now),
         entity.getHealth() <= entity.getMaxHealth() * 0.60F && !entity.isCrystalArmorActive()
            && canCast(entity, "Temperature", FanaticAssassinRules.TEMPERATURE_MP, FanaticAssassinRules.TEMPERATURE_COOLDOWN, now),
         nearbyEnemies.size() >= 3
            && canCast(entity, "Marrow", FanaticAssassinRules.MARROW_MP, FanaticAssassinRules.MARROW_COOLDOWN, now),
         distance <= FanaticAssassinRules.HEARTBEAT_RANGE && lineOfSight && isStrongTarget(target)
            && canCast(entity, "Heartbeat", FanaticAssassinRules.HEARTBEAT_MP, FanaticAssassinRules.HEARTBEAT_COOLDOWN, now),
         !lineOfSight && !entity.isNervesActive()
            && canCast(entity, "Nerves", FanaticAssassinRules.NERVES_MP, FanaticAssassinRules.NERVES_COOLDOWN, now),
         (distance > 8.0 || nearbyEnemies.size() >= 2) && !hasOwnedJinn(entity)
            && canCast(entity, "Jinn", FanaticAssassinRules.JINN_MP, FanaticAssassinRules.JINN_COOLDOWN, now),
         distance > 2.0 && distance <= FanaticAssassinRules.HAIR_RANGE && lineOfSight
            && canCast(entity, "Hair", FanaticAssassinRules.HAIR_MP, FanaticAssassinRules.HAIR_COOLDOWN, now),
         distance <= 3.0 && !entity.isToxinStanceActive()
            && canCast(entity, "Toxin", FanaticAssassinRules.TOXIN_MP, FanaticAssassinRules.TOXIN_COOLDOWN, now)
      );
      switch (decision) {
         case COMPUTER -> castComputer(entity, target, now);
         case TEMPERATURE -> castTemperature(entity, now);
         case MARROW -> castMarrow(entity, nearbyEnemies, now);
         case HEARTBEAT -> castHeartbeat(entity, target, now);
         case NERVES -> castNerves(entity, now);
         case JINN -> castJinn(entity, now);
         case HAIR -> castHair(entity, target, now);
         case TOXIN -> castToxin(entity, now);
         case BASIC -> {
         }
      }
      if (decision != FanaticAssassinRules.TechniqueDecision.BASIC) {
         startCombo(entity, decision);
         return;
      }

      if (distance > 2.2) {
         ServantNavigationHelper.moveToTargetThrottled(entity, target, 1.28, now,
            ServantNavigationHelper.SHORT_REPATH_INTERVAL, 0.7, "FanaticChasePath");
      } else if (now - entity.getPersistentData().getLong(LAST_BASIC) >= 10L && !entity.isPerformingAction()) {
         entity.getPersistentData().putLong(LAST_BASIC, now);
         entity.triggerNamedActionAnimation("melee");
         entity.doHurtTarget(target);
      }
   }

   private static void castHeartbeat(FanaticAssassinEntity entity, LivingEntity target, long now) {
      commit(entity, "Heartbeat", FanaticAssassinRules.HEARTBEAT_MP, now, FanaticAssassinEntity.TECHNIQUE_HEARTBEAT, "heartbeat", 20);
      ServantVoiceHelper.tryPlayFanaticTechnique(entity, FanaticAssassinEntity.TECHNIQUE_HEARTBEAT);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().source(FanaticDamageTypes.HEARTBEAT, entity), heartbeatDamageFor(target));
      target.addEffect(new MobEffectInstance(ModMobEffects.FANATIC_WOUNDED, FanaticAssassinRules.WOUNDED_DURATION, 0, false, true, true), entity);
      if (entity.level() instanceof ServerLevel level) {
         spawnHeartbeatFx(level, entity, target);
      }
   }

   private static void castMarrow(FanaticAssassinEntity entity, List<LivingEntity> enemies, long now) {
      commit(entity, "Marrow", FanaticAssassinRules.MARROW_MP, now, FanaticAssassinEntity.TECHNIQUE_MARROW, "marrow", 12);
      ServantVoiceHelper.tryPlayFanaticTechnique(entity, FanaticAssassinEntity.TECHNIQUE_MARROW);
      for (LivingEntity target : enemies) {
         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().source(FanaticDamageTypes.MARROW, entity), FanaticAssassinRules.MARROW_DAMAGE);
         target.addEffect(new MobEffectInstance(ModMobEffects.REVERSE_MOVEMENT, FanaticAssassinRules.CONFUSION_DURATION, 0, false, true, true), entity);
         target.addEffect(new MobEffectInstance(ModMobEffects.FANATIC_CIRCUIT_DISRUPTION,
            FanaticAssassinRules.CIRCUIT_DISRUPTION_DURATION, 0, false, true, true), entity);
      }
      if (entity.level() instanceof ServerLevel level) {
         spawnMarrowFx(level, entity, enemies);
      }
   }

   private static void castHair(FanaticAssassinEntity entity, LivingEntity primaryTarget, long now) {
      commit(entity, "Hair", FanaticAssassinRules.HAIR_MP, now, FanaticAssassinEntity.TECHNIQUE_HAIR, "hair", 12);
      ServantVoiceHelper.tryPlayFanaticTechnique(entity, FanaticAssassinEntity.TECHNIQUE_HAIR);
      // LookControl applies its requested rotation later in the tick. Aim from the
      // actual target position so the first cast cannot use the previous heading.
      Vec3 forward = directionToTarget(entity, primaryTarget);
      for (LivingEntity target : enemiesAround(entity, FanaticAssassinRules.HAIR_RANGE)) {
         Vec3 to = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
         if (to.lengthSqr() <= 1.0E-5 || forward.dot(to.normalize()) < Math.cos(Math.PI / 4.0)) continue;
         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().mobAttack(entity), FanaticAssassinRules.HAIR_DAMAGE);
         if (entity.isToxinStanceActive()) applyToxin(entity, target);
      }
      if (entity.level() instanceof ServerLevel level) {
         spawnHairFx(level, entity, forward);
      }
   }

   private static void castTemperature(FanaticAssassinEntity entity, long now) {
      commit(entity, "Temperature", FanaticAssassinRules.TEMPERATURE_MP, now,
         FanaticAssassinEntity.TECHNIQUE_TEMPERATURE, "temperature", FanaticAssassinRules.TEMPERATURE_DURATION);
      entity.activateCrystalArmor(now + FanaticAssassinRules.TEMPERATURE_DURATION);
      ServantVoiceHelper.tryPlayFanaticTechnique(entity, FanaticAssassinEntity.TECHNIQUE_TEMPERATURE);
      if (entity.level() instanceof ServerLevel level) {
         spawnTemperatureActivation(level, entity);
      }
   }

   private static void castNerves(FanaticAssassinEntity entity, long now) {
      commit(entity, "Nerves", FanaticAssassinRules.NERVES_MP, now,
         FanaticAssassinEntity.TECHNIQUE_NERVES, "nerves", 10);
      entity.getPersistentData().putLong("FanaticNervesUntil", now + FanaticAssassinRules.NERVES_DURATION);
      ServantVoiceHelper.tryPlayFanaticTechnique(entity, FanaticAssassinEntity.TECHNIQUE_NERVES);
      if (entity.level() instanceof ServerLevel level) {
         spawnNervesFx(level, entity);
      }
   }

   private static void castComputer(FanaticAssassinEntity entity, LivingEntity target, long now) {
      commit(entity, "Computer", FanaticAssassinRules.COMPUTER_MP, now,
         FanaticAssassinEntity.TECHNIQUE_COMPUTER, "computer", 16);
      ServantVoiceHelper.tryPlayFanaticTechnique(entity, FanaticAssassinEntity.TECHNIQUE_COMPUTER);
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().source(FanaticDamageTypes.COMPUTER, entity), FanaticAssassinRules.COMPUTER_DAMAGE);
      target.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, FanaticAssassinRules.STUN_DURATION, 0, false, true, true), entity);
      AABB blast = new AABB(center, center).inflate(FanaticAssassinRules.COMPUTER_SPLASH_RADIUS);
      for (LivingEntity bystander : entity.level().getEntitiesOfClass(LivingEntity.class, blast,
         living -> living != entity && living != target && living.isAlive())) {
         float splash = FanaticAssassinRules.computerSplashDamage(bystander.position().add(0.0, bystander.getBbHeight() * 0.5, 0.0).distanceTo(center));
         if (splash <= 0.0F) continue;
         bystander.invulnerableTime = 0;
         bystander.hurt(entity.damageSources().source(FanaticDamageTypes.COMPUTER_SPLASH, entity), splash);
      }
      entity.hurt(entity.damageSources().magic(), FanaticAssassinRules.COMPUTER_BACKLASH);
      if (entity.level() instanceof ServerLevel level) {
         spawnComputerFx(level, target, center);
      }
   }

   private static void castToxin(FanaticAssassinEntity entity, long now) {
      commit(entity, "Toxin", FanaticAssassinRules.TOXIN_MP, now,
         FanaticAssassinEntity.TECHNIQUE_TOXIN, "toxin", FanaticAssassinRules.TOXIN_STANCE_DURATION);
      entity.getPersistentData().putLong("FanaticToxinUntil", now + FanaticAssassinRules.TOXIN_STANCE_DURATION);
      if (entity.level() instanceof ServerLevel level) {
         spawnToxinActivation(level, entity);
      }
   }

   private static void castJinn(FanaticAssassinEntity entity, long now) {
      commit(entity, "Jinn", FanaticAssassinRules.JINN_MP, now,
         FanaticAssassinEntity.TECHNIQUE_JINN, "jinn", 16);
      if (!(entity.level() instanceof ServerLevel level)) return;
      FanaticAssassinJinnEntity jinn = ModEntities.FANATIC_ASSASSIN_JINN.get().create(level);
      if (jinn == null) return;
      Vec3 forward = directionToTarget(entity, entity.getTarget());
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      float yaw = (float)Math.toDegrees(Math.atan2(-forward.x, forward.z));
      jinn.moveTo(entity.getX() + side.x * 1.5, entity.getY(), entity.getZ() + side.z * 1.5, yaw, 0.0F);
      jinn.initialize(entity, now + FanaticAssassinRules.JINN_LIFETIME);
      level.addFreshEntity(jinn);
      spawnJinnSummonFx(level, entity, jinn);
   }

   private static void startCombo(FanaticAssassinEntity entity,
                                  FanaticAssassinRules.TechniqueDecision opener) {
      clearCombo(entity);
      if (!FanaticAssassinRules.shouldAttemptCombo(entity.getRandom().nextFloat())) return;
      entity.getPersistentData().putInt(COMBO_COUNT, 1);
      entity.getPersistentData().putString(COMBO_PREVIOUS, opener.name());
   }

   private static boolean continueCombo(FanaticAssassinEntity entity, LivingEntity target, long now) {
      if (!entity.getPersistentData().contains(COMBO_COUNT)) return false;
      int techniquesCast = entity.getPersistentData().getInt(COMBO_COUNT);
      if (!FanaticAssassinRules.canContinueCombo(techniquesCast)) {
         clearCombo(entity);
         return false;
      }

      FanaticAssassinRules.TechniqueDecision previous;
      try {
         previous = FanaticAssassinRules.TechniqueDecision.valueOf(
            entity.getPersistentData().getString(COMBO_PREVIOUS));
      } catch (IllegalArgumentException exception) {
         clearCombo(entity);
         return false;
      }

      FanaticAssassinRules.TechniqueDecision next = chooseComboTechnique(entity, target, previous, now);
      if (next == FanaticAssassinRules.TechniqueDecision.BASIC) {
         clearCombo(entity);
         return false;
      }
      castComboTechnique(entity, target, next, now);
      techniquesCast++;
      if (FanaticAssassinRules.canContinueCombo(techniquesCast)) {
         entity.getPersistentData().putInt(COMBO_COUNT, techniquesCast);
         entity.getPersistentData().putString(COMBO_PREVIOUS, next.name());
      } else {
         clearCombo(entity);
      }
      return true;
   }

   private static void clearCombo(FanaticAssassinEntity entity) {
      entity.getPersistentData().remove(COMBO_COUNT);
      entity.getPersistentData().remove(COMBO_PREVIOUS);
   }

   private static FanaticAssassinRules.TechniqueDecision chooseComboTechnique(
      FanaticAssassinEntity entity, LivingEntity target,
      FanaticAssassinRules.TechniqueDecision previous, long now) {
      boolean targetAlive = target != null && target.isAlive();
      double distance = targetAlive ? entity.distanceTo(target) : Double.MAX_VALUE;
      boolean lineOfSight = targetAlive && (entity.getSensing().hasLineOfSight(target) || entity.isNervesActive());
      List<LivingEntity> nearbyEnemies = enemiesAround(entity, FanaticAssassinRules.MARROW_RADIUS);

      // Rotate the first choice after the opener so combos vary while still
      // respecting range, ownership, MP and each technique's independent cooldown.
      FanaticAssassinRules.TechniqueDecision[] order = switch (previous) {
         case COMPUTER -> new FanaticAssassinRules.TechniqueDecision[] {
            FanaticAssassinRules.TechniqueDecision.TOXIN, FanaticAssassinRules.TechniqueDecision.HEARTBEAT,
            FanaticAssassinRules.TechniqueDecision.MARROW, FanaticAssassinRules.TechniqueDecision.HAIR,
            FanaticAssassinRules.TechniqueDecision.TEMPERATURE, FanaticAssassinRules.TechniqueDecision.NERVES,
            FanaticAssassinRules.TechniqueDecision.JINN
         };
         case TOXIN -> new FanaticAssassinRules.TechniqueDecision[] {
            FanaticAssassinRules.TechniqueDecision.HAIR, FanaticAssassinRules.TechniqueDecision.COMPUTER,
            FanaticAssassinRules.TechniqueDecision.HEARTBEAT, FanaticAssassinRules.TechniqueDecision.MARROW,
            FanaticAssassinRules.TechniqueDecision.JINN, FanaticAssassinRules.TechniqueDecision.TEMPERATURE,
            FanaticAssassinRules.TechniqueDecision.NERVES
         };
         default -> new FanaticAssassinRules.TechniqueDecision[] {
            FanaticAssassinRules.TechniqueDecision.COMPUTER, FanaticAssassinRules.TechniqueDecision.HEARTBEAT,
            FanaticAssassinRules.TechniqueDecision.MARROW, FanaticAssassinRules.TechniqueDecision.HAIR,
            FanaticAssassinRules.TechniqueDecision.TOXIN, FanaticAssassinRules.TechniqueDecision.JINN,
            FanaticAssassinRules.TechniqueDecision.TEMPERATURE, FanaticAssassinRules.TechniqueDecision.NERVES
         };
      };
      for (FanaticAssassinRules.TechniqueDecision candidate : order) {
         if (comboTechniqueAvailable(entity, target, targetAlive, distance, lineOfSight, nearbyEnemies, candidate, now)) {
            return candidate;
         }
      }
      return FanaticAssassinRules.TechniqueDecision.BASIC;
   }

   private static boolean comboTechniqueAvailable(FanaticAssassinEntity entity, LivingEntity target,
                                                   boolean targetAlive,
                                                   double distance, boolean lineOfSight,
                                                   List<LivingEntity> nearbyEnemies,
                                                   FanaticAssassinRules.TechniqueDecision technique, long now) {
      return switch (technique) {
         case COMPUTER -> targetAlive && distance <= FanaticAssassinRules.COMPUTER_RANGE
            && canCast(entity, "Computer", FanaticAssassinRules.COMPUTER_MP, FanaticAssassinRules.COMPUTER_COOLDOWN, now);
         case HEARTBEAT -> targetAlive && distance <= FanaticAssassinRules.HEARTBEAT_RANGE && lineOfSight
            && isStrongTarget(target)
            && canCast(entity, "Heartbeat", FanaticAssassinRules.HEARTBEAT_MP, FanaticAssassinRules.HEARTBEAT_COOLDOWN, now);
         case MARROW -> !nearbyEnemies.isEmpty()
            && canCast(entity, "Marrow", FanaticAssassinRules.MARROW_MP, FanaticAssassinRules.MARROW_COOLDOWN, now);
         case HAIR -> targetAlive && distance <= FanaticAssassinRules.HAIR_RANGE && lineOfSight
            && canCast(entity, "Hair", FanaticAssassinRules.HAIR_MP, FanaticAssassinRules.HAIR_COOLDOWN, now);
         case TOXIN -> !entity.isToxinStanceActive()
            && canCast(entity, "Toxin", FanaticAssassinRules.TOXIN_MP, FanaticAssassinRules.TOXIN_COOLDOWN, now);
         case JINN -> targetAlive && !hasOwnedJinn(entity)
            && canCast(entity, "Jinn", FanaticAssassinRules.JINN_MP, FanaticAssassinRules.JINN_COOLDOWN, now);
         case TEMPERATURE -> !entity.isCrystalArmorActive()
            && canCast(entity, "Temperature", FanaticAssassinRules.TEMPERATURE_MP, FanaticAssassinRules.TEMPERATURE_COOLDOWN, now);
         case NERVES -> !entity.isNervesActive()
            && canCast(entity, "Nerves", FanaticAssassinRules.NERVES_MP, FanaticAssassinRules.NERVES_COOLDOWN, now);
         default -> false;
      };
   }

   private static void castComboTechnique(FanaticAssassinEntity entity, LivingEntity target,
                                          FanaticAssassinRules.TechniqueDecision technique, long now) {
      switch (technique) {
         case COMPUTER -> castComputer(entity, target, now);
         case HEARTBEAT -> castHeartbeat(entity, target, now);
         case MARROW -> castMarrow(entity, enemiesAround(entity, FanaticAssassinRules.MARROW_RADIUS), now);
         case HAIR -> castHair(entity, target, now);
         case TOXIN -> castToxin(entity, now);
         case JINN -> castJinn(entity, now);
         case TEMPERATURE -> castTemperature(entity, now);
         case NERVES -> castNerves(entity, now);
         default -> {
         }
      }
   }

   public static void applyToxin(LivingEntity entity, LivingEntity target) {
      target.getPersistentData().putUUID(net.xxxjk.TYPE_MOON_WORLD.effect.FanaticToxinEffect.TAG_OWNER, entity.getUUID());
      target.addEffect(new MobEffectInstance(ModMobEffects.FANATIC_TOXIN, FanaticAssassinRules.TOXIN_DURATION, 0, false, true, true), entity);
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FanaticAssassinRules.TOXIN_DURATION, 0, false, true, true), entity);
   }

   public static float heartbeatDamageFor(LivingEntity target) {
      ServantDefinition definition = ServantIdentityHelper.definitionOf(target);
      return FanaticAssassinRules.heartbeatDamage(
         MagicResistanceHelper.getMagicResistanceRank(target),
         definition == null ? null : definition.parameters().luck());
   }

   public static boolean hasOwnedJinn(LivingEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return false;
      return !level.getEntitiesOfClass(FanaticAssassinJinnEntity.class, owner.getBoundingBox().inflate(64.0),
         jinn -> jinn.isAlive() && jinn.getOwner() == owner).isEmpty();
   }

   public static void discardOwnedJinn(LivingEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      for (FanaticAssassinJinnEntity jinn : level.getEntitiesOfClass(FanaticAssassinJinnEntity.class,
         owner.getBoundingBox().inflate(128.0), entity -> entity.getOwner() == owner)) jinn.discard();
   }

   public static void spawnSustainedTechniqueEffects(FanaticAssassinEntity entity, ServerLevel level) {
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      if (entity.isCrystalArmorActive() && entity.tickCount % 5 == 0) {
         spawnSphere(level, center, 0.52, CRYSTAL, 12);
         if (entity.tickCount % 20 == 0) {
            for (int i = 0; i < 6; i++) {
               double angle = Math.PI * 2.0 * i / 6.0;
               Vec3 low = entity.position().add(Math.cos(angle) * 0.33, 0.12, Math.sin(angle) * 0.33);
               spawnLine(level, low, low.add(0.0, 1.45, 0.0), STEEL, 7);
            }
         }
      }
      if (entity.isNervesActive() && entity.tickCount % 10 == 0) {
         spawnRing(level, entity.position().add(0.0, 0.05, 0.0), 0.85, SENSE_BLUE);
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getEyeY(), entity.getZ(),
            5, 0.25, 0.12, 0.25, 0.025);
      }
      if (entity.isToxinStanceActive() && entity.tickCount % 5 == 0) {
         level.sendParticles(TOXIN, entity.getX(), entity.getY() + 0.85, entity.getZ(),
            5, 0.36, 0.62, 0.36, 0.012);
         level.sendParticles(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, entity.getX(), entity.getY() + 1.2,
            entity.getZ(), 2, 0.28, 0.4, 0.28, 0.0);
      }
   }

   public static void spawnCardSustainedTechniqueEffects(LivingEntity entity, ServerLevel level,
                                                           boolean crystal, boolean nerves, boolean toxin) {
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      if (crystal && entity.tickCount % 5 == 0) {
         spawnSphere(level, center, 0.52, CRYSTAL, 12);
      }
      if (nerves && entity.tickCount % 10 == 0) {
         spawnRing(level, entity.position().add(0.0, 0.05, 0.0), 0.85, SENSE_BLUE);
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getEyeY(), entity.getZ(),
            5, 0.25, 0.12, 0.25, 0.025);
      }
      if (toxin && entity.tickCount % 5 == 0) {
         level.sendParticles(TOXIN, entity.getX(), entity.getY() + 0.85, entity.getZ(),
            5, 0.36, 0.62, 0.36, 0.012);
         level.sendParticles(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, entity.getX(), entity.getY() + 1.2,
            entity.getZ(), 2, 0.28, 0.4, 0.28, 0.0);
      }
   }

   public static void spawnTemperatureRelease(ServerLevel level, LivingEntity entity) {
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      spawnSphere(level, center, 0.62, STEEL, 28);
      level.sendParticles(ParticleTypes.WAX_OFF, center.x, center.y, center.z, 18, 0.42, 0.75, 0.42, 0.02);
   }

   public static void spawnJinnTransitionFx(ServerLevel level, FanaticAssassinJinnEntity jinn) {
      Vec3 center = jinn.position().add(0.0, 0.8, 0.0);
      spawnSphere(level, center, 0.8, JINN_GRAY, 28);
      spawnSphere(level, center, 0.55, JINN_PURPLE, 18);
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
   }

   public static void spawnJinnAura(ServerLevel level, FanaticAssassinJinnEntity jinn) {
      Vec3 base = jinn.position();
      level.sendParticles(JINN_GRAY, jinn.getX(), jinn.getY() + 0.7, jinn.getZ(),
         jinn.getForm() == FanaticAssassinJinnEntity.FORM_BEAST ? 9 : 6,
         jinn.getForm() == FanaticAssassinJinnEntity.FORM_BEAST ? 0.72 : 0.38,
         0.65, jinn.getForm() == FanaticAssassinJinnEntity.FORM_BEAST ? 0.72 : 0.38, 0.012);
      if (jinn.getForm() == FanaticAssassinJinnEntity.FORM_BEAST) {
         spawnRing(level, base.add(0.0, 0.55, 0.0), 0.68, JINN_PURPLE);
         spawnRing(level, base.add(0.0, 1.05, 0.0), 0.48, JINN_GRAY);
         Vec3 forward = horizontalLook(jinn.getLookAngle());
         Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
         Vec3 chest = base.add(0.0, 0.78, 0.0);
         spawnLine(level, chest.add(right.scale(0.36)), chest.add(right.scale(0.62)).add(forward.scale(0.95)).add(0.0, -0.4, 0.0), JINN_GRAY, 6);
         spawnLine(level, chest.add(right.scale(-0.36)), chest.add(right.scale(-0.62)).add(forward.scale(0.95)).add(0.0, -0.4, 0.0), JINN_GRAY, 6);
      } else {
         Vec3 forward = horizontalLook(jinn.getLookAngle());
         Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
         for (int i = 0; i < 24; i++) {
            double t = i / 23.0;
            double angle = t * Math.PI * 5.0;
            Vec3 point = base.add(forward.scale(t * 2.4)).add(right.scale(Math.sin(angle) * 0.32))
               .add(0.0, 0.55 + Math.cos(angle) * 0.28, 0.0);
            send(level, JINN_PURPLE, point);
         }
      }
   }

   public static void spawnHeartbeatFx(ServerLevel level, LivingEntity entity, LivingEntity target) {
      Vec3 forward = directionToTarget(entity, target);
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 start = entity.position().add(forward.scale(-0.35)).add(right.scale(0.32)).add(0.0, 1.25, 0.0);
      Vec3 chest = target.position().add(0.0, target.getBbHeight() * 0.62, 0.0);
      Vec3 control = start.lerp(chest, 0.48).add(right.scale(0.55)).add(0.0, 0.28, 0.0);
      spawnCurve(level, start, control, chest, HEART_PURPLE, 26);
      spawnCurve(level, start.add(right.scale(-0.08)), control.add(0.0, 0.1, 0.0), chest, HEART_BRIGHT, 18);
      Vec3 approach = chest.subtract(start).normalize();
      spawnLine(level, chest.subtract(approach.scale(0.35)), chest, HEART_BRIGHT, 7);
      spawnSphere(level, chest, 0.18, HEART_PURPLE, 20);
      spawnOrientedRing(level, chest, approach, 0.38, HEART_BRIGHT, 24);
      TYPE_MOON_WORLD.queueServerWork(4, () -> {
         spawnSphere(level, chest, 0.28, HEART_BRIGHT, 28);
         spawnOrientedRing(level, chest, approach, 0.82, HEART_PURPLE, 32);
      });
      TYPE_MOON_WORLD.queueServerWork(8, () -> {
         spawnSphere(level, chest, 0.42, HEART_RED, 40);
         spawnOrientedRing(level, chest, approach, 1.8, HEART_PURPLE, 46);
         level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, chest.x, chest.y, chest.z, 14, 0.3, 0.35, 0.3, 0.08);
      });
   }

   public static void spawnHairFx(ServerLevel level, LivingEntity entity, Vec3 forward) {
      Vec3 direction = horizontalLook(forward);
      Vec3 right = new Vec3(-direction.z, 0.0, direction.x);
      Vec3 origin = entity.position().add(direction.scale(-0.22)).add(0.0, entity.getBbHeight() * 0.83, 0.0);
      level.sendParticles(ParticleTypes.SQUID_INK, origin.x, origin.y, origin.z, 18, 0.36, 0.28, 0.36, 0.012);
      for (int blade = -6; blade <= 6; blade++) {
         double angle = Math.toRadians(blade * 7.0);
         Vec3 bladeDirection = direction.scale(Math.cos(angle)).add(right.scale(Math.sin(angle))).normalize();
         Vec3 start = origin.add(right.scale(blade * 0.035)).add(0.0, Math.sin(blade * 0.9) * 0.12, 0.0);
         Vec3 end = start.add(bladeDirection.scale(FanaticAssassinRules.HAIR_RANGE));
         spawnLine(level, start, end, HAIR_BLACK, 11);
         spawnLine(level, end.subtract(bladeDirection.scale(0.72)), end, HAIR_SILVER, 5);
      }
      TYPE_MOON_WORLD.queueServerWork(4, () -> {
         for (int blade = -5; blade <= 5; blade += 2) {
            double angle = Math.toRadians(blade * 8.0);
            Vec3 bladeDirection = direction.scale(Math.cos(angle)).add(right.scale(Math.sin(angle))).normalize();
            spawnLine(level, origin.add(bladeDirection.scale(2.0)), origin.add(bladeDirection.scale(7.5)), HAIR_SILVER, 8);
         }
      });
      TYPE_MOON_WORLD.queueServerWork(10, () -> {
         for (int blade = -5; blade <= 5; blade += 2) {
            double angle = Math.toRadians(blade * 8.0);
            Vec3 bladeDirection = direction.scale(Math.cos(angle)).add(right.scale(Math.sin(angle))).normalize();
            spawnLine(level, origin.add(bladeDirection.scale(6.5)), origin, HAIR_BLACK, 7);
         }
      });
   }

   public static void spawnMarrowFx(ServerLevel level, LivingEntity entity, List<LivingEntity> targets) {
      Vec3 throat = entity.position().add(0.0, entity.getBbHeight() * 0.78, 0.0);
      Vec3 forward = entity instanceof FanaticAssassinEntity fanatic
         ? directionToTarget(entity, fanatic.getTarget()) : horizontalLook(entity.getLookAngle());
      double[] radii = {1.5, 3.0, 5.0, 9.0, 15.0};
      for (int i = 0; i < radii.length; i++) {
         double radius = radii[i];
         int delay = i * 2;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            spawnRing(level, entity.position().add(0.0, 0.12, 0.0), radius, MARROW_DARK);
            spawnOrientedRing(level, throat, forward, Math.min(radius, 5.0), MARROW_PURPLE,
               Math.max(20, (int)(Math.min(radius, 5.0) * 9.0)));
         });
      }
      for (LivingEntity target : targets) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         spawnBodySpiral(level, target, MARROW_DARK, 20);
         spawnSphere(level, target.position().add(0.0, target.getEyeHeight(), 0.0), 0.34, MARROW_PURPLE, 18);
         spawnLine(level, throat, center, MARROW_DARK, 9);
      }
   }

   public static void spawnComputerFx(ServerLevel level, LivingEntity target, Vec3 center) {
      Vec3 feet = target.position().add(0.0, 0.05, 0.0);
      spawnRing(level, feet, 0.35, COMPUTER_DARK);
      spawnRing(level, feet, 1.2, COMPUTER_RED);
      spawnRing(level, feet, 1.8, COMPUTER_DARK);
      spawnBodySpiral(level, target, COMPUTER_RED, 28);
      Vec3 head = target.position().add(0.0, target.getEyeHeight(), 0.0);
      spawnSphere(level, head, 0.28, COMPUTER_DARK, 18);
      TYPE_MOON_WORLD.queueServerWork(4, () -> {
         spawnSphere(level, head, 0.48, COMPUTER_RED, 30);
         spawnOrientedRing(level, head, new Vec3(0.0, 1.0, 0.0), 0.65, COMPUTER_RED, 28);
      });
      TYPE_MOON_WORLD.queueServerWork(8, () -> {
         spawnSphere(level, head, 1.15, COMPUTER_RED, 54);
         level.sendParticles(ParticleTypes.FLASH, head.x, head.y, head.z, 2, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.EXPLOSION, head.x, head.y, head.z, 4, 0.22, 0.22, 0.22, 0.01);
      });
      TYPE_MOON_WORLD.queueServerWork(12, () -> {
         level.sendParticles(ParticleTypes.ASH, center.x, center.y + 0.35, center.z, 22, 0.65, 0.75, 0.65, 0.018);
         level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.3, center.z, 10, 0.5, 0.65, 0.5, 0.015);
      });
   }

   public static void spawnTemperatureActivation(ServerLevel level, LivingEntity entity) {
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      spawnSphere(level, center, 0.42, STEEL, 24);
      TYPE_MOON_WORLD.queueServerWork(3, () -> spawnSphere(level, center, 0.68, CRYSTAL, 42));
      TYPE_MOON_WORLD.queueServerWork(7, () -> {
         spawnSphere(level, center, 0.82, CRYSTAL, 52);
         level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
         for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            Vec3 low = entity.position().add(Math.cos(angle) * 0.35, 0.12, Math.sin(angle) * 0.35);
            spawnLine(level, low, low.add(0.0, 1.5, 0.0), STEEL, 8);
         }
      });
   }

   public static void spawnNervesFx(ServerLevel level, LivingEntity entity) {
      Vec3 eyes = entity.position().add(0.0, entity.getEyeHeight(), 0.0);
      spawnSphere(level, eyes, 0.22, SENSE_PURPLE, 24);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, eyes.x, eyes.y, eyes.z, 16, 0.25, 0.15, 0.25, 0.035);
      double[] radii = {0.5, 3.0, 6.0, 10.0, 15.0};
      Vec3 origin = entity.position().add(0.0, 0.08, 0.0);
      for (int i = 0; i < radii.length; i++) {
         double radius = radii[i];
         TYPE_MOON_WORLD.queueServerWork(i * 2, () -> spawnRing(level, origin, radius, SENSE_PURPLE));
      }
      double[] collapse = {10.0, 6.0, 3.0, 0.5};
      for (int i = 0; i < collapse.length; i++) {
         double radius = collapse[i];
         TYPE_MOON_WORLD.queueServerWork(10 + i, () -> spawnRing(level, origin, radius, SENSE_BLUE));
      }
      TYPE_MOON_WORLD.queueServerWork(4, () -> spawnTerrainGrid(level, origin, 6, SENSE_BLUE));
      List<LivingEntity> sensed = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(15.0),
         living -> living != entity && living.isAlive()).stream().limit(12).toList();
      for (LivingEntity living : sensed) {
         Vec3 source = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0);
         spawnLine(level, source, eyes, SENSE_BLUE, 8);
      }
   }

   public static void spawnToxinActivation(ServerLevel level, LivingEntity entity) {
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      spawnBodySpiral(level, entity, TOXIN_BRIGHT, 30);
      spawnSphere(level, center, 0.5, TOXIN, 28);
      spawnRing(level, entity.position().add(0.0, 0.08, 0.0), 1.0, TOXIN_BRIGHT);
      TYPE_MOON_WORLD.queueServerWork(5, () -> {
         spawnSphere(level, center, 1.25, TOXIN, 42);
         level.sendParticles(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, entity.getX(), entity.getY() + 1.35,
            entity.getZ(), 12, 0.35, 0.45, 0.35, 0.0);
      });
      TYPE_MOON_WORLD.queueServerWork(9, () -> spawnRing(level, entity.position().add(0.0, 0.08, 0.0), 2.0, TOXIN));
   }

   public static void spawnToxinRelease(ServerLevel level, LivingEntity entity) {
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      spawnRing(level, entity.position().add(0.0, 0.08, 0.0), 2.0, TOXIN);
      spawnRing(level, entity.position().add(0.0, 0.1, 0.0), 0.55, TOXIN_BRIGHT);
      for (int i = 0; i < 24; i++) {
         double angle = Math.PI * 2.0 * i / 24.0;
         Vec3 outer = center.add(Math.cos(angle) * 1.2, (i % 5) * 0.12 - 0.3, Math.sin(angle) * 1.2);
         spawnLine(level, outer, center, TOXIN, 5);
      }
   }

   public static void spawnJinnSummonFx(ServerLevel level, LivingEntity entity, FanaticAssassinJinnEntity jinn) {
      Vec3 center = jinn.position().add(0.0, 0.75, 0.0);
      level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 36, 0.85, 0.7, 0.85, 0.025);
      spawnSphere(level, center, 0.7, JINN_GRAY, 34);
      spawnRing(level, jinn.position().add(0.0, 0.08, 0.0), 1.5, JINN_PURPLE);
      TYPE_MOON_WORLD.queueServerWork(4, () -> {
         spawnSphere(level, center, 1.6, JINN_GRAY, 54);
         spawnJinnAura(level, jinn);
      });
      TYPE_MOON_WORLD.queueServerWork(8, () -> {
         spawnJinnTransitionFx(level, jinn);
         spawnLine(level, entity.position().add(0.0, 1.0, 0.0), center, JINN_PURPLE, 14);
      });
   }

   private static void retreat(FanaticAssassinEntity entity, LivingEntity threat, long now) {
      if (now - entity.getPersistentData().getLong("FanaticLastRetreatPath") < 20L) return;
      entity.getPersistentData().putLong("FanaticLastRetreatPath", now);
      BlockPos center = entity.blockPosition();
      BlockPos best = null;
      double bestScore = entity.distanceToSqr(threat);
      for (int attempt = 0; attempt < 48; attempt++) {
         BlockPos pos = center.offset(entity.getRandom().nextIntBetweenInclusive(-16, 16),
            entity.getRandom().nextIntBetweenInclusive(-3, 3), entity.getRandom().nextIntBetweenInclusive(-16, 16));
         BlockState state = entity.level().getBlockState(pos);
         if (!state.getCollisionShape(entity.level(), pos).isEmpty()
            || !entity.level().getBlockState(pos.above()).getCollisionShape(entity.level(), pos.above()).isEmpty()
            || !entity.level().getBlockState(pos.below()).isFaceSturdy(entity.level(), pos.below(), Direction.UP)) continue;
         double distance = threat.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
         double score = distance - entity.level().getMaxLocalRawBrightness(pos) * 6.0;
         if (score > bestScore) {
            bestScore = score;
            best = pos;
         }
      }
      if (best != null) ServantNavigationHelper.moveToPositionThrottled(entity,
         Vec3.atBottomCenterOf(best), 1.25, now, 20, 1.5, "FanaticRetreatPath");
      else {
         Vec3 away = entity.position().subtract(threat.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() > 1.0E-4) entity.getNavigation().moveTo(
            entity.getX() + away.normalize().x * 12.0, entity.getY(), entity.getZ() + away.normalize().z * 12.0, 1.25);
      }
   }

   private static void commit(FanaticAssassinEntity entity, String key, int mp, long now,
                               int technique, String animation, int visualDuration) {
      entity.revealForCombat();
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - mp));
      entity.getPersistentData().putLong("FanaticLast" + key, now);
      entity.getPersistentData().putLong(BUSY_UNTIL, now + Math.min(24, visualDuration));
      entity.setActiveTechnique(technique, now + visualDuration);
      entity.triggerNamedActionAnimation(animation);
      entity.getNavigation().stop();
   }

   private static boolean tryConcealedApproach(FanaticAssassinEntity entity, LivingEntity target, long now) {
      double distance = entity.distanceTo(target);
      if (!entity.isPresenceConcealed() || distance < 4.0 || distance > 11.0
         || now - entity.getPersistentData().getLong("FanaticLastShadowStep") < 70L) return false;
      Vec3 look = target.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-5) look = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-5) return false;
      Vec3 destination = target.position().subtract(look.normalize().scale(1.7));
      Vec3 offset = destination.subtract(entity.position());
      if (!entity.level().noCollision(entity, entity.getBoundingBox().move(offset))) return false;
      entity.getPersistentData().putLong("FanaticLastShadowStep", now);
      entity.getNavigation().stop();
      entity.teleportTo(destination.x, target.getY(), destination.z);
      entity.fallDistance = 0.0F;
      entity.faceToward(target.position());
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getY() + 0.8, entity.getZ(),
            14, 0.35, 0.65, 0.35, 0.03);
      }
      return true;
   }

   private static boolean canCast(FanaticAssassinEntity entity, String key, int mp, int cooldown, long now) {
      if (entity.getCurrentMp() < mp) return false;
      String tag = "FanaticLast" + key;
      return FanaticAssassinRules.cooldownReady(now, entity.getPersistentData().contains(tag),
         entity.getPersistentData().getLong(tag), cooldown);
   }

   private static List<LivingEntity> enemiesAround(FanaticAssassinEntity entity, double radius) {
      return entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius),
         candidate -> isValidTarget(entity, candidate));
   }

   public static boolean isValidTarget(FanaticAssassinEntity entity, LivingEntity candidate) {
      if (candidate == null || candidate == entity || !candidate.isAlive() || EntityUtils.isImmunePlayerTarget(candidate)
         || entity.isAlliedTo(candidate) || candidate.isAlliedTo(entity)
         || ServantMasterTargeting.isContractMaster(entity, candidate)) return false;
      if (isDoctrineTarget(candidate)) return true;
      if (candidate instanceof Enemy) return true;
      if (entity.getLastHurtByMob() == candidate && entity.tickCount - entity.getLastHurtByMobTimestamp() <= 200) return true;
      return candidate instanceof Mob mob && mob.getTarget() == entity;
   }

   private static boolean isDoctrineTarget(LivingEntity candidate) {
      if (candidate.getType().is(DEAD_APOSTLES) || candidate instanceof MysticMagicianEntity || candidate instanceof Witch) return true;
      if (candidate instanceof Player player) {
         return player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).is_magus;
      }
      ServantDefinition definition = ServantIdentityHelper.definitionOf(candidate);
      return definition != null && definition.classType() == ServantClassType.CASTER;
   }

   private static boolean isStrongTarget(LivingEntity target) {
      return target.getMaxHealth() >= 200.0F || target instanceof EnderDragon || target instanceof WitherBoss
         || ServantIdentityHelper.isServantLike(target);
   }

   private static double targetScore(FanaticAssassinEntity entity, LivingEntity target) {
      double score = -entity.distanceToSqr(target);
      if (isDoctrineTarget(target)) score += 10000.0;
      if (target instanceof Mob mob && mob.getTarget() == entity) score += 2000.0;
      if (target instanceof Enemy) score += 1000.0;
      if (isStrongTarget(target)) score += 100.0;
      return score;
   }

   private static DustParticleOptions dust(float red, float green, float blue, float scale) {
      return new DustParticleOptions(new Vector3f(red, green, blue), scale);
   }

   private static Vec3 horizontalLook(Vec3 look) {
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      return horizontal.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   public static Vec3 directionToTarget(LivingEntity entity, LivingEntity target) {
      if (target == null) return horizontalLook(entity.getLookAngle());
      Vec3 from = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(from);
      return horizontalLook(to);
   }

   private static void send(ServerLevel level, ParticleOptions particle, Vec3 point) {
      level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
   }

   private static void spawnLine(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int count) {
      int points = Math.max(2, count);
      for (int i = 0; i < points; i++) {
         send(level, particle, start.lerp(end, i / (double)(points - 1)));
      }
   }

   private static void spawnCurve(ServerLevel level, Vec3 start, Vec3 control, Vec3 end,
                                  ParticleOptions particle, int count) {
      int points = Math.max(3, count);
      for (int i = 0; i < points; i++) {
         double t = i / (double)(points - 1);
         double inverse = 1.0 - t;
         Vec3 point = start.scale(inverse * inverse).add(control.scale(2.0 * inverse * t)).add(end.scale(t * t));
         send(level, particle, point);
      }
   }

   private static void spawnSphere(ServerLevel level, Vec3 center, double radius,
                                   ParticleOptions particle, int count) {
      int points = Math.max(8, count);
      double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
      for (int i = 0; i < points; i++) {
         double y = 1.0 - 2.0 * (i + 0.5) / points;
         double radial = Math.sqrt(Math.max(0.0, 1.0 - y * y));
         double angle = goldenAngle * i;
         Vec3 offset = new Vec3(Math.cos(angle) * radial, y, Math.sin(angle) * radial).scale(radius);
         send(level, particle, center.add(offset));
      }
   }

   private static void spawnOrientedRing(ServerLevel level, Vec3 center, Vec3 normal, double radius,
                                         ParticleOptions particle, int count) {
      Vec3 n = normal.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 1.0, 0.0) : normal.normalize();
      Vec3 reference = Math.abs(n.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
      Vec3 axisA = n.cross(reference).normalize();
      Vec3 axisB = n.cross(axisA).normalize();
      int points = Math.max(12, count);
      for (int i = 0; i < points; i++) {
         double angle = Math.PI * 2.0 * i / points;
         Vec3 point = center.add(axisA.scale(Math.cos(angle) * radius)).add(axisB.scale(Math.sin(angle) * radius));
         send(level, particle, point);
      }
   }

   private static void spawnBodySpiral(ServerLevel level, LivingEntity entity, ParticleOptions particle, int count) {
      int points = Math.max(12, count);
      double height = Math.max(0.8, entity.getBbHeight());
      for (int i = 0; i < points; i++) {
         double t = i / (double)(points - 1);
         double angle = t * Math.PI * 5.0;
         double radius = entity.getBbWidth() * (0.42 + Math.sin(t * Math.PI) * 0.16);
         Vec3 point = entity.position().add(Math.cos(angle) * radius, 0.08 + t * height * 0.9, Math.sin(angle) * radius);
         send(level, particle, point);
      }
   }

   private static void spawnTerrainGrid(ServerLevel level, Vec3 center, int radius, ParticleOptions particle) {
      for (int offset = -radius; offset <= radius; offset += 2) {
         spawnLine(level, center.add(-radius, 0.0, offset), center.add(radius, 0.0, offset), particle, radius + 1);
         spawnLine(level, center.add(offset, 0.0, -radius), center.add(offset, 0.0, radius), particle, radius + 1);
      }
   }

   private static void spawnRing(ServerLevel level, Vec3 center, double radius, ParticleOptions particle) {
      int count = Math.max(16, Math.min(80, (int)(radius * 6.0)));
      for (int i = 0; i < count; i++) {
         double angle = Math.PI * 2.0 * i / count;
         level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y,
            center.z + Math.sin(angle) * radius, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }
}
