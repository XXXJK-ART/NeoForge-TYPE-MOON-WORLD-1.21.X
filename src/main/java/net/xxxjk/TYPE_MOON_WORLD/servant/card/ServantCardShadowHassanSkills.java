package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanDamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanPursuitData;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanRules;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class ServantCardShadowHassanSkills {
   public static final String SERVANT_ID = "shadow_hassan";
   public static final int COMPLETE_CONCEALMENT_AMPLIFIER = 1;
   public static final long ACTIVE_CONCEALMENT_DURATION_TICKS = 100L;
   public static final int ACTIVE_CONCEALMENT_MANA_COST = 20;
   public static final int ACTIVE_CONCEALMENT_COOLDOWN_TICKS = 200;
   private static final String TAG_CONCEALED = "ShadowHassanCardConcealed";
   private static final String TAG_PASSIVE_CONCEALED = "ShadowHassanCardPassiveConcealed";
   private static final String TAG_ACTIVE_CONCEALMENT_UNTIL = "ShadowHassanActiveConcealmentUntil";
   private static final String TAG_BODY_INVISIBILITY_MANAGED = "ShadowHassanCardBodyInvisibilityManaged";
   private static final String TAG_NP_CONSUMED = "ShadowHassanCardNpConsumed";
   private static final String TAG_DARK_WARNING = "ShadowHassanCardDarkWarningTick";
   private static final String TAG_EXPOSED_UNTIL = "ShadowHassanCardExposedUntil";
   private static final String TAG_SLASH_UNTIL = "ShadowHassanCardSlashUntil";
   private static final String TAG_POSSESSION_HOST = "ShadowHassanPossessionHost";
   private static final String TAG_POSSESSION_DIMENSION = "ShadowHassanPossessionDimension";
   private static final String TAG_POSSESSION_NEXT_MANA = "ShadowHassanPossessionNextMana";
   private static final String TAG_POSSESSION_PREVIOUS_NO_PHYSICS = "ShadowHassanPossessionPreviousNoPhysics";
   private static final String TAG_POSSESSION_PREVIOUS_NO_GRAVITY = "ShadowHassanPossessionPreviousNoGravity";
   private static final String TAG_POSSESSION_EXIT_REASON = "ShadowHassanPossessionExitReason";
   private static final String TAG_POSSESSION_CAMERA_ACTIVE = "ShadowHassanPossessionCameraActive";
   private static final String TAG_SHADOW_STEP_READY = "ShadowHassanCardShadowStepReady";
   private static final ResourceLocation DARK_MOVEMENT_MODIFIER = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "shadow_hassan_total_darkness_movement");
   private static final int SLASH_STRIKE_COUNT = 200;
   private static final int SLASH_STRIKES_PER_TICK = 2;
   private static final long SLASH_DURATION_TICKS = 100L;

   private ServantCardShadowHassanSkills() {
   }

   public static void initialize(ServerPlayer player) {
      clear(player);
      player.getPersistentData().putBoolean(TAG_BODY_INVISIBILITY_MANAGED, true);
      setConcealed(player, false);
      setNoblePhantasmConsumed(player, false);
   }

   public static void clear(ServerPlayer player) {
      if (isPossessing(player)) endPossession(player, "card_removed", false);
      boolean managedInvisibility = player.getPersistentData().getBoolean(TAG_BODY_INVISIBILITY_MANAGED) || isConcealed(player);
      player.getPersistentData().remove(TAG_CONCEALED);
      player.getPersistentData().remove(TAG_PASSIVE_CONCEALED);
      player.getPersistentData().remove(TAG_ACTIVE_CONCEALMENT_UNTIL);
      player.getPersistentData().remove(TAG_BODY_INVISIBILITY_MANAGED);
      if (managedInvisibility) player.removeEffect(MobEffects.INVISIBILITY);
      player.getPersistentData().remove(TAG_DARK_WARNING);
      player.getPersistentData().remove(TAG_EXPOSED_UNTIL);
      player.getPersistentData().remove(TAG_SLASH_UNTIL);
      player.getPersistentData().remove(TAG_SHADOW_STEP_READY);
      setDarkMovement(player, false);
      if (managedInvisibility) player.setInvisible(false);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!isShadowHassan(player, vars)) return;
      player.getPersistentData().putBoolean(TAG_BODY_INVISIBILITY_MANAGED, true);
      boolean totalDarkness = ShadowHassanRules.isTotalDarkness(player.level(), player.blockPosition());
      setDarkMovement(player, totalDarkness);
      if (isPossessing(player)) {
         if (!tickPossession(player, vars)) return;
      } else if (player.tickCount % ShadowHassanRules.MANA_RESTORE_INTERVAL_TICKS == 0
         && isInShadowZone(player, null)) {
         restoreMana(player, vars, ShadowHassanRules.MANA_RESTORE_PER_SECOND);
      }
      refreshConcealment(player);
      if (!isConcealed(player) && player.tickCount % 4 == 0 && player.level() instanceof ServerLevel level) {
         spawnVisibleShadowParticles(level, player);
      }
   }

   public static boolean toggleConcealment(ServerPlayer player) {
      if (!isShadowHassan(player) || isPossessing(player) || !isActiveConcealment(player)) return false;
      player.getPersistentData().remove(TAG_ACTIVE_CONCEALMENT_UNTIL);
      refreshConcealment(player);
      return true;
   }

   public static boolean performActiveConcealment(ServerPlayer player) {
      if (!isShadowHassan(player) || isPossessing(player)
         || !ShadowHassanRules.isDirectSunlight(player.level(), player.blockPosition())) return false;
      player.getPersistentData().putLong(TAG_ACTIVE_CONCEALMENT_UNTIL,
         player.level().getGameTime() + ACTIVE_CONCEALMENT_DURATION_TICKS);
      refreshConcealment(player);
      return true;
   }

   public static void revealForAttack(ServerPlayer player) {
      if (!isShadowHassan(player)) return;
      player.getPersistentData().putLong(TAG_EXPOSED_UNTIL,
         player.level().getGameTime() + ShadowHassanRules.CONCEALMENT_EXPOSURE_TICKS);
      player.getPersistentData().remove(TAG_ACTIVE_CONCEALMENT_UNTIL);
      setConcealedState(player, false);
      maintainBodyInvisibility(player, false);
   }

   public static boolean canAttack(ServerPlayer player) {
      if (!isShadowHassan(player)) return true;
      if (isPossessing(player) || !ShadowHassanRules.isTotalDarkness(player.level(), player.blockPosition())) return !isPossessing(player);
      long now = player.level().getGameTime();
      if (now >= player.getPersistentData().getLong(TAG_DARK_WARNING)) {
         player.getPersistentData().putLong(TAG_DARK_WARNING, now + 20L);
         player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_hassan_card.total_darkness"), true);
      }
      return false;
   }

   public static boolean performShadowLantern(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      return isShadowHassan(player) && !isPossessing(player);
   }

   public static boolean performShadowPossession(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel)) return false;
      if (isPossessing(player)) return endPossession(player, "player_released", true);
      if (!canAttack(player)) return false;
      LivingEntity target = validPossessionTarget(player);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      return beginPossession(player, target);
   }

   public static boolean performShadowWandering(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      if (isPossessing(player)) return endPossession(player, "player_released", true);
      if (!canAttack(player)) return false;
      LivingEntity target = validPossessionTarget(player);
      if (target != null) return beginPossession(player, target);
      if (!canUseShadow(player, null)) return false;
      long now = level.getGameTime();
      if (now < player.getPersistentData().getLong(TAG_SHADOW_STEP_READY)) return false;
      Vec3 desired = player.position().add(horizontal(player.getLookAngle()).scale(8.0));
      BlockPos destination = findNearestShadow(level, player, desired, ShadowHassanRules.SHADOW_POINT_SEARCH_RADIUS, null,
         player.position(), ShadowHassanRules.SHADOW_STEP_MAX_DISTANCE);
      if (!teleport(player, destination)) return false;
      player.getPersistentData().putLong(TAG_SHADOW_STEP_READY,
         now + ShadowHassanRules.SHADOW_STEP_COOLDOWN_TICKS);
      return true;
   }

   public static boolean performShadowAmbush(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      LivingEntity target = validBladeTarget(player, 18.0);
      if (target == null || !canUseShadow(player, target)) return false;
      Vec3 behind = target.position().subtract(horizontal(target.getLookAngle()).scale(1.8));
      BlockPos destination = findNearestShadow(level, player, behind, ShadowHassanRules.SHADOW_POINT_SEARCH_RADIUS,
         target, player.position(), ShadowHassanRules.SHADOW_STEP_MAX_DISTANCE);
      if (!teleport(player, destination)) return false;
      revealForAttack(player);
      directStrike(player, target, 34.0F);
      target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
      spawnImpact(level, target, 18);
      return true;
   }

   public static boolean performShadowBind(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      LivingEntity target = validBladeTarget(player, 16.0);
      if (target == null || !canAttack(player)) return false;
      revealForAttack(player);
      directStrike(player, target, 12.0F);
      target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false, true));
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, true, true));
      spawnImpact(level, target, 24);
      return true;
   }

   public static boolean performShadowFlurry(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      LivingEntity target = validBladeTarget(player, 7.0);
      if (target == null || !canAttack(player)) return false;
      revealForAttack(player);
      for (int delay : new int[]{0, 4, 8}) {
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (!player.isAlive() || !target.isAlive() || player.distanceToSqr(target) > 9.0 * 9.0) return;
            directStrike(player, target, 15.0F);
            if (player.level() instanceof ServerLevel currentLevel) spawnImpact(currentLevel, target, 10);
         });
      }
      return true;
   }

   /** Two hundred full basic strikes over five seconds. */
   public static boolean performSlash(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel) || !canAttack(player)) return false;
      long now = player.level().getGameTime();
      if (player.getPersistentData().getLong(TAG_SLASH_UNTIL) > now) return false;
      LivingEntity target = validBladeTarget(player, 8.0);
      if (target == null) return false;
      revealForAttack(player);
      player.getPersistentData().putLong(TAG_SLASH_UNTIL, now + SLASH_DURATION_TICKS);
      float damage = Math.max(1.0F, (float)player.getAttributeValue(
         net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
      for (int strike = 0; strike < SLASH_STRIKE_COUNT; strike++) {
         int delay = 1 + strike / SLASH_STRIKES_PER_TICK;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (!player.isAlive() || !target.isAlive() || player.level() != target.level()
               || player.distanceToSqr(target) > 8.0 * 8.0
               || target.getType().is(ShadowHassanDamageTypes.BLADE_IMMUNE)) return;
            revealForAttack(player);
            directStrike(player, target, damage);
            if (player.level() instanceof ServerLevel currentLevel) spawnImpact(currentLevel, target, 12);
         });
      }
      return true;
   }

   public static boolean performShadowRetreat(ServerPlayer player) {
      if (!canUseShadow(player) || !(player.level() instanceof ServerLevel level)) return false;
      LivingEntity threat = ServantCardSkillUtils.findLookTarget(player, 20.0, 3.0);
      if (threat == null) threat = player.getLastHurtByMob();
      Vec3 away = threat == null
         ? player.position().subtract(horizontal(player.getLookAngle()).scale(12.0))
         : player.position().add(horizontal(player.position().subtract(threat.position())).scale(12.0));
      BlockPos destination = findRetreatShadow(level, player, away, threat);
      return destination != null && ShadowHassanRules.isWithinShadowStepRange(player.position(), Vec3.atBottomCenterOf(destination))
         && teleport(player, destination);
   }

   public static boolean performMeditativeSensitivity(ServerPlayer player) {
      if (!canAttack(player) || isNoblePhantasmConsumed(player) || !(player.level() instanceof ServerLevel level)) {
         if (isNoblePhantasmConsumed(player)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_hassan_card.np_consumed"), true);
         }
         return false;
      }
      LivingEntity target = findMeditativeSensitivityTarget(player);
      if (target == null) return false;
      revealForAttack(player);
      setNoblePhantasmConsumed(player, true);
      ShadowHassanPursuitData.get(level.getServer()).addPursuit(level, player.position(), target);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper.tryProtectWithAvalon(player)) {
         vars.syncPlayerVariables(player);
         return true;
      }
      player.setHealth(0.0F);
      player.die(player.damageSources().genericKill());
      return true;
   }

   public static void onPlayerDeath(ServerPlayer player, @Nullable LivingEntity killer) {
      if (!isShadowHassan(player) || isNoblePhantasmConsumed(player)) return;
      setNoblePhantasmConsumed(player, true);
      if (killer == null || !killer.isAlive() || ShadowHassanPursuitData.isPaleRider(killer)
         || !(player.level() instanceof ServerLevel level)) return;
      revealForAttack(player);
      ShadowHassanPursuitData.get(level.getServer()).addPursuit(level, player.position(), killer);
      level.playSound(null, player.blockPosition(), ModSounds.SHADOW_HASSAN_VOICE_MEDITATIVE_SENSITIVITY.get(), SoundSource.VOICE, 1.2F, 1.0F);
   }

   public static boolean isConcealed(ServerPlayer player) {
      return (wantsConcealment(player) || isActiveConcealment(player))
         && player.level().getGameTime() >= player.getPersistentData().getLong(TAG_EXPOSED_UNTIL);
   }

   public static boolean isActiveConcealment(ServerPlayer player) {
      return player != null && player.getPersistentData().getLong(TAG_ACTIVE_CONCEALMENT_UNTIL)
         > player.level().getGameTime();
   }

   public static boolean isPassiveConcealed(ServerPlayer player) {
      return player != null && player.getPersistentData().getBoolean(TAG_PASSIVE_CONCEALED)
         && player.level().getGameTime() >= player.getPersistentData().getLong(TAG_EXPOSED_UNTIL);
   }

   public static boolean isPossessing(ServerPlayer player) {
      return player != null && player.getPersistentData().hasUUID(TAG_POSSESSION_HOST);
   }

   public static boolean isPossessingHost(ServerPlayer player, java.util.UUID hostUuid) {
      return isPossessing(player) && hostUuid.equals(player.getPersistentData().getUUID(TAG_POSSESSION_HOST));
   }

   public static boolean hasPossessionTarget(ServerPlayer player) {
      return validPossessionTarget(player) != null;
   }

   public static boolean blocksPlayerInput(ServerPlayer player) {
      return isPossessing(player);
   }

   public static void onDamageApplied(ServerPlayer player) {
      if (isPossessing(player)) endPossession(player, "external_damage", true);
   }

   public static void forceEndPossession(ServerPlayer player, String reason, boolean expose) {
      endPossession(player, reason, expose);
   }

   private static boolean wantsConcealment(ServerPlayer player) {
      return player.getPersistentData().getBoolean(TAG_CONCEALED);
   }

   public static boolean isShadowHassan(ServerPlayer player) {
      return isShadowHassan(player, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
   }

   public static boolean hasMeditativeSensitivityTarget(ServerPlayer player) {
      return findMeditativeSensitivityTarget(player) != null;
   }

   private static boolean isShadowHassan(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      return player != null && vars.servant_card_transformed && SERVANT_ID.equals(vars.servant_card_id);
   }

   @Nullable
   private static LivingEntity findMeditativeSensitivityTarget(ServerPlayer player) {
      if (!(EntityUtils.getRayTraceTarget(player, 32.0) instanceof EntityHitResult hit)
         || !(hit.getEntity() instanceof LivingEntity target)
         || !target.isAlive()
         || EntityUtils.isImmunePlayerTarget(target)
         || ShadowHassanPursuitData.isPaleRider(target)) return null;
      return target;
   }

   @Nullable
   private static LivingEntity validPossessionTarget(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, ShadowHassanRules.POSSESSION_RANGE, 2.0);
      if (target == null || target == player || !target.isAlive() || target.isRemoved()
         || target.level() != player.level() || !player.hasLineOfSight(target)
         || EntityUtils.isImmunePlayerTarget(target)) return null;
      return target;
   }

   private static boolean beginPossession(ServerPlayer player, LivingEntity host) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      boolean inRange = player.distanceToSqr(host)
         <= (double)ShadowHassanRules.POSSESSION_RANGE * ShadowHassanRules.POSSESSION_RANGE;
      boolean legalTarget = host.isAlive() && !host.isRemoved() && host != player && host.level() == level
         && !EntityUtils.isImmunePlayerTarget(host);
      if (!ShadowHassanRules.canBeginPossession(ShadowHassanRules.isTotalDarkness(level, player.blockPosition()),
         ShadowHassanRules.hasUsableShadowAnchor(level, host), inRange, player.hasLineOfSight(host), legalTarget)
         || !canUseShadow(player, host)) return false;
      BlockPos anchor = ShadowHassanRules.findNearestShadowPoint(level, player, host.position(), 2, host, null,
         Double.MAX_VALUE);
      if (anchor == null) return false;
      var data = player.getPersistentData();
      data.putUUID(TAG_POSSESSION_HOST, host.getUUID());
      data.putString(TAG_POSSESSION_DIMENSION, level.dimension().location().toString());
      data.putLong(TAG_POSSESSION_NEXT_MANA, level.getGameTime() + ShadowHassanRules.POSSESSION_MANA_INTERVAL_TICKS);
      data.putBoolean(TAG_POSSESSION_PREVIOUS_NO_PHYSICS, player.noPhysics);
      data.putBoolean(TAG_POSSESSION_PREVIOUS_NO_GRAVITY, player.isNoGravity());
      data.putBoolean(TAG_POSSESSION_CAMERA_ACTIVE, false);
      data.putString(TAG_POSSESSION_EXIT_REASON, "");
      player.noPhysics = true;
      player.setNoGravity(true);
      moveToAnchor(player, anchor);
      if (!setPossessionCamera(player, host)) {
         endPossession(player, "camera_unavailable", false);
         return false;
      }
      data.putBoolean(TAG_POSSESSION_CAMERA_ACTIVE, true);
      return true;
   }

   private static boolean tickPossession(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!(player.level() instanceof ServerLevel level)) {
         endPossession(player, "dimension_unavailable", true);
         return false;
      }
      var data = player.getPersistentData();
      if (!level.dimension().location().toString().equals(data.getString(TAG_POSSESSION_DIMENSION))) {
         endPossession(player, "dimension_changed", true);
         return false;
      }
      Entity loaded = level.getEntity(data.getUUID(TAG_POSSESSION_HOST));
      if (!(loaded instanceof LivingEntity host) || !host.isAlive() || host.isRemoved()) {
         endPossession(player, "host_unavailable", true);
         return false;
      }
      if (vars.servantCardState().mana() <= 0.0) {
         endPossession(player, "mana_depleted", true);
         return false;
      }
      if (!ShadowHassanRules.hasUsableShadowAnchor(level, host)) {
         endPossession(player, "shadow_lost", true);
         return false;
      }
      BlockPos anchor = ShadowHassanRules.findNearestShadowPoint(level, player, host.position(), 2, host, null,
         Double.MAX_VALUE);
      if (anchor == null) {
         endPossession(player, "shadow_lost", true);
         return false;
      }
      moveToAnchor(player, anchor);
      if (!data.getBoolean(TAG_POSSESSION_CAMERA_ACTIVE)) {
         if (!setPossessionCamera(player, host)) {
            endPossession(player, "camera_unavailable", false);
            return false;
         }
         data.putBoolean(TAG_POSSESSION_CAMERA_ACTIVE, true);
      }
      player.setDeltaMovement(Vec3.ZERO);
      player.hurtMarked = true;
      long nextMana = data.getLong(TAG_POSSESSION_NEXT_MANA);
      if (level.getGameTime() >= nextMana) {
         vars.servantCardState().setMana(ShadowHassanRules.possessionManaAfterInterval(
            vars.servantCardState().mana(), vars.servantCardState().maxMana(), isInShadowZone(player, host)));
         vars.syncMana(player);
         data.putLong(TAG_POSSESSION_NEXT_MANA, level.getGameTime() + ShadowHassanRules.POSSESSION_MANA_INTERVAL_TICKS);
         if (vars.servantCardState().mana() <= 0.0) {
            endPossession(player, "mana_depleted", true);
            return false;
         }
      }
      return true;
   }

   private static void moveToAnchor(ServerPlayer player, BlockPos anchor) {
      player.setPos(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);
      player.fallDistance = 0.0F;
   }

   private static boolean endPossession(ServerPlayer player, String reason, boolean expose) {
      var data = player.getPersistentData();
      if (!data.hasUUID(TAG_POSSESSION_HOST)) return false;
      data.putString(TAG_POSSESSION_EXIT_REASON, reason);
      data.remove(TAG_POSSESSION_HOST);
      data.remove(TAG_POSSESSION_DIMENSION);
      data.remove(TAG_POSSESSION_NEXT_MANA);
      data.remove(TAG_POSSESSION_CAMERA_ACTIVE);
      player.noPhysics = data.getBoolean(TAG_POSSESSION_PREVIOUS_NO_PHYSICS);
      data.remove(TAG_POSSESSION_PREVIOUS_NO_PHYSICS);
      player.setNoGravity(data.getBoolean(TAG_POSSESSION_PREVIOUS_NO_GRAVITY));
      data.remove(TAG_POSSESSION_PREVIOUS_NO_GRAVITY);
      setPossessionCamera(player, player);
      player.setDeltaMovement(Vec3.ZERO);
      if (expose) revealForAttack(player);
      return true;
   }

   private static boolean setPossessionCamera(ServerPlayer player, Entity camera) {
      if (player == null || camera == null || camera.isRemoved() || camera.level() != player.level()) return false;
      try {
         player.setCamera(camera);
         return true;
      } catch (RuntimeException ignored) {
         return false;
      }
   }

   private static boolean isInShadowZone(LivingEntity body, @Nullable LivingEntity caster) {
      return ShadowHassanRules.isUsableShadowZone(body.level(), body.blockPosition(), caster);
   }

   @Nullable
   private static LivingEntity nearbyShadowCaster(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return null;
      AABB search = player.getBoundingBox().inflate(4.0);
      return level.getEntitiesOfClass(LivingEntity.class, search,
         candidate -> candidate != player && ShadowHassanRules.castsSunlightShadow(level, candidate)
            && player.blockPosition().distSqr(candidate.blockPosition()) <= 16.0).stream().findFirst().orElse(null);
   }

   private static void setDarkMovement(ServerPlayer player, boolean dark) {
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed == null) return;
      if (dark) {
         if (!speed.hasModifier(DARK_MOVEMENT_MODIFIER)) {
            speed.addTransientModifier(new AttributeModifier(DARK_MOVEMENT_MODIFIER,
               ShadowHassanRules.TOTAL_DARKNESS_MOVEMENT_MULTIPLIER - 1.0,
               AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
         }
      } else {
         speed.removeModifier(DARK_MOVEMENT_MODIFIER);
      }
   }

   private static boolean canUseShadow(ServerPlayer player) {
      return canUseShadow(player, null);
   }

   private static boolean canUseShadow(ServerPlayer player, @Nullable LivingEntity target) {
      if (!canAttack(player)) return false;
      if (isInShadowZone(player, target)) return true;
      player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_hassan_card.no_shadow"), true);
      return false;
   }

   private static void setConcealed(ServerPlayer player, boolean concealed) {
      setConcealedState(player, concealed);
      player.getPersistentData().remove(TAG_EXPOSED_UNTIL);
      maintainBodyInvisibility(player, concealed);
   }

   private static void refreshConcealment(ServerPlayer player) {
      var data = player.getPersistentData();
      long now = player.level().getGameTime();
      long activeUntil = data.getLong(TAG_ACTIVE_CONCEALMENT_UNTIL);
      if (activeUntil > 0L && now >= activeUntil) data.remove(TAG_ACTIVE_CONCEALMENT_UNTIL);
      boolean exposed = now < data.getLong(TAG_EXPOSED_UNTIL);
      boolean passive = !exposed && isInShadowZone(player, nearbyShadowCaster(player));
      boolean active = !exposed && isActiveConcealment(player);
      data.putBoolean(TAG_PASSIVE_CONCEALED, passive);
      setConcealedState(player, passive || active);
      maintainBodyInvisibility(player, passive || active);
   }

   private static void setConcealedState(ServerPlayer player, boolean concealed) {
      player.getPersistentData().putBoolean(TAG_CONCEALED, concealed);
   }

   private static void maintainBodyInvisibility(ServerPlayer player, boolean completeConcealment) {
      if (completeConcealment) {
         ServantCardConcealmentHelper.clearEnemyAggroForMaskOnly(player);
      } else {
         MobEffectInstance invisibility = player.getEffect(MobEffects.INVISIBILITY);
         if (invisibility != null && invisibility.getAmplifier() >= COMPLETE_CONCEALMENT_AMPLIFIER) {
            player.removeEffect(MobEffects.INVISIBILITY);
         }
      }
      player.setInvisible(false);
   }

   private static boolean isNoblePhantasmConsumed(ServerPlayer player) {
      return persisted(player).getBoolean(TAG_NP_CONSUMED);
   }

   private static void setNoblePhantasmConsumed(ServerPlayer player, boolean consumed) {
      CompoundTag persisted = persisted(player);
      persisted.putBoolean(TAG_NP_CONSUMED, consumed);
      player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
   }

   private static CompoundTag persisted(ServerPlayer player) {
      return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
   }

   private static void restoreMana(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double amount) {
      double before = vars.servantCardState().mana();
      vars.servantCardState().addMana(amount);
      if (vars.servantCardState().mana() != before) vars.syncMana(player);
   }

   @Nullable
   private static LivingEntity validBladeTarget(ServerPlayer player, double range) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, range, 2.2);
      return target == null || target.getType().is(ShadowHassanDamageTypes.BLADE_IMMUNE) ? null : target;
   }

   private static void directStrike(ServerPlayer player, LivingEntity target, float damage) {
      target.invulnerableTime = 0;
      target.hurt(ShadowHassanDamageSource.forTarget(player.damageSources().playerAttack(player), target), damage);
      target.invulnerableTime = 0;
      target.hurtTime = 0;
      target.hurtDuration = 0;
   }

   private static Vec3 horizontal(Vec3 direction) {
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      return horizontal.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   @Nullable
   private static BlockPos findNearestShadow(ServerLevel level, ServerPlayer player, Vec3 desired, int radius) {
      return findNearestShadow(level, player, desired, radius, null, null, Double.MAX_VALUE);
   }

   @Nullable
   private static BlockPos findNearestShadow(ServerLevel level, ServerPlayer player, Vec3 desired, int radius,
      @Nullable LivingEntity shadowCaster, @Nullable Vec3 origin, double maxTravel) {
      return ShadowHassanRules.findNearestShadowPoint(level, player, desired, radius, shadowCaster, origin, maxTravel);
   }

   @Nullable
   private static BlockPos findRetreatShadow(ServerLevel level, ServerPlayer player, Vec3 desired, @Nullable LivingEntity threat) {
      return ShadowHassanRules.findNearestShadowPoint(level, player, desired,
         ShadowHassanRules.SHADOW_POINT_SEARCH_RADIUS, threat, player.position(), ShadowHassanRules.SHADOW_STEP_MAX_DISTANCE);
   }

   private static boolean isValidShadowDestination(ServerLevel level, ServerPlayer player, BlockPos pos) {
      return isValidShadowDestination(level, player, pos, null);
   }

   private static boolean isValidShadowDestination(ServerLevel level, ServerPlayer player, BlockPos pos,
      @Nullable LivingEntity sunlightShadowCaster) {
      return ShadowHassanRules.isValidShadowPoint(level, pos, player, sunlightShadowCaster);
   }

   private static boolean teleport(ServerPlayer player, @Nullable BlockPos destination) {
      if (destination == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_hassan_card.no_shadow"), true);
         return false;
      }
      player.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
      player.fallDistance = 0.0F;
      return true;
   }

   private static void spawnVisibleShadowParticles(ServerLevel level, ServerPlayer player) {
      level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 0.9, player.getZ(), 2, 0.25, 0.75, 0.25, 0.01);
      level.sendParticles(new DustParticleOptions(new Vector3f(0.015F, 0.015F, 0.02F), 1.2F),
         player.getX(), player.getY() + 0.9, player.getZ(), 3, 0.28, 0.78, 0.28, 0.004);
   }

   private static void spawnImpact(ServerLevel level, LivingEntity target, int count) {
      level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
         count, 0.3, 0.4, 0.3, 0.025);
      level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
         Math.max(3, count / 3), 0.2, 0.25, 0.2, 0.06);
   }
}
