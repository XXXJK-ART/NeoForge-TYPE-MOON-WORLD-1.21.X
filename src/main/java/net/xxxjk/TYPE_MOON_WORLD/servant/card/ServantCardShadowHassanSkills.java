package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanPursuitData;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanRules;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class ServantCardShadowHassanSkills {
   public static final String SERVANT_ID = "shadow_hassan";
   public static final int COMPLETE_CONCEALMENT_AMPLIFIER = 1;
   private static final String TAG_CONCEALED = "ShadowHassanCardConcealed";
   private static final String TAG_BODY_INVISIBILITY_MANAGED = "ShadowHassanCardBodyInvisibilityManaged";
   private static final String TAG_NP_CONSUMED = "ShadowHassanCardNpConsumed";
   private static final String TAG_DARK_WARNING = "ShadowHassanCardDarkWarningTick";
   private static final String TAG_EXPOSED_UNTIL = "ShadowHassanCardExposedUntil";
   private static final int PASSIVE_MANA_INTERVAL = 20;
   private static final double PASSIVE_MANA_RESTORE = 5.0;

   private ServantCardShadowHassanSkills() {
   }

   public static void initialize(ServerPlayer player) {
      clear(player);
      player.getPersistentData().putBoolean(TAG_BODY_INVISIBILITY_MANAGED, true);
      maintainBodyInvisibility(player, false);
      setNoblePhantasmConsumed(player, false);
   }

   public static void clear(ServerPlayer player) {
      boolean managedInvisibility = player.getPersistentData().getBoolean(TAG_BODY_INVISIBILITY_MANAGED) || isConcealed(player);
      player.getPersistentData().remove(TAG_CONCEALED);
      player.getPersistentData().remove(TAG_BODY_INVISIBILITY_MANAGED);
      if (managedInvisibility) player.removeEffect(MobEffects.INVISIBILITY);
      player.getPersistentData().remove(TAG_DARK_WARNING);
      player.getPersistentData().remove(TAG_EXPOSED_UNTIL);
      if (managedInvisibility) player.setInvisible(false);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!isShadowHassan(player, vars)) return;
      player.getPersistentData().putBoolean(TAG_BODY_INVISIBILITY_MANAGED, true);
      maintainBodyInvisibility(player, isConcealed(player));

      int light = player.level().getMaxLocalRawBrightness(player.blockPosition());
      if (ShadowHassanRules.isTotalDarkness(light)) {
         player.setDeltaMovement(Vec3.ZERO);
         player.hurtMarked = true;
         player.fallDistance = 0.0F;
         return;
      }
      if (player.tickCount % PASSIVE_MANA_INTERVAL == 0 && ShadowHassanRules.isShadowLight(light)) {
         restoreMana(player, vars, PASSIVE_MANA_RESTORE);
      }
      if (!isConcealed(player) && player.tickCount % 4 == 0 && player.level() instanceof ServerLevel level) {
         spawnVisibleShadowParticles(level, player);
      }
   }

   public static boolean toggleConcealment(ServerPlayer player) {
      if (!isShadowHassan(player)) return false;
      boolean concealed = !wantsConcealment(player);
      setConcealed(player, concealed);
      player.displayClientMessage(Component.translatable(concealed
         ? "message.typemoonworld.shadow_hassan_card.concealment_on"
         : "message.typemoonworld.shadow_hassan_card.concealment_off"), true);
      return true;
   }

   public static void revealForAttack(ServerPlayer player) {
      if (!isShadowHassan(player) || !wantsConcealment(player)) return;
      player.getPersistentData().putLong(TAG_EXPOSED_UNTIL,
         player.level().getGameTime() + ShadowHassanRules.CONCEALMENT_EXPOSURE_TICKS);
      maintainBodyInvisibility(player, false);
   }

   public static boolean canAttack(ServerPlayer player) {
      if (!isShadowHassan(player)) return true;
      if (!ShadowHassanRules.isTotalDarkness(player.level().getMaxLocalRawBrightness(player.blockPosition()))) return true;
      long now = player.level().getGameTime();
      if (now >= player.getPersistentData().getLong(TAG_DARK_WARNING)) {
         player.getPersistentData().putLong(TAG_DARK_WARNING, now + 20L);
         player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_hassan_card.total_darkness"), true);
      }
      return false;
   }

   public static boolean performShadowLantern(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!canUseShadow(player)) return false;
      restoreMana(player, vars, 25.0);
      return true;
   }

   public static boolean performShadowWandering(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 18.0, 2.0);
      if (!canUseShadow(player, target)) return false;
      Vec3 desired;
      if (target != null) {
         Vec3 forward = horizontal(target.getLookAngle());
         desired = target.position().subtract(forward.scale(2.0));
      } else {
         desired = player.position().add(horizontal(player.getLookAngle()).scale(8.0));
      }
      BlockPos destination = findNearestShadow(level, player, desired, 5, target);
      return teleport(player, destination);
   }

   public static boolean performShadowAmbush(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      LivingEntity target = validBladeTarget(player, 18.0);
      if (target == null || !canUseShadow(player, target)) return false;
      Vec3 behind = target.position().subtract(horizontal(target.getLookAngle()).scale(1.8));
      BlockPos destination = findNearestShadow(level, player, behind, 5, target);
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
      if (target == null || !canUseShadow(player, target)) return false;
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
      if (target == null || !canUseShadow(player, target)) return false;
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

   /** Ten full basic strikes, one every five ticks. */
   public static boolean performSlash(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel) || !canAttack(player)) return false;
      LivingEntity target = validBladeTarget(player, 8.0);
      if (target == null) return false;
      revealForAttack(player);
      float damage = Math.max(1.0F, (float)player.getAttributeValue(
         net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
      for (int strike = 0; strike < 10; strike++) {
         int delay = strike * 5;
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
      return teleport(player, destination);
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
      return wantsConcealment(player)
         && player.level().getGameTime() >= player.getPersistentData().getLong(TAG_EXPOSED_UNTIL);
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

   private static boolean canUseShadow(ServerPlayer player) {
      return canUseShadow(player, null);
   }

   private static boolean canUseShadow(ServerPlayer player, @Nullable LivingEntity target) {
      if (!canAttack(player)) return false;
      int light = player.level().getMaxLocalRawBrightness(player.blockPosition());
      if (ShadowHassanRules.isShadowLight(light)) return true;
      if (player.level() instanceof ServerLevel level && target != null && ShadowHassanRules.castsSunlightShadow(level, target)) return true;
      player.displayClientMessage(Component.translatable("message.typemoonworld.shadow_hassan_card.no_shadow"), true);
      return false;
   }

   private static void setConcealed(ServerPlayer player, boolean concealed) {
      player.getPersistentData().putBoolean(TAG_CONCEALED, concealed);
      player.getPersistentData().remove(TAG_EXPOSED_UNTIL);
      maintainBodyInvisibility(player, concealed);
   }

   private static void maintainBodyInvisibility(ServerPlayer player, boolean completeConcealment) {
      player.setInvisible(completeConcealment);
      if (completeConcealment) {
         ServantCardConcealmentHelper.maintain(player, 40, COMPLETE_CONCEALMENT_AMPLIFIER);
      } else {
         MobEffectInstance invisibility = player.getEffect(MobEffects.INVISIBILITY);
         if (invisibility != null && invisibility.getAmplifier() >= COMPLETE_CONCEALMENT_AMPLIFIER) {
            player.removeEffect(MobEffects.INVISIBILITY);
         }
      }
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
      double before = vars.servant_card_mana;
      vars.servant_card_mana = Math.min(vars.servant_card_max_mana, vars.servant_card_mana + amount);
      if (vars.servant_card_mana != before) vars.syncMana(player);
   }

   @Nullable
   private static LivingEntity validBladeTarget(ServerPlayer player, double range) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, range, 2.2);
      return target == null || target.getType().is(ShadowHassanDamageTypes.BLADE_IMMUNE) ? null : target;
   }

   private static void directStrike(ServerPlayer player, LivingEntity target, float damage) {
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().playerAttack(player), damage);
      target.invulnerableTime = 0;
   }

   private static Vec3 horizontal(Vec3 direction) {
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      return horizontal.lengthSqr() < 1.0E-5 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   @Nullable
   private static BlockPos findNearestShadow(ServerLevel level, ServerPlayer player, Vec3 desired, int radius) {
      return findNearestShadow(level, player, desired, radius, null);
   }

   @Nullable
   private static BlockPos findNearestShadow(ServerLevel level, ServerPlayer player, Vec3 desired, int radius,
      @Nullable LivingEntity sunlightShadowCaster) {
      BlockPos base = BlockPos.containing(desired);
      boolean targetCastsShadow = sunlightShadowCaster != null && ShadowHassanRules.castsSunlightShadow(level, sunlightShadowCaster);
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;
      for (BlockPos candidate : BlockPos.betweenClosed(base.offset(-radius, -2, -radius), base.offset(radius, 2, radius))) {
         if (!isValidShadowDestination(level, player, candidate, targetCastsShadow ? sunlightShadowCaster : null)) continue;
         double distance = candidate.distToCenterSqr(desired.x, desired.y, desired.z);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = candidate.immutable();
         }
      }
      return best;
   }

   @Nullable
   private static BlockPos findRetreatShadow(ServerLevel level, ServerPlayer player, Vec3 desired, @Nullable LivingEntity threat) {
      BlockPos center = player.blockPosition();
      BlockPos best = null;
      double bestScore = Double.NEGATIVE_INFINITY;
      for (BlockPos candidate : BlockPos.betweenClosed(center.offset(-16, -3, -16), center.offset(16, 3, 16))) {
         if (candidate.distSqr(center) > 16.0 * 16.0 || !isValidShadowDestination(level, player, candidate)) continue;
         Vec3 position = Vec3.atBottomCenterOf(candidate);
         double threatDistance = threat == null ? 0.0 : threat.distanceToSqr(position);
         double desiredDistance = position.distanceToSqr(desired);
         double score = threatDistance - desiredDistance * 0.25;
         if (score > bestScore) {
            bestScore = score;
            best = candidate.immutable();
         }
      }
      return best;
   }

   private static boolean isValidShadowDestination(ServerLevel level, ServerPlayer player, BlockPos pos) {
      return isValidShadowDestination(level, player, pos, null);
   }

   private static boolean isValidShadowDestination(ServerLevel level, ServerPlayer player, BlockPos pos,
      @Nullable LivingEntity sunlightShadowCaster) {
      boolean naturalShadow = ShadowHassanRules.isShadowLight(level.getMaxLocalRawBrightness(pos));
      boolean castShadow = sunlightShadowCaster != null
         && pos.distSqr(sunlightShadowCaster.blockPosition()) <= 16.0;
      if (!(naturalShadow || castShadow)
         || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
         || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
         || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
         || !level.getWorldBorder().isWithinBounds(pos)) return false;
      Vec3 destination = Vec3.atBottomCenterOf(pos);
      AABB moved = player.getBoundingBox().move(destination.subtract(player.position()));
      return level.noCollision(player, moved);
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
