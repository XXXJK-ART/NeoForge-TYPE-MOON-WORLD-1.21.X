package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RedSkeletonHajunEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.UBWInstanceManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class OdaNobunagaCombatHelper {
   public static final String TAG_LAST_STRATEGY = "OdaLastStrategyTick";
   public static final String TAG_STRATEGY_UNTIL = "OdaStrategyUntil";
   public static final String TAG_LAST_MAOU = "OdaLastMaouTick";
   public static final String TAG_MAOU_UNTIL = "OdaMaouUntil";
   public static final String TAG_LAST_THREE_THOUSAND = "OdaLastThreeThousandTick";
   public static final String TAG_LAST_RIFLE_SHOT = "OdaLastRifleShotTick";
   public static final String TAG_LAST_HAJUN = "OdaLastHajunTick";
   public static final String TAG_LAST_FLOATING_MATCHLOCK = "OdaLastFloatingMatchlockTick";
   public static final String TAG_LAST_MATCHLOCK_VOLLEY = "OdaLastMatchlockVolleyTick";
   public static final String TAG_LAST_FIRE_BARRAGE = "OdaLastFireBarrageTick";
   public static final String TAG_LAST_ATSUMORI_STEP = "OdaLastAtsumoriStepTick";
   public static final String TAG_LAST_ANTI_MYSTERY_SPARK = "OdaLastAntiMysterySparkTick";
   public static final String TAG_LAST_DEMON_KING_PRESSURE = "OdaLastDemonKingPressureTick";
   public static final String TAG_LAST_HASEBE_REPEL = "OdaLastHasebeRepelTick";
   public static final String TAG_LAST_ASH_FIELD = "OdaLastAshFieldTick";
   public static final String TAG_LAST_SCORCHED_BANNER = "OdaLastScorchedBannerTick";
   public static final String TAG_LAST_THREE_LINE_ROTATION = "OdaLastThreeLineRotationTick";
   public static final String TAG_HAJUN_CHANT_END = "OdaHajunChantEnd";
   public static final String TAG_HAJUN_CHANT_TARGET = "OdaHajunChantTarget";
   public static final String TAG_HAJUN_ACTIVE_UNTIL = "OdaHajunActiveUntil";
   public static final String TAG_HAJUN_RETURN_DIM = "OdaHajunReturnDim";
   public static final String TAG_HAJUN_RETURN_X = "OdaHajunReturnX";
   public static final String TAG_HAJUN_RETURN_Y = "OdaHajunReturnY";
   public static final String TAG_HAJUN_RETURN_Z = "OdaHajunReturnZ";
   public static final String TAG_HAJUN_TARGET_OWNER = "OdaHajunTargetOwner";
   public static final String TAG_HAJUN_TARGET_RETURN_DIM = "OdaHajunTargetReturnDim";
   public static final String TAG_HAJUN_TARGET_RETURN_X = "OdaHajunTargetReturnX";
   public static final String TAG_HAJUN_TARGET_RETURN_Y = "OdaHajunTargetReturnY";
   public static final String TAG_HAJUN_TARGET_RETURN_Z = "OdaHajunTargetReturnZ";
   public static final String TAG_DIVINE_BREAK_UNTIL = "OdaDivineDefenseBreakUntil";

   private static final int STRATEGY_COOLDOWN = 20 * 20;
   private static final int STRATEGY_DURATION = 25 * 20;
   private static final int MAOU_COOLDOWN = 18 * 20;
   private static final int MAOU_DURATION = 15 * 20;
   private static final int THREE_THOUSAND_COOLDOWN = 30 * 20;
   private static final int HAJUN_COOLDOWN = 45 * 20;
   private static final int HAJUN_CHANT_TICKS = 5 * 20;
   private static final int HAJUN_DURATION = 15 * 20;
   private static final double HAJUN_RADIUS = 25.0;
   private static final int FLOATING_MATCHLOCK_COOLDOWN = 8 * 20;
   private static final int MATCHLOCK_VOLLEY_COOLDOWN = 5 * 20;
   private static final int FIRE_BARRAGE_COOLDOWN = 7 * 20;
   private static final int ATSUMORI_STEP_COOLDOWN = 6 * 20;
   private static final int ANTI_MYSTERY_SPARK_COOLDOWN = 10 * 20;
   private static final int DEMON_KING_PRESSURE_COOLDOWN = 12 * 20;
   private static final int HASEBE_REPEL_COOLDOWN = 5 * 20;
   private static final int ASH_FIELD_COOLDOWN = 14 * 20;
   private static final int SCORCHED_BANNER_COOLDOWN = 16 * 20;
   private static final int THREE_LINE_ROTATION_COOLDOWN = 18 * 20;

   private OdaNobunagaCombatHelper() {
   }

   public static void tick(OdaNobunagaEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }

      long now = level.getGameTime();
      if (tickHajunChant(entity, level, now) || tickHajunField(entity, level, now)) {
         return;
      }

      expireBuffs(entity, now);
      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         return;
      }

      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.55, 0.0));
      double distance = entity.distanceTo(target);
      ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
      boolean normalOrDecisive = phase.id() >= ServantCombatPhase.NORMAL.id();
      boolean decisive = phase == ServantCombatPhase.DECISIVE || entity.getHealth() <= entity.getMaxHealth() * 0.6F;
      boolean divine = hasTrait(target, ServantTraitTag.DIVINE);
      boolean mounted = hasTrait(target, ServantTraitTag.MOUNTED) || target.isPassenger() || target.getVehicle() != null;
      boolean mystery = divine || hasTrait(target, ServantTraitTag.DEMONIC) || hasTrait(target, ServantTraitTag.FAIRY_TALE);

      maintainFloatingMatchlocks(entity, level, now, target, phase);

      if (tryCastStrategy(entity, level, now)) {
         return;
      }
      if ((divine || decisive) && tryBeginHajun(entity, level, target, now)) {
         return;
      }
      if ((divine || mounted || normalOrDecisive) && tryCastThreeThousandWorlds(entity, level, target, now, divine || mounted || decisive)) {
         return;
      }
      if (tryCastMaou(entity, level, now, target, normalOrDecisive)) {
         return;
      }
      if (distance < 4.2 && tryCastHasebeRepel(entity, level, target, now)) {
         return;
      }
      if ((divine || decisive) && tryCastDemonKingPressure(entity, level, target, now)) {
         return;
      }
      if (mystery && tryCastAntiMysterySpark(entity, level, target, now)) {
         return;
      }
      if (distance < 7.0 && tryCastAtsumoriStep(entity, level, target, now)) {
         return;
      }
      if ((mounted || normalOrDecisive) && tryCastThreeLineRotation(entity, level, target, now)) {
         return;
      }
      if (tryCastScorchedBanner(entity, level, now, normalOrDecisive)) {
         return;
      }
      if (distance <= 18.0 && tryCastAshField(entity, level, target, now, decisive || divine)) {
         return;
      }
      if (distance <= 24.0 && tryCastFireBarrage(entity, level, target, now)) {
         return;
      }
      if (distance <= 26.0 && tryCastMatchlockVolley(entity, level, target, now)) {
         return;
      }

      if (distance < 8.0) {
         kiteBack(entity, target, 3.5);
      } else if (distance > 22.0) {
         entity.getNavigation().moveTo(target, 1.05);
      } else {
         entity.getNavigation().stop();
      }

      if (canUse(now, entity.getPersistentData().getLong(TAG_LAST_RIFLE_SHOT), hasMaou(entity, now) ? 18 : 26)) {
         shootRifle(entity, level, target, now);
      }
   }

   public static float scaleThreeThousandWorldsDamage(LivingEntity owner, LivingEntity target, float base) {
      float damage = base;
      if (hasTrait(target, ServantTraitTag.MOUNTED) || target.isPassenger() || target.getVehicle() != null) {
         damage *= 2.0F;
      }
      if (hasTrait(target, ServantTraitTag.DIVINE)) {
         damage *= 2.0F;
      }
      if (owner instanceof OdaNobunagaEntity nobu) {
         damage = applyTenkaFubuCap(nobu, target, damage);
      }
      return damage;
   }

   public static void applyDivineDefenseBreak(LivingEntity owner, LivingEntity target) {
      if (!(owner.level() instanceof ServerLevel level) || !hasTrait(target, ServantTraitTag.DIVINE)) {
         return;
      }
      target.getPersistentData().putLong(TAG_DIVINE_BREAK_UNTIL, level.getGameTime() + 100L);
      target.invulnerableTime = 0;
      target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      target.removeEffect(MobEffects.ABSORPTION);
      target.removeEffect(MobEffects.FIRE_RESISTANCE);
   }

   public static void cleanupHajun(OdaNobunagaEntity entity) {
      if (entity.level() instanceof ServerLevel level && ModDimensions.isHajunDimension(level.dimension().location())) {
         returnFromHajun(entity, level);
      }
   }

   private static boolean tryCastStrategy(OdaNobunagaEntity entity, ServerLevel level, long now) {
      if (entity.getCurrentMp() < 15.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_STRATEGY), STRATEGY_COOLDOWN)) {
         return false;
      }
      boolean hasAlly = !level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(20.0), living -> living != entity && living.isAlive() && living.isAlliedTo(entity)).isEmpty();
      if (!hasAlly && entity.getHealth() > entity.getMaxHealth() * 0.65F && entity.getRandom().nextInt(100) > 25) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 15.0);
      entity.getPersistentData().putLong(TAG_LAST_STRATEGY, now);
      entity.getPersistentData().putLong(TAG_STRATEGY_UNTIL, now + STRATEGY_DURATION);
      entity.triggerChargeAnimation();
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.8F, 1.35F);
      level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 1.0, entity.getZ(), 24, 1.0, 0.5, 1.0, 0.03);
      for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(20.0), living -> living == entity || living.isAlliedTo(entity))) {
         ally.getPersistentData().putLong(TAG_STRATEGY_UNTIL, now + STRATEGY_DURATION);
      }
      return true;
   }

   private static boolean tryCastMaou(OdaNobunagaEntity entity, ServerLevel level, long now, LivingEntity target, boolean allowedPhase) {
      if (!allowedPhase || target == null || entity.getCurrentMp() < 15.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_MAOU), MAOU_COOLDOWN)) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 15.0);
      entity.getPersistentData().putLong(TAG_LAST_MAOU, now);
      entity.getPersistentData().putLong(TAG_MAOU_UNTIL, now + MAOU_DURATION);
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MAOU_DURATION, 0));
      entity.triggerRoarAnimation();
      level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.75F);
      level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 1.0, entity.getZ(), 32, 0.7, 0.6, 0.7, 0.05);
      return true;
   }

   private static boolean tryCastThreeThousandWorlds(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now, boolean highPriority) {
      if (entity.getCurrentMp() < 80.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_THREE_THOUSAND), THREE_THOUSAND_COOLDOWN)) {
         return false;
      }
      if (!highPriority && entity.getRandom().nextInt(100) > 35) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 80.0);
      entity.getPersistentData().putLong(TAG_LAST_THREE_THOUSAND, now);
      entity.triggerNamedActionAnimation("bow_shot");
      ServantVoiceHelper.tryPlayOdaNobunagaNp(entity);
      Vec3 center = target.position().add(entity.position()).scale(0.5);
      Vec3 forward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      summonMatchlockArray(entity, level, center, forward);
      level.playSound(null, entity.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 1.2F, 0.7F);
      return true;
   }

   private static void maintainFloatingMatchlocks(OdaNobunagaEntity entity, ServerLevel level, long now, LivingEntity target, ServantCombatPhase phase) {
      int desired = phase == ServantCombatPhase.DECISIVE ? 5 : phase.id() >= ServantCombatPhase.NORMAL.id() ? 3 : 1;
      if (target != null && hasTrait(target, ServantTraitTag.DIVINE)) {
         desired = Math.min(5, desired + 1);
      }
      int active = countFloatingMatchlocks(entity, level);
      if (active >= desired || entity.getCurrentMp() < 8.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_FLOATING_MATCHLOCK), FLOATING_MATCHLOCK_COOLDOWN)) {
         return;
      }
      summonFloatingMatchlocks(entity, level, desired - active, desired, 10 * 20 + entity.getRandom().nextInt(5 * 20));
      entity.setCurrentMp(entity.getCurrentMp() - 8.0);
      entity.getPersistentData().putLong(TAG_LAST_FLOATING_MATCHLOCK, now);
      entity.triggerNamedActionAnimation("summon_guns");
      level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.7F, 1.45F);
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 18, 0.55, 0.4, 0.55, 0.08);
   }

   private static int countFloatingMatchlocks(OdaNobunagaEntity entity, ServerLevel level) {
      int count = 0;
      for (OdaMatchlockGunEntity gun : level.getEntitiesOfClass(OdaMatchlockGunEntity.class, entity.getBoundingBox().inflate(48.0))) {
         if (gun.isFloatingFor(entity.getUUID())) {
            count++;
         }
      }
      return count;
   }

   private static void summonFloatingMatchlocks(OdaNobunagaEntity entity, ServerLevel level, int addCount, int orbitCount, int durationTicks) {
      int start = countFloatingMatchlocks(entity, level);
      int toAdd = Mth.clamp(addCount, 1, 5 - start);
      for (int i = 0; i < toAdd; i++) {
         OdaMatchlockGunEntity gun = OdaMatchlockGunEntity.floating(level, entity, start + i, orbitCount, durationTicks, 5 + i * 4);
         level.addFreshEntity(gun);
      }
   }

   private static boolean tryCastMatchlockVolley(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 10.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_MATCHLOCK_VOLLEY), MATCHLOCK_VOLLEY_COOLDOWN) || entity.getRandom().nextInt(100) > 30) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 10.0);
      entity.getPersistentData().putLong(TAG_LAST_MATCHLOCK_VOLLEY, now);
      entity.triggerNamedActionAnimation("fire_command");
      Vec3 forward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      int count = 3 + entity.getRandom().nextInt(3);
      Vec3 center = entity.position().add(forward.scale(2.2)).add(0.0, entity.getBbHeight() * 0.8, 0.0);
      for (int i = 0; i < count; i++) {
         double offset = i - (count - 1) * 0.5;
         Vec3 pos = center.add(side.scale(offset * 0.9)).add(0.0, Math.abs(offset) * 0.16, 0.0);
         OdaMatchlockGunEntity gun = new OdaMatchlockGunEntity(level, entity, pos, target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(pos), i * 3);
         level.addFreshEntity(gun);
      }
      level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 18, 0.65, 0.15, 0.65, 0.04);
      level.playSound(null, entity.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.HOSTILE, 0.8F, 0.8F);
      return true;
   }

   private static boolean tryCastFireBarrage(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 12.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_FIRE_BARRAGE), FIRE_BARRAGE_COOLDOWN) || entity.getRandom().nextInt(100) > 24) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 12.0);
      entity.getPersistentData().putLong(TAG_LAST_FIRE_BARRAGE, now);
      entity.triggerNamedActionAnimation("fire_command");
      Vec3 start = entity.position().add(0.0, entity.getBbHeight() * 0.72, 0.0);
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.52, 0.0);
      for (int i = 0; i < 4; i++) {
         Vec3 end = aim.add((entity.getRandom().nextDouble() - 0.5) * 0.7, (entity.getRandom().nextDouble() - 0.5) * 0.45, (entity.getRandom().nextDouble() - 0.5) * 0.7);
         spawnTracer(level, start, end, i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.CRIT);
      }
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), applyTenkaFubuCap(entity, target, 14.0F));
      target.invulnerableTime = 0;
      target.igniteForSeconds(5.0F);
      level.sendParticles(ParticleTypes.LAVA, aim.x, aim.y, aim.z, 8, 0.35, 0.35, 0.35, 0.02);
      level.playSound(null, target.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.9F, 1.15F);
      return true;
   }

   private static boolean tryCastAtsumoriStep(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 8.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_ATSUMORI_STEP), ATSUMORI_STEP_COOLDOWN)) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 8.0);
      entity.getPersistentData().putLong(TAG_LAST_ATSUMORI_STEP, now);
      entity.triggerNamedActionAnimation("step_back");
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 70, 1));
      kiteBack(entity, target, 6.0);
      Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() > 1.0E-4) {
         away = away.normalize();
         entity.setDeltaMovement(away.x * 0.75, Math.max(entity.getDeltaMovement().y, 0.16), away.z * 0.75);
         entity.hasImpulse = true;
      }
      level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 0.35, entity.getZ(), 20, 0.35, 0.12, 0.35, 0.05);
      level.sendParticles(ParticleTypes.ASH, entity.getX(), entity.getY() + 0.6, entity.getZ(), 16, 0.45, 0.18, 0.45, 0.04);
      level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 0.55F, 1.55F);
      return true;
   }

   private static boolean tryCastAntiMysterySpark(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 14.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_ANTI_MYSTERY_SPARK), ANTI_MYSTERY_SPARK_COOLDOWN) || entity.getRandom().nextInt(100) > 38) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 14.0);
      entity.getPersistentData().putLong(TAG_LAST_ANTI_MYSTERY_SPARK, now);
      entity.triggerNamedActionAnimation("maou");
      Vec3 start = entity.position().add(0.0, entity.getBbHeight() * 0.78, 0.0);
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
      spawnTracer(level, start, end, ParticleTypes.SOUL_FIRE_FLAME);
      spawnTracer(level, start, end, ParticleTypes.ENCHANT);
      float damage = hasTrait(target, ServantTraitTag.DIVINE) ? 24.0F : 14.0F;
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().magic(), applyTenkaFubuCap(entity, target, damage));
      target.invulnerableTime = 0;
      target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, hasTrait(target, ServantTraitTag.DIVINE) ? 1 : 0));
      level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.playSound(null, target.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 0.75F, 1.75F);
      return true;
   }

   private static boolean tryCastDemonKingPressure(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 16.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_DEMON_KING_PRESSURE), DEMON_KING_PRESSURE_COOLDOWN) || entity.distanceTo(target) > 12.0) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 16.0);
      entity.getPersistentData().putLong(TAG_LAST_DEMON_KING_PRESSURE, now);
      entity.triggerNamedActionAnimation("maou");
      int hit = 0;
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(8.0), living -> living != entity && living.isAlive() && !living.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(living))) {
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, hasTrait(living, ServantTraitTag.DIVINE) ? 2 : 1));
         living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, hasTrait(living, ServantTraitTag.DIVINE) ? 1 : 0));
         living.igniteForSeconds(hasTrait(living, ServantTraitTag.DIVINE) ? 5.0F : 3.0F);
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().magic(), applyTenkaFubuCap(entity, living, hasTrait(living, ServantTraitTag.DIVINE) ? 18.0F : 8.0F));
         living.invulnerableTime = 0;
         hit++;
      }
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + 0.9, entity.getZ(), 46, 2.3, 0.7, 2.3, 0.08);
      level.sendParticles(ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getY() + 0.45, entity.getZ(), 28, 2.0, 0.3, 2.0, 0.04);
      level.playSound(null, entity.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 0.75F, 0.85F);
      return hit > 0;
   }

   private static boolean tryCastHasebeRepel(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 8.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_HASEBE_REPEL), HASEBE_REPEL_COOLDOWN)) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 8.0);
      entity.getPersistentData().putLong(TAG_LAST_HASEBE_REPEL, now);
      entity.triggerNamedActionAnimation("hasebe_slash");
      Vec3 dir = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      dir = dir.normalize();
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), applyTenkaFubuCap(entity, target, 12.0F));
      target.invulnerableTime = 0;
      target.push(dir.x * 1.25, 0.22, dir.z * 1.25);
      target.hurtMarked = true;
      Vec3 slash = entity.position().add(dir.scale(1.6)).add(0.0, entity.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, slash.x, slash.y, slash.z, 3, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.FLAME, slash.x, slash.y, slash.z, 12, 0.35, 0.18, 0.35, 0.05);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.85F, 1.1F);
      return true;
   }

   private static boolean tryCastAshField(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now, boolean highPriority) {
      if (entity.getCurrentMp() < 18.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_ASH_FIELD), ASH_FIELD_COOLDOWN)) {
         return false;
      }
      if (!highPriority && entity.getRandom().nextInt(100) > 22) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 18.0);
      entity.getPersistentData().putLong(TAG_LAST_ASH_FIELD, now);
      entity.triggerNamedActionAnimation("fire_command");
      Vec3 center = target.position();
      for (int delay = 0; delay <= 40; delay += 10) {
         TYPE_MOON_WORLD.queueServerWork(delay, () -> applyAshFieldPulse(entity, center));
      }
      level.playSound(null, target.blockPosition(), SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 0.85F, 0.75F);
      return true;
   }

   private static void applyAshFieldPulse(OdaNobunagaEntity entity, Vec3 center) {
      if (!entity.isAlive() || !(entity.level() instanceof ServerLevel level)) {
         return;
      }
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.12, center.z, 34, 3.0, 0.12, 3.0, 0.035);
      level.sendParticles(ParticleTypes.ASH, center.x, center.y + 0.4, center.z, 40, 3.4, 0.2, 3.4, 0.025);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(4.5), living -> living != entity && living.isAlive() && !living.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(living))) {
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 0));
         living.igniteForSeconds(3.0F);
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().magic(), applyTenkaFubuCap(entity, living, hasTrait(living, ServantTraitTag.DIVINE) ? 9.0F : 5.0F));
         living.invulnerableTime = 0;
      }
   }

   private static boolean tryCastScorchedBanner(OdaNobunagaEntity entity, ServerLevel level, long now, boolean allowedPhase) {
      if (!allowedPhase || entity.getCurrentMp() < 15.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_SCORCHED_BANNER), SCORCHED_BANNER_COOLDOWN) || entity.getRandom().nextInt(100) > 18) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 15.0);
      entity.getPersistentData().putLong(TAG_LAST_SCORCHED_BANNER, now);
      entity.triggerNamedActionAnimation("strategy");
      for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(16.0), living -> living == entity || living.isAlliedTo(entity))) {
         ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0));
         ally.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 160, 0));
      }
      level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 1.1, entity.getZ(), 32, 1.2, 0.55, 1.2, 0.055);
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1.15, entity.getZ(), 22, 1.0, 0.45, 1.0, 0.08);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.85F, 0.9F);
      return true;
   }

   private static boolean tryCastThreeLineRotation(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 20.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_THREE_LINE_ROTATION), THREE_LINE_ROTATION_COOLDOWN) || entity.getRandom().nextInt(100) > 28) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 20.0);
      entity.getPersistentData().putLong(TAG_LAST_THREE_LINE_ROTATION, now);
      entity.triggerNamedActionAnimation("summon_guns");
      for (int wave = 0; wave < 3; wave++) {
         int delay = wave * 12;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> releaseThreeLineWave(entity, target));
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.9F, 0.75F);
      return true;
   }

   private static void releaseThreeLineWave(OdaNobunagaEntity entity, LivingEntity target) {
      if (!entity.isAlive() || target == null || !target.isAlive() || !(entity.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 forward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 base = entity.position().add(forward.scale(3.0)).add(0.0, entity.getBbHeight() * 0.78, 0.0);
      for (int i = 0; i < 4; i++) {
         Vec3 pos = base.add(side.scale((i - 1.5) * 1.05)).add(0.0, (i % 2) * 0.35, 0.0);
         level.addFreshEntity(new OdaMatchlockGunEntity(level, entity, pos, target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(pos), i * 2));
      }
      level.sendParticles(ParticleTypes.SMOKE, base.x, base.y, base.z, 12, 0.8, 0.16, 0.8, 0.04);
   }

   private static void summonMatchlockArray(OdaNobunagaEntity entity, ServerLevel level, Vec3 center, Vec3 forward) {
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      int spawned = 0;
      int[] perRing = {16, 16, 18, 18, 15, 16};
      double[] heights = {0.8, 1.9, 3.0, 4.1, 5.2, 6.3};
      for (int layer = 0; layer < perRing.length; layer++) {
         int count = perRing[layer];
         double radius = 5.0 + layer * 0.85;
         for (int i = 0; i < count && spawned < 99; i++) {
            double angle = Math.PI * 2.0 * i / count + layer * 0.27;
            Vec3 radial = forward.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
            Vec3 pos = center.add(radial.scale(radius)).add(0.0, heights[layer], 0.0);
            OdaMatchlockGunEntity gun = new OdaMatchlockGunEntity(level, entity, pos, center.subtract(pos), (spawned % 18) + layer * 2);
            level.addFreshEntity(gun);
            spawned++;
         }
      }
      while (spawned < 99) {
         double angle = Math.PI * 2.0 * spawned / 99.0;
         Vec3 radial = forward.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
         Vec3 pos = center.add(radial.scale(8.0)).add(0.0, 2.0 + (spawned % 4), 0.0);
         level.addFreshEntity(new OdaMatchlockGunEntity(level, entity, pos, center.subtract(pos), spawned % 20));
         spawned++;
      }
   }

   private static boolean tryBeginHajun(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 100.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_HAJUN), HAJUN_COOLDOWN)) {
         return false;
      }
      if (UBWInstanceManager.isUbwDimension(level)) {
         ServantVoiceHelper.tryPlayFail(entity);
         entity.getPersistentData().putLong(TAG_LAST_HAJUN, now);
         return false;
      }
      if (ModDimensions.isHajunDimension(level.dimension().location())) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 100.0);
      entity.getPersistentData().putLong(TAG_LAST_HAJUN, now);
      entity.getPersistentData().putLong(TAG_HAJUN_CHANT_END, now + HAJUN_CHANT_TICKS);
      entity.getPersistentData().putInt(TAG_HAJUN_CHANT_TARGET, target.getId());
      entity.triggerChargeAnimation();
      ServantVoiceHelper.tryPlayOdaNobunagaHajun(entity);
      level.playSound(null, entity.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8F, 0.6F);
      return true;
   }

   private static boolean tickHajunChant(OdaNobunagaEntity entity, ServerLevel level, long now) {
      long chantEnd = entity.getPersistentData().getLong(TAG_HAJUN_CHANT_END);
      if (chantEnd <= 0L) {
         return false;
      }
      entity.getNavigation().stop();
      if (now % 5L == 0L) {
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + 0.15, entity.getZ(), 16, 1.5, 0.15, 1.5, 0.03);
         scorchChantSurface(level, entity.blockPosition(), 10);
      }
      if (now < chantEnd) {
         return true;
      }
      LivingEntity target = null;
      Entity resolved = level.getEntity(entity.getPersistentData().getInt(TAG_HAJUN_CHANT_TARGET));
      if (resolved instanceof LivingEntity living && living.isAlive()) {
         target = living;
      } else if (entity.getTarget() != null && entity.getTarget().isAlive()) {
         target = entity.getTarget();
      }
      entity.getPersistentData().remove(TAG_HAJUN_CHANT_END);
      entity.getPersistentData().remove(TAG_HAJUN_CHANT_TARGET);
      if (target != null && entity.isAlive()) {
         activateHajun(entity, level, target, now);
      }
      return true;
   }

   private static void activateHajun(OdaNobunagaEntity entity, ServerLevel source, LivingEntity primary, long now) {
      ServerLevel hajunLevel = source.getServer().getLevel(ModDimensions.HAJUN_KEY);
      if (hajunLevel == null) {
         return;
      }
      List<LivingEntity> pulled = collectHajunTargets(entity, source, primary);
      CompoundTag data = entity.getPersistentData();
      data.putString(TAG_HAJUN_RETURN_DIM, source.dimension().location().toString());
      data.putDouble(TAG_HAJUN_RETURN_X, entity.getX());
      data.putDouble(TAG_HAJUN_RETURN_Y, entity.getY());
      data.putDouble(TAG_HAJUN_RETURN_Z, entity.getZ());
      Vec3 entry = new Vec3(entity.getRandom().nextInt(120) - 60 + 0.5, 72.0, entity.getRandom().nextInt(120) - 60 + 0.5);
      entry = new Vec3(entry.x, findSafeSpawnY(hajunLevel, Mth.floor(entry.x), Mth.floor(entry.z)), entry.z);
      LivingEntity movedPrimary = moveHajunTargets(entity, source, hajunLevel, pulled, primary, entry);
      Entity moved = entity.changeDimension(new DimensionTransition(hajunLevel, entry, Vec3.ZERO, entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));
      if (moved instanceof OdaNobunagaEntity nobu) {
         nobu.getPersistentData().putLong(TAG_HAJUN_ACTIVE_UNTIL, hajunLevel.getGameTime() + HAJUN_DURATION);
         nobu.getPersistentData().putString(TAG_HAJUN_RETURN_DIM, source.dimension().location().toString());
         nobu.getPersistentData().putDouble(TAG_HAJUN_RETURN_X, data.getDouble(TAG_HAJUN_RETURN_X));
         nobu.getPersistentData().putDouble(TAG_HAJUN_RETURN_Y, data.getDouble(TAG_HAJUN_RETURN_Y));
         nobu.getPersistentData().putDouble(TAG_HAJUN_RETURN_Z, data.getDouble(TAG_HAJUN_RETURN_Z));
         if (movedPrimary != null) {
            nobu.setTarget(movedPrimary);
         }
         RedSkeletonHajunEntity skeleton = new RedSkeletonHajunEntity(hajunLevel, nobu, HAJUN_DURATION);
         hajunLevel.addFreshEntity(skeleton);
         hajunLevel.playSound(null, nobu.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.2F, 0.55F);
         applyHajunInstantDamage(nobu, hajunLevel);
      }
   }

   private static boolean tickHajunField(OdaNobunagaEntity entity, ServerLevel level, long now) {
      long activeUntil = entity.getPersistentData().getLong(TAG_HAJUN_ACTIVE_UNTIL);
      if (activeUntil <= 0L || !ModDimensions.isHajunDimension(level.dimension().location())) {
         return false;
      }
      if (now % 20L == 0L) {
         applyHajunDot(entity, level);
      }
      if (now % 4L == 0L) {
         level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 0.2, entity.getZ(), 18, 8.0, 0.15, 8.0, 0.02);
         stainHajunSurface(level, entity.blockPosition(), 20);
      }
      if (now >= activeUntil || !hasActiveHajunEnemy(entity, level)) {
         returnFromHajun(entity, level);
         return true;
      }
      return false;
   }

   private static List<LivingEntity> collectHajunTargets(OdaNobunagaEntity owner, ServerLevel source, LivingEntity primary) {
      List<LivingEntity> targets = new ArrayList<>();
      if (isHajunPullTarget(owner, primary, source)) {
         targets.add(primary);
      }
      for (LivingEntity living : source.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(HAJUN_RADIUS), living -> living != primary && isHajunPullTarget(owner, living, source))) {
         targets.add(living);
      }
      return targets;
   }

   private static boolean isHajunPullTarget(OdaNobunagaEntity owner, LivingEntity living, ServerLevel source) {
      return living != null && living.isAlive() && living != owner && living.level() == source && !living.isAlliedTo(owner) && !EntityUtils.isImmunePlayerTarget(living) && living.position().subtract(owner.position()).lengthSqr() <= HAJUN_RADIUS * HAJUN_RADIUS;
   }

   private static LivingEntity moveHajunTargets(OdaNobunagaEntity owner, ServerLevel source, ServerLevel hajunLevel, List<LivingEntity> targets, LivingEntity primary, Vec3 entry) {
      LivingEntity movedPrimary = moveOneHajunTarget(owner, source, hajunLevel, primary, entry);
      for (LivingEntity living : targets) {
         if (living != primary) {
            moveOneHajunTarget(owner, source, hajunLevel, living, entry);
         }
      }
      return movedPrimary;
   }

   private static LivingEntity moveOneHajunTarget(OdaNobunagaEntity owner, ServerLevel source, ServerLevel hajunLevel, LivingEntity living, Vec3 entry) {
      if (!isHajunPullTarget(owner, living, source)) {
         return null;
      }
      double relX = Mth.clamp(living.getX() - owner.getX(), -16.0, 16.0);
      double relZ = Mth.clamp(living.getZ() - owner.getZ(), -16.0, 16.0);
      double targetX = entry.x + relX;
      double targetZ = entry.z + relZ;
      double targetY = findSafeSpawnY(hajunLevel, Mth.floor(targetX), Mth.floor(targetZ));
      markHajunTarget(owner, living, source);
      Entity moved = living.changeDimension(new DimensionTransition(hajunLevel, new Vec3(targetX, targetY, targetZ), Vec3.ZERO, living.getYRot(), living.getXRot(), DimensionTransition.DO_NOTHING));
      if (moved instanceof LivingEntity movedLiving) {
         markHajunTarget(owner, movedLiving, source);
         return movedLiving;
      }
      return null;
   }

   private static void markHajunTarget(OdaNobunagaEntity owner, LivingEntity living, ServerLevel source) {
      CompoundTag data = living.getPersistentData();
      data.putUUID(TAG_HAJUN_TARGET_OWNER, owner.getUUID());
      data.putString(TAG_HAJUN_TARGET_RETURN_DIM, source.dimension().location().toString());
      data.putDouble(TAG_HAJUN_TARGET_RETURN_X, living.getX());
      data.putDouble(TAG_HAJUN_TARGET_RETURN_Y, living.getY());
      data.putDouble(TAG_HAJUN_TARGET_RETURN_Z, living.getZ());
   }

   private static void applyHajunInstantDamage(OdaNobunagaEntity entity, ServerLevel level) {
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(HAJUN_RADIUS), living -> isPulledByHajun(entity.getUUID(), living))) {
         float damage = hasTrait(living, ServantTraitTag.DIVINE) ? 400.0F : 100.0F;
         living.igniteForSeconds(8.0F);
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().magic(), damage);
         living.invulnerableTime = 0;
      }
   }

   private static void applyHajunDot(OdaNobunagaEntity entity, ServerLevel level) {
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(HAJUN_RADIUS), living -> living != entity && living.isAlive() && !living.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(living))) {
         float damage = hasTrait(living, ServantTraitTag.DIVINE) ? 40.0F : 10.0F;
         living.igniteForSeconds(4.0F);
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().magic(), damage);
         living.invulnerableTime = 0;
         if (hasTrait(living, ServantTraitTag.DIVINE)) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 1));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, 0));
         }
      }
   }

   private static boolean hasActiveHajunEnemy(OdaNobunagaEntity entity, ServerLevel level) {
      UUID ownerId = entity.getUUID();
      for (Entity candidate : level.getEntities().getAll()) {
         if (candidate instanceof LivingEntity living && living.isAlive() && isPulledByHajun(ownerId, living) && !living.isAlliedTo(entity)) {
            return true;
         }
      }
      return false;
   }

   private static void returnFromHajun(OdaNobunagaEntity entity, ServerLevel level) {
      CompoundTag data = entity.getPersistentData();
      ServerLevel returnLevel = resolveDimensionOrOverworld(level, data.getString(TAG_HAJUN_RETURN_DIM));
      Vec3 returnPos = new Vec3(data.getDouble(TAG_HAJUN_RETURN_X), data.getDouble(TAG_HAJUN_RETURN_Y), data.getDouble(TAG_HAJUN_RETURN_Z));
      returnPulledHajunTargets(entity.getUUID(), level, returnLevel);
      clearHajunState(entity);
      if (entity.isAlive()) {
         Entity moved = entity.changeDimension(new DimensionTransition(returnLevel, returnPos, Vec3.ZERO, entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));
         if (moved instanceof OdaNobunagaEntity returned) {
            clearHajunState(returned);
            returned.getNavigation().stop();
         }
      }
      for (RedSkeletonHajunEntity skeleton : level.getEntitiesOfClass(RedSkeletonHajunEntity.class, new AABB(-256.0, level.getMinBuildHeight(), -256.0, 256.0, level.getMaxBuildHeight(), 256.0))) {
         skeleton.discard();
      }
   }

   private static void returnPulledHajunTargets(UUID ownerId, ServerLevel sourceLevel, ServerLevel fallbackLevel) {
      List<LivingEntity> toReturn = new ArrayList<>();
      for (Entity candidate : sourceLevel.getEntities().getAll()) {
         if (candidate instanceof LivingEntity living && living.isAlive() && isPulledByHajun(ownerId, living)) {
            toReturn.add(living);
         }
      }
      for (LivingEntity living : toReturn) {
         CompoundTag data = living.getPersistentData();
         ServerLevel returnLevel = resolveDimensionOrFallback(sourceLevel, data.getString(TAG_HAJUN_TARGET_RETURN_DIM), fallbackLevel);
         Vec3 returnPos = new Vec3(data.getDouble(TAG_HAJUN_TARGET_RETURN_X), data.getDouble(TAG_HAJUN_TARGET_RETURN_Y), data.getDouble(TAG_HAJUN_TARGET_RETURN_Z));
         clearHajunTarget(living);
         Entity moved = living.changeDimension(new DimensionTransition(returnLevel, returnPos, Vec3.ZERO, living.getYRot(), living.getXRot(), DimensionTransition.DO_NOTHING));
         if (moved instanceof LivingEntity movedLiving) {
            clearHajunTarget(movedLiving);
         }
      }
   }

   private static void shootRifle(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(TAG_LAST_RIFLE_SHOT, now);
      entity.triggerNamedActionAnimation("bow_shot");
      ServantVoiceHelper.tryPlayAttack(entity);
      Vec3 start = entity.position().add(0.0, entity.getBbHeight() * 0.72, 0.0).add(entity.getLookAngle().scale(0.6));
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      spawnTracer(level, start, end, ParticleTypes.CRIT);
      float damage = 9.0F;
      if (hasMaou(entity, now) && entity.getRandom().nextFloat() < 0.5F) {
         damage *= 1.5F;
      }
      damage = applyTenkaFubuCap(entity, target, damage);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
      level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.35F, 1.85F);
   }

   private static float applyTenkaFubuCap(OdaNobunagaEntity owner, LivingEntity target, float damageAfterOtherMultipliers) {
      if (!hasTrait(target, ServantTraitTag.DIVINE)) {
         return damageAfterOtherMultipliers;
      }
      float cappedMultiplier = divineMultiplierCap(target);
      float withTenkaFubu = damageAfterOtherMultipliers * 2.0F;
      return Math.min(withTenkaFubu, damageAfterOtherMultipliers * cappedMultiplier);
   }

   private static float divineMultiplierCap(LivingEntity target) {
      if (target instanceof ServantEntity servant && servant.getDefinition() != null) {
         List<String> skills = servant.getDefinition().skillIds();
         if (skills.contains("divinity_a") || skills.contains("god_hand_a")) {
            return 16.0F;
         }
         if (skills.contains("divinity_b_plus")) {
            return 10.0F;
         }
         if (skills.contains("divinity_b")) {
            return 8.0F;
         }
         if (skills.contains("divinity_c")) {
            return 6.0F;
         }
         if (skills.contains("divinity_d")) {
            return 4.0F;
         }
         if (skills.contains("divinity_e") || skills.contains("divinity_e_minus")) {
            return 2.0F;
         }
      }
      return 8.0F;
   }

   private static boolean hasTrait(LivingEntity entity, ServantTraitTag trait) {
      if (entity instanceof ServantEntity servant && servant.getDefinition() != null) {
         return servant.getDefinition().traits().contains(trait);
      }
      return false;
   }

   private static void expireBuffs(OdaNobunagaEntity entity, long now) {
      if (entity.getPersistentData().getLong(TAG_STRATEGY_UNTIL) > now && now % 20L == 0L) {
         entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + 1.25));
      }
      if (entity.getPersistentData().getLong(TAG_MAOU_UNTIL) <= now) {
         entity.getPersistentData().remove(TAG_MAOU_UNTIL);
      }
   }

   private static boolean hasMaou(OdaNobunagaEntity entity, long now) {
      return entity.getPersistentData().getLong(TAG_MAOU_UNTIL) > now;
   }

   private static void kiteBack(OdaNobunagaEntity entity, LivingEntity target, double distance) {
      Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() > 1.0E-4) {
         Vec3 retreat = entity.position().add(away.normalize().scale(distance));
         entity.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 1.15);
      }
   }

   private static int findSafeSpawnY(ServerLevel level, int x, int z) {
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getMaxBuildHeight() - 2, z);
      for (int y = level.getMaxBuildHeight() - 2; y > level.getMinBuildHeight(); y--) {
         pos.set(x, y, z);
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && state.isFaceSturdy(level, pos, Direction.UP) && level.getBlockState(pos.above()).isAir()) {
            return y + 1;
         }
      }
      return Mth.clamp(72, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
   }

   private static ServerLevel resolveDimensionOrOverworld(ServerLevel current, String id) {
      return resolveDimensionOrFallback(current, id, current.getServer().overworld());
   }

   private static ServerLevel resolveDimensionOrFallback(ServerLevel current, String id, ServerLevel fallback) {
      ResourceLocation location = ResourceLocation.tryParse(id);
      if (location != null) {
         ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location);
         ServerLevel resolved = current.getServer().getLevel(key);
         if (resolved != null) {
            return resolved;
         }
      }
      return fallback;
   }

   private static boolean isPulledByHajun(UUID ownerId, LivingEntity living) {
      return living.getPersistentData().hasUUID(TAG_HAJUN_TARGET_OWNER) && ownerId.equals(living.getPersistentData().getUUID(TAG_HAJUN_TARGET_OWNER));
   }

   private static void clearHajunState(OdaNobunagaEntity entity) {
      CompoundTag data = entity.getPersistentData();
      data.remove(TAG_HAJUN_ACTIVE_UNTIL);
      data.remove(TAG_HAJUN_RETURN_DIM);
      data.remove(TAG_HAJUN_RETURN_X);
      data.remove(TAG_HAJUN_RETURN_Y);
      data.remove(TAG_HAJUN_RETURN_Z);
   }

   private static void clearHajunTarget(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      data.remove(TAG_HAJUN_TARGET_OWNER);
      data.remove(TAG_HAJUN_TARGET_RETURN_DIM);
      data.remove(TAG_HAJUN_TARGET_RETURN_X);
      data.remove(TAG_HAJUN_TARGET_RETURN_Y);
      data.remove(TAG_HAJUN_TARGET_RETURN_Z);
   }

   private static void spawnTracer(ServerLevel level, Vec3 start, Vec3 end, net.minecraft.core.particles.SimpleParticleType particle) {
      Vec3 diff = end.subtract(start);
      int steps = Math.max(2, Mth.ceil(diff.length() * 2.2));
      Vec3 step = diff.scale(1.0 / steps);
      for (int i = 0; i <= steps; i++) {
         Vec3 p = start.add(step.scale(i));
         level.sendParticles(particle, p.x, p.y, p.z, 1, 0.01, 0.01, 0.01, 0.0);
      }
   }

   private static void scorchChantSurface(ServerLevel level, BlockPos center, int attempts) {
      for (int i = 0; i < attempts; i++) {
         BlockPos pos = center.offset(level.random.nextInt(13) - 6, level.random.nextInt(3) - 1, level.random.nextInt(13) - 6);
         BlockPos surface = findNearbySurface(level, pos);
         if (surface != null) {
            level.sendParticles(ParticleTypes.FLAME, surface.getX() + 0.5, surface.getY() + 1.05, surface.getZ() + 0.5, 1, 0.1, 0.02, 0.1, 0.01);
         }
      }
   }

   private static void stainHajunSurface(ServerLevel level, BlockPos center, int attempts) {
      for (int i = 0; i < attempts; i++) {
         BlockPos pos = center.offset(level.random.nextInt(25) - 12, level.random.nextInt(5) - 2, level.random.nextInt(25) - 12);
         BlockPos surface = findNearbySurface(level, pos);
         if (surface != null && level.random.nextInt(4) == 0 && canReplaceHajunSurface(level.getBlockState(surface))) {
            level.setBlock(surface, level.random.nextBoolean() ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState(), 3);
         }
      }
   }

   private static BlockPos findNearbySurface(ServerLevel level, BlockPos sample) {
      int startY = Mth.clamp(sample.getY() + 4, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
      int minY = Math.max(level.getMinBuildHeight() + 1, sample.getY() - 8);
      for (int y = startY; y >= minY; y--) {
         BlockPos pos = new BlockPos(sample.getX(), y, sample.getZ());
         if (!level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
            return pos;
         }
      }
      return null;
   }

   private static boolean canReplaceHajunSurface(BlockState state) {
      return !state.isAir() && !state.hasBlockEntity() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.MAGMA_BLOCK) && !state.is(Blocks.NETHERRACK);
   }

   private static boolean canUse(long now, long lastUse, int cooldown) {
      return lastUse <= 0L || now - lastUse >= cooldown;
   }
}
