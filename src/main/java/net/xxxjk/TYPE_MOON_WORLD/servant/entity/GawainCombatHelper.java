package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class GawainCombatHelper {
   private static final String TAG_SUN_BLESSING = "GawainSunBlessingActive";
   private static final String TAG_LAST_SUN_VFX = "GawainLastSunBlessingVfx";
   private static final String TAG_CHARISMA_UNTIL = "GawainCharismaUntil";
   private static final String TAG_LAST_CHARISMA_PULSE = "GawainLastCharismaPulse";
   private static final String TAG_LAST_BELT = "GawainLastBelt";
   private static final String TAG_BELT_UNTIL = "GawainBeltUntil";
   private static final String TAG_GUTS_READY = "GawainGutsReady";
   private static final String TAG_GUTS_SOLAR = "GawainGutsSolar";
   private static final String TAG_RECOVERY_ACTIVE = "BattleContinuationRecoveryActive";
   private static final String TAG_LAST_GALLATIN = "GawainLastGallatin";
   private static final String TAG_GALLATIN_WINDUP_UNTIL = "GawainGallatinWindupUntil";
   private static final String TAG_GALLATIN_TARGET = "GawainGallatinTarget";
   private static final String TAG_LAST_GALLATIN_CHARGE_VFX = "GawainLastGallatinChargeVfx";
   private static final String TAG_LAST_WALL_BREAK = "GawainLastWallBreak";
   private static final String TAG_LAST_SUNLIT_PURSUIT = "GawainLastSunlitPursuit";
   private static final String TAG_LAST_NOON_GUARD = "GawainLastNoonGuard";
   private static final String TAG_LAST_GALLATIN_SPARK = "GawainLastGallatinSpark";
   private static final String TAG_LAST_SOLAR_REBUKE = "GawainLastSolarRebuke";
   private static final String TAG_LAST_RADIANT_FIELD = "GawainLastRadiantField";

   private static final int BELT_DURATION = 400;
   private static final int BELT_COOLDOWN = 500;
   private static final int BELT_SOLAR_COOLDOWN = 400;
   private static final int BELT_MP_COST = 25;
   private static final int GALLATIN_WINDUP = 100;
   private static final int GALLATIN_COOLDOWN = 700;
   private static final int GALLATIN_SOLAR_COOLDOWN = 500;
   private static final int WALL_BREAK_COOLDOWN = 10;
   private static final int SUNLIT_PURSUIT_COOLDOWN = 110;
   private static final int NOON_GUARD_COOLDOWN = 280;
   private static final int GALLATIN_SPARK_COOLDOWN = 150;
   private static final int SOLAR_REBUKE_COOLDOWN = 120;
   private static final int RADIANT_FIELD_COOLDOWN = 260;
   private static final double GALLATIN_RANGE = 100.0;
   private static final double GALLATIN_HALF_ANGLE_COS = Math.cos(Math.toRadians(35.0));
   private static final double GAWAIN_VFX_RADIUS = 128.0;

   private static final ResourceLocation SUN_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gawain_sun_health");
   private static final ResourceLocation SUN_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gawain_sun_attack");
   private static final ResourceLocation SUN_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gawain_sun_speed");
   private static final ResourceLocation SUN_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gawain_sun_armor");
   private static final ResourceLocation CHARISMA_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gawain_charisma_attack");

   private GawainCombatHelper() {
   }

   public static boolean tick(GawainEntity entity, ServantAiContext context) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return false;
      }

      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      tickSunBlessing(entity, level, data, now);
      tickCharisma(entity, level, data, now);
      tickBeltManaRecovery(entity, level, data, now);

      LivingEntity target = context.target();
      if (target != null && target.isAlive() && !EntityUtils.isImmunePlayerTarget(target)) {
         tryBreakCombatWall(entity, target, level, data, now, hasSunBlessing(entity));
      }
      if (tickGallatinState(entity, level, data, target, now)) {
         return true;
      }
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      if (ServantCombatSystem.cannotAct(entity) || ServantCombatSystem.skillsSuppressed(entity)) {
         return false;
      }

      double distance = entity.distanceTo(target);
      entity.getLookControl().setLookAt(target, 40.0F, 40.0F);
      boolean decisive = ServantCombatSystem.getPhase(entity) == ServantCombatPhase.DECISIVE
         || entity.getHealth() <= entity.getMaxHealth() * 0.45F;
      if (entity.getHealth() <= entity.getMaxHealth() * 0.35F && tryUseBelt(entity, level, data, now)) {
         return true;
      }
      if (tryGawainSmallSkill(entity, target, level, data, now, distance)) {
         return true;
      }
      if (decisive && distance <= GALLATIN_RANGE && entity.getSensing().hasLineOfSight(target) && tryStartGallatin(entity, target, level, data, now)) {
         return true;
      }
      if (hasSunBlessing(entity) && distance >= 4.0 && distance <= 12.0 && now - data.getLong("GawainLastFireSlash") > 120L) {
         performSolarFireSlash(entity, target, level, data, now);
         return true;
      }
      return false;
   }

   public static void tickSharedBuffCleanup(LivingEntity entity) {
      if (entity == null || entity.level().isClientSide()) {
         return;
      }
      long until = entity.getPersistentData().getLong(TAG_CHARISMA_UNTIL);
      if (until > 0L && until <= entity.level().getGameTime()) {
         removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), CHARISMA_ATTACK_ID);
         entity.getPersistentData().remove(TAG_CHARISMA_UNTIL);
      }
   }

   public static boolean hasSunBlessing(GawainEntity entity) {
      return entity != null && entity.getPersistentData().getBoolean(TAG_SUN_BLESSING);
   }

   public static boolean tryConsumeGuts(GawainEntity entity) {
      if (entity == null || entity.level().isClientSide() || !entity.getPersistentData().getBoolean(TAG_GUTS_READY)) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      data.putBoolean(TAG_GUTS_READY, false);
      data.putBoolean(TAG_RECOVERY_ACTIVE, true);
      float ratio = data.getBoolean(TAG_GUTS_SOLAR) ? 0.50F : 0.20F;
      entity.setHealth(Math.max(1.0F, entity.getMaxHealth() * ratio));
      entity.clearFire();
      entity.invulnerableTime = Math.max(entity.invulnerableTime, 40);
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(), 40, 0.5, 0.7, 0.5, 0.14);
         level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), 28, 0.42, 0.45, 0.42, 0.08);
         level.playSound(null, entity.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.1F, 0.85F);
      }
      TYPE_MOON_WORLD.queueServerWork(40, () -> {
         if (entity.isAlive()) {
            entity.getPersistentData().putBoolean(TAG_RECOVERY_ACTIVE, false);
         }
      });
      return true;
   }

   private static void tickSunBlessing(GawainEntity entity, ServerLevel level, CompoundTag data, long now) {
      boolean active = isUnderSun(level, entity.blockPosition());
      boolean wasActive = data.getBoolean(TAG_SUN_BLESSING);
      if (active != wasActive) {
         float ratio = entity.getMaxHealth() > 0.0F ? entity.getHealth() / entity.getMaxHealth() : 1.0F;
         data.putBoolean(TAG_SUN_BLESSING, active);
         updateModifier(entity.getAttribute(Attributes.MAX_HEALTH), SUN_HEALTH_ID, active ? 2.0 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
         updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), SUN_ATTACK_ID, active ? 2.0 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
         updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), SUN_SPEED_ID, active ? 2.0 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
         updateModifier(entity.getAttribute(Attributes.ARMOR), SUN_ARMOR_ID, active ? 2.0 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
         entity.setHealth(Math.max(1.0F, Math.min(entity.getMaxHealth(), entity.getMaxHealth() * ratio)));
         spawnSunTransitionFx(entity, level, active);
      }
      if (active && now - data.getLong(TAG_LAST_SUN_VFX) >= 38L) {
         data.putLong(TAG_LAST_SUN_VFX, now);
         VFXServerEffects.spawn(level, "servant_gawain_sun_blessing", entity, 64.0);
      }
   }

   private static boolean isUnderSun(ServerLevel level, BlockPos pos) {
      long dayTime = level.getDayTime() % 24000L;
      return level.dimensionType().hasSkyLight()
         && dayTime >= 0L && dayTime < 12000L
         && !level.isRaining() && !level.isThundering()
         && level.canSeeSky(pos.above());
   }

   private static void tickCharisma(GawainEntity entity, ServerLevel level, CompoundTag data, long now) {
      if (now - data.getLong(TAG_LAST_CHARISMA_PULSE) < 40L) {
         return;
      }
      data.putLong(TAG_LAST_CHARISMA_PULSE, now);
      applyCharisma(entity, now + 55L, 0.15);
      for (ServantEntity ally : level.getEntitiesOfClass(ServantEntity.class, entity.getBoundingBox().inflate(20.0), servant -> servant.isAlive() && servant != entity && servant.isAlliedTo(entity))) {
         applyCharisma(ally, now + 55L, 0.10);
      }
   }

   private static void applyCharisma(LivingEntity entity, long until, double amount) {
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), CHARISMA_ATTACK_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      entity.getPersistentData().putLong(TAG_CHARISMA_UNTIL, until);
   }

   private static boolean tryUseBelt(GawainEntity entity, ServerLevel level, CompoundTag data, long now) {
      int cooldown = hasSunBlessing(entity) ? BELT_SOLAR_COOLDOWN : BELT_COOLDOWN;
      if (entity.getCurrentMp() < BELT_MP_COST || now - data.getLong(TAG_LAST_BELT) < cooldown) {
         return false;
      }
      data.putLong(TAG_LAST_BELT, now);
      data.putLong(TAG_BELT_UNTIL, now + BELT_DURATION);
      data.putBoolean(TAG_GUTS_READY, true);
      data.putBoolean(TAG_GUTS_SOLAR, hasSunBlessing(entity));
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - BELT_MP_COST));
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.62, entity.getZ(), 36, 0.45, 0.55, 0.45, 0.08);
      level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), 12, 0.36, 0.38, 0.36, 0.04);
      level.playSound(null, entity.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.0F, 1.25F);
      return true;
   }

   private static void tickBeltManaRecovery(GawainEntity entity, ServerLevel level, CompoundTag data, long now) {
      if (data.getLong(TAG_BELT_UNTIL) <= now || entity.tickCount % 20 != 0 || entity.getCurrentMp() >= entity.getMaxMp()) {
         return;
      }
      entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + Math.max(1.0, entity.getMaxMp() * 0.015)));
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + entity.getBbHeight() * 0.58, entity.getZ(), 3, 0.25, 0.25, 0.25, 0.02);
   }

   private static boolean tryGawainSmallSkill(GawainEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now, double distance) {
      boolean solar = hasSunBlessing(entity);
      double healthRatio = entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
      int nearbyEnemies = countNearbyEnemies(entity, level, 5.0);
      if ((healthRatio <= 0.45 || nearbyEnemies >= 3) && tryNoonGuard(entity, level, data, now, solar)) {
         return true;
      }
      if (nearbyEnemies >= 3 && distance <= 8.5 && tryRadiantField(entity, level, data, now, solar)) {
         return true;
      }
      if (distance <= 3.2 && entity.getRandom().nextFloat() < 0.45F && trySolarRebuke(entity, target, level, data, now, solar)) {
         return true;
      }
      double vertical = target.getY() - entity.getY();
      if ((vertical > 1.35 || distance >= 5.0) && distance <= 18.0 && entity.getSensing().hasLineOfSight(target) && tryGallatinSpark(entity, target, level, data, now, solar)) {
         return true;
      }
      return distance >= 7.0 && distance <= 24.0 && trySunlitPursuit(entity, target, level, data, now, solar);
   }

   private static boolean trySunlitPursuit(GawainEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now, boolean solar) {
      double mpCost = solar ? 10.0 : 14.0;
      if (!consumeSkill(entity, data, now, TAG_LAST_SUNLIT_PURSUIT, SUNLIT_PURSUIT_COOLDOWN, mpCost)) {
         return false;
      }
      Vec3 dir = horizontalDirection(entity, target);
      Vec3 destination = target.position().subtract(dir.scale(1.65));
      entity.faceToward(target.position());
      entity.triggerChargeAnimation();
      spawnTrail(level, entity.position().add(0.0, 0.55, 0.0), destination.add(0.0, 0.55, 0.0), ParticleTypes.END_ROD, 18);
      entity.teleportTo(destination.x, Math.max(entity.getY(), target.getY()), destination.z);
      entity.setDeltaMovement(dir.x * 0.35, Math.max(entity.getDeltaMovement().y, 0.08), dir.z * 0.35);
      damageTarget(entity, target, (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * (solar ? 1.05 : 0.85) + 8.0));
      target.push(dir.x * 0.55, 0.12, dir.z * 0.55);
      target.hurtMarked = true;
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.45F);
      return true;
   }

   private static boolean tryNoonGuard(GawainEntity entity, ServerLevel level, CompoundTag data, long now, boolean solar) {
      double mpCost = solar ? 12.0 : 18.0;
      if (!consumeSkill(entity, data, now, TAG_LAST_NOON_GUARD, solar ? NOON_GUARD_COOLDOWN - 60 : NOON_GUARD_COOLDOWN, mpCost)) {
         return false;
      }
      entity.triggerNamedActionAnimation("charge");
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, true));
      entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, false, true));
      entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0, false, true));
      level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + entity.getBbHeight() * 0.48, entity.getZ(), 36, 0.8, 0.45, 0.8, 0.055);
      level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.8, entity.getZ(), 22, 0.55, 0.55, 0.55, 0.035);
      level.playSound(null, entity.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0F, 1.25F);
      return true;
   }

   private static boolean tryGallatinSpark(GawainEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now, boolean solar) {
      double mpCost = solar ? 12.0 : 16.0;
      if (!consumeSkill(entity, data, now, TAG_LAST_GALLATIN_SPARK, GALLATIN_SPARK_COOLDOWN, mpCost)) {
         return false;
      }
      entity.faceToward(target.position());
      entity.triggerHorizontalSwingAnimation();
      Vec3 start = entity.position().add(0.0, entity.getBbHeight() * 0.62, 0.0);
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 dir = end.subtract(start);
      if (dir.lengthSqr() < 1.0E-4) {
         return false;
      }
      dir = dir.normalize();
      spawnTrail(level, start, end, ParticleTypes.FLAME, 22);
      spawnTrail(level, start, end, ParticleTypes.END_ROD, 10);
      AABB box = new AABB(start, end).inflate(1.1, 1.1, 1.1);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e -> canHit(entity, e))) {
         damageTarget(entity, living, solar ? 48.0F : 34.0F);
         living.igniteForSeconds(solar ? 5.0F : 3.0F);
         pushAway(living, dir, 0.85, 0.18);
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.05F, 1.15F);
      return true;
   }

   private static boolean trySolarRebuke(GawainEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now, boolean solar) {
      double mpCost = solar ? 8.0 : 12.0;
      if (!consumeSkill(entity, data, now, TAG_LAST_SOLAR_REBUKE, SOLAR_REBUKE_COOLDOWN, mpCost)) {
         return false;
      }
      entity.faceToward(target.position());
      entity.triggerSweepAnimation();
      Vec3 dir = horizontalDirection(entity, target);
      int hit = 0;
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(3.8), e -> canHit(entity, e))) {
         Vec3 away = living.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-4) {
            away = dir;
         }
         away = away.normalize();
         damageTarget(entity, living, solar ? 42.0F : 28.0F);
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 55, solar ? 1 : 0, false, true));
         living.push(away.x * 1.15, 0.24, away.z * 1.15);
         living.hurtMarked = true;
         hit++;
      }
      Vec3 center = entity.position().add(dir.scale(1.25)).add(0.0, entity.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 4, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 18, 0.45, 0.2, 0.45, 0.06);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.15F, 0.95F);
      return hit > 0;
   }

   private static boolean tryRadiantField(GawainEntity entity, ServerLevel level, CompoundTag data, long now, boolean solar) {
      double mpCost = solar ? 16.0 : 22.0;
      if (!consumeSkill(entity, data, now, TAG_LAST_RADIANT_FIELD, RADIANT_FIELD_COOLDOWN, mpCost)) {
         return false;
      }
      entity.triggerNamedActionAnimation("charge");
      Vec3 center = entity.position();
      for (int delay = 0; delay <= 30; delay += 10) {
         TYPE_MOON_WORLD.queueServerWork(delay, () -> applyRadiantFieldPulse(entity, center, solar));
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.82F);
      return true;
   }

   private static void applyRadiantFieldPulse(GawainEntity entity, Vec3 center, boolean solar) {
      if (!entity.isAlive() || !(entity.level() instanceof ServerLevel level)) {
         return;
      }
      double radius = solar ? 6.0 : 4.8;
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.1, center.z, 30, radius * 0.55, 0.12, radius * 0.55, 0.035);
      level.sendParticles(ParticleTypes.ASH, center.x, center.y + 0.45, center.z, 24, radius * 0.55, 0.22, radius * 0.55, 0.02);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), e -> canHit(entity, e))) {
         damageTarget(entity, living, solar ? 18.0F : 12.0F);
         living.igniteForSeconds(solar ? 4.0F : 3.0F);
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 0, false, true));
      }
   }

   private static boolean consumeSkill(GawainEntity entity, CompoundTag data, long now, String tag, int cooldown, double mpCost) {
      if (entity.getCurrentMp() < mpCost || now - data.getLong(tag) < cooldown) {
         return false;
      }
      data.putLong(tag, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - mpCost));
      return true;
   }

   private static boolean tickGallatinState(GawainEntity entity, ServerLevel level, CompoundTag data, LivingEntity fallbackTarget, long now) {
      long windupUntil = data.getLong(TAG_GALLATIN_WINDUP_UNTIL);
      if (windupUntil > now) {
         LivingEntity target = getGallatinTarget(level, data, fallbackTarget);
         entity.getNavigation().stop();
         entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.25, 1.0, 0.25));
         if (target != null && target.isAlive()) {
            entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
            entity.getLookControl().setLookAt(target, 90.0F, 90.0F);
         }
         if (now - data.getLong(TAG_LAST_GALLATIN_CHARGE_VFX) >= 28L) {
            data.putLong(TAG_LAST_GALLATIN_CHARGE_VFX, now);
            VFXServerEffects.spawn(level, "servant_gawain_gallatin_charge", entity, GAWAIN_VFX_RADIUS);
         }
         spawnGallatinChantParticles(entity, level);
         return true;
      }
      if (windupUntil > 0L) {
         releaseGallatin(entity, level, data, getGallatinTarget(level, data, fallbackTarget));
         return true;
      }
      return false;
   }

   private static boolean tryStartGallatin(GawainEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      boolean solar = hasSunBlessing(entity);
      double mpCost = solar ? 100.0 : 80.0;
      int cooldown = solar ? GALLATIN_SOLAR_COOLDOWN : GALLATIN_COOLDOWN;
      if (entity.getCurrentMp() < mpCost || now - data.getLong(TAG_LAST_GALLATIN) < cooldown) {
         return false;
      }
      data.putLong(TAG_LAST_GALLATIN, now);
      data.putLong(TAG_GALLATIN_WINDUP_UNTIL, now + GALLATIN_WINDUP);
      data.putUUID(TAG_GALLATIN_TARGET, target.getUUID());
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - mpCost));
      entity.faceToward(target.position());
      entity.triggerNamedActionAnimation("gallatin_chant");
      ServantVoiceHelper.tryPlayGawainNp(entity);
      VFXServerEffects.spawnReplayable(level, "servant_gawain_gallatin_charge", entity, 5.2F);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.35F, 0.72F);
      return true;
   }

   private static void releaseGallatin(GawainEntity entity, ServerLevel level, CompoundTag data, LivingEntity target) {
      data.remove(TAG_GALLATIN_WINDUP_UNTIL);
      data.remove(TAG_GALLATIN_TARGET);
      if (!entity.isAlive()) {
         return;
      }
      Vec3 look = horizontalLook(entity);
      if (target != null && target.isAlive()) {
         Vec3 toTarget = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
         if (toTarget.lengthSqr() > 1.0E-4) {
            look = toTarget.normalize();
            entity.faceVector(look);
         }
      }
      entity.triggerNamedActionAnimation("gallatin_release");
      VFXServerEffects.spawnReplayable(level, "servant_gawain_gallatin", entity, 3.0F);
      level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.2F, 0.62F);
      performGallatinCone(entity, level, look, hasSunBlessing(entity) ? 3000.0F : 1000.0F);
   }

   private static LivingEntity getGallatinTarget(ServerLevel level, CompoundTag data, LivingEntity fallback) {
      if (data.hasUUID(TAG_GALLATIN_TARGET)) {
         Entity entity = level.getEntity(data.getUUID(TAG_GALLATIN_TARGET));
         if (entity instanceof LivingEntity living && living.isAlive() && !EntityUtils.isImmunePlayerTarget(living)) {
            return living;
         }
      }
      return fallback != null && fallback.isAlive() && !EntityUtils.isImmunePlayerTarget(fallback) ? fallback : null;
   }

   private static void performSolarFireSlash(GawainEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now) {
      data.putLong("GawainLastFireSlash", now);
      entity.faceToward(target.position());
      entity.triggerHorizontalSwingAnimation();
      ServantVoiceHelper.tryPlayGawainFireAttack(entity);
      Vec3 look = horizontalLook(entity);
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
      Set<Integer> hit = new HashSet<>();
      for (double dist = 1.5; dist <= 12.0; dist += 0.75) {
         Vec3 pos = origin.add(look.scale(dist));
         level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 5, 0.22, 0.18, 0.22, 0.04);
         for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(1.35), e -> canHit(entity, e))) {
            if (hit.add(living.getId())) {
               living.hurt(entity.damageSources().magic(), 80.0F);
               living.igniteForSeconds(4.0F);
               pushAway(living, look, 1.2, 0.22);
            }
         }
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.2F, 0.85F);
   }

   private static void performGallatinCone(GawainEntity entity, ServerLevel level, Vec3 look, float damage) {
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
      Set<Integer> hit = new HashSet<>();
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(GALLATIN_RANGE + 3.0), e -> canHit(entity, e))) {
         Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.45, 0.0).subtract(origin);
         Vec3 horizontal = new Vec3(to.x, 0.0, to.z);
         double distance = horizontal.length();
         if (distance > GALLATIN_RANGE || distance < 0.2) {
            continue;
         }
         Vec3 dir = horizontal.normalize();
         if (dir.dot(look) < GALLATIN_HALF_ANGLE_COS) {
            continue;
         }
         if (hit.add(living.getId())) {
            applyFixedDamage(entity, living, damage);
            living.igniteForSeconds(5.0F);
            pushAway(living, look, 5.0, 0.32);
         }
      }
      spawnGallatinReleaseParticles(level, origin, look);
      breakGallatinPath(entity, level, origin, look);
   }

   private static void applyFixedDamage(GawainEntity entity, LivingEntity target, float damage) {
      damage = MagicResistanceHelper.applyNoblePhantasmMagicResistance(target, damage);
      if (damage <= 0.0F) {
         return;
      }
      float before = target.getHealth();
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
      if (target.getPersistentData().getBoolean("GodHandActive")) {
         return;
      }
      float desired = Math.max(0.0F, before - damage);
      if (target.getHealth() > desired && target.getHealth() <= before) {
         target.setHealth(desired);
      }
   }

   private static void pushAway(LivingEntity living, Vec3 dir, double horizontal, double vertical) {
      living.push(dir.x * horizontal, vertical, dir.z * horizontal);
      living.hurtMarked = true;
      living.hasImpulse = true;
   }

   private static boolean canHit(GawainEntity entity, LivingEntity target) {
      return target != entity && target.isAlive() && !target.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(target);
   }

   private static int countNearbyEnemies(GawainEntity entity, ServerLevel level, double radius) {
      return level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius), e -> canHit(entity, e)).size();
   }

   private static void damageTarget(GawainEntity entity, LivingEntity target, float damage) {
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
   }

   private static Vec3 horizontalDirection(LivingEntity from, LivingEntity to) {
      Vec3 dir = to.position().subtract(from.position()).multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = from.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      return dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
   }

   private static void spawnTrail(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int count) {
      int samples = Math.max(2, count);
      for (int i = 0; i < samples; i++) {
         double t = samples <= 1 ? 0.0 : (double)i / (double)(samples - 1);
         Vec3 pos = start.lerp(end, t);
         level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0.01);
      }
   }

   private static boolean tryBreakCombatWall(GawainEntity entity, LivingEntity target, ServerLevel level, CompoundTag data, long now, boolean solar) {
      if (now - data.getLong(TAG_LAST_WALL_BREAK) < WALL_BREAK_COOLDOWN) {
         return false;
      }
      boolean pressingWall = entity.horizontalCollision || entity.getNavigation().isInProgress() && !entity.getSensing().hasLineOfSight(target);
      if (!pressingWall) {
         return false;
      }
      Vec3 dir = horizontalDirection(entity, target);
      int broken = breakWallColumn(level, entity, dir, solar ? 50.0F : 32.0F, solar ? 18 : 10);
      if (broken <= 0) {
         return false;
      }
      data.putLong(TAG_LAST_WALL_BREAK, now);
      Vec3 fx = entity.position().add(dir.scale(1.2)).add(0.0, entity.getBbHeight() * 0.45, 0.0);
      level.sendParticles(ParticleTypes.CLOUD, fx.x, fx.y, fx.z, 8, 0.2, 0.18, 0.2, 0.035);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.8F, 0.75F);
      return true;
   }

   private static int breakWallColumn(ServerLevel level, GawainEntity entity, Vec3 dir, float hardnessLimit, int maxBroken) {
      int broken = 0;
      Vec3 right = new Vec3(-dir.z, 0.0, dir.x);
      BlockPos base = entity.blockPosition();
      int maxY = Math.max(1, Mth.ceil(entity.getBbHeight()));
      for (int forward = 1; forward <= 2 && broken < maxBroken; forward++) {
         for (int side = -1; side <= 1 && broken < maxBroken; side++) {
            Vec3 offset = dir.scale(forward).add(right.scale(side * 0.65));
            BlockPos column = base.offset(Mth.floor(offset.x + 0.5), 0, Mth.floor(offset.z + 0.5));
            for (int y = 0; y <= maxY && broken < maxBroken; y++) {
               BlockPos pos = column.above(y);
               BlockState state = level.getBlockState(pos);
               float hardness = state.getDestroySpeed(level, pos);
               if (!state.isAir() && hardness >= 0.0F && hardness <= hardnessLimit && !state.is(Blocks.BEDROCK) && state.getExplosionResistance(level, pos, null) < 1200.0F) {
                  if (level.removeBlock(pos, false)) {
                     broken++;
                  }
               }
            }
         }
      }
      return broken;
   }

   private static void breakGallatinPath(GawainEntity entity, ServerLevel level, Vec3 origin, Vec3 look) {
      int broken = 0;
      int limit = hasSunBlessing(entity) ? 280 : 160;
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      for (double dist = 2.0; dist <= GALLATIN_RANGE && broken < limit; dist += 2.0) {
         double halfWidth = Math.min(20.0, dist * 0.7);
         for (double side = -halfWidth; side <= halfWidth && broken < limit; side += 2.0) {
            BlockPos center = BlockPos.containing(origin.add(look.scale(dist)).add(right.scale(side)));
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(0, -1, 0), center.offset(0, 2, 0))) {
               BlockState state = level.getBlockState(pos);
               float hardness = state.getDestroySpeed(level, pos);
               if (!state.isAir() && hardness >= 0.0F && hardness < 55.0F && !state.is(Blocks.BEDROCK)) {
                  if (level.removeBlock(pos, false)) {
                     broken++;
                     if (broken >= limit) {
                        break;
                     }
                  }
               }
            }
         }
      }
   }

   private static void spawnSunTransitionFx(GawainEntity entity, ServerLevel level, boolean active) {
      level.sendParticles(active ? ParticleTypes.FLASH : ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.7, entity.getZ(), active ? 2 : 18, 0.18, 0.2, 0.18, 0.02);
      level.sendParticles(active ? ParticleTypes.FLAME : ParticleTypes.CLOUD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), active ? 42 : 20, 0.45, 0.65, 0.45, active ? 0.08 : 0.03);
      level.playSound(null, entity.blockPosition(), active ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 0.9F, active ? 1.45F : 0.8F);
   }

   private static void spawnGallatinChantParticles(GawainEntity entity, ServerLevel level) {
      if (entity.tickCount % 2 != 0) {
         return;
      }
      Vec3 look = horizontalLook(entity);
      Vec3 front = entity.position().add(look.scale(1.2)).add(0.0, entity.getBbHeight() * 0.72, 0.0);
      level.sendParticles(ParticleTypes.FLAME, front.x, front.y, front.z, 5, 0.28, 0.24, 0.28, 0.035);
      level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() + 2.2, entity.getZ(), 4, 0.8, 0.24, 0.8, 0.02);
      if (entity.tickCount % 10 == 0) {
         level.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + entity.getBbHeight() + 2.2, entity.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static void spawnGallatinReleaseParticles(ServerLevel level, Vec3 origin, Vec3 look) {
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      for (double dist = 1.0; dist <= GALLATIN_RANGE; dist += 3.0) {
         double halfWidth = Math.min(20.0, dist * 0.7);
         Vec3 center = origin.add(look.scale(dist));
         level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 10, halfWidth * 0.32, 0.3, halfWidth * 0.32, 0.1);
         level.sendParticles(ParticleTypes.LAVA, center.x, center.y - 0.45, center.z, 2, halfWidth * 0.2, 0.1, halfWidth * 0.2, 0.0);
         if (((int)dist) % 6 == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y - 0.2, center.z, 1, halfWidth * 0.16, 0.08, halfWidth * 0.16, 0.0);
         }
         if (((int)dist) % 9 == 0) {
            Vec3 edge = center.add(right.scale(level.random.nextBoolean() ? halfWidth : -halfWidth));
            level.sendParticles(ParticleTypes.FLAME, edge.x, edge.y, edge.z, 6, 0.22, 0.38, 0.22, 0.08);
         }
      }
   }

   private static Vec3 horizontalLook(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
      if (attribute == null) {
         return;
      }
      if (Math.abs(amount) < 1.0E-6) {
         removeModifier(attribute, id);
         return;
      }
      AttributeModifier existing = attribute.getModifier(id);
      if (existing != null && existing.operation() == operation && Math.abs(existing.amount() - amount) < 1.0E-6) {
         return;
      }
      if (existing != null) {
         attribute.removeModifier(id);
      }
      attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null && attribute.getModifier(id) != null) {
         attribute.removeModifier(id);
      }
   }
}
