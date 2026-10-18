package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.ArrayList;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaDamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaRules;
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
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantFlightHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantFlightCombatService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantEngagementService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantNoblePhantasmResourceService;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

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
   public static final String TAG_HAJUN_POWER_SCALE = "OdaHajunPowerScale";
   public static final String TAG_HAJUN_OVERDRAFT = "OdaHajunOverdraft";
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
   private static final String TAG_LAST_PERSISTENT_STATE_TICK = "OdaLastPersistentStateTick";
   private static final String TAG_PERSISTENT_STATE_BUSY = "OdaPersistentStateBusy";
   private static final int FLIGHT_RAMPUP_TICKS = 20;
   private static final int FLIGHT_HOLD_TICKS = 8 * 20;
   private static final int NP_LAND_TICKS = 10;
   private static final int NP_REFLIGHT_TICKS = 18;

   private static final int THREE_THOUSAND_COOLDOWN = 3600;
   private static final int THREE_THOUSAND_CHANT_TICKS = 6 * 20;
   private static final int HAJUN_CHANT_TICKS = 5 * 20;
   private static final int HAJUN_DURATION = 15 * 20;
   private static final double HAJUN_RADIUS = 25.0;
   private static final double HAJUN_PULL_RADIUS = 32.0;
   private static final int HAJUN_TERRAIN_RADIUS = 25;
   private static final int HAJUN_CHANT_SURFACE_SPREAD_DELAY = 10;
   private static final int HAJUN_CHANT_SURFACE_RADIUS = 16;
   private static final Map<UUID, Map<BlockPos, BlockBackup>> ODA_HAJUN_BLOCKS = new HashMap<>();
   private static final Map<UUID, Map<BlockPos, BlockBackup>> ODA_HAJUN_CHANT_BLOCKS = new HashMap<>();

   private OdaNobunagaCombatHelper() {
   }

   public static void tick(OdaNobunagaEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      long now = level.getGameTime();
      if (tickPersistentState(entity)) return;
      LivingEntity target = entity.getTarget();
      if (target == null || !OdaAbilityService.enemy(entity, target)) { updateSeriousModeFlight(entity, null, now); return; }
      entity.faceToward(target.getBoundingBox().getCenter());
      double distance = entity.distanceTo(target);
      float hp = entity.getHealth() / Math.max(1, entity.getMaxHealth());
      updateSeriousModeFlight(entity, target, now);
      if (hp <= .6F) {
         if (hasTrait(target, ServantTraitTag.DIVINE) && tryBeginHajun(entity, level, target, now)) return;
         if (tryCastThreeThousandWorlds(entity, level, target, now, true)) return;
         if (tryBeginHajun(entity, level, target, now)) return;
      }
      // Distance and phase drive selection, while every effect/cost/cooldown uses the card's contract.
      if (distance < 5 && castResetSkill(entity, -1, now)) return;
      if (OdaAbilityService.orbitCount(entity) < 5 && castResetSkill(entity, 0, now)) return;
      if (hp <= .8F && castResetSkill(entity, 7, now)) return;
      int[] candidates = distance < 6 ? new int[]{6, 4, 1, 5, 2} : distance > 22 ? new int[]{3, 2, 1, 4, 5} : new int[]{2, 4, 1, 5, 3};
      if (hp <= .8F || entity.getRandom().nextInt(4) == 0) {
         for (int slot : candidates) if (castResetSkill(entity, slot, now)) return;
      }
      handleMovementAndRifle(entity, level, target, now, distance);
   }

   private static boolean castResetSkill(OdaNobunagaEntity entity, int slot, long now) {
      var skill = OdaRules.skill(slot);
      String key = "OdaResetCooldown" + slot;
      if (entity.getPersistentData().getLong(key) > now || entity.getCurrentMp() < skill.mp() || !OdaAbilityService.canCast(entity, slot)) return false;
      if (!OdaAbilityService.cast(entity, slot)) return false;
      entity.setCurrentMp(entity.getCurrentMp() - skill.mp());
      ServantVoiceHelper.tryPlayOdaNobunagaSkill(entity);
      entity.getPersistentData().putLong(key, now + skill.cooldown());
      entity.triggerNamedActionAnimation(slot == -1 || slot == 6 ? "hasebe_thrust" : slot == 7 ? "maou" : "summon_guns");
      entity.getPersistentData().putLong("OdaResetRecovery", now + (slot == 3 ? 40 : 12));
      return true;
   }

   public static float scaleThreeThousandWorldsDamage(LivingEntity owner, LivingEntity target, float base) {
      return base; // Damage provenance is resolved once by OdaCombatEvents.
   }

   public static void applyDivineDefenseBreak(LivingEntity owner, LivingEntity target) {
      if (!(owner.level() instanceof ServerLevel level) || net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaCombatEvents.divinity(target) <= 0) {
         return;
      }
      target.getPersistentData().putLong(TAG_DIVINE_BREAK_UNTIL, level.getGameTime() + 100L);
      target.invulnerableTime = 0;
      target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      target.removeEffect(MobEffects.ABSORPTION);
      target.setAbsorptionAmount(0);
      target.removeEffect(MobEffects.FIRE_RESISTANCE);
   }

   public static void cleanupHajun(OdaNobunagaEntity entity) {
      OdaAbilityService.clear(entity);
      if (entity.level() instanceof ServerLevel level) {
         if (entity.getPersistentData().getLong(TAG_HAJUN_ACTIVE_UNTIL) > 0 && (ModDimensions.isHajunDimension(level.dimension().location()) || entity.getPersistentData().getBoolean("OdaHajunLocal"))) {
            returnFromHajun(entity, level);
            return;
         }
         restoreHajunChantTerrain(entity, level, Integer.MAX_VALUE);
         clearHajunState(entity);
      }
   }

   public static boolean tickPersistentState(OdaNobunagaEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) return false;
      long now = level.getGameTime();
      OdaAbilityService.prepare(entity);
      CompoundTag data = entity.getPersistentData();
      if (data.contains(TAG_LAST_PERSISTENT_STATE_TICK) && data.getLong(TAG_LAST_PERSISTENT_STATE_TICK) == now) {
         return data.getBoolean(TAG_PERSISTENT_STATE_BUSY) || data.getLong("OdaResetRecovery") > now;
      }
      data.putLong(TAG_LAST_PERSISTENT_STATE_TICK, now);
      tickThreeThousandWorldsChant(entity, level, now);
      boolean busy = tickHajunChant(entity, level, now) || tickHajunField(entity, level, now);
      if (!busy) expireBuffs(entity, level, now);
      data.putBoolean(TAG_PERSISTENT_STATE_BUSY, busy);
      return busy || entity.getPersistentData().getLong("OdaResetRecovery") > now;
   }

   private static boolean tryCastThreeThousandWorlds(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now, boolean highPriority) {
      if (entity.getPersistentData().getLong("OdaSharedNpUntil") > now || entity.getPersistentData().getLong(TAG_THREE_THOUSAND_CHANT_END) > now) {
         return false;
      }
      if (entity.getCurrentMp() < 95.0 || !canUse(now, entity.getPersistentData().getLong(TAG_LAST_THREE_THOUSAND), THREE_THOUSAND_COOLDOWN)) {
         return false;
      }
      if (!highPriority && entity.getRandom().nextInt(100) > 35) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 95.0);
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.noteNpcPayment(entity, 95.0);
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.begin(entity, "oda_three_thousand", THREE_THOUSAND_COOLDOWN,
         ticks -> { entity.getPersistentData().putLong(TAG_LAST_THREE_THOUSAND, entity.level().getGameTime() - THREE_THOUSAND_COOLDOWN + ticks); entity.getPersistentData().putLong("OdaSharedNpUntil", entity.level().getGameTime() + ticks); }, () -> { entity.getPersistentData().remove(TAG_THREE_THOUSAND_CHANT_END); entity.getPersistentData().remove(TAG_THREE_THOUSAND_TARGET); });
      entity.getPersistentData().putLong(TAG_LAST_THREE_THOUSAND, now);
      entity.getPersistentData().putLong("OdaSharedNpUntil", now + 3600);
      entity.getPersistentData().putLong(TAG_THREE_THOUSAND_CHANT_END, net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.voiceEnd(entity));
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.deferEffect(entity, () -> { });
      entity.getPersistentData().putLong(TAG_LAND_FOR_NP_UNTIL, now + THREE_THOUSAND_CHANT_TICKS + NP_LAND_TICKS);
      entity.getPersistentData().putLong(TAG_FLIGHT_UNTIL, now + THREE_THOUSAND_CHANT_TICKS + FLIGHT_HOLD_TICKS);
      entity.getPersistentData().putInt(TAG_THREE_THOUSAND_TARGET, target.getId());
      landForNoblePhantasm(entity);
      entity.triggerNamedActionAnimation("strategy");
      ServantVoiceHelper.tryPlayOdaNobunagaNp(entity);
      VFXServerEffects.spawn(level, "servant_oda_three_thousand", entity, 160.0);
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
      ServantEngagementService.maintainRangedPosition(
         entity,
         target,
         now,
         8.0,
         15.0,
         22.0,
         1.05,
         "OdaRiflePosition"
      );

      if (canUse(now, entity.getPersistentData().getLong(TAG_LAST_RIFLE_SHOT), 16)) {
         shootRifle(entity, level, target, now);
      }
   }

   private static void releaseThreeThousandWorlds(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target) {
      OdaAbilityService.threeThousand(entity, target, true, entity.position().add(0, 1.2, 0));
   }

   private static boolean tryBeginHajun(OdaNobunagaEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 130 || entity.getPersistentData().getLong("OdaSharedNpUntil") > now) return false;
      if (!hasTrait(target, ServantTraitTag.DIVINE) && !shouldRarelyUseHajun(entity, target)) return false;
      if (UBWInstanceManager.isUbwDimension(level) || ModDimensions.isHajunDimension(level.dimension().location())) return false;
      entity.setCurrentMp(entity.getCurrentMp() - 35);
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.noteNpcPayment(entity, 35);
      entity.getPersistentData().putLong(TAG_HAJUN_CHANT_END, now + HAJUN_CHANT_TICKS);
      entity.getPersistentData().putLong(TAG_HAJUN_START_TICK, now);
      entity.getPersistentData().putInt(TAG_HAJUN_CHANT_TARGET, target.getId());
      entity.getPersistentData().putDouble(TAG_HAJUN_POWER_SCALE, 1);
      entity.getPersistentData().putBoolean(TAG_HAJUN_OVERDRAFT, false);
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.begin(entity, "oda_hajun", 3600,
         ticks -> entity.getPersistentData().putLong("OdaSharedNpUntil", entity.level().getGameTime() + ticks),
         () -> entity.getPersistentData().remove(TAG_HAJUN_CHANT_END));
      entity.getPersistentData().putLong(TAG_HAJUN_CHANT_END, net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.releaseAt(entity) + HAJUN_CHANT_TICKS);
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.deferEffect(entity, () -> { });
      landForNoblePhantasm(entity); entity.triggerChargeAnimation();
      ServantVoiceHelper.tryPlayOdaNobunagaHajun(entity);
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
      if (target != null && entity.isAlive() && entity.getCurrentMp() >= 95) {
         entity.setCurrentMp(entity.getCurrentMp() - 95);
         entity.getPersistentData().putLong("OdaSharedNpUntil", now + 3600);
         activateHajun(entity, level, target, now);
         relightAfterNoblePhantasm(entity, now);
      } else {
         restoreHajunChantTerrain(entity, level, Integer.MAX_VALUE);
      }
      return true;
   }

   private static void activateHajun(OdaNobunagaEntity entity, ServerLevel source, LivingEntity primary, long now) {
      ServerLevel hajunLevel = source.getServer().getLevel(ModDimensions.HAJUN_KEY);
      if (hajunLevel == null || ModDimensions.isHajunDimension(source.dimension().location())) {
         applyHajunState(entity, now, entity.blockPosition());
         entity.getPersistentData().putBoolean("OdaHajunLocal", true);
         for (LivingEntity target : collectHajunTargets(entity, source, primary)) markHajunTarget(entity, target, source, target.getX(), target.getY(), target.getZ(), target == primary);
         applyHajunInstantDamage(entity, source);
         return;
      }
      recordHajunLockedTarget(entity, primary);
      List<LivingEntity> pulled = collectHajunTargets(entity, source, primary);
      if (pulled.isEmpty()) {
         restoreHajunChantTerrain(entity, source, Integer.MAX_VALUE);
         clearHajunState(entity);
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

      Vec3 randomEntry = UBWInstanceManager.randomEntryPosition(entity.getRandom());
      Vec3 entry = new Vec3(randomEntry.x, 72.0, randomEntry.z);
      entry = new Vec3(entry.x, findSafeSpawnY(hajunLevel, Mth.floor(entry.x), Mth.floor(entry.z)), entry.z);
      BlockPos entryBlock = BlockPos.containing(entry);
      LivingEntity movedPrimary = moveHajunTargets(entity, source, hajunLevel, pulled, primary, entry);
      if (movedPrimary == null) {
         restoreHajunChantTerrain(entity, source, Integer.MAX_VALUE);
         clearHajunState(entity);
         return;
      }
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
      String fieldId = entity.getPersistentData().getString("OdaNpcFieldDimension");
      if (!fieldId.isBlank() && !level.dimension().location().toString().equals(fieldId)) {
         ServerLevel field = resolveDimensionOrFallback(level, fieldId, level);
         ServerLevel origin = resolveDimensionOrOverworld(level, entity.getPersistentData().getString(TAG_HAJUN_RETURN_DIM));
         returnPulledHajunTargets(entity.getUUID(), field, origin);
         restoreHajunTerrain(entity, field, Integer.MAX_VALUE);
         restoreHajunChantTerrain(entity, field, Integer.MAX_VALUE);
         clearHajunState(entity);
         return false;
      }
      boolean inHajunDimension = ModDimensions.isHajunDimension(level.dimension().location());
      long minUntil = entity.getPersistentData().getLong(TAG_HAJUN_MIN_UNTIL);
      if (!inHajunDimension && !entity.getPersistentData().getBoolean("OdaHajunLocal")) {
         restoreHajunChantTerrain(entity, level, Integer.MAX_VALUE);
         restoreHajunTerrain(entity, level, 120);
         clearHajunState(entity);
         return false;
      }
      if (activeUntil <= now) {
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
      for (LivingEntity living : source.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(HAJUN_PULL_RADIUS), living -> living != primary && isHajunPullTarget(owner, living, source))) {
         targets.add(living);
      }
      return targets;
   }

   private static boolean isHajunPullTarget(OdaNobunagaEntity owner, LivingEntity living, ServerLevel source) {
      if (living == null || !living.isAlive() || living == owner || living.level() != source) {
         return false;
      }
      Vec3 rel = living.position().subtract(owner.position());
      return rel.y >= -2.0 && rel.lengthSqr() <= HAJUN_PULL_RADIUS * HAJUN_PULL_RADIUS;
   }

   private static LivingEntity moveHajunTargets(OdaNobunagaEntity owner, ServerLevel source, ServerLevel hajunLevel, List<LivingEntity> targets, LivingEntity primary, Vec3 entry) {
      LivingEntity firstMoved = null;
      for (LivingEntity living : targets) {
         LivingEntity moved = moveOneHajunTarget(owner, source, hajunLevel, living, entry, living == primary);
         if (firstMoved == null && moved != null) {
            firstMoved = moved;
         }
      }
      return firstMoved;
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
      clearHajunTarget(living);
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
      OdaAbilityService.hajunPulse(entity, level, true);
   }

   private static void applyHajunDot(OdaNobunagaEntity entity, ServerLevel level) {
      OdaAbilityService.hajunPulse(entity, level, false);
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
      if (entity.getPersistentData().getBoolean("OdaHajunLocal")) {
         for (Entity candidate : level.getEntities().getAll()) if (candidate instanceof LivingEntity living && isPulledByHajun(entity.getUUID(), living)) clearHajunTarget(living);
         restoreHajunTerrain(entity, level, Integer.MAX_VALUE);
         restoreHajunChantTerrain(entity, level, Integer.MAX_VALUE);
         entity.getPersistentData().remove("OdaHajunLocal");
         clearHajunState(entity);
         return;
      }
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

   /** Breaks an NPC-owned Hajun field when an anti-world noble phantasm is fired. */
   public static boolean breakHajunForEa(OdaNobunagaEntity entity) {
      if (entity == null || !(entity.level() instanceof ServerLevel level)
         || !ModDimensions.isHajunDimension(level.dimension().location())) {
         return false;
      }
      if (entity.getPersistentData().getLong(TAG_HAJUN_ACTIVE_UNTIL) <= 0L) return false;
      returnFromHajun(entity, level);
      return true;
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
      if (entity.getCurrentMp() < 3) return;
      entity.setCurrentMp(entity.getCurrentMp() - 3);
      entity.getPersistentData().putLong(TAG_LAST_RIFLE_SHOT, now);
      entity.triggerNamedActionAnimation("bow_shot");
      Vec3 start = entity.getEyePosition();
      OdaAbilityService.shoot(entity, start, target.getBoundingBox().getCenter().subtract(start), 18, 3.3F, OdaDamageSource.Ability.WEAPON);
   }

   private static boolean hasTrait(LivingEntity entity, ServantTraitTag trait) {
      return ServantIdentityHelper.hasTrait(entity, trait);
   }

   private static void expireBuffs(OdaNobunagaEntity entity, ServerLevel level, long now) {

      if (entity.getPersistentData().getLong(TAG_MAOU_UNTIL) <= now) {
         entity.getPersistentData().remove(TAG_MAOU_UNTIL);
      }
      if (entity.getPersistentData().getLong(TAG_FLIGHT_UNTIL) <= now) {
         double desiredY = ServantFlightHelper.desiredHoverY(entity, null);
         if (entity.isNoGravity() && entity.getY() > desiredY + 0.45) {
            entity.getPersistentData().putLong(TAG_FLIGHT_UNTIL, now + FLIGHT_RAMPUP_TICKS);
         } else {
            entity.getPersistentData().remove(TAG_FLIGHT_UNTIL);
            if (entity.isNoGravity()) {
               entity.setNoGravity(false);
            }
            clearFootSupportGun(entity, level);
         }
      }
      if (entity.getPersistentData().getLong(TAG_LAND_FOR_NP_UNTIL) <= now) {
         entity.getPersistentData().remove(TAG_LAND_FOR_NP_UNTIL);
      }
   }

   public static boolean isCombatFlying(OdaNobunagaEntity entity, long now) {
      if (entity == null || entity.isSpiritualDissolving()
         || entity.getPersistentData().getLong(TAG_LAND_FOR_NP_UNTIL) > now) {
         return false;
      }
      return entity.getHealth() <= entity.getMaxHealth() * 0.6F
         || entity.getPersistentData().getLong(TAG_MAOU_UNTIL) > now
         || entity.getPersistentData().getLong(TAG_THREE_THOUSAND_CHANT_END) > now
         || entity.getPersistentData().getLong(TAG_HAJUN_CHANT_END) > now
         || entity.getPersistentData().getLong(TAG_FLIGHT_UNTIL) > now;
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

      if (entity.getCurrentMp() < .28) { entity.setNoGravity(false); clearFootSupportGun(entity, (ServerLevel)entity.level()); return; }
      entity.setCurrentMp(entity.getCurrentMp() - .28);
      entity.setNoGravity(true);
      ServantFlightCombatService.markControlled(entity, now);
      entity.fallDistance = 0.0F;
      ensureFootSupportGun(entity, now);
      double hoverY = ServantFlightHelper.desiredHoverY(entity, target);
      if (target != null && target.isAlive()) {
         Vec3 toTarget = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
         Vec3 move = toTarget.lengthSqr() > 1.0E-4 ? toTarget.normalize() : entity.getLookAngle().multiply(1.0, 0.0, 1.0);
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
      } else {
         double yMotion = ServantFlightHelper.verticalVelocityToward(entity.getY(), hoverY, 0.12, 0.025, 0.16, 0.24);
         entity.setDeltaMovement(
            entity.getDeltaMovement().x * 0.82,
            Mth.clamp(entity.getDeltaMovement().y * 0.65 + yMotion, -0.24, 0.18),
            entity.getDeltaMovement().z * 0.82
         );
         entity.hasImpulse = true;
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
      data.remove("OdaNpcFieldDimension");
      data.remove("OdaHajunLocal");
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
      data.putString("OdaNpcFieldDimension", entity.level().dimension().location().toString());
      data.putLong(TAG_HAJUN_ACTIVE_UNTIL, now + HAJUN_DURATION);
      data.putLong(TAG_HAJUN_MIN_UNTIL, now + 10L * 20L);
      data.putLong(TAG_HAJUN_START_TICK, now);
      data.putLong(TAG_HAJUN_NEXT_TERRAIN, now + 1L);
      data.putInt(TAG_HAJUN_CENTER_X, center.getX());
      data.putInt(TAG_HAJUN_CENTER_Y, center.getY());
      data.putInt(TAG_HAJUN_CENTER_Z, center.getZ());
      data.putBoolean(TAG_HAJUN_STARTED, false);
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

   public static void restoreLegacyDuelState(LivingEntity living) {
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
