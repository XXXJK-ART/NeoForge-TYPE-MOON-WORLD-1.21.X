package net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanPersonaEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;

public final class HundredFacesHassanCombatHelper {
   private static final String LAST_RETREAT = "HundredFacesLastRetreat";
   private static final String LAST_SWARM = "HundredFacesLastSwarm";
   private static final String LAST_BASIC = "HundredFacesLastBasic";
   private static final String LAST_DIRK_TICK = "HundredFacesLastDirkTick";
   private static final String LAST_REPOSITION_TICK = "HundredFacesLastRepositionTick";
   private static final String LAST_SHADOW_STEP_TICK = "HundredFacesLastShadowStepTick";
   private static final String LAST_STAB_COMBO_TICK = "HundredFacesLastStabComboTick";
   private static final String LAST_KNIFE_FEINT_TICK = "HundredFacesLastKnifeFeintTick";
   private static final String LAST_SHADOW_LUNGE_TICK = "HundredFacesLastShadowLungeTick";
   private static final String LAST_PERSONA_DIRK_TICK = "HundredFacesPersonaLastDirkTick";
   private static final String LAST_PERSONA_SHADOW_STEP_TICK = "HundredFacesPersonaLastShadowStepTick";
   private static final String LAST_PERSONA_SHADOW_LUNGE_TICK = "HundredFacesPersonaLastShadowLungeTick";

   private HundredFacesHassanCombatHelper() {
   }

   public static void tick(HundredFacesHassanEntity entity, ServantAiContext context) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      LivingEntity target = entity.getTarget();
      if (target == null || !isValidTarget(entity, target)) {
         target = scanTarget(entity);
         entity.setTarget(target);
      }
      if (target == null) {
         maybeSummonScouts(entity, level, context.gameTick());
         return;
      }

      long now = context.gameTick();
      entity.getLookControl().setLookAt(target, 45.0F, 45.0F);
      List<LivingEntity> enemies = enemiesAround(entity, 10.0);
      if (shouldRetreat(entity, target, enemies, now)) {
         startRetreat(entity, target, now);
         summonPersonas(entity, level, target, HundredFacesHassanEntity.PersonaMode.TACTICS, desiredBatch(entity, enemies, 10));
         return;
      }

      int live = countOwnedPersonas(entity);
      int desired = enemies.size() >= 3 || isStrongTarget(target) ? 10 : 4;
      if (live < desired && now - entity.getPersistentData().getLong(LAST_SWARM) >= 30L) {
         HundredFacesHassanEntity.PersonaMode mode = enemies.size() >= 3
            ? HundredFacesHassanEntity.PersonaMode.TACTICS
            : HundredFacesHassanEntity.PersonaMode.ASSASSINATION;
         summonPersonas(entity, level, target, mode, desired - live);
      }

