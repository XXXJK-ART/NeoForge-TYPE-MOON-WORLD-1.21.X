package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.level.block.entity.BlockEntity;
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
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class OdaNobunagaCombatHelper {
   public static final String TAG_LAST_STRATEGY = "OdaLastStrategyTick";
   public static final String TAG_STRATEGY_UNTIL = "OdaStrategyUntil";
   public static final String TAG_LAST_MAOU = "OdaLastMaouTick";
   public static final String TAG_MAOU_UNTIL = "OdaMaouUntil";
   public static final String TAG_LAST_THREE_THOUSAND = "OdaLastThreeThousandTick";
   public static final String TAG_THREE_THOUSAND_CHANT_END = "OdaThreeThousandChantEnd";
   public static final String TAG_THREE_THOUSAND_TARGET = "OdaThreeThousandTarget";
   public static final String TAG_LAST_RIFLE_SHOT = "OdaLastRifleShotTick";
   public static final String TAG_LAST_HAJUN = "OdaLastHajunTick";
   public static final String TAG_LAST_FLOATING_MATCHLOCK = "OdaLastFloatingMatchlockTick";
   public static final String TAG_LAST_MATCHLOCK_VOLLEY = "OdaLastMatchlockVolleyTick";
   public static final String TAG_LAST_FIRE_BARRAGE = "OdaLastFireBarrageTick";
   public static final String TAG_LAST_ATSUMORI_STEP = "OdaLastAtsumoriStepTick";
   public static final String TAG_LAST_ANTI_MYSTERY_SPARK = "OdaLastAntiMysterySparkTick";
   public static final String TAG_LAST_DEMON_KING_PRESSURE = "OdaLastDemonKingPressureTick";
   public static final String TAG_LAST_HASEBE_REPEL = "OdaLastHasebeRepelTick";
   public static final String TAG_LAST_HASEBE_THRUST = "OdaLastHasebeThrustTick";
   public static final String TAG_LAST_SCORCHED_CROSSCUT = "OdaLastScorchedCrosscutTick";
   public static final String TAG_LAST_CLOSE_COUNTER = "OdaLastCloseCounterTick";
   public static final String TAG_LAST_ASH_FIELD = "OdaLastAshFieldTick";
   public static final String TAG_LAST_SCORCHED_BANNER = "OdaLastScorchedBannerTick";
   public static final String TAG_LAST_THREE_LINE_ROTATION = "OdaLastThreeLineRotationTick";
   public static final String TAG_HAJUN_CHANT_END = "OdaHajunChantEnd";
   public static final String TAG_HAJUN_CHANT_TARGET = "OdaHajunChantTarget";
   public static final String TAG_HAJUN_ACTIVE_UNTIL = "OdaHajunActiveUntil";
   public static final String TAG_HAJUN_MIN_UNTIL = "OdaHajunMinUntil";
   public static final String TAG_HAJUN_START_TICK = "OdaHajunStartTick";
   public static final String TAG_HAJUN_STARTED = "OdaHajunStarted";
   public static final String TAG_HAJUN_NEXT_TERRAIN = "OdaHajunNextTerrain";
   public static final String TAG_HAJUN_CENTER_X = "OdaHajunCenterX";
   public static final String TAG_HAJUN_CENTER_Y = "OdaHajunCenterY";
   public static final String TAG_HAJUN_CENTER_Z = "OdaHajunCenterZ";
   public static final String TAG_HAJUN_RETURN_DIM = "OdaHajunReturnDim";
   public static final String TAG_HAJUN_RETURN_X = "OdaHajunReturnX";
   public static final String TAG_HAJUN_RETURN_Y = "OdaHajunReturnY";
   public static final String TAG_HAJUN_RETURN_Z = "OdaHajunReturnZ";
   public static final String TAG_HAJUN_LOCKED_TARGET = "OdaHajunLockedTarget";
   public static final String TAG_HAJUN_TARGET_OWNER = "OdaHajunTargetOwner";
   public static final String TAG_HAJUN_TARGET_RETURN_DIM = "OdaHajunTargetReturnDim";
   public static final String TAG_HAJUN_TARGET_RETURN_X = "OdaHajunTargetReturnX";
   public static final String TAG_HAJUN_TARGET_RETURN_Y = "OdaHajunTargetReturnY";
   public static final String TAG_HAJUN_TARGET_RETURN_Z = "OdaHajunTargetReturnZ";
   public static final String TAG_HAJUN_TARGET_PRIMARY = "OdaHajunTargetPrimary";
   public static final String TAG_HAJUN_OFFSCREEN_DUEL = "OdaHajunOffscreenDuel";
   public static final String TAG_HAJUN_OFFSCREEN_PREVIOUS_INVISIBLE = "OdaHajunOffscreenPrevInvisible";
   public static final String TAG_HAJUN_OFFSCREEN_PREVIOUS_INVULNERABLE = "OdaHajunOffscreenPrevInvulnerable";
   public static final String TAG_HAJUN_OFFSCREEN_PREVIOUS_NO_AI = "OdaHajunOffscreenPrevNoAi";
   public static final String TAG_DIVINE_BREAK_UNTIL = "OdaDivineDefenseBreakUntil";
   public static final String TAG_FLIGHT_UNTIL = "OdaFlightUntil";
   public static final String TAG_LAND_FOR_NP_UNTIL = "OdaLandForNpUntil";
   public static final String TAG_FOOT_SUPPORT_GUN = "OdaFootSupportGun";
   private static final int FLIGHT_RAMPUP_TICKS = 20;
   private static final int FLIGHT_HOLD_TICKS = 8 * 20;
   private static final int NP_LAND_TICKS = 10;
   private static final int NP_REFLIGHT_TICKS = 18;

   private static final int STRATEGY_COOLDOWN = 20 * 20;
   private static final int STRATEGY_DURATION = 25 * 20;
   private static final int MAOU_COOLDOWN = 18 * 20;
   private static final int MAOU_DURATION = 15 * 20;
   private static final int THREE_THOUSAND_COOLDOWN = 30 * 20;
   private static final int THREE_THOUSAND_CHANT_TICKS = 6 * 20;
   private static final int HAJUN_COOLDOWN = 45 * 20;
   private static final int HAJUN_CHANT_TICKS = 5 * 20;
   private static final int HAJUN_DURATION = 15 * 20;
   private static final double HAJUN_RADIUS = 25.0;
   private static final int HAJUN_TERRAIN_RADIUS = 25;
   private static final int HAJUN_CHANT_SURFACE_SPREAD_DELAY = 10;
   private static final int HAJUN_CHANT_SURFACE_RADIUS = 16;
   private static final int FLOATING_MATCHLOCK_COOLDOWN = 8 * 20;
   private static final int MATCHLOCK_VOLLEY_COOLDOWN = 5 * 20;
   private static final int FIRE_BARRAGE_COOLDOWN = 7 * 20;
   private static final int ATSUMORI_STEP_COOLDOWN = 6 * 20;
   private static final int ANTI_MYSTERY_SPARK_COOLDOWN = 10 * 20;
   private static final int DEMON_KING_PRESSURE_COOLDOWN = 12 * 20;
   private static final int HASEBE_REPEL_COOLDOWN = 5 * 20;
   private static final int HASEBE_THRUST_COOLDOWN = 4 * 20;
   private static final int SCORCHED_CROSSCUT_COOLDOWN = 7 * 20;
   private static final int CLOSE_COUNTER_COOLDOWN = 9 * 20;
   private static final int ASH_FIELD_COOLDOWN = 14 * 20;
   private static final int SCORCHED_BANNER_COOLDOWN = 16 * 20;
   private static final int THREE_LINE_ROTATION_COOLDOWN = 18 * 20;
   private static final Map<UUID, Map<BlockPos, BlockBackup>> ODA_HAJUN_BLOCKS = new HashMap<>();
   private static final Map<UUID, Map<BlockPos, BlockBackup>> ODA_HAJUN_CHANT_BLOCKS = new HashMap<>();

   private OdaNobunagaCombatHelper() {
   }

   public static void tick(OdaNobunagaEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }

      long now = level.getGameTime();
      tickThreeThousandWorldsChant(entity, level, now);
      if (tickHajunChant(entity, level, now) || tickHajunField(entity, level, now)) {
         return;
      }

      expireBuffs(entity, level, now);
      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         updateSeriousModeFlight(entity, null, now);
         return;
      }

      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.55, 0.0));
      double distance = entity.distanceTo(target);
      ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
      float hpRate = entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
      boolean openingStage = hpRate > 0.8F;
      boolean pressureStage = hpRate <= 0.8F && hpRate > 0.6F;
      boolean decisive = hpRate <= 0.6F;
      updateSeriousModeFlight(entity, target, now);
      boolean divine = hasTrait(target, ServantTraitTag.DIVINE);
      boolean mounted = hasTrait(target, ServantTraitTag.MOUNTED) || target.isPassenger() || target.getVehicle() != null;
      boolean mystery = divine || hasTrait(target, ServantTraitTag.DEMONIC) || hasTrait(target, ServantTraitTag.FAIRY_TALE);

      maintainFloatingMatchlocks(entity, level, now, target, phase);

      if (openingStage) {
         if (tryCastStrategy(entity, level, now)) {
            return;
         }
         if (distance < 4.2 && tryCastHasebeRepel(entity, level, target, now)) {
            return;
         }
         handleMovementAndRifle(entity, level, target, now, distance);
         return;
      }

      if (pressureStage) {
         if (tryCastMaou(entity, level, now, target, true)) {
            return;
         }
         if (distance <= 26.0 && tryCastMatchlockVolley(entity, level, target, now)) {
            return;
         }
         if (distance <= 24.0 && tryCastFireBarrage(entity, level, target, now)) {
            return;
         }
         if (distance < 4.2 && tryCastHasebeRepel(entity, level, target, now)) {
            return;
         }
         if (distance < 4.8 && tryCastHasebeThrust(entity, level, target, now)) {
            return;
         }
         if (distance < 6.0 && tryCastCloseCounter(entity, level, target, now)) {
            return;
         }
         if (distance < 7.0 && tryCastAtsumoriStep(entity, level, target, now)) {
            return;
         }
         handleMovementAndRifle(entity, level, target, now, distance);
         return;
      }

      if (divine && tryBeginHajun(entity, level, target, now)) {
         return;
      }
      if (tryCastThreeThousandWorlds(entity, level, target, now, true)) {
         return;
      }
      if (!divine && tryBeginHajun(entity, level, target, now)) {
         return;
      }
      if (tryCastMaou(entity, level, now, target, true)) {
         return;
      }
      if (distance < 4.2 && tryCastHasebeRepel(entity, level, target, now)) {
         return;
      }
      if (distance < 4.8 && tryCastHasebeThrust(entity, level, target, now)) {
         return;
      }
      if (distance < 5.4 && tryCastScorchedCrosscut(entity, level, target, now, divine || decisive)) {
         return;
      }
      if (distance < 6.0 && tryCastCloseCounter(entity, level, target, now)) {
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
      if (tryCastThreeLineRotation(entity, level, target, now)) {
         return;
      }
      if (tryCastScorchedBanner(entity, level, now, true)) {
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

      handleMovementAndRifle(entity, level, target, now, distance);
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
      if (entity.level() instanceof ServerLevel level) {
         if (ModDimensions.isHajunDimension(level.dimension().location())) {
            returnFromHajun(entity, level);
            return;
         }
         restoreHajunChantTerrain(entity, level, Integer.MAX_VALUE);
         clearHajunState(entity);
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
      entity.getPersistentData().putLong(TAG_FLIGHT_UNTIL, now + FLIGHT_HOLD_TICKS);
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MAOU_DURATION, 0));
      entity.triggerRoarAnimation();
      level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.75F);
      level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 1.0, entity.getZ(), 32, 0.7, 0.6, 0.7, 0.05);
      return true;
   }

   private static boolean tryCastThreeThousandWorlds(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now, boolean highPriority) {
      if (entity.getPersistentData().getLong(TAG_THREE_THOUSAND_CHANT_END) > now) {
         return false;
      }
      if (entity.getCurrentMp() < 80.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_THREE_THOUSAND), THREE_THOUSAND_COOLDOWN)) {
         return false;
      }
      if (!highPriority && entity.getRandom().nextInt(100) > 35) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 80.0);
      entity.getPersistentData().putLong(TAG_LAST_THREE_THOUSAND, now);
      entity.getPersistentData().putLong(TAG_THREE_THOUSAND_CHANT_END, now + THREE_THOUSAND_CHANT_TICKS);
      entity.getPersistentData().putLong(TAG_LAND_FOR_NP_UNTIL, now + THREE_THOUSAND_CHANT_TICKS + NP_LAND_TICKS);
      entity.getPersistentData().putLong(TAG_FLIGHT_UNTIL, now + THREE_THOUSAND_CHANT_TICKS + FLIGHT_HOLD_TICKS);
      entity.getPersistentData().putInt(TAG_THREE_THOUSAND_TARGET, target.getId());
      landForNoblePhantasm(entity);
      entity.triggerNamedActionAnimation("strategy");
      ServantVoiceHelper.tryPlayOdaNobunagaNp(entity);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.95F, 0.85F);
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1.15, entity.getZ(), 36, 1.2, 0.55, 1.2, 0.08);
      return true;
   }

   private static void tickThreeThousandWorldsChant(OdaNobunagaEntity entity, ServerLevel level, long now) {
      long chantEnd = entity.getPersistentData().getLong(TAG_THREE_THOUSAND_CHANT_END);
      if (chantEnd <= 0L) {
         return;
      }
      if (now % 10L == 0L) {
         level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 0.2, entity.getZ(), 12, 1.0, 0.12, 1.0, 0.025);
         level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 1.0, entity.getZ(), 10, 0.7, 0.35, 0.7, 0.035);
      }
      if (now < chantEnd) {
         return;
      }
      Entity resolved = level.getEntity(entity.getPersistentData().getInt(TAG_THREE_THOUSAND_TARGET));
      LivingEntity target = resolved instanceof LivingEntity living && living.isAlive() ? living : entity.getTarget();
      entity.getPersistentData().remove(TAG_THREE_THOUSAND_CHANT_END);
      entity.getPersistentData().remove(TAG_THREE_THOUSAND_TARGET);
      if (target != null && target.isAlive() && entity.isAlive()) {
         releaseThreeThousandWorlds(entity, level, target);
         relightAfterNoblePhantasm(entity, now);
      }
   }

   private static void handleMovementAndRifle(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now, double distance) {
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

   private static boolean tryCastHasebeThrust(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 7.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_HASEBE_THRUST), HASEBE_THRUST_COOLDOWN) || entity.getRandom().nextInt(100) > 46) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 7.0);
      entity.getPersistentData().putLong(TAG_LAST_HASEBE_THRUST, now);
      entity.triggerNamedActionAnimation("hasebe_slash");
      Vec3 dir = horizontalDirection(entity, target);
      entity.setDeltaMovement(dir.x * 0.45, Math.max(entity.getDeltaMovement().y, 0.08), dir.z * 0.45);
      entity.hasImpulse = true;
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), applyTenkaFubuCap(entity, target, 10.0F));
      target.invulnerableTime = 0;
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 55, 0));
      Vec3 point = entity.position().add(dir.scale(1.25)).add(0.0, entity.getBbHeight() * 0.72, 0.0);
      spawnTracer(level, point, target.position().add(0.0, target.getBbHeight() * 0.52, 0.0), ParticleTypes.CRIT);
      level.sendParticles(ParticleTypes.ENCHANTED_HIT, point.x, point.y, point.z, 10, 0.18, 0.18, 0.18, 0.08);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 0.8F, 1.35F);
      return true;
   }

   private static boolean tryCastScorchedCrosscut(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now, boolean highPriority) {
      if (entity.getCurrentMp() < 12.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_SCORCHED_CROSSCUT), SCORCHED_CROSSCUT_COOLDOWN)) {
         return false;
      }
      if (!highPriority && entity.getRandom().nextInt(100) > 34) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 12.0);
      entity.getPersistentData().putLong(TAG_LAST_SCORCHED_CROSSCUT, now);
      entity.triggerNamedActionAnimation("hasebe_slash");
      Vec3 dir = horizontalDirection(entity, target);
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.52, 0.0);
      for (int i = 0; i < 2; i++) {
         Vec3 start = center.add(side.scale(i == 0 ? -1.1 : 1.1)).add(0.0, i == 0 ? 0.22 : -0.16, 0.0);
         Vec3 end = center.add(side.scale(i == 0 ? 1.1 : -1.1)).add(0.0, i == 0 ? -0.16 : 0.22, 0.0);
         spawnTracer(level, start, end, ParticleTypes.FLAME);
      }
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), applyTenkaFubuCap(entity, target, hasTrait(target, ServantTraitTag.DIVINE) ? 18.0F : 13.0F));
      target.invulnerableTime = 0;
      target.igniteForSeconds(hasTrait(target, ServantTraitTag.DIVINE) ? 6.0F : 4.0F);
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0));
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 4, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 8, 0.28, 0.22, 0.28, 0.02);
      level.playSound(null, target.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8F, 1.35F);
      return true;
   }

   private static boolean tryCastCloseCounter(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 10.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_CLOSE_COUNTER), CLOSE_COUNTER_COOLDOWN) || entity.getRandom().nextInt(100) > 32) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 10.0);
      entity.getPersistentData().putLong(TAG_LAST_CLOSE_COUNTER, now);
      entity.triggerNamedActionAnimation("step_back");
      Vec3 away = horizontalDirection(target, entity);
      entity.setDeltaMovement(away.x * 0.72, Math.max(entity.getDeltaMovement().y, 0.12), away.z * 0.72);
      entity.hasImpulse = true;
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), applyTenkaFubuCap(entity, target, 8.0F));
      target.invulnerableTime = 0;
      target.push(-away.x * 0.65, 0.16, -away.z * 0.65);
      target.hurtMarked = true;
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
      Vec3 center = entity.position().add(horizontalDirection(entity, target).scale(1.1)).add(0.0, 0.75, 0.0);
      level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 16, 0.32, 0.18, 0.32, 0.04);
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 12, 0.28, 0.12, 0.28, 0.05);
      level.playSound(null, entity.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.75F, 1.5F);
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

   private static void releaseThreeThousandWorlds(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target) {
      entity.triggerNamedActionAnimation("summon_guns");
      Vec3 center = target.position().add(entity.position()).scale(0.5);
      Vec3 forward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      for (int batch = 0; batch < 6; batch++) {
         int delayedBatch = batch;
         Vec3 delayedCenter = center;
         Vec3 delayedForward = forward;
         TYPE_MOON_WORLD.queueServerWork(delayedBatch * 6, () -> summonMatchlockArrayBatch(entity, target, delayedCenter, delayedForward, delayedBatch));
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 1.2F, 0.7F);
   }

   private static void summonMatchlockArray(OdaNobunagaEntity entity, ServerLevel level, Vec3 center, Vec3 forward) {
      for (int batch = 0; batch < 6; batch++) {
         summonMatchlockArrayBatch(entity, entity.getTarget(), center, forward, batch);
      }
   }

   private static void summonMatchlockArrayBatch(OdaNobunagaEntity entity, LivingEntity target, Vec3 center, Vec3 forward, int batch) {
      if (!entity.isAlive() || !(entity.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity resolvedTarget = target != null && target.isAlive() ? target : entity.getTarget();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      int startIndex = Mth.clamp(batch, 0, 5) * 17;
      int endIndex = batch >= 5 ? 99 : Math.min(99, startIndex + 17);
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
            if (spawned >= startIndex && spawned < endIndex) {
               OdaMatchlockGunEntity gun = OdaMatchlockGunEntity.threeThousandWorlds(
                  level,
                  entity,
                  resolvedTarget,
                  pos,
                  resolvedTarget != null ? resolvedTarget.position().add(0.0, resolvedTarget.getBbHeight() * 0.55, 0.0).subtract(pos) : center.subtract(pos),
                  8 + (spawned % 3) * 20
               );
               level.addFreshEntity(gun);
            }
            spawned++;
         }
      }
      while (spawned < 99) {
         double angle = Math.PI * 2.0 * spawned / 99.0;
         Vec3 radial = forward.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
         Vec3 pos = center.add(radial.scale(8.0)).add(0.0, 2.0 + (spawned % 4), 0.0);
         if (spawned >= startIndex && spawned < endIndex) {
            level.addFreshEntity(OdaMatchlockGunEntity.threeThousandWorlds(
               level,
               entity,
               resolvedTarget,
               pos,
               resolvedTarget != null ? resolvedTarget.position().add(0.0, resolvedTarget.getBbHeight() * 0.55, 0.0).subtract(pos) : center.subtract(pos),
               8 + (spawned % 3) * 20
            ));
         }
         spawned++;
      }
      level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 2.0, center.z, 18, 4.0, 2.2, 4.0, 0.1);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.55F, 1.2F + batch * 0.05F);
   }

   private static boolean tryBeginHajun(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 100.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_HAJUN), HAJUN_COOLDOWN)) {
         return false;
      }
      if (!hasTrait(target, ServantTraitTag.DIVINE) && !shouldRarelyUseHajun(entity, target)) {
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
      entity.getPersistentData().putLong(TAG_LAND_FOR_NP_UNTIL, now + HAJUN_CHANT_TICKS + NP_LAND_TICKS);
      entity.getPersistentData().putLong(TAG_FLIGHT_UNTIL, now + HAJUN_CHANT_TICKS + HAJUN_DURATION);
      entity.getPersistentData().putInt(TAG_HAJUN_CHANT_TARGET, target.getId());
      landForNoblePhantasm(entity);
      entity.triggerChargeAnimation();
      ServantVoiceHelper.tryPlayOdaNobunagaHajun(entity);
      level.playSound(null, entity.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8F, 0.6F);
      return true;
   }

   private static boolean shouldRarelyUseHajun(OdaNobunagaEntity entity, LivingEntity target) {
      if (target == null || !target.isAlive()) {
         return false;
      }
      if (hasTrait(target, ServantTraitTag.DIVINE)) {
         return true;
      }
      boolean highValueTarget = target.getMaxHealth() >= 120.0F || target.getHealth() >= 90.0F || hasTrait(target, ServantTraitTag.DEMONIC);
      int chance = highValueTarget ? 10 : 3;
      return entity.getRandom().nextInt(100) < chance;
   }

   private static boolean tickHajunChant(OdaNobunagaEntity entity, ServerLevel level, long now) {
      long chantEnd = entity.getPersistentData().getLong(TAG_HAJUN_CHANT_END);
      if (chantEnd <= 0L) {
         return false;
      }
      entity.getNavigation().stop();
      if (now % 5L == 0L) {
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + 0.15, entity.getZ(), 16, 1.5, 0.15, 1.5, 0.03);
         stainHajunChantSurfaceFromCaster(entity, level, now, 16);
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
         relightAfterNoblePhantasm(entity, now);
      } else {
         restoreHajunChantTerrain(entity, level, Integer.MAX_VALUE);
      }
      return true;
   }

   private static void activateHajun(OdaNobunagaEntity entity, ServerLevel source, LivingEntity primary, long now) {
      ServerLevel hajunLevel = source.getServer().getLevel(ModDimensions.HAJUN_KEY);
      if (hajunLevel == null) {
         return;
      }
      recordHajunLockedTarget(entity, primary);
      List<LivingEntity> pulled = collectHajunTargets(entity, source, primary);
      if (!pulled.isEmpty() && pulled.stream().noneMatch(ServerPlayer.class::isInstance)) {
         restoreHajunChantTerrain(entity, source, Integer.MAX_VALUE);
         startOffscreenHajunDuel(entity, source, pulled.get(0), now);
         return;
      }

      CompoundTag data = entity.getPersistentData();
      data.putString(TAG_HAJUN_RETURN_DIM, source.dimension().location().toString());
      data.putDouble(TAG_HAJUN_RETURN_X, entity.getX());
      data.putDouble(TAG_HAJUN_RETURN_Y, entity.getY());
      data.putDouble(TAG_HAJUN_RETURN_Z, entity.getZ());
      UUID lockedTargetId = data.hasUUID(TAG_HAJUN_LOCKED_TARGET) ? data.getUUID(TAG_HAJUN_LOCKED_TARGET) : null;
      BlockPos sourceCenter = entity.blockPosition();
      source.playSound(null, sourceCenter, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.2F, 0.55F);
      for (int i = 0; i < 44; i++) {
         double angle = Math.PI * 2.0 * i / 44.0;
         double radius = 5.0 + i % 7;
         double px = entity.getX() + Math.cos(angle) * radius;
         double pz = entity.getZ() + Math.sin(angle) * radius;
         source.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, entity.getY() + 0.1, pz, 3, 0.16, 0.04, 0.16, 0.015);
         source.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, px, entity.getY() + 0.2, pz, 2, 0.08, 0.02, 0.08, 0.0);
      }

      Vec3 entry = new Vec3(entity.getRandom().nextInt(120) - 60 + 0.5, 72.0, entity.getRandom().nextInt(120) - 60 + 0.5);
      entry = new Vec3(entry.x, findSafeSpawnY(hajunLevel, Mth.floor(entry.x), Mth.floor(entry.z)), entry.z);
      BlockPos entryBlock = BlockPos.containing(entry);
      LivingEntity movedPrimary = moveHajunTargets(entity, source, hajunLevel, pulled, primary, entry);
      Entity moved = entity.changeDimension(new DimensionTransition(hajunLevel, entry, Vec3.ZERO, entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));
      if (moved instanceof OdaNobunagaEntity nobu) {
         applyHajunState(nobu, hajunLevel.getGameTime(), entryBlock);
         nobu.getPersistentData().putString(TAG_HAJUN_RETURN_DIM, source.dimension().location().toString());
         nobu.getPersistentData().putDouble(TAG_HAJUN_RETURN_X, data.getDouble(TAG_HAJUN_RETURN_X));
         nobu.getPersistentData().putDouble(TAG_HAJUN_RETURN_Y, data.getDouble(TAG_HAJUN_RETURN_Y));
         nobu.getPersistentData().putDouble(TAG_HAJUN_RETURN_Z, data.getDouble(TAG_HAJUN_RETURN_Z));
         if (lockedTargetId != null) {
            nobu.getPersistentData().putUUID(TAG_HAJUN_LOCKED_TARGET, lockedTargetId);
         }
         LivingEntity lockedInHajun = lockedTargetId != null ? findLivingByUuid(hajunLevel, lockedTargetId) : null;
         LivingEntity targetInHajun = lockedInHajun != null && lockedInHajun.isAlive() ? lockedInHajun : movedPrimary;
         if (targetInHajun != null && targetInHajun.isAlive()) {
            forceHajunCombatTarget(nobu, targetInHajun, hajunLevel);
         }
         RedSkeletonHajunEntity skeleton = new RedSkeletonHajunEntity(hajunLevel, nobu, HAJUN_DURATION);
         hajunLevel.addFreshEntity(skeleton);
         hajunLevel.playSound(null, entryBlock, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.2F, 0.55F);
         hajunLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entry.x, entry.y + 0.2, entry.z, 72, 4.0, 0.18, 4.0, 0.04);
         spreadHajunTerrain(nobu, hajunLevel, 220);
         applyHajunInstantDamage(nobu, hajunLevel);
      }
      restoreHajunChantTerrain(entity, source, Integer.MAX_VALUE);
   }

   private static boolean tickHajunField(OdaNobunagaEntity entity, ServerLevel level, long now) {
      long activeUntil = entity.getPersistentData().getLong(TAG_HAJUN_ACTIVE_UNTIL);
      if (activeUntil <= 0L) {
         return false;
      }
      boolean inHajunDimension = ModDimensions.isHajunDimension(level.dimension().location());
      long minUntil = entity.getPersistentData().getLong(TAG_HAJUN_MIN_UNTIL);
      if (!inHajunDimension) {
         restoreHajunChantTerrain(entity, level, Integer.MAX_VALUE);
         restoreHajunTerrain(entity, level, 120);
         clearHajunState(entity);
         return false;
      }
      if (activeUntil <= now) {
         if (!entity.isAlive() || now >= minUntil && !hasActiveHajunEnemy(entity, level)) {
            returnFromHajun(entity, level);
            return true;
         }
         entity.getPersistentData().putLong(TAG_HAJUN_ACTIVE_UNTIL, now + 20L);
      }
      boolean minElapsed = minUntil <= 0L || now >= minUntil;
      if (minElapsed && !hasActiveHajunEnemy(entity, level)) {
         returnFromHajun(entity, level);
         return true;
      }
      if (now % 20L == 0L) {
         applyHajunDot(entity, level);
      }
      if (now % 6L == 0L) {
         emitHajunAmbientFx(entity, level);
      }
      if (now >= entity.getPersistentData().getLong(TAG_HAJUN_NEXT_TERRAIN)) {
         level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 0.2, entity.getZ(), 18, 8.0, 0.15, 8.0, 0.02);
         spreadHajunTerrain(entity, level, 30);
         entity.getPersistentData().putLong(TAG_HAJUN_NEXT_TERRAIN, now + 5L);
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
      if (living == null || !living.isAlive() || living == owner || living.level() != source) {
         return false;
      }
      Vec3 rel = living.position().subtract(owner.position());
      return rel.y >= -2.0 && rel.lengthSqr() <= HAJUN_RADIUS * HAJUN_RADIUS;
   }

   private static LivingEntity moveHajunTargets(OdaNobunagaEntity owner, ServerLevel source, ServerLevel hajunLevel, List<LivingEntity> targets, LivingEntity primary, Vec3 entry) {
      LivingEntity movedPrimary = moveOneHajunTarget(owner, source, hajunLevel, primary, entry, true);
      for (LivingEntity living : targets) {
         if (living != primary) {
            moveOneHajunTarget(owner, source, hajunLevel, living, entry, false);
         }
      }
      return movedPrimary;
   }

   private static LivingEntity moveOneHajunTarget(OdaNobunagaEntity owner, ServerLevel source, ServerLevel hajunLevel, LivingEntity living, Vec3 entry, boolean primaryTarget) {
      if (!isHajunPullTarget(owner, living, source)) {
         return null;
      }
      double relX = Mth.clamp(living.getX() - owner.getX(), -16.0, 16.0);
      double relZ = Mth.clamp(living.getZ() - owner.getZ(), -16.0, 16.0);
      double targetX = entry.x + relX;
      double targetZ = entry.z + relZ;
      double targetY = findSafeSpawnY(hajunLevel, Mth.floor(targetX), Mth.floor(targetZ));
      double returnX = living.getX();
      double returnY = living.getY();
      double returnZ = living.getZ();
      markHajunTarget(owner, living, source, returnX, returnY, returnZ, primaryTarget);
      Entity moved = living.changeDimension(new DimensionTransition(hajunLevel, new Vec3(targetX, targetY, targetZ), Vec3.ZERO, living.getYRot(), living.getXRot(), DimensionTransition.DO_NOTHING));
      if (moved instanceof LivingEntity movedLiving) {
         markHajunTarget(owner, movedLiving, source, returnX, returnY, returnZ, primaryTarget);
         return movedLiving;
      }
      return null;
   }

   private static void markHajunTarget(OdaNobunagaEntity owner, LivingEntity living, ServerLevel source, double returnX, double returnY, double returnZ, boolean primaryTarget) {
      CompoundTag data = living.getPersistentData();
      data.putUUID(TAG_HAJUN_TARGET_OWNER, owner.getUUID());
      data.putString(TAG_HAJUN_TARGET_RETURN_DIM, source.dimension().location().toString());
      data.putDouble(TAG_HAJUN_TARGET_RETURN_X, returnX);
      data.putDouble(TAG_HAJUN_TARGET_RETURN_Y, returnY);
      data.putDouble(TAG_HAJUN_TARGET_RETURN_Z, returnZ);
      if (primaryTarget) {
         data.putBoolean(TAG_HAJUN_TARGET_PRIMARY, true);
      } else {
         data.remove(TAG_HAJUN_TARGET_PRIMARY);
      }
   }

   private static void applyHajunInstantDamage(OdaNobunagaEntity entity, ServerLevel level) {
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         entity.getBoundingBox().inflate(HAJUN_RADIUS),
         living -> isPulledByHajun(entity.getUUID(), living) && living != entity && !living.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(living)
      )) {
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
         if (candidate instanceof LivingEntity living
            && living.isAlive()
            && isPulledByHajun(ownerId, living)
            && !living.isAlliedTo(entity)
            && !EntityUtils.isImmunePlayerTarget(living)) {
            return true;
         }
      }

      LivingEntity target = entity.getTarget();
      return target != null
         && target.isAlive()
         && target.level() == level
         && target != entity
         && !target.isAlliedTo(entity)
         && !EntityUtils.isImmunePlayerTarget(target);
   }

   private static void returnFromHajun(OdaNobunagaEntity entity, ServerLevel level) {
      UUID ownerId = entity.getUUID();
      CompoundTag data = entity.getPersistentData();
      UUID lockedTargetId = data.hasUUID(TAG_HAJUN_LOCKED_TARGET) ? data.getUUID(TAG_HAJUN_LOCKED_TARGET) : null;
      ServerLevel returnLevel = resolveDimensionOrOverworld(level, data.getString(TAG_HAJUN_RETURN_DIM));
      Vec3 returnPos = new Vec3(data.getDouble(TAG_HAJUN_RETURN_X), data.getDouble(TAG_HAJUN_RETURN_Y), data.getDouble(TAG_HAJUN_RETURN_Z));
      UUID primaryReturnedId = returnPulledHajunTargets(ownerId, level, returnLevel);
      restoreHajunTerrain(entity, level, Integer.MAX_VALUE);
      clearHajunState(entity);
      if (entity.isAlive()) {
         Entity moved = entity.changeDimension(new DimensionTransition(returnLevel, returnPos, Vec3.ZERO, entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));
         if (moved instanceof OdaNobunagaEntity returned) {
            clearHajunState(returned);
            returned.getNavigation().stop();
            restoreReturnedHajunCombatTarget(returned, returnLevel, lockedTargetId != null ? lockedTargetId : primaryReturnedId);
         }
      }
      for (RedSkeletonHajunEntity skeleton : level.getEntitiesOfClass(RedSkeletonHajunEntity.class, new AABB(-256.0, level.getMinBuildHeight(), -256.0, 256.0, level.getMaxBuildHeight(), 256.0))) {
         skeleton.discard();
      }
   }

   private static UUID returnPulledHajunTargets(UUID ownerId, ServerLevel sourceLevel, ServerLevel fallbackLevel) {
      List<LivingEntity> toReturn = new ArrayList<>();
      for (Entity candidate : sourceLevel.getEntities().getAll()) {
         if (candidate instanceof LivingEntity living && living.isAlive() && isPulledByHajun(ownerId, living)) {
            toReturn.add(living);
         }
      }
      UUID primaryReturnedId = null;
      for (LivingEntity living : toReturn) {
         CompoundTag data = living.getPersistentData();
         boolean primaryTarget = data.getBoolean(TAG_HAJUN_TARGET_PRIMARY);
         ServerLevel returnLevel = resolveDimensionOrFallback(sourceLevel, data.getString(TAG_HAJUN_TARGET_RETURN_DIM), fallbackLevel);
         Vec3 returnPos = new Vec3(data.getDouble(TAG_HAJUN_TARGET_RETURN_X), data.getDouble(TAG_HAJUN_TARGET_RETURN_Y), data.getDouble(TAG_HAJUN_TARGET_RETURN_Z));
         clearHajunTarget(living);
         Entity moved = living.changeDimension(new DimensionTransition(returnLevel, returnPos, Vec3.ZERO, living.getYRot(), living.getXRot(), DimensionTransition.DO_NOTHING));
         if (moved instanceof LivingEntity movedLiving) {
            if (primaryTarget) {
               primaryReturnedId = movedLiving.getUUID();
            }
            clearHajunTarget(movedLiving);
         } else if (primaryTarget) {
            primaryReturnedId = living.getUUID();
         }
      }
      return primaryReturnedId;
   }

   private static LivingEntity findLivingByUuid(ServerLevel level, UUID id) {
      if (id == null) {
         return null;
      }
      Entity entity = level.getEntity(id);
      return entity instanceof LivingEntity living ? living : null;
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

   private static void expireBuffs(OdaNobunagaEntity entity, ServerLevel level, long now) {
      if (entity.getPersistentData().getLong(TAG_STRATEGY_UNTIL) > now && now % 20L == 0L) {
         entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + 1.25));
      }
      if (entity.getPersistentData().getLong(TAG_MAOU_UNTIL) <= now) {
         entity.getPersistentData().remove(TAG_MAOU_UNTIL);
      }
      if (entity.getPersistentData().getLong(TAG_FLIGHT_UNTIL) <= now) {
         entity.getPersistentData().remove(TAG_FLIGHT_UNTIL);
         if (entity.isNoGravity()) {
            entity.setNoGravity(false);
         }
         clearFootSupportGun(entity, level);
      }
      if (entity.getPersistentData().getLong(TAG_LAND_FOR_NP_UNTIL) <= now) {
         entity.getPersistentData().remove(TAG_LAND_FOR_NP_UNTIL);
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

   private static void updateSeriousModeFlight(OdaNobunagaEntity entity, LivingEntity target, long now) {
      if (entity.getPersistentData().getLong(TAG_LAND_FOR_NP_UNTIL) > now) {
         landForNoblePhantasm(entity);
         return;
      }
      boolean serious = entity.getHealth() <= entity.getMaxHealth() * 0.6F
         || entity.getPersistentData().getLong(TAG_MAOU_UNTIL) > now
         || entity.getPersistentData().getLong(TAG_THREE_THOUSAND_CHANT_END) > now
         || entity.getPersistentData().getLong(TAG_HAJUN_CHANT_END) > now
         || entity.getPersistentData().getLong(TAG_FLIGHT_UNTIL) > now;
      if (!serious || entity.isSpiritualDissolving()) {
         if (entity.isNoGravity()) {
            entity.setNoGravity(false);
         }
         return;
      }

      entity.setNoGravity(true);
      entity.fallDistance = 0.0F;
      ensureFootSupportGun(entity, now);
      if (target != null && target.isAlive()) {
         Vec3 toTarget = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
         Vec3 move = toTarget.lengthSqr() > 1.0E-4 ? toTarget.normalize() : entity.getLookAngle().multiply(1.0, 0.0, 1.0);
         double hoverY = target.getY() + target.getBbHeight() * 0.58 + 0.95;
         double yError = hoverY - entity.getY();
         double yDelta = Mth.clamp(yError * 0.12, -0.28, 0.22);
         entity.setDeltaMovement(
            entity.getDeltaMovement().x * 0.86 + move.x * 0.12,
            Mth.clamp(entity.getDeltaMovement().y * 0.68 + yDelta, -0.24, 0.28),
            entity.getDeltaMovement().z * 0.86 + move.z * 0.12
         );
         entity.hasImpulse = true;
         if (entity.getY() < hoverY - 0.65) {
            entity.setDeltaMovement(entity.getDeltaMovement().x, Math.max(entity.getDeltaMovement().y, 0.14), entity.getDeltaMovement().z);
         } else if (entity.getY() > hoverY + 1.25) {
            entity.setDeltaMovement(entity.getDeltaMovement().x, Math.min(entity.getDeltaMovement().y, -0.06), entity.getDeltaMovement().z);
         }
      }
      if (entity.getPersistentData().getLong(TAG_FLIGHT_UNTIL) <= now) {
         entity.getPersistentData().putLong(TAG_FLIGHT_UNTIL, now + FLIGHT_RAMPUP_TICKS);
      }
   }

   private static void ensureFootSupportGun(OdaNobunagaEntity entity, long now) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      UUID gunId = entity.getPersistentData().hasUUID(TAG_FOOT_SUPPORT_GUN) ? entity.getPersistentData().getUUID(TAG_FOOT_SUPPORT_GUN) : null;
      Entity resolved = gunId == null ? null : level.getEntity(gunId);
      if (resolved instanceof OdaMatchlockGunEntity gun && gun.isAlive() && gun.isFootSupportFor(entity.getUUID())) {
         gun.setPos(entity.getX(), entity.getY() + 0.18, entity.getZ());
         return;
      }
      OdaMatchlockGunEntity gun = OdaMatchlockGunEntity.footSupport(level, entity);
      gun.setPos(entity.getX(), entity.getY() + 0.18, entity.getZ());
      level.addFreshEntity(gun);
      entity.getPersistentData().putUUID(TAG_FOOT_SUPPORT_GUN, gun.getUUID());
      entity.getPersistentData().putLong(TAG_FLIGHT_UNTIL, now + FLIGHT_RAMPUP_TICKS);
   }

   private static void clearFootSupportGun(OdaNobunagaEntity entity, ServerLevel level) {
      if (!entity.getPersistentData().hasUUID(TAG_FOOT_SUPPORT_GUN)) {
         return;
      }
      Entity resolved = level.getEntity(entity.getPersistentData().getUUID(TAG_FOOT_SUPPORT_GUN));
      if (resolved != null) {
         resolved.discard();
      }
      entity.getPersistentData().remove(TAG_FOOT_SUPPORT_GUN);
   }

   private static void landForNoblePhantasm(OdaNobunagaEntity entity) {
      entity.setNoGravity(false);
      entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.4, 0.0, 0.4));
      entity.fallDistance = 0.0F;
   }

   private static void relightAfterNoblePhantasm(OdaNobunagaEntity entity, long now) {
      entity.getPersistentData().putLong(TAG_FLIGHT_UNTIL, now + NP_REFLIGHT_TICKS);
   }

   private static Vec3 horizontalDirection(LivingEntity from, LivingEntity to) {
      Vec3 dir = to.position().subtract(from.position()).multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = from.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      return dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
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
      data.remove(TAG_HAJUN_MIN_UNTIL);
      data.remove(TAG_HAJUN_START_TICK);
      data.remove(TAG_HAJUN_STARTED);
      data.remove(TAG_HAJUN_NEXT_TERRAIN);
      data.remove(TAG_HAJUN_CENTER_X);
      data.remove(TAG_HAJUN_CENTER_Y);
      data.remove(TAG_HAJUN_CENTER_Z);
      data.remove(TAG_HAJUN_RETURN_DIM);
      data.remove(TAG_HAJUN_RETURN_X);
      data.remove(TAG_HAJUN_RETURN_Y);
      data.remove(TAG_HAJUN_RETURN_Z);
      data.remove(TAG_HAJUN_LOCKED_TARGET);
      clearHajunDuelState(entity);
   }

   private static void clearHajunTarget(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      data.remove(TAG_HAJUN_TARGET_OWNER);
      data.remove(TAG_HAJUN_TARGET_RETURN_DIM);
      data.remove(TAG_HAJUN_TARGET_RETURN_X);
      data.remove(TAG_HAJUN_TARGET_RETURN_Y);
      data.remove(TAG_HAJUN_TARGET_RETURN_Z);
      data.remove(TAG_HAJUN_TARGET_PRIMARY);
   }

   private static void recordHajunLockedTarget(OdaNobunagaEntity entity, LivingEntity target) {
      if (target != null && target.isAlive()) {
         entity.getPersistentData().putUUID(TAG_HAJUN_LOCKED_TARGET, target.getUUID());
      }
   }

   private static void forceHajunCombatTarget(OdaNobunagaEntity entity, LivingEntity target, ServerLevel level) {
      if (target == null || !target.isAlive() || target == entity) {
         return;
      }
      entity.setTarget(target);
      if (target instanceof Mob mob) {
         mob.setTarget(entity);
      }
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
      entity.getPersistentData().putLong("TypeMoonCombatLastCombatTick", level.getGameTime());
      recordHajunLockedTarget(entity, target);
   }

   private static void restoreReturnedHajunCombatTarget(OdaNobunagaEntity entity, ServerLevel level, UUID lockedTargetId) {
      LivingEntity closest = findReturnedHajunPrimaryTarget(level, lockedTargetId);
      double closestDistance = Double.MAX_VALUE;
      if (closest == null) {
         AABB area = entity.getBoundingBox().inflate(32.0);
         for (LivingEntity living : level.getEntitiesOfClass(
            LivingEntity.class,
            area,
            e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
         )) {
            double distance = living.distanceToSqr(entity);
            if (distance < closestDistance) {
               closest = living;
               closestDistance = distance;
            }
         }
      }
      if (closest != null) {
         forceHajunCombatTarget(entity, closest, level);
         level.sendParticles(ParticleTypes.ANGRY_VILLAGER, entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ(), 3, 0.25, 0.25, 0.25, 0.0);
      }
   }

   private static LivingEntity findReturnedHajunPrimaryTarget(ServerLevel level, UUID primaryReturnedId) {
      if (primaryReturnedId == null) {
         return null;
      }
      Entity entity = level.getEntity(primaryReturnedId);
      return entity instanceof LivingEntity living && living.isAlive() ? living : null;
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

   private static void applyHajunState(OdaNobunagaEntity entity, long now, BlockPos center) {
      CompoundTag data = entity.getPersistentData();
      data.putLong(TAG_HAJUN_ACTIVE_UNTIL, now + HAJUN_DURATION);
      data.putLong(TAG_HAJUN_MIN_UNTIL, now + 10L * 20L);
      data.putLong(TAG_HAJUN_START_TICK, now);
      data.putLong(TAG_HAJUN_NEXT_TERRAIN, now + 1L);
      data.putInt(TAG_HAJUN_CENTER_X, center.getX());
      data.putInt(TAG_HAJUN_CENTER_Y, center.getY());
      data.putInt(TAG_HAJUN_CENTER_Z, center.getZ());
      data.putBoolean(TAG_HAJUN_STARTED, false);
   }

   private static void startOffscreenHajunDuel(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (target == null || !target.isAlive()) {
         return;
      }
      int duration = 100 + entity.getRandom().nextInt(101);
      Vec3 center = entity.position().add(target.position()).scale(0.5);
      entity.triggerNamedActionAnimation("hajun_chant");
      ServantVoiceHelper.tryPlayOdaNobunagaHajun(entity);
      level.playSound(null, BlockPos.containing(center), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.15F, 0.58F);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.2, center.z, 44, 2.2, 0.25, 2.2, 0.04);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y + 0.25, center.z, 32, 1.2, 0.18, 1.2, 0.03);
      hideForHajunDuel(entity);
      hideForHajunDuel(target);
      entity.setTarget(null);
      if (target instanceof Mob mobTarget) {
         mobTarget.setTarget(null);
      }

      double nobuScore = duelScore(entity) + entity.getRandom().nextDouble() * 55.0;
      double targetScore = duelScore(target) + target.getRandom().nextDouble() * 55.0;
      int outcome = nobuScore > targetScore + 16.0 ? 1 : (targetScore > nobuScore + 16.0 ? -1 : 0);
      TYPE_MOON_WORLD.queueServerWork(duration, () -> finishOffscreenHajunDuel(entity, target, outcome));
   }

   private static void stainHajunChantSurfaceFromCaster(OdaNobunagaEntity entity, ServerLevel level, long now, int budget) {
      long chantAge = Math.max(0L, now - entity.getPersistentData().getLong(TAG_HAJUN_CHANT_END) + HAJUN_CHANT_TICKS);
      if (chantAge < HAJUN_CHANT_SURFACE_SPREAD_DELAY) {
         return;
      }
      int radius = Mth.clamp(2 + (int)((chantAge - HAJUN_CHANT_SURFACE_SPREAD_DELAY) / 4L), 2, HAJUN_CHANT_SURFACE_RADIUS);
      BlockPos center = entity.blockPosition();
      Map<BlockPos, BlockBackup> backups = ODA_HAJUN_CHANT_BLOCKS.computeIfAbsent(entity.getUUID(), key -> new HashMap<>());
      int changed = 0;
      int attempts = budget * 4;
      for (int i = 0; i < attempts && changed < budget; i++) {
         double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
         double distance = Math.sqrt(entity.getRandom().nextDouble()) * radius;
         BlockPos sample = center.offset(Mth.floor(Math.cos(angle) * distance), 0, Mth.floor(Math.sin(angle) * distance));
         BlockPos surface = findNearbySurface(level, sample);
         if (surface == null || backups.containsKey(surface)) {
            continue;
         }
         BlockState current = level.getBlockState(surface);
         if (!canReplaceHajunSurface(current)) {
            continue;
         }
         backups.put(surface.immutable(), new BlockBackup(current, saveBlockEntity(level, surface)));
         level.setBlock(surface, level.random.nextBoolean() ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState(), 3);
         changed++;
         if (entity.getRandom().nextInt(2) == 0) {
            level.sendParticles(ParticleTypes.FLAME, surface.getX() + 0.5, surface.getY() + 1.05, surface.getZ() + 0.5, 1, 0.1, 0.03, 0.1, 0.01);
         }
      }
   }

   private static void emitHajunAmbientFx(OdaNobunagaEntity entity, ServerLevel level) {
      double x = entity.getX();
      double y = entity.getY() + entity.getBbHeight() * 0.45;
      double z = entity.getZ();
      level.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 2.5, 0.35, 2.5, 0.01);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.2, z, 1, 2.0, 0.25, 2.0, 0.005);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.1, z, 2, 2.2, 0.2, 2.2, 0.01);
      if (level.random.nextInt(3) == 0) {
         level.sendParticles(ParticleTypes.FLAME, x, y + 0.15, z, 2, 1.8, 0.18, 1.8, 0.01);
      }
   }

   private static void hideForHajunDuel(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      data.putBoolean(TAG_HAJUN_OFFSCREEN_DUEL, true);
      data.putBoolean(TAG_HAJUN_OFFSCREEN_PREVIOUS_INVISIBLE, living.isInvisible());
      data.putBoolean(TAG_HAJUN_OFFSCREEN_PREVIOUS_INVULNERABLE, living.isInvulnerable());
      if (living instanceof Mob mob) {
         data.putBoolean(TAG_HAJUN_OFFSCREEN_PREVIOUS_NO_AI, mob.isNoAi());
         mob.setNoAi(true);
         mob.getNavigation().stop();
      }
      living.setInvisible(true);
      living.setInvulnerable(true);
      living.setDeltaMovement(Vec3.ZERO);
      living.hurtMarked = true;
   }

   private static void finishOffscreenHajunDuel(OdaNobunagaEntity entity, LivingEntity target, int outcome) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = entity.position().add(target.position()).scale(0.5);
      restoreFromHajunDuel(entity);
      restoreFromHajunDuel(target);
      if (!entity.isAlive() || !target.isAlive()) {
         return;
      }
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.2, center.z, 34, 1.8, 0.2, 1.8, 0.035);
      level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 0.8, entity.getZ(), 16, 0.35, 0.35, 0.35, 0.04);
      level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 0.8, target.getZ(), 16, 0.35, 0.35, 0.35, 0.04);
      level.playSound(null, BlockPos.containing(center), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.75F);

      if (outcome > 0) {
         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().magic(), Math.max(target.getMaxHealth() + 1.0F, 80.0F));
         target.invulnerableTime = 0;
      } else if (outcome < 0) {
         entity.invulnerableTime = 0;
         entity.hurt(target.damageSources().magic(), Math.max(entity.getMaxHealth() + 1.0F, 80.0F));
         entity.invulnerableTime = 0;
      } else {
         entity.setHealth(Math.max(1.0F, entity.getHealth() * 0.55F));
         target.setHealth(Math.max(1.0F, target.getHealth() * 0.55F));
         forceHajunCombatTarget(entity, target, level);
      }
   }

   private static void restoreFromHajunDuel(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      if (!data.getBoolean(TAG_HAJUN_OFFSCREEN_DUEL)) {
         return;
      }
      living.setInvisible(data.getBoolean(TAG_HAJUN_OFFSCREEN_PREVIOUS_INVISIBLE));
      living.setInvulnerable(data.getBoolean(TAG_HAJUN_OFFSCREEN_PREVIOUS_INVULNERABLE));
      if (living instanceof Mob mob) {
         mob.setNoAi(data.getBoolean(TAG_HAJUN_OFFSCREEN_PREVIOUS_NO_AI));
      }
      clearHajunDuelState(living);
   }

   private static void clearHajunDuelState(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      data.remove(TAG_HAJUN_OFFSCREEN_DUEL);
      data.remove(TAG_HAJUN_OFFSCREEN_PREVIOUS_INVISIBLE);
      data.remove(TAG_HAJUN_OFFSCREEN_PREVIOUS_INVULNERABLE);
      data.remove(TAG_HAJUN_OFFSCREEN_PREVIOUS_NO_AI);
   }

   private static CompoundTag saveBlockEntity(ServerLevel level, BlockPos pos) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      return blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess());
   }

   private static double duelScore(LivingEntity living) {
      double score = living.getHealth() / Math.max(1.0F, living.getMaxHealth()) * 35.0;
      score += living.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.4;
      score += living.getAttributeValue(Attributes.ARMOR) * 0.75;
      score += living.getAttributeValue(Attributes.MOVEMENT_SPEED) * 55.0;
      if (living instanceof ServantEntity servant && servant.getDefinition() != null) {
         ServantParams params = servant.getDefinition().parameters();
         score += effectiveRank(params.strength(), params.strengthPlus()) * 0.45;
         score += effectiveRank(params.agility(), params.agilityPlus()) * 0.38;
         score += effectiveRank(params.magic(), params.magicPlus()) * 0.28;
         score += effectiveRank(params.luck(), params.luckPlus()) * 0.18;
      }
      return score;
   }

   private static int effectiveRank(net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank rank, boolean plus) {
      return plus ? rank.plusCoefficient() : rank.coefficient();
   }

   private static void spreadHajunTerrain(OdaNobunagaEntity entity, ServerLevel level, int budget) {
      BlockPos center = new BlockPos(
         entity.getPersistentData().getInt(TAG_HAJUN_CENTER_X),
         entity.getPersistentData().getInt(TAG_HAJUN_CENTER_Y),
         entity.getPersistentData().getInt(TAG_HAJUN_CENTER_Z)
      );
      Map<BlockPos, BlockBackup> backups = ODA_HAJUN_BLOCKS.computeIfAbsent(entity.getUUID(), key -> new HashMap<>());
      long age = Math.max(0L, level.getGameTime() - entity.getPersistentData().getLong(TAG_HAJUN_START_TICK));
      int radius = Mth.clamp(6 + (int)(age / 3L), 6, HAJUN_TERRAIN_RADIUS);
      for (int i = 0; i < budget; i++) {
         double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
         double distance = Math.sqrt(entity.getRandom().nextDouble()) * radius;
         BlockPos sample = center.offset(Mth.floor(Math.cos(angle) * distance), 0, Mth.floor(Math.sin(angle) * distance));
         BlockPos surface = findNearbySurface(level, sample);
         if (surface == null || backups.containsKey(surface)) {
            continue;
         }
         BlockState current = level.getBlockState(surface);
         if (!canReplaceHajunSurface(current)) {
            continue;
         }
         backups.put(surface.immutable(), new BlockBackup(current, saveBlockEntity(level, surface)));
         level.setBlock(surface, level.random.nextBoolean() ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState(), 3);
         if (entity.getRandom().nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.FLAME, surface.getX() + 0.5, surface.getY() + 1.05, surface.getZ() + 0.5, 1, 0.1, 0.03, 0.1, 0.01);
         }
      }
   }

   private static void restoreHajunTerrain(OdaNobunagaEntity entity, ServerLevel level, int budget) {
      Map<BlockPos, BlockBackup> backups = ODA_HAJUN_BLOCKS.get(entity.getUUID());
      if (backups == null || backups.isEmpty()) {
         return;
      }
      Iterator<Map.Entry<BlockPos, BlockBackup>> iterator = backups.entrySet().iterator();
      int restored = 0;
      while (iterator.hasNext() && restored++ < budget) {
         Map.Entry<BlockPos, BlockBackup> entry = iterator.next();
         BlockPos pos = entry.getKey();
         BlockBackup backup = entry.getValue();
         level.setBlock(pos, backup.state(), 3);
         if (backup.blockEntityNbt() != null && level.getBlockEntity(pos) instanceof BlockEntity blockEntity) {
            blockEntity.loadWithComponents(backup.blockEntityNbt(), level.registryAccess());
            blockEntity.setChanged();
         }
         iterator.remove();
      }
      if (backups.isEmpty()) {
         ODA_HAJUN_BLOCKS.remove(entity.getUUID());
      }
   }

   private static void restoreHajunChantTerrain(OdaNobunagaEntity entity, ServerLevel level, int budget) {
      Map<BlockPos, BlockBackup> backups = ODA_HAJUN_CHANT_BLOCKS.get(entity.getUUID());
      if (backups == null || backups.isEmpty()) {
         return;
      }
      Iterator<Map.Entry<BlockPos, BlockBackup>> iterator = backups.entrySet().iterator();
      int restored = 0;
      while (iterator.hasNext() && restored++ < budget) {
         Map.Entry<BlockPos, BlockBackup> entry = iterator.next();
         BlockPos pos = entry.getKey();
         BlockBackup backup = entry.getValue();
         level.setBlock(pos, backup.state(), 3);
         if (backup.blockEntityNbt() != null && level.getBlockEntity(pos) instanceof BlockEntity blockEntity) {
            blockEntity.loadWithComponents(backup.blockEntityNbt(), level.registryAccess());
            blockEntity.setChanged();
         }
         iterator.remove();
      }
      if (backups.isEmpty()) {
         ODA_HAJUN_CHANT_BLOCKS.remove(entity.getUUID());
      }
   }

   private static boolean canReplaceHajunSurface(BlockState state) {
      return !state.isAir() && !state.hasBlockEntity() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.MAGMA_BLOCK) && !state.is(Blocks.NETHERRACK);
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

   private static boolean canUse(long now, long lastUse, int cooldown) {
      return lastUse <= 0L || now - lastUse >= cooldown;
   }

   private record BlockBackup(BlockState state, CompoundTag blockEntityNbt) {
   }
}
