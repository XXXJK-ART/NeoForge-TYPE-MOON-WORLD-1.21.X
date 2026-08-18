package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.OkitaShinsengumiEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public final class ServantCardOkitaSoujiSaberSkills {
   public static final String SERVANT_ID = "okita_souji_saber";
   private static final String TAG_WEAK_UNTIL = "ServantCardOkitaWeakUntil";
   private static final String TAG_HAORI_RUSH_UNTIL = "ServantCardOkitaHaoriRushUntil";
   private static final String TAG_MIND_EYE_UNTIL = "ServantCardOkitaMindEyeUntil";
   private static final String TAG_COMMAND_TARGET = "ServantCardOkitaCommandTarget";
   private static final String SHINSENGUMI_OWNER_TAG = "OkitaShinsengumiOwner";
   private static final ResourceLocation HAORI_RUSH_SPEED_ID = id("servant_card_okita_haori_rush_speed");
   private static final ResourceLocation HAORI_RUSH_ATTACK_SPEED_ID = id("servant_card_okita_haori_rush_attack_speed");
   private static final ResourceLocation HAORI_RUSH_ATTACK_ID = id("servant_card_okita_haori_rush_attack");
   private static final ResourceLocation WEAK_ATTACK_ID = id("servant_card_okita_weak_attack");
   private static final ResourceLocation WEAK_SPEED_ID = id("servant_card_okita_weak_speed");
   private static final DustParticleOptions ASAGI = new DustParticleOptions(new Vector3f(0.25F, 0.82F, 0.95F), 1.1F);
   private static final int FLAG_COUNT = 13;
   private static final int FLAG_DURATION = 120 * 20;

   private ServantCardOkitaSoujiSaberSkills() {
   }

   public static void initialize(ServerPlayer player) {
      clear(player);
   }

   public static void tick(ServerPlayer player, net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      boolean haoriRush = data.getLong(TAG_HAORI_RUSH_UNTIL) > now;
      updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), HAORI_RUSH_SPEED_ID,
         haoriRush ? 0.35 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(player.getAttribute(Attributes.ATTACK_SPEED), HAORI_RUSH_ATTACK_SPEED_ID,
         haoriRush ? 0.35 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), HAORI_RUSH_ATTACK_ID,
         haoriRush ? 0.15 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      boolean weak = isWeak(player);
      updateModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), WEAK_ATTACK_ID,
         weak ? -0.30 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), WEAK_SPEED_ID,
         weak ? -0.30 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      if (!weak) {
         data.remove(TAG_WEAK_UNTIL);
      }
      if (data.getLong(TAG_MIND_EYE_UNTIL) <= now) {
         data.remove(TAG_MIND_EYE_UNTIL);
      } else if (player.level() instanceof ServerLevel level && (player.tickCount & 3) == 0) {
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.85, player.getZ(), 2, 0.18, 0.25, 0.18, 0.01);
      }
   }

   public static void clear(ServerPlayer player) {
      cleanupPlayerSoldiers(player);
      CompoundTag data = player.getPersistentData();
      data.remove(TAG_WEAK_UNTIL);
      data.remove(TAG_HAORI_RUSH_UNTIL);
      data.remove(TAG_MIND_EYE_UNTIL);
      data.remove(TAG_COMMAND_TARGET);
      updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), HAORI_RUSH_SPEED_ID, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(player.getAttribute(Attributes.ATTACK_SPEED), HAORI_RUSH_ATTACK_SPEED_ID, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), HAORI_RUSH_ATTACK_ID, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), WEAK_ATTACK_ID, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), WEAK_SPEED_ID, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
   }

   public static boolean isWeak(ServerPlayer player) {
      return player.getPersistentData().getLong(TAG_WEAK_UNTIL) > player.level().getGameTime();
   }

   public static boolean performShukuchi(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      Vec3 start = player.position();
      LivingEntity target = findEnemyLookTarget(player, 5.0, 1.5);
      Vec3 destination = target == null ? findLookBlockDestination(player, 5.0) : findBehindTarget(player, target);
      if (destination == null || !trySafeTeleport(player, destination)) {
         return false;
      }
      if (target != null) {
         face(player, target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
      }
      spawnStepEffects(level, start, player.position(), SoundSource.PLAYERS);
      return true;
   }

   public static boolean performIchimonji(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      Vec3 direction = horizontalLook(player);
      player.setDeltaMovement(direction.x * 1.15, Math.max(0.08, player.getDeltaMovement().y), direction.z * 1.15);
      player.hurtMarked = true;
      float damage = (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.35 + 6.0);
      List<LivingEntity> hit = hitForward(player, 4.5, 0.72, damage, false);
      spawnSlashLine(level, player.position().add(0.0, 0.9, 0.0), direction, 4.5);
      level.playSound(null, player.blockPosition(), hit.isEmpty() ? SoundEvents.TRIDENT_THROW.value() : SoundEvents.PLAYER_ATTACK_CRIT,
         SoundSource.PLAYERS, 0.9F, hit.isEmpty() ? 1.75F : 1.45F);
      return true;
   }

   public static boolean performKaifuu(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      float damage = (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9 + 5.0);
      boolean hitAny = false;
      for (LivingEntity target : nearbyEnemies(player, 3.0)) {
         hurtPhysical(player, target, damage);
         Vec3 away = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-4) {
            away = horizontalLook(player);
         }
         away = away.normalize();
         target.push(away.x * 0.9, 0.18, away.z * 0.9);
         target.hurtMarked = true;
         hitAny = true;
      }
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 0.8, player.getZ(), 10, 1.0, 0.2, 1.0, 0.0);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.35F);
      return hitAny;
   }

   public static boolean performMindEye(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.putLong(TAG_MIND_EYE_UNTIL, player.level().getGameTime() + 80L);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, true, true));
      LivingEntity target = findNearestEnemy(player, 5.0);
      if (target != null) {
         Vec3 away = player.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-4) {
            away = horizontalLook(player).scale(-1.0);
         }
         player.setDeltaMovement(away.normalize().scale(0.85).add(0.0, 0.12, 0.0));
         player.hurtMarked = true;
         hurtPhysical(player, target, (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75 + 4.0));
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.9, player.getZ(), 18, 0.35, 0.35, 0.35, 0.025);
         level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.8F);
      }
      return true;
   }

   public static boolean performHaoriRush(ServerPlayer player) {
      player.getPersistentData().putLong(TAG_HAORI_RUSH_UNTIL, player.level().getGameTime() + 200L);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ASAGI, player.getX(), player.getY() + 0.9, player.getZ(), 42, 0.55, 0.55, 0.55, 0.04);
         level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9F, 1.45F);
      }
      return true;
   }

   public static boolean performFeignedRetreat(ServerPlayer player) {
      LivingEntity target = findNearestEnemy(player, 8.0);
      Vec3 away = target == null ? horizontalLook(player).scale(-1.0) : player.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) {
         away = horizontalLook(player).scale(-1.0);
      }
      double power = isWeak(player) ? 1.65 : 1.25;
      player.setDeltaMovement(away.normalize().scale(power).add(0.0, 0.18, 0.0));
      player.hurtMarked = true;
      player.fallDistance = 0.0F;
      if (!isWeak(player)) {
         player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
         player.removeEffect(MobEffects.DIG_SLOWDOWN);
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.6, player.getZ(), 20, 0.25, 0.35, 0.25, 0.05);
         level.playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.8F, 1.25F);
      }
      return true;
   }

   public static boolean performStanceBreak(ServerPlayer player) {
      LivingEntity target = findEnemyLookTarget(player, 4.0, 1.6);
      if (target == null) {
         return false;
      }
      target.stopUsingItem();
      target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      target.removeEffect(MobEffects.ABSORPTION);
      target.getPersistentData().putLong("OkitaMumyoudanDefenseDisabledUntil", player.level().getGameTime() + 100L);
      hurtPhysical(player, target, (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.15 + 8.0));
      if (player.level() instanceof ServerLevel level) {
         spawnSlashLine(level, player.position().add(0.0, 0.9, 0.0), target.position().subtract(player.position()), 4.0);
         level.playSound(null, target.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.9F, 1.25F);
      }
      return true;
   }

   public static boolean performCommand(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      LivingEntity target = findEnemyLookTarget(player, 24.0, 2.4);
      int commanded = 0;
      for (OkitaShinsengumiEntity soldier : playerSoldiers(level, player, 96.0)) {
         if (target != null) {
            soldier.assignOkitaCardTarget(player, target);
         }
         commanded++;
      }
      if (commanded <= 0) {
         return false;
      }
      if (target != null) {
         player.getPersistentData().putUUID(TAG_COMMAND_TARGET, target.getUUID());
      } else {
         player.getPersistentData().remove(TAG_COMMAND_TARGET);
      }
      level.sendParticles(ASAGI, player.getX(), player.getY() + 1.0, player.getZ(), 36, 1.2, 0.5, 1.2, 0.035);
      level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.9F, 1.4F);
      return true;
   }

   public static boolean performMumyoudanZuki(ServerPlayer player) {
      if (isWeak(player) || !(player.level() instanceof ServerLevel level)) {
         return false;
      }
      LivingEntity primary = findEnemyLookTarget(player, 3.0, 1.8);
      if (primary == null) {
         primary = findNearestEnemy(player, 3.0);
      }
      if (primary == null) {
         return false;
      }
      face(player, primary.position().add(0.0, primary.getBbHeight() * 0.5, 0.0));
      level.playSound(null, player.blockPosition(), ModSounds.OKITA_SOUJI_SABER_VOICE_NP.get(), SoundSource.PLAYERS, 1.15F, 1.05F);
      float damage = (float)(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3.0);
      DamageSource source = player.damageSources().source(OkitaSoujiSaberDamageTypes.MUMYOUDAN_ZUKI, player);
      for (int i = 0; i < 3 && primary.isAlive(); i++) {
         primary.stopUsingItem();
         primary.removeEffect(MobEffects.DAMAGE_RESISTANCE);
         primary.removeEffect(MobEffects.ABSORPTION);
         primary.invulnerableTime = 0;
         primary.hurt(source, damage);
         primary.invulnerableTime = 0;
      }
      primary.getPersistentData().putLong("OkitaMumyoudanDefenseDisabledUntil", level.getGameTime() + 100L);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, primary.getX(), primary.getY() + primary.getBbHeight() * 0.55, primary.getZ(), 3, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.CRIT, primary.getX(), primary.getY() + primary.getBbHeight() * 0.55, primary.getZ(), 36, 0.25, 0.35, 0.25, 0.08);
      level.playSound(null, primary.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.25F, 1.55F);
      if (player.getRandom().nextFloat() < 0.30F) {
         triggerWeak(player);
      }
      return true;
   }

   public static boolean performFlagOfSincerity(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      LivingEntity target = findEnemyLookTarget(player, 24.0, 2.4);
      long expiresAt = level.getGameTime() + FLAG_DURATION;
      int summoned = 0;
      for (int i = 0; i < FLAG_COUNT; i++) {
         OkitaShinsengumiEntity soldier = ModEntities.OKITA_SHINSENGUMI.get().create(level);
         if (soldier == null) {
            continue;
         }
         Vec3 pos = findSummonPosition(level, player, i, FLAG_COUNT);
         soldier.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
         soldier.initializeForOkitaCard(player, target, expiresAt);
         if (level.addFreshEntity(soldier)) {
            summoned++;
         }
      }
      if (summoned <= 0) {
         return false;
      }
      level.playSound(null, player.blockPosition(), ModSounds.OKITA_SOUJI_SABER_VOICE_NP.get(), SoundSource.PLAYERS, 1.18F, 1.05F);
      level.sendParticles(ASAGI, player.getX(), player.getY() + 1.1, player.getZ(), 100, 2.0, 1.0, 2.0, 0.05);
      level.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 1.1F, 1.35F);
      return true;
   }

   private static void triggerWeak(ServerPlayer player) {
      player.getPersistentData().putLong(TAG_WEAK_UNTIL, player.level().getGameTime() + 300L);
      player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 1, true, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 0, true, true, true));
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 18, 0.25, 0.35, 0.25, 0.04);
         level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.75F, 1.45F);
      }
   }

   private static LivingEntity findEnemyLookTarget(ServerPlayer player, double range, double inflate) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(inflate);
      LivingEntity best = null;
      double bestScore = 0.78;
      for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, box,
         entity -> isEnemy(player, entity) && player.hasLineOfSight(entity))) {
         Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(eye);
         double distance = to.length();
         if (distance <= 0.01 || distance > range) {
            continue;
         }
         double score = look.dot(to.normalize());
         if (score > bestScore) {
            bestScore = score;
            best = living;
         }
      }
      return best;
   }

   private static LivingEntity findNearestEnemy(ServerPlayer player, double radius) {
      LivingEntity best = null;
      double bestDistance = radius * radius + 1.0;
      for (LivingEntity living : nearbyEnemies(player, radius)) {
         double distance = player.distanceToSqr(living);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = living;
         }
      }
      return best;
   }

   private static List<LivingEntity> nearbyEnemies(ServerPlayer player, double radius) {
      if (!(player.level() instanceof ServerLevel level)) {
         return List.of();
      }
      return new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
         entity -> isEnemy(player, entity)));
   }

   private static boolean isEnemy(ServerPlayer player, LivingEntity entity) {
      return entity != player && entity.isAlive() && !player.isAlliedTo(entity) && !entity.isAlliedTo(player)
         && !ServantMasterTargeting.isContractMaster(player, entity)
         && !EntityUtils.isImmunePlayerTarget(entity);
   }

   private static Vec3 findBehindTarget(ServerPlayer player, LivingEntity target) {
      Vec3 back = target.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (back.lengthSqr() < 1.0E-4) {
         back = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
      }
      if (back.lengthSqr() < 1.0E-4) {
         back = new Vec3(0.0, 0.0, 1.0);
      }
      back = back.normalize();
      Vec3 side = new Vec3(-back.z, 0.0, back.x);
      Vec3[] candidates = {
         target.position().subtract(back.scale(1.25)),
         target.position().add(side.scale(1.25)),
         target.position().subtract(side.scale(1.25)),
         target.position().subtract(back.scale(2.0))
      };
      for (Vec3 candidate : candidates) {
         Vec3 safe = safePositionNear(player, candidate);
         if (safe != null) {
            return safe;
         }
      }
      return null;
   }

   private static Vec3 findLookBlockDestination(ServerPlayer player, double range) {
      if (!(player.level() instanceof ServerLevel level)) {
         return null;
      }
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      HitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
      if (hit.getType() != HitResult.Type.BLOCK) {
         return null;
      }
      BlockHitResult blockHit = (BlockHitResult)hit;
      Vec3 base = blockHit.getLocation().subtract(look.scale(0.35));
      Vec3 safe = safePositionNear(player, base);
      return safe != null ? safe : safePositionNear(player, Vec3.atBottomCenterOf(blockHit.getBlockPos().relative(blockHit.getDirection())));
   }

   private static boolean trySafeTeleport(ServerPlayer player, Vec3 destination) {
      Vec3 safe = safePositionNear(player, destination);
      if (safe == null) {
         return false;
      }
      player.teleportTo(safe.x, safe.y, safe.z);
      player.fallDistance = 0.0F;
      return true;
   }

   private static Vec3 safePositionNear(ServerPlayer player, Vec3 base) {
      if (!(player.level() instanceof ServerLevel level)) {
         return null;
      }
      for (int y = 1; y >= -2; y--) {
         Vec3 candidate = new Vec3(base.x, base.y + y, base.z);
         BlockPos blockPos = BlockPos.containing(candidate);
         AABB moved = player.getBoundingBox().move(candidate.subtract(player.position()));
         if (!level.getWorldBorder().isWithinBounds(blockPos) || !level.noCollision(player, moved)) {
            continue;
         }
         BlockPos belowPos = BlockPos.containing(candidate.x, candidate.y - 0.08, candidate.z);
         BlockState below = level.getBlockState(belowPos);
         if (below.isFaceSturdy(level, belowPos, net.minecraft.core.Direction.UP)) {
            return candidate;
         }
      }
      return null;
   }

   private static List<LivingEntity> hitForward(ServerPlayer player, double range, double minDot, float damage, boolean bypassPhysical) {
      Vec3 direction = horizontalLook(player);
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
      AABB area = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(1.3, 1.1, 1.3);
      List<LivingEntity> hit = new ArrayList<>();
      for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, entity -> isEnemy(player, entity))) {
         Vec3 relative = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(origin);
         if (relative.lengthSqr() > range * range || new Vec3(relative.x, 0.0, relative.z).normalize().dot(direction) < minDot) {
            continue;
         }
         if (bypassPhysical) {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().source(OkitaSoujiSaberDamageTypes.MUMYOUDAN_ZUKI, player), damage);
            target.invulnerableTime = 0;
         } else {
            hurtPhysical(player, target, damage);
         }
         hit.add(target);
      }
      return hit;
   }

   private static void hurtPhysical(ServerPlayer player, LivingEntity target, float damage) {
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().playerAttack(player), damage);
      target.invulnerableTime = 0;
   }

   private static void face(ServerPlayer player, Vec3 point) {
      Vec3 diff = point.subtract(player.getEyePosition());
      double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
      float yaw = (float)(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
      float pitch = (float)(-Math.toDegrees(Math.atan2(diff.y, horizontal)));
      player.setYRot(yaw);
      player.setXRot(pitch);
      player.yHeadRot = yaw;
      player.yBodyRot = yaw;
   }

   private static Vec3 horizontalLook(ServerPlayer player) {
      Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
      return look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
   }

   private static Vec3 findSummonPosition(ServerLevel level, LivingEntity owner, int index, int total) {
      double angle = Math.PI * 2.0 * index / Math.max(1, total);
      double radius = 2.2 + (index % 3) * 1.1;
      Vec3 base = owner.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
      for (int y = 1; y >= -3; y--) {
         Vec3 candidate = new Vec3(base.x, base.y + y, base.z);
         AABB box = new AABB(candidate.x - 0.3, candidate.y, candidate.z - 0.3, candidate.x + 0.3, candidate.y + 1.8, candidate.z + 0.3);
         BlockPos below = BlockPos.containing(candidate.x, candidate.y - 0.08, candidate.z);
         if (level.noCollision(box) && level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
            return candidate;
         }
      }
      return owner.position();
   }

   private static List<OkitaShinsengumiEntity> playerSoldiers(ServerLevel level, ServerPlayer player, double radius) {
      UUID owner = player.getUUID();
      return level.getEntitiesOfClass(OkitaShinsengumiEntity.class, player.getBoundingBox().inflate(radius),
         soldier -> soldier.isAlive() && soldier.getPersistentData().hasUUID(SHINSENGUMI_OWNER_TAG)
            && owner.equals(soldier.getPersistentData().getUUID(SHINSENGUMI_OWNER_TAG)));
   }

   private static void cleanupPlayerSoldiers(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      for (OkitaShinsengumiEntity soldier : playerSoldiers(level, player, 128.0)) {
         soldier.discard();
      }
   }

   private static void spawnStepEffects(ServerLevel level, Vec3 start, Vec3 destination, SoundSource source) {
      level.sendParticles(ASAGI, start.x, start.y + 0.45, start.z, 16, 0.25, 0.35, 0.25, 0.03);
      level.sendParticles(ASAGI, destination.x, destination.y + 0.45, destination.z, 20, 0.25, 0.35, 0.25, 0.03);
      level.playSound(null, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT, source, 0.85F, 1.75F);
   }

   private static void spawnSlashLine(ServerLevel level, Vec3 origin, Vec3 direction, double length) {
      Vec3 dir = new Vec3(direction.x, 0.0, direction.z);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = new Vec3(0.0, 0.0, 1.0);
      }
      dir = dir.normalize();
      for (double d = 0.0; d <= length; d += 0.45) {
         Vec3 point = origin.add(dir.scale(d));
         level.sendParticles(ASAGI, point.x, point.y, point.z, 2, 0.03, 0.03, 0.03, 0.0);
      }
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount,
                                      AttributeModifier.Operation operation) {
      if (attribute == null) {
         return;
      }
      AttributeModifier existing = attribute.getModifier(id);
      if (Math.abs(amount) < 1.0E-6) {
         if (existing != null) {
            attribute.removeModifier(id);
         }
         return;
      }
      if (existing != null && existing.operation() == operation && Math.abs(existing.amount() - amount) < 1.0E-6) {
         return;
      }
      if (existing != null) {
         attribute.removeModifier(id);
      }
      attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }
}