      if (entity.isRetreating()) {
         retreatFrom(entity, target, now);
         return;
      }
      double distance = entity.distanceTo(target);
      if (tryShadowStepBackstab(entity, target, distance, now)) return;
      if (tryShadowLunge(entity, target, distance, now)) return;
      if (tryKnifeFeint(entity, target, distance, now)) return;
      if (tryThrowDirk(entity, target, distance, now)) return;
      if (tryKnifeCombo(entity, target, distance, now)) return;
      if (distance > 2.25) {
         boolean moving = ServantNavigationHelper.moveToTargetThrottled(entity, target, 1.12, now,
            ServantNavigationHelper.SHORT_REPATH_INTERVAL, 0.8, "HundredFacesChase");
         if (!moving) tryRepositionNearTarget(entity, target, distance, now);
      } else if (now - entity.getPersistentData().getLong(LAST_BASIC) >= 18L && !entity.isPerformingAction()) {
         entity.getPersistentData().putLong(LAST_BASIC, now);
         entity.triggerAssassinStabAnimation();
         entity.doHurtTarget(target);
      }
   }

   public static boolean tryPersonaCombatSkill(HundredFacesHassanPersonaEntity entity, LivingEntity target, double distance, long now) {
      if (target == null || !target.isAlive() || entity.isPerformingAction()) return false;
      if (tryPersonaShadowStepBackstab(entity, target, distance, now)) return true;
      if (tryPersonaShadowLunge(entity, target, distance, now)) return true;
      return tryPersonaThrowDirk(entity, target, distance, now);
   }

   public static int summonPersonas(HundredFacesHassanEntity owner, ServerLevel level, @Nullable LivingEntity target,
                                    HundredFacesHassanEntity.PersonaMode mode, int requested) {
      int live = countOwnedPersonas(owner);
      int count = HundredFacesHassanRules.affordableSummonCount(owner.getCurrentMp(), live, requested);
      if (count <= 0) return 0;
      owner.revealForCombat();
      owner.triggerNamedActionAnimation("zabaniya");
      owner.setCurrentMp(Math.max(0.0, owner.getCurrentMp() - count * HundredFacesHassanRules.MP_PER_PERSONA));
      owner.getPersistentData().putLong(LAST_SWARM, level.getGameTime());
      Vec3 forward = horizontal(owner.getLookAngle());
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      int spawned = 0;
      for (int i = 0; i < count; i++) {
         HundredFacesHassanPersonaEntity persona = ModEntities.HUNDRED_FACES_HASSAN_PERSONA.get().create(level);
         if (persona == null) continue;
         int index = live + spawned;
         double angle = Math.PI * 2.0 * index / Math.max(6.0, live + count);
         Vec3 offset = forward.scale(Math.cos(angle) * 1.7).add(right.scale(Math.sin(angle) * 1.7));
         Vec3 pos = safeSpawnPosition(owner, offset);
         float yaw = (float)Math.toDegrees(Math.atan2(-forward.x, forward.z));
         persona.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
         persona.initialize(owner, target, mode, live + spawned + 1);
         level.addFreshEntity(persona);
         spawned++;
         level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 0.9, pos.z, 12, 0.35, 0.55, 0.35, 0.02);
      }
      if (spawned > 0) {
         level.sendParticles(ParticleTypes.SMOKE, owner.getX(), owner.getY() + 1.0, owner.getZ(),
            24, 0.8, 0.8, 0.8, 0.03);
         rescaleOwnedPersonas(owner);
      }
      return spawned;
   }

   public static int countOwnedPersonas(HundredFacesHassanEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return 0;
      return ownedPersonas(level, owner, true).size();
   }

   public static void rescaleOwnedPersonas(HundredFacesHassanEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      List<HundredFacesHassanPersonaEntity> personas = ownedPersonas(level, owner, true);
      int live = Math.max(1, personas.size());
      for (HundredFacesHassanPersonaEntity persona : personas) {
         persona.applyPersonaAttributes(live, false);
      }
   }

   public static void discardOwnedPersonas(HundredFacesHassanEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      for (HundredFacesHassanPersonaEntity persona : ownedPersonas(level, owner, false)) {
         persona.discard();
      }
   }

   private static List<HundredFacesHassanPersonaEntity> ownedPersonas(ServerLevel level, HundredFacesHassanEntity owner,
                                                                      boolean aliveOnly) {
      List<HundredFacesHassanPersonaEntity> personas = new ArrayList<>();
      for (net.minecraft.world.entity.Entity entity : level.getEntities().getAll()) {
         if (entity instanceof HundredFacesHassanPersonaEntity persona
            && owner.getUUID().equals(persona.getOwnerUuid())
            && (!aliveOnly || persona.isAlive())) {
            personas.add(persona);
         }
      }
      return personas;
   }

   public static void cleanseHarmfulEffects(LivingEntity entity) {
      for (MobEffectInstance effect : List.copyOf(entity.getActiveEffects())) {
         if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            entity.removeEffect(effect.getEffect());
         }
      }
   }

   public static void applyWeakPoison(LivingEntity attacker, LivingEntity target) {
      target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true, true), attacker);
   }

   private static boolean tryThrowDirk(HundredFacesHassanEntity entity, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (distance < 3.0 || distance > 14.0 || entity.getCurrentMp() < HundredFacesHassanRules.MP_DIRK_THROW
         || now - data.getLong(LAST_DIRK_TICK) < HundredFacesHassanRules.DIRK_COOLDOWN_TICKS) return false;
      if (!entity.getSensing().hasLineOfSight(target) || entity.isPerformingAction()) return false;
      data.putLong(LAST_DIRK_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - HundredFacesHassanRules.MP_DIRK_THROW);
      entity.revealForCombat();
      entity.triggerDirkThrowAnimation();
      spawnDirkProjectile(entity, target, HundredFacesHassanRules.MAIN_DIRK_DAMAGE, 1.9F);
      return true;
   }

   private static boolean tryKnifeCombo(HundredFacesHassanEntity entity, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (distance > 2.55 || entity.getCurrentMp() < HundredFacesHassanRules.MP_STAB_COMBO
         || now - data.getLong(LAST_STAB_COMBO_TICK) < HundredFacesHassanRules.STAB_COMBO_COOLDOWN_TICKS
         || entity.isPerformingAction()) return false;
      data.putLong(LAST_STAB_COMBO_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - HundredFacesHassanRules.MP_STAB_COMBO);
      entity.revealForCombat();
      entity.triggerAssassinStabAnimation();
      for (int i = 0; i < 3 && target.isAlive(); i++) {
         dealScaledKnifeDamage(entity, target, entity.getPersonaMode() == HundredFacesHassanEntity.PersonaMode.ASSASSINATION ? 0.62F : 0.52F);
         target.invulnerableTime = 0;
      }
      if (entity.getPersonaMode() == HundredFacesHassanEntity.PersonaMode.POISON) {
         applyWeakPoison(entity, target);
      }
      spawnCritFx(entity, target, 12, 1.55F);
      return true;
   }

   private static boolean tryKnifeFeint(HundredFacesHassanEntity entity, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (distance < 2.5 || distance > 9.0 || entity.getCurrentMp() < HundredFacesHassanRules.MP_KNIFE_FEINT
         || now - data.getLong(LAST_KNIFE_FEINT_TICK) < HundredFacesHassanRules.KNIFE_FEINT_COOLDOWN_TICKS
         || !entity.getSensing().hasLineOfSight(target) || entity.isPerformingAction()) return false;
      data.putLong(LAST_KNIFE_FEINT_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - HundredFacesHassanRules.MP_KNIFE_FEINT);
      entity.revealForCombat();
      entity.faceToward(target.position());
      entity.triggerDirkThrowAnimation();
      dealScaledKnifeDamage(entity, target, 0.72F);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 1, false, true, true), entity);
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 35, 0, false, true, true), entity);
      spawnCritFx(entity, target, 10, 1.75F);
      return true;
   }

   private static boolean tryShadowLunge(HundredFacesHassanEntity entity, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (distance < 2.0 || distance > 6.5 || entity.getCurrentMp() < HundredFacesHassanRules.MP_SHADOW_LUNGE
         || now - data.getLong(LAST_SHADOW_LUNGE_TICK) < HundredFacesHassanRules.SHADOW_LUNGE_COOLDOWN_TICKS
         || !entity.getSensing().hasLineOfSight(target) || entity.isPerformingAction()) return false;
      data.putLong(LAST_SHADOW_LUNGE_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - HundredFacesHassanRules.MP_SHADOW_LUNGE);
      entity.revealForCombat();
      doShadowLunge(entity, target, 1.45, 1.0F);
      return true;
   }

   private static boolean tryShadowStepBackstab(HundredFacesHassanEntity entity, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (distance < 3.0 || distance > 8.0 || entity.getCurrentMp() < HundredFacesHassanRules.MP_SHADOW_STEP
         || now - data.getLong(LAST_SHADOW_STEP_TICK) < HundredFacesHassanRules.SHADOW_STEP_COOLDOWN_TICKS
         || !entity.getSensing().hasLineOfSight(target)) return false;
      if (!teleportBehind(entity, target, 1.35)) return false;
      data.putLong(LAST_SHADOW_STEP_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - HundredFacesHassanRules.MP_SHADOW_STEP);
      entity.revealForCombat();
      entity.triggerShadowStepAnimation();
      dealScaledKnifeDamage(entity, target, entity.getPersonaMode() == HundredFacesHassanEntity.PersonaMode.ASSASSINATION ? 1.45F : 1.25F);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true), entity);
      spawnBackstabFx(entity, target);
      return true;
   }

   private static boolean tryRepositionNearTarget(HundredFacesHassanEntity entity, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (distance <= 3.0 || distance > 20.0 || now - data.getLong(LAST_REPOSITION_TICK) < HundredFacesHassanRules.REPOSITION_COOLDOWN_TICKS
         || !(entity.level() instanceof ServerLevel level)) return false;
      Vec3 away = entity.position().subtract(target.position());
      Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
      }
      horizontal = horizontal.normalize();
      Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x);
      Vec3[] candidates = new Vec3[]{
         target.position().add(horizontal.scale(1.65)),
         target.position().add(side.scale(1.75)),
         target.position().subtract(side.scale(1.75)),
         target.position().subtract(target.getLookAngle().normalize().scale(1.45))
      };
      for (Vec3 candidate : candidates) {
         BlockPos feet = findSafeTeleportFeet(level, BlockPos.containing(candidate.x, target.getY(), candidate.z));
         if (feet == null) continue;
         Vec3 destination = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
         if (!level.noCollision(entity, entity.getBoundingBox().move(destination.subtract(entity.position())))) continue;
         data.putLong(LAST_REPOSITION_TICK, now);
         entity.revealForCombat();
         spawnShadowStepFx(entity);
         entity.teleportTo(destination.x, destination.y, destination.z);
         entity.setDeltaMovement(Vec3.ZERO);
         entity.fallDistance = 0.0F;
         entity.getNavigation().stop();
         entity.setTarget(target);
         entity.faceToward(target.position());
         entity.getLookControl().setLookAt(target, 60.0F, 60.0F);
         entity.triggerShadowStepAnimation();
         return true;
      }
      return false;
   }

   private static boolean tryPersonaThrowDirk(HundredFacesHassanPersonaEntity entity, LivingEntity target, double distance, long now) {
      if (distance < 4.0 || distance > 12.0 || !entity.getSensing().hasLineOfSight(target)) return false;
      if (!personaCooldownReady(entity, LAST_PERSONA_DIRK_TICK, now, HundredFacesHassanRules.PERSONA_DIRK_COOLDOWN_TICKS)) return false;
      entity.revealForCombat();
      entity.triggerDirkThrowAnimation();
      spawnDirkProjectile(entity, target, HundredFacesHassanRules.PERSONA_DIRK_DAMAGE, 1.75F);
      return true;
   }

   private static boolean tryPersonaShadowLunge(HundredFacesHassanPersonaEntity entity, LivingEntity target, double distance, long now) {
      if (distance < 2.2 || distance > 5.8 || !entity.getSensing().hasLineOfSight(target)) return false;
      if (!personaCooldownReady(entity, LAST_PERSONA_SHADOW_LUNGE_TICK, now, HundredFacesHassanRules.PERSONA_SHADOW_LUNGE_COOLDOWN_TICKS)) return false;
      entity.revealForCombat();
      doShadowLunge(entity, target, 1.25, 0.78F);
      return true;
   }

   private static boolean tryPersonaShadowStepBackstab(HundredFacesHassanPersonaEntity entity, LivingEntity target, double distance, long now) {
      if (distance < 3.0 || distance > 7.0 || !entity.getSensing().hasLineOfSight(target)) return false;
      if (!personaCooldownReady(entity, LAST_PERSONA_SHADOW_STEP_TICK, now, HundredFacesHassanRules.PERSONA_SHADOW_STEP_COOLDOWN_TICKS)) return false;
      if (!teleportBehind(entity, target, 1.2)) return false;
      entity.revealForCombat();
      entity.triggerShadowStepAnimation();
      dealScaledKnifeDamage(entity, target, 1.05F);
      spawnBackstabFx(entity, target);
      return true;
   }

   private static boolean shouldRetreat(HundredFacesHassanEntity entity, LivingEntity target,
                                        List<LivingEntity> enemies, long now) {
      if (entity.getCurrentMp() < HundredFacesHassanRules.MP_PER_PERSONA) return false;
      if (now - entity.getPersistentData().getLong(LAST_RETREAT) < HundredFacesHassanRules.RETREAT_COOLDOWN_TICKS) return false;
      return entity.getHealth() <= entity.getMaxHealth() * 0.35F
         || enemies.size() >= 3
         || target.getMaxHealth() >= entity.getMaxHealth() * 1.8F;
   }

   private static void startRetreat(HundredFacesHassanEntity entity, LivingEntity target, long now) {
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - HundredFacesHassanRules.MP_PER_PERSONA));
      entity.getPersistentData().putLong(LAST_RETREAT, now);
      entity.startRetreat(now);
      cleanseHarmfulEffects(entity);
      retreatFrom(entity, target, now);
   }

   private static void maybeSummonScouts(HundredFacesHassanEntity entity, ServerLevel level, long now) {
      if (now - entity.getPersistentData().getLong(LAST_SWARM) < 200L || countOwnedPersonas(entity) >= 5) return;
      summonPersonas(entity, level, null, HundredFacesHassanEntity.PersonaMode.SCOUT, 3);
   }

   private static int desiredBatch(HundredFacesHassanEntity entity, List<LivingEntity> enemies, int fallback) {
      int live = countOwnedPersonas(entity);
      int desired = enemies.size() >= 3 ? 20 : fallback;
      return Math.max(0, desired - live);
   }

   @Nullable
   private static LivingEntity scanTarget(HundredFacesHassanEntity entity) {
      return entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(28.0),
         candidate -> isValidTarget(entity, candidate)).stream()
         .max(Comparator.comparingDouble(candidate -> targetScore(entity, candidate))).orElse(null);
   }

   private static boolean isValidTarget(HundredFacesHassanEntity entity, @Nullable LivingEntity candidate) {
      if (candidate == null || candidate == entity || !candidate.isAlive()
         || candidate instanceof HundredFacesHassanPersonaEntity
         || EntityUtils.isImmunePlayerTarget(candidate)
         || entity.isAlliedTo(candidate) || candidate.isAlliedTo(entity)
         || ServantMasterTargeting.isContractMaster(entity, candidate)) return false;
      if (candidate instanceof Player) return true;
      if (candidate instanceof Enemy) return true;
      if (entity.getLastHurtByMob() == candidate && entity.tickCount - entity.getLastHurtByMobTimestamp() <= 200) return true;
      return candidate instanceof Mob mob && mob.getTarget() == entity;
   }

   private static List<LivingEntity> enemiesAround(HundredFacesHassanEntity entity, double radius) {
      return entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius),
         candidate -> isValidTarget(entity, candidate));
   }

   private static boolean isStrongTarget(LivingEntity target) {
      return target.getMaxHealth() >= 120.0F
         || target.getAttributeValue(Attributes.ATTACK_DAMAGE) >= 12.0;
   }

   private static double targetScore(HundredFacesHassanEntity entity, LivingEntity target) {
      double score = -entity.distanceToSqr(target);
      if (target instanceof Player) score += 2000.0;
      if (target instanceof Enemy) score += 1000.0;
      if (isStrongTarget(target)) score += 250.0;
      return score;
   }

   private static void retreatFrom(HundredFacesHassanEntity entity, LivingEntity threat, long now) {
      if (now - entity.getPersistentData().getLong("HundredFacesLastRetreatPath") < 20L) return;
      entity.getPersistentData().putLong("HundredFacesLastRetreatPath", now);
      BlockPos center = entity.blockPosition();
      BlockPos best = null;
      double bestScore = entity.distanceToSqr(threat);
      for (int attempt = 0; attempt < 36; attempt++) {
         BlockPos pos = center.offset(entity.getRandom().nextIntBetweenInclusive(-16, 16),
            entity.getRandom().nextIntBetweenInclusive(-3, 3), entity.getRandom().nextIntBetweenInclusive(-16, 16));
         if (!isValidRetreatDestination(entity, pos)) continue;
         double distance = threat.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
         double score = distance - entity.level().getMaxLocalRawBrightness(pos) * 4.0;
         if (score > bestScore) {
            bestScore = score;
            best = pos;
         }
      }
      if (best != null) {
         ServantNavigationHelper.moveToPositionThrottled(entity, Vec3.atBottomCenterOf(best), 1.35, now, 20, 1.5, "HundredFacesRetreat");
      } else {
         Vec3 away = entity.position().subtract(threat.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() > 1.0E-4) entity.getNavigation().moveTo(
            entity.getX() + away.normalize().x * 12.0, entity.getY(), entity.getZ() + away.normalize().z * 12.0, 1.25);
      }
   }

   private static boolean personaCooldownReady(ServantEntity entity, String key, long now, int cooldown) {
      CompoundTag data = entity.getPersistentData();
      if (!data.contains(key)) {
         data.putLong(key, now + entity.getRandom().nextInt(Math.max(1, cooldown)));
         return false;
      }
      if (now < data.getLong(key)) return false;
      data.putLong(key, now + cooldown + entity.getRandom().nextInt(40));
      return true;
   }

   private static void spawnDirkProjectile(ServantEntity entity, LivingEntity target, float damage, float velocity) {
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      DirkProjectileEntity projectile = new DirkProjectileEntity(entity.level(), entity);
      projectile.setDamage(damage);
      projectile.setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ());
      Vec3 direction = aim.subtract(projectile.position()).normalize();
      projectile.shoot(direction.x, direction.y + 0.03, direction.z, velocity, 0.0F);
      entity.level().addFreshEntity(projectile);
      if (entity.level() instanceof ServerLevel level) {
         level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.72F, 1.55F);
      }
   }

   private static boolean teleportBehind(ServantEntity entity, LivingEntity target, double offset) {
      Vec3 look = target.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      if (horizontal.lengthSqr() < 1.0E-4) return false;
      Vec3 behind = target.position().subtract(horizontal.normalize().scale(offset));
      if (!entity.level().noCollision(entity, entity.getBoundingBox().move(behind.subtract(entity.position())))) return false;
      spawnShadowStepFx(entity);
      entity.teleportTo(behind.x, target.getY(), behind.z);
      entity.setDeltaMovement(Vec3.ZERO);
      entity.fallDistance = 0.0F;
      entity.getNavigation().stop();
      entity.setTarget(target);
      entity.faceToward(target.position());
      entity.getLookControl().setLookAt(target, 60.0F, 60.0F);
      return true;
   }

   private static void doShadowLunge(ServantEntity entity, LivingEntity target, double pushSpeed, float damageScale) {
      Vec3 dir = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() < 1.0E-4) return;
      horizontal = horizontal.normalize();
      entity.faceVector(horizontal);
      entity.triggerNamedActionAnimation("shadow_lunge");
      entity.setDeltaMovement(horizontal.x * pushSpeed, Math.max(entity.getDeltaMovement().y, 0.16), horizontal.z * pushSpeed);
      entity.hasImpulse = true;
      dealScaledKnifeDamage(entity, target, damageScale);
      target.push(horizontal.x * 0.55, 0.18, horizontal.z * 0.55);
      target.hurtMarked = true;
      spawnShadowStepFx(entity);
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
         level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.76F, 1.62F);
      }
   }

   private static void dealScaledKnifeDamage(ServantEntity entity, LivingEntity target, float scale) {
      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * scale);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
   }

   private static BlockPos findSafeTeleportFeet(ServerLevel level, BlockPos anchor) {
      for (int dy = -2; dy <= 3; dy++) {
         BlockPos feet = anchor.offset(0, dy, 0);
         BlockPos below = feet.below();
         if (level.getBlockState(below).isSolidRender(level, below)
            && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
            && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
            return feet;
         }
      }
      return null;
   }

   private static void spawnShadowStepFx(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      level.sendParticles(ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), 16, 0.25, 0.35, 0.25, 0.04);
      level.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(), 6, 0.2, 0.25, 0.2, 0.01);
      level.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.7F, 1.38F);
   }

   private static void spawnBackstabFx(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 8, 0.18, 0.22, 0.18, 0.03);
      level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 0.78F, 1.28F);
   }

   private static void spawnCritFx(ServantEntity entity, LivingEntity target, int particles, float pitch) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.58, target.getZ(), particles, 0.26, 0.24, 0.26, 0.08);
      level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.72F, pitch);
   }

   private static boolean isValidRetreatDestination(HundredFacesHassanEntity entity, BlockPos pos) {
      BlockState state = entity.level().getBlockState(pos);
      return state.getCollisionShape(entity.level(), pos).isEmpty()
         && entity.level().getBlockState(pos.above()).getCollisionShape(entity.level(), pos.above()).isEmpty()
         && entity.level().getBlockState(pos.below()).isFaceSturdy(entity.level(), pos.below(), Direction.UP);
   }

   private static Vec3 safeSpawnPosition(HundredFacesHassanEntity owner, Vec3 offset) {
      Vec3 preferred = owner.position().add(offset.x, 0.0, offset.z);
      if (owner.level().noCollision(owner.getBoundingBox().move(preferred.subtract(owner.position())))) return preferred;
      return owner.position();
   }

   private static Vec3 horizontal(Vec3 look) {
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      return horizontal.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }
}
