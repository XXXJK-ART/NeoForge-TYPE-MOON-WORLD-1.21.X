package net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanPersonaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;

public final class HundredFacesHassanCombatHelper {
   private static final String LAST_RETREAT = "HundredFacesLastRetreat";
   private static final String LAST_SWARM = "HundredFacesLastSwarm";
   private static final String LAST_BASIC = "HundredFacesLastBasic";
   private static final String LAST_DIRK_TICK = "HundredFacesLastDirkTick";
   private static final String LAST_REPOSITION_TICK = "HundredFacesLastRepositionTick";
   private static final String LAST_SHADOW_STEP_TICK = "HundredFacesLastShadowStepTick";
   private static final String LAST_KNIFE_FEINT_TICK = "HundredFacesLastKnifeFeintTick";
   private static final String LAST_SHADOW_LUNGE_TICK = "HundredFacesLastShadowLungeTick";
   private static final String LAST_REACTIVE_DODGE_TICK = "HundredFacesLastReactiveDodgeTick";
   private static final String FOOTWORK_SIDE = "HundredFacesFootworkSide";
   private static final String FOOTWORK_SIDE_UNTIL = "HundredFacesFootworkSideUntil";
   private static final String LAST_PERSONA_DIRK_TICK = "HundredFacesPersonaLastDirkTick";

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
         summonPersonas(entity, level, target, HundredFacesHassanEntity.PersonaMode.THROWING, desiredBatch(entity, enemies, 10));
         return;
      }

      int live = countOwnedPersonas(entity);
      int desired = enemies.size() >= 3 || isStrongTarget(target) ? 10 : 4;
      if (live < desired && now - entity.getPersistentData().getLong(LAST_SWARM) >= 30L) {
         HundredFacesHassanEntity.PersonaMode mode = enemies.size() >= 3
            ? HundredFacesHassanEntity.PersonaMode.THROWING
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
      boolean footwork = applyAssassinFootwork(entity, target, distance, now);
      if (distance > 2.25) {
         if (footwork && distance <= 5.8) return;
         boolean moving = ServantNavigationHelper.moveToTargetThrottled(entity, target, 1.12, now,
            ServantNavigationHelper.SHORT_REPATH_INTERVAL, 0.8, "HundredFacesChase");
         if (!moving) tryRepositionNearTarget(entity, target, distance, now);
      } else if (now - entity.getPersistentData().getLong(LAST_BASIC) >= 18L && !entity.isPerformingAction()) {
         entity.getPersistentData().putLong(LAST_BASIC, now);
         entity.triggerAssassinStabAnimation();
         entity.doHurtTarget(target);
      }
   }

   public static void tryReactiveDodge(HundredFacesHassanEntity entity, DamageSource source) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive() || entity.isPerformingAction()) return;
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(LAST_REACTIVE_DODGE_TICK) < 28L) return;
      Vec3 direction = dodgeDirection(entity, source);
      if (direction.lengthSqr() < 1.0E-4) return;
      Vec3 sidestep = direction.normalize().scale(1.15 + entity.getRandom().nextDouble() * 0.45);
      if (!level.noCollision(entity, entity.getBoundingBox().move(sidestep.x, 0.12, sidestep.z))) {
         sidestep = sidestep.scale(-1.0);
         if (!level.noCollision(entity, entity.getBoundingBox().move(sidestep.x, 0.12, sidestep.z))) return;
      }
      data.putLong(LAST_REACTIVE_DODGE_TICK, now);
      entity.triggerShadowStepAnimation();
      entity.setDeltaMovement(entity.getDeltaMovement().x + sidestep.x * 0.38,
         Math.max(entity.getDeltaMovement().y, 0.12),
         entity.getDeltaMovement().z + sidestep.z * 0.38);
      entity.hasImpulse = true;
      entity.invulnerableTime = Math.max(entity.invulnerableTime, 8);
      ServantNavigationHelper.rememberMovementWriter(entity, now, "HundredFacesReactiveDodge");
      spawnShadowStepFx(entity);
   }

   public static boolean isFriendlyFire(LivingEntity victim, DamageSource source) {
      if (victim == null || source == null) return false;
      LivingEntity attacker = resolveLivingAttacker(source);
      return attacker != null && areSameHundredFacesSide(victim, attacker);
   }

   public static boolean areSameHundredFacesSide(@Nullable Entity first, @Nullable Entity second) {
      if (first == null || second == null || first == second) return false;
      UUID firstOwner = hundredFacesOwnerKey(first);
      UUID secondOwner = hundredFacesOwnerKey(second);
      return firstOwner != null && firstOwner.equals(secondOwner) && (isHundredFacesUnit(first) || isHundredFacesUnit(second));
   }

   @Nullable
   private static LivingEntity resolveLivingAttacker(DamageSource source) {
      Entity attacker = source.getEntity();
      if (attacker instanceof LivingEntity living) return living;
      Entity direct = source.getDirectEntity();
      if (direct instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) return owner;
      return direct instanceof LivingEntity living ? living : null;
   }

   @Nullable
   private static UUID hundredFacesOwnerKey(Entity entity) {
      if (entity instanceof HundredFacesHassanPersonaEntity persona) return persona.getOwnerUuid();
      if (entity instanceof HundredFacesHassanEntity) return entity.getUUID();
      if (entity instanceof Player) return entity.getUUID();
      return null;
   }

   private static boolean isHundredFacesUnit(Entity entity) {
      return entity instanceof HundredFacesHassanEntity || entity instanceof HundredFacesHassanPersonaEntity;
   }

   public static boolean tryPersonaCombatSkill(HundredFacesHassanPersonaEntity entity, LivingEntity target, double distance, long now) {
      if (target == null || !target.isAlive() || entity.isPerformingAction()) return false;
      return switch (entity.getPersonaMode()) {
         case THROWING -> tryPersonaThrowDirk(entity, target, distance, now);
         case INDUCE -> tryPersonaInduce(entity, target, distance, now);
         case ASSASSINATION, POISON, SCOUT, SUPPORT -> false;
      };
   }

   public static boolean isBehindTarget(LivingEntity attacker, LivingEntity target) {
      if (attacker == null || target == null) return false;
      Vec3 facing = target.getLookAngle().multiply(1.0, 0.0, 1.0);
      Vec3 fromTarget = attacker.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      return facing.lengthSqr() > 1.0E-4 && fromTarget.lengthSqr() > 1.0E-4
         && facing.normalize().dot(fromTarget.normalize()) < -0.5;
   }

   public static int summonPersonas(HundredFacesHassanEntity owner, ServerLevel level, @Nullable LivingEntity target,
                                    HundredFacesHassanEntity.PersonaMode mode, int requested) {
      int live = countOwnedPersonas(owner);
      int alreadySplit = owner.getTotalSplitCount();
      int count = HundredFacesHassanRules.affordableSummonCount(owner.getCurrentMp(), alreadySplit,
         Math.min(requested, HundredFacesHassanRules.MAX_SUMMON_BATCH));
      if (count <= 0) return 0;
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
         if (pos == null) break;
         float yaw = (float)Math.toDegrees(Math.atan2(-forward.x, forward.z));
         persona.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
         persona.initialize(owner, target, chooseNpcPersonaMode(owner, target, alreadySplit + spawned, mode), alreadySplit + spawned + 1);
         if (!level.addFreshEntity(persona)) {
            persona.discard();
            continue;
         }
         spawned++;
         level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 0.9, pos.z, 12, 0.35, 0.55, 0.35, 0.02);
      }
      if (spawned > 0) {
         owner.revealForCombat();
         owner.triggerNamedActionAnimation("zabaniya");
         owner.setCurrentMp(Math.max(0.0, owner.getCurrentMp() - spawned * HundredFacesHassanRules.MP_PER_PERSONA));
         owner.getPersistentData().putLong(LAST_SWARM, level.getGameTime());
         if (spawned >= 20) {
            ServantVoiceHelper.tryPlayHundredFacesNp(owner);
         }
         level.sendParticles(ParticleTypes.SMOKE, owner.getX(), owner.getY() + 1.0, owner.getZ(),
            24, 0.8, 0.8, 0.8, 0.03);
         owner.addTotalSplitCount(spawned);
         rescaleOwnedPersonas(owner);
      }
      return spawned;
   }

   private static HundredFacesHassanEntity.PersonaMode chooseNpcPersonaMode(HundredFacesHassanEntity owner,
      @Nullable LivingEntity target, int index, HundredFacesHassanEntity.PersonaMode requestedMode) {
      if (target == null) return index % 3 == 0 ? HundredFacesHassanEntity.PersonaMode.SCOUT
         : HundredFacesHassanEntity.PersonaMode.SUPPORT;
      int nearbyEnemies = enemiesAround(owner, 10.0).size();
      if (nearbyEnemies >= 2 && index % 3 == 0) return HundredFacesHassanEntity.PersonaMode.INDUCE;
      if (owner.getHealth() <= owner.getMaxHealth() * 0.35F && index % 5 == 0) return HundredFacesHassanEntity.PersonaMode.SUPPORT;
      if (owner.distanceTo(target) > 6.0 || requestedMode == HundredFacesHassanEntity.PersonaMode.THROWING && index % 2 == 0)
         return HundredFacesHassanEntity.PersonaMode.THROWING;
      return index % 3 == 0 ? HundredFacesHassanEntity.PersonaMode.POISON
         : HundredFacesHassanEntity.PersonaMode.ASSASSINATION;
   }

   public static int countOwnedPersonas(HundredFacesHassanEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return 0;
      return ownedPersonas(level, owner, true).size();
   }

   public static void rescaleOwnedPersonas(HundredFacesHassanEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      List<HundredFacesHassanPersonaEntity> personas = ownedPersonas(level, owner, true);
      int splitCount = Math.max(1, owner.getTotalSplitCount());
      owner.applySplitAttributes(false);
      for (HundredFacesHassanPersonaEntity persona : personas) {
         persona.syncOwnerCombatAttributes(owner);
         persona.applyPersonaAttributes(splitCount, false);
      }
   }

   public static void discardOwnedPersonas(HundredFacesHassanEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      for (HundredFacesHassanPersonaEntity persona : ownedPersonas(level, owner, false)) {
         persona.discard();
      }
   }

   public static boolean tryTransferBodyOnLethalDamage(HundredFacesHassanEntity owner, DamageSource source) {
      if (!(owner.level() instanceof ServerLevel level) || owner.getPersistentData().getBoolean("HundredFacesBodyTransferActive")) {
         return false;
      }
      HundredFacesHassanPersonaEntity vessel = selectTransferVessel(level, owner);
      if (vessel == null) return false;

      owner.getPersistentData().putBoolean("HundredFacesBodyTransferActive", true);
      CompoundTag snapshot = owner.saveWithoutId(new CompoundTag());
      snapshot.remove("UUID");
      HundredFacesHassanEntity replacement = ModEntities.HUNDRED_FACES_HASSAN.get().create(level);
      if (replacement == null) {
         owner.getPersistentData().remove("HundredFacesBodyTransferActive");
         return false;
      }

      Vec3 vesselPos = vessel.position();
      float vesselYaw = vessel.getYRot();
      float vesselPitch = vessel.getXRot();
      float recoveryHealth = Math.max(1.0F,
         Math.min((float)owner.getMaxHealth(), Math.max(vessel.getHealth(), (float)(owner.getMaxHealth() * 0.35F))));
      LivingEntity target = owner.getTarget();

      replacement.load(snapshot);
      replacement.moveTo(vesselPos.x, vesselPos.y, vesselPos.z, vesselYaw, vesselPitch);
      replacement.setYBodyRot(vessel.yBodyRot);
      replacement.setYHeadRot(vessel.getYHeadRot());
      replacement.setDeltaMovement(Vec3.ZERO);
      replacement.clearFire();
      replacement.setTarget(target != null && target.isAlive() ? target : vessel.getTarget());
      replacement.setPersonaMode(vessel.getPersonaMode());
      replacement.getPersistentData().merge(owner.getPersistentData().copy());
      replacement.getPersistentData().remove("HundredFacesBodyTransferActive");
      replacement.getPersistentData().putLong("HundredFacesTransferredAt", level.getGameTime());
      replacement.setCurrentMp(owner.getCurrentMp());
      replacement.applySplitAttributes(false);
      replacement.setHealth(recoveryHealth);
      replacement.invulnerableTime = 40;
      replacement.applyBodyTransferConcealment(50);
      replacement.setPersistenceRequired();
      replacement.refreshDimensions();

      if (!level.addFreshEntity(replacement)) {
         replacement.discard();
         owner.getPersistentData().remove("HundredFacesBodyTransferActive");
         return false;
      }

      vessel.discard();
      owner.remove(RemovalReason.DISCARDED);
      reassignOwnedPersonas(level, owner.getUUID(), replacement.getUUID());
      rescaleOwnedPersonas(replacement);
      retargetEnemies(level, owner, replacement);
      playBodyTransferEffects(level, owner.position(), vesselPos);
      return true;
   }

   @Nullable
   private static HundredFacesHassanPersonaEntity selectTransferVessel(ServerLevel level, HundredFacesHassanEntity owner) {
      return ownedPersonas(level, owner, true).stream()
         .max(Comparator.comparingDouble(HundredFacesHassanPersonaEntity::getHealth)
            .thenComparingDouble(persona -> -persona.distanceToSqr(owner)))
         .orElse(null);
   }

   private static void reassignOwnedPersonas(ServerLevel level, java.util.UUID oldOwner, java.util.UUID newOwner) {
      for (Entity entity : level.getEntities().getAll()) {
         if (entity instanceof HundredFacesHassanPersonaEntity persona && oldOwner.equals(persona.getOwnerUuid())) {
            persona.setOwnerUuid(newOwner);
         }
      }
   }

   private static void retargetEnemies(ServerLevel level, HundredFacesHassanEntity oldOwner, HundredFacesHassanEntity replacement) {
      for (Mob mob : level.getEntitiesOfClass(Mob.class, replacement.getBoundingBox().inflate(96.0))) {
         if (mob.getTarget() == oldOwner) mob.setTarget(replacement);
         if (mob.getLastHurtByMob() == oldOwner) mob.setLastHurtByMob(replacement);
      }
   }

   private static void playBodyTransferEffects(ServerLevel level, Vec3 oldPos, Vec3 newPos) {
      level.sendParticles(ParticleTypes.SQUID_INK, oldPos.x, oldPos.y + 0.8, oldPos.z,
         30, 0.45, 0.55, 0.45, 0.03);
      level.sendParticles(ParticleTypes.SMOKE, oldPos.x, oldPos.y + 0.6, oldPos.z,
         24, 0.55, 0.45, 0.55, 0.035);
      level.sendParticles(ParticleTypes.SOUL, newPos.x, newPos.y + 0.6, newPos.z,
         24, 0.45, 0.7, 0.45, 0.04);
      level.sendParticles(ParticleTypes.POOF, newPos.x, newPos.y + 0.5, newPos.z,
         18, 0.45, 0.45, 0.45, 0.04);
      level.playSound(null, BlockPos.containing(newPos), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 0.8F, 1.3F);
      level.playSound(null, BlockPos.containing(oldPos), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.75F, 1.45F);
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
      if (distance < 3.0 || distance > 18.0 || entity.getCurrentMp() < HundredFacesHassanRules.MP_DIRK_THROW
         || now - data.getLong(LAST_DIRK_TICK) < HundredFacesHassanRules.DIRK_COOLDOWN_TICKS) return false;
      if (!entity.getSensing().hasLineOfSight(target) || entity.isPerformingAction() || !hasUsableDirks(entity, 3)) return false;
      data.putLong(LAST_DIRK_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - HundredFacesHassanRules.MP_DIRK_THROW);
      consumeDirks(entity, 3);
      entity.revealForCombat();
      entity.triggerDirkThrowAnimation();
      spawnDirkVolley(entity, target, 14.0F, 2.2F);
      return true;
   }

   private static boolean tryKnifeFeint(HundredFacesHassanEntity entity, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (distance < 2.5 || distance > 9.0 || entity.getCurrentMp() < HundredFacesHassanRules.MP_KNIFE_FEINT
         || now - data.getLong(LAST_KNIFE_FEINT_TICK) < HundredFacesHassanRules.KNIFE_FEINT_COOLDOWN_TICKS
         || !entity.getSensing().hasLineOfSight(target) || entity.isPerformingAction() || !hasUsableDirks(entity, 1)) return false;
      data.putLong(LAST_KNIFE_FEINT_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - HundredFacesHassanRules.MP_KNIFE_FEINT);
      entity.revealForCombat();
      entity.faceToward(target.position());
      entity.triggerDirkThrowAnimation();
      consumeDirks(entity, 1);
      spawnDirkProjectile(entity, target, 10.0F, 2.2F, 35, 35);
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
      boolean landed = doShadowLunge(entity, target, 1.45, 1.0F);
      if (landed && entity.getPersonaMode() == HundredFacesHassanEntity.PersonaMode.POISON) applyWeakPoison(entity, target);
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
      boolean landed = dealScaledKnifeDamage(entity, target, 1.45F);
      if (landed) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true), entity);
      if (landed && entity.getPersonaMode() == HundredFacesHassanEntity.PersonaMode.POISON) applyWeakPoison(entity, target);
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
      if (distance < 4.0 || distance > 18.0 || !entity.getSensing().hasLineOfSight(target)
         || !hasUsableDirks(entity, 1)
         || !personaActionReady(entity, LAST_PERSONA_DIRK_TICK, now, HundredFacesHassanRules.MP_DIRK_THROW)) return false;
      consumeDirks(entity, 1);
      commitPersonaAction(entity, LAST_PERSONA_DIRK_TICK, now, HundredFacesHassanRules.DIRK_COOLDOWN_TICKS,
         HundredFacesHassanRules.MP_DIRK_THROW);
      entity.revealForCombat(HundredFacesHassanRules.PERSONA_DIRK_EXPOSURE_TICKS);
      entity.triggerDirkThrowAnimation();
      spawnDirkProjectile(entity, target, (float)entity.getAttributeValue(Attributes.ATTACK_DAMAGE), 1.75F);
      return true;
   }

   private static boolean tryPersonaInduce(HundredFacesHassanPersonaEntity entity, LivingEntity target, double distance, long now) {
      if (distance < 2.5 || distance > 9.0 || !entity.getSensing().hasLineOfSight(target)
         || !hasUsableDirks(entity, 1)
         || !personaActionReady(entity, LAST_PERSONA_DIRK_TICK, now, HundredFacesHassanRules.MP_KNIFE_FEINT)) return false;
      consumeDirks(entity, 1);
      commitPersonaAction(entity, LAST_PERSONA_DIRK_TICK, now, HundredFacesHassanRules.KNIFE_FEINT_COOLDOWN_TICKS,
         HundredFacesHassanRules.MP_KNIFE_FEINT);
      entity.revealForCombat(HundredFacesHassanRules.PERSONA_DIRK_EXPOSURE_TICKS);
      entity.triggerDirkThrowAnimation();
      spawnDirkProjectile(entity, target, 10.0F, 2.2F, 35, 35);
      return true;
   }

   private static boolean applyAssassinFootwork(HundredFacesHassanEntity entity, LivingEntity target, double distance, long now) {
      if (target == null || !target.isAlive() || entity.isPerformingAction() || distance > 7.0) return false;
      if (!ServantNavigationHelper.movementAvailable(entity, now, "HundredFacesFootwork")) return false;
      CompoundTag data = entity.getPersistentData();
      if (!data.contains(FOOTWORK_SIDE) || now >= data.getLong(FOOTWORK_SIDE_UNTIL)) {
         int side = data.contains(FOOTWORK_SIDE) ? -data.getInt(FOOTWORK_SIDE)
            : (entity.getRandom().nextBoolean() ? 1 : -1);
         data.putInt(FOOTWORK_SIDE, side == 0 ? 1 : side);
         data.putLong(FOOTWORK_SIDE_UNTIL, now + 14L + entity.getRandom().nextInt(18));
      }

      entity.getNavigation().stop();
      entity.getLookControl().setLookAt(target, 70.0F, 60.0F);
      entity.faceToward(target.position());
      int side = data.getInt(FOOTWORK_SIDE);
      float forward = distance < 1.8 ? -0.34F : distance > 4.8 ? 0.34F : 0.08F;
      float lateral = side * (distance < 2.8 ? 0.78F : 0.62F);
      entity.getMoveControl().strafe(forward, lateral);

      Vec3 toward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (toward.lengthSqr() > 1.0E-4) {
         Vec3 direction = toward.normalize();
         Vec3 right = new Vec3(-direction.z, 0.0, direction.x).scale(side);
         Vec3 desired = direction.scale(distance > 4.8 ? 0.12 : distance < 1.8 ? -0.10 : 0.02)
            .add(right.scale(0.18 + Math.min(0.10, entity.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.25)));
         if (entity.level().noCollision(entity, entity.getBoundingBox().move(desired.x, 0.0, desired.z))) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x * 0.72 + desired.x, motion.y, motion.z * 0.72 + desired.z);
            entity.hasImpulse = true;
         }
      }
      ServantNavigationHelper.rememberMovementWriter(entity, now, "HundredFacesFootwork");
      return true;
   }

   private static Vec3 dodgeDirection(HundredFacesHassanEntity entity, DamageSource source) {
      Vec3 threat = source.getSourcePosition();
      if (threat == null && source.getEntity() != null) threat = source.getEntity().position();
      Vec3 away = threat == null ? Vec3.ZERO : entity.position().subtract(threat).multiply(1.0, 0.0, 1.0);
      LivingEntity target = entity.getTarget();
      if (target != null && target.isAlive()) {
         Vec3 towardTarget = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
         if (towardTarget.lengthSqr() > 1.0E-4) {
            Vec3 side = new Vec3(-towardTarget.z, 0.0, towardTarget.x)
               .scale(entity.getRandom().nextBoolean() ? 1.0 : -1.0);
            if (away.lengthSqr() > 1.0E-4 && side.dot(away) < 0.0) side = side.scale(-1.0);
            return side.add(away.lengthSqr() > 1.0E-4 ? away.normalize().scale(0.45) : Vec3.ZERO);
         }
      }
      if (away.lengthSqr() > 1.0E-4) return away;
      double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
      return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
   }

   private static boolean shouldRetreat(HundredFacesHassanEntity entity, LivingEntity target,
                                        List<LivingEntity> enemies, long now) {
      if (entity.getCurrentMp() < HundredFacesHassanRules.MP_PER_PERSONA) return false;
      if (now - entity.getPersistentData().getLong(LAST_RETREAT) < HundredFacesHassanRules.RETREAT_COOLDOWN_TICKS) return false;
      return entity.getHealth() <= entity.getMaxHealth() * 0.35F
         || enemies.size() >= 2;
   }

   private static void startRetreat(HundredFacesHassanEntity entity, LivingEntity target, long now) {
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - HundredFacesHassanRules.MP_PER_PERSONA));
      entity.getPersistentData().putLong(LAST_RETREAT, now);
      entity.startRetreat(now);
      cleanseHarmfulEffects(entity);
      retreatFrom(entity, target, now);
   }

   private static void maybeSummonScouts(HundredFacesHassanEntity entity, ServerLevel level, long now) {
      if (now - entity.getPersistentData().getLong(LAST_SWARM) < 200L
         || countOwnedPersonas(entity) >= 5
         || entity.getTotalSplitCount() >= HundredFacesHassanRules.MAX_PERSONAS) return;
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

   private static boolean personaActionReady(ServantEntity entity, String key, long now, double manaCost) {
      CompoundTag data = entity.getPersistentData();
      return entity.getCurrentMp() >= manaCost && now >= data.getLong(key);
   }

   private static void commitPersonaAction(ServantEntity entity, String key, long now, int cooldown, double manaCost) {
      entity.getPersistentData().putLong(key, now + cooldown);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - manaCost));
   }

   private static void spawnDirkProjectile(ServantEntity entity, LivingEntity target, float damage, float velocity) {
      spawnDirkProjectile(entity, target, damage, velocity, 0, 0);
   }

   private static void spawnDirkProjectile(ServantEntity entity, LivingEntity target, float damage, float velocity,
                                          int slowTicks, int weaknessTicks) {
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      DirkProjectileEntity projectile = new DirkProjectileEntity(entity.level(), entity);
      projectile.setDamage(damage);
      projectile.setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ());
      projectile.setMaxDistance(18.0);
      projectile.setOnHitEffects(slowTicks, weaknessTicks);
      Vec3 direction = aim.subtract(projectile.position()).normalize();
      projectile.shoot(direction.x, direction.y + 0.03, direction.z, velocity, 0.0F);
      entity.level().addFreshEntity(projectile);
      if (entity.level() instanceof ServerLevel level) {
         level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.72F, 1.55F);
      }
   }

   private static void spawnDirkVolley(ServantEntity entity, LivingEntity target, float damage, float velocity) {
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0)
         .subtract(entity.position().add(0.0, entity.getBbHeight() * 0.65, 0.0)).normalize();
      for (int i = -1; i <= 1; i++) {
         Vec3 direction = aim.yRot((float)Math.toRadians(i * 5.0F));
         DirkProjectileEntity projectile = new DirkProjectileEntity(entity.level(), entity);
         projectile.setDamage(damage);
         projectile.setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ());
         projectile.setMaxDistance(18.0);
         projectile.shoot(direction.x, direction.y, direction.z, velocity, 0.0F);
         entity.level().addFreshEntity(projectile);
      }
   }

   private static ItemStack dirkStack(ServantEntity entity) {
      ItemStack mainHand = entity.getMainHandItem();
      if (mainHand.is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.DIRK_SMALL_KNIFE.get())) return mainHand;
      ItemStack offHand = entity.getOffhandItem();
      return offHand.is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.DIRK_SMALL_KNIFE.get()) ? offHand : ItemStack.EMPTY;
   }

   private static boolean hasUsableDirks(ServantEntity entity, int count) {
      ItemStack stack = dirkStack(entity);
      return !stack.isEmpty() && stack.getMaxDamage() - stack.getDamageValue() >= count;
   }

   private static void consumeDirks(ServantEntity entity, int count) {
      ItemStack stack = dirkStack(entity);
      if (stack.isEmpty()) return;
      EquipmentSlot slot = stack == entity.getMainHandItem() ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
      for (int i = 0; i < count && !stack.isEmpty(); i++) {
         stack.hurtAndBreak(1, entity, slot);
      }
   }

   private static boolean teleportBehind(ServantEntity entity, LivingEntity target, double offset) {
      Vec3 look = target.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      if (horizontal.lengthSqr() < 1.0E-4 || !(entity.level() instanceof ServerLevel level)) return false;
      Vec3 behind = target.position().subtract(horizontal.normalize().scale(offset));
      BlockPos feet = findSafeTeleportFeet(level, BlockPos.containing(behind.x, target.getY(), behind.z));
      if (feet == null || !level.getWorldBorder().isWithinBounds(feet)) return false;
      Vec3 destination = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
      if (!level.noCollision(entity, entity.getBoundingBox().move(destination.subtract(entity.position())))) return false;
      Vec3 startEye = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
      Vec3 endEye = destination.add(0.0, entity.getBbHeight() * 0.55, 0.0);
      if (level.clip(new ClipContext(startEye, endEye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType()
         != net.minecraft.world.phys.HitResult.Type.MISS) return false;
      spawnShadowStepFx(entity);
      entity.teleportTo(destination.x, destination.y, destination.z);
      entity.setDeltaMovement(Vec3.ZERO);
      entity.fallDistance = 0.0F;
      entity.getNavigation().stop();
      entity.setTarget(target);
      entity.faceToward(target.position());
      entity.getLookControl().setLookAt(target, 60.0F, 60.0F);
      return true;
   }

   private static boolean doShadowLunge(ServantEntity entity, LivingEntity target, double pushSpeed, float damageScale) {
      Vec3 dir = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() < 1.0E-4) return false;
      horizontal = horizontal.normalize();
      double travelDistance = Math.min(pushSpeed, Math.max(0.0, entity.distanceTo(target) - 1.0));
      if (travelDistance <= 0.0) return false;
      for (double travel = 0.2; travel <= travelDistance + 1.0E-6; travel += 0.2) {
         Vec3 candidate = entity.position().add(horizontal.scale(Math.min(travel, travelDistance)));
         if (!entity.level().noCollision(entity, entity.getBoundingBox().move(candidate.subtract(entity.position())))) return false;
      }
      entity.faceVector(horizontal);
      entity.triggerNamedActionAnimation("shadow_lunge");
      entity.setDeltaMovement(horizontal.x * pushSpeed, Math.max(entity.getDeltaMovement().y, 0.16), horizontal.z * pushSpeed);
      entity.hasImpulse = true;
      boolean landed = dealScaledKnifeDamage(entity, target, damageScale);
      if (landed) {
         target.push(horizontal.x * 0.55, 0.18, horizontal.z * 0.55);
         target.hurtMarked = true;
      }
      spawnShadowStepFx(entity);
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
         level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.76F, 1.62F);
      }
      return landed;
   }

   private static boolean dealScaledKnifeDamage(ServantEntity entity, LivingEntity target, float scale) {
      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * scale);
      boolean landed = target.hurt(entity.damageSources().mobAttack(entity), damage);
      return landed;
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

   @Nullable
   private static Vec3 safeSpawnPosition(HundredFacesHassanEntity owner, Vec3 offset) {
      Vec3 preferred = owner.position().add(offset.x, 0.0, offset.z);
      if (owner.level().noCollision(owner.getBoundingBox().move(preferred.subtract(owner.position())))) return preferred;
      BlockPos base = owner.blockPosition();
      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            Vec3 candidate = new Vec3(base.getX() + dx + 0.5, owner.getY(), base.getZ() + dz + 0.5);
            if (owner.level().noCollision(owner.getBoundingBox().move(candidate.subtract(owner.position())))) return candidate;
         }
      }
      return null;
   }

   private static Vec3 horizontal(Vec3 look) {
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      return horizontal.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }
}
