package net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import org.jetbrains.annotations.Nullable;

public final class ShadowHassanRules {
   public static final TagKey<MobEffect> REVEALING_EFFECTS = TagKey.create(Registries.MOB_EFFECT,
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "presence_concealment_revealing"));
   public static final int CONCEALMENT_EXPOSURE_TICKS = 10;
   public static final int SHADOW_STEP_COOLDOWN_TICKS = 100;
   public static final int SHADOW_STEP_MAX_DISTANCE = 12;
   public static final int SHADOW_POINT_SEARCH_RADIUS = 5;
   public static final int SHADOW_RETREAT_COOLDOWN_TICKS = 140;
   public static final double SHADOW_RETREAT_MANA_COST = 12.0;
   public static final int POSSESSION_RANGE = 18;
   public static final int POSSESSION_MANA_INTERVAL_TICKS = 20;
   public static final double POSSESSION_MANA_PER_INTERVAL = 1.0;
   public static final double TOTAL_DARKNESS_MOVEMENT_MULTIPLIER = 0.05;
   public static final int MANA_RESTORE_INTERVAL_TICKS = 20;
   public static final double MANA_RESTORE_PER_SECOND = 5.0;
   public static final double NOBLE_PHANTASM_HEALTH_RATIO = 0.60;
   public static final double DEATH_SHADOW_SPEED_PER_TICK = 0.70;
   public static final double DEATH_SHADOW_CONTACT_DISTANCE = 1.0;
   public static final double MAX_MELEE_DISTANCE = 6.0;

   private ShadowHassanRules() {
   }

   public static boolean isShadowLight(int brightness) {
      return brightness >= 1 && brightness <= 11;
   }

   public static boolean isTotalDarkness(int brightness) {
      return brightness <= 0;
   }

   public static boolean isTotalDarkness(Level level, BlockPos pos) {
      return isTotalDarkness(level.getBrightness(LightLayer.SKY, pos), level.getBrightness(LightLayer.BLOCK, pos));
   }

   public static boolean isTotalDarkness(int skyLight, int blockLight) {
      return skyLight <= 0 && blockLight <= 0;
   }

   public static boolean isUsableShadowZone(Level level, BlockPos pos, @Nullable LivingEntity shadowCaster) {
      int skyLight = level.getBrightness(LightLayer.SKY, pos);
      int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
      if (isTotalDarkness(skyLight, blockLight)) return false;
      boolean covered = !level.canSeeSky(pos);
      boolean projected = shadowCaster != null && castsSunlightShadow(level, shadowCaster)
         && pos.distSqr(shadowCaster.blockPosition()) <= 16.0;
      return isUsableShadowZone(skyLight, blockLight, covered, projected);
   }

   public static boolean hasUsableShadowAnchor(Level level, @Nullable LivingEntity host) {
      return host != null && host.level() == level && host.isAlive() && !host.isRemoved()
         && isUsableShadowZone(level, host.blockPosition(), host);
   }

   public static boolean canBeginPossession(boolean totalDarkness, boolean hostHasShadow,
      boolean inRange, boolean visible, boolean legalTarget) {
      return !totalDarkness && hostHasShadow && inRange && visible && legalTarget;
   }

   public static boolean isFullyExposed(boolean hasUsableShadow, boolean attackExposureActive) {
      return !hasUsableShadow && !attackExposureActive;
   }

   public static boolean isNpcAssassinationTarget(LivingEntity hassan, LivingEntity target) {
      return hassan != null && target != null && target.isAlive() && target != hassan
         && !(target instanceof Pig)
         && (target instanceof Player || target instanceof Enemy || target instanceof ServantEntity
            || target instanceof Mob mob && mob.getTarget() == hassan);
   }

   public static double possessionManaAfterInterval(double currentMana, double maxMana, boolean inShadowZone) {
      if (currentMana <= 0.0) return 0.0;
      double mana = inShadowZone ? Math.min(maxMana, currentMana + MANA_RESTORE_PER_SECOND) : currentMana;
      return Math.max(0.0, mana - POSSESSION_MANA_PER_INTERVAL);
   }

   public static boolean isUsableShadowZone(int skyLight, int blockLight, boolean covered, boolean projected) {
      if (isTotalDarkness(skyLight, blockLight)) return false;
      return isShadowLight(Math.max(skyLight, blockLight)) || covered || projected;
   }

   public static boolean isValidShadowPoint(Level level, BlockPos pos, Entity body,
      @Nullable LivingEntity shadowCaster) {
      if (!isUsableShadowZone(level, pos, shadowCaster)
         || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
         || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
         || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
         || !level.getWorldBorder().isWithinBounds(pos)) return false;
      Vec3 destination = Vec3.atBottomCenterOf(pos);
      AABB moved = body.getBoundingBox().move(destination.subtract(body.position()));
      return level.noCollision(body, moved);
   }

   @Nullable
   public static BlockPos findNearestShadowPoint(ServerLevel level, Entity body, Vec3 desired, int radius,
      @Nullable LivingEntity shadowCaster, @Nullable Vec3 origin, double maxTravel) {
      BlockPos base = BlockPos.containing(desired);
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;
      for (BlockPos candidate : BlockPos.betweenClosed(base.offset(-radius, -2, -radius), base.offset(radius, 2, radius))) {
         if (!isValidShadowPoint(level, candidate, body, shadowCaster)) continue;
         Vec3 destination = Vec3.atBottomCenterOf(candidate);
         if (origin != null && destination.distanceTo(origin) > maxTravel) continue;
         double distance = candidate.distToCenterSqr(desired.x, desired.y, desired.z);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = candidate.immutable();
         }
      }
      return best;
   }

   public static boolean isWithinShadowStepRange(Vec3 origin, Vec3 destination) {
      return origin.distanceToSqr(destination) <= (double)SHADOW_STEP_MAX_DISTANCE * SHADOW_STEP_MAX_DISTANCE;
   }

   public static double movementMultiplier(int skyLight, int blockLight) {
      return isTotalDarkness(skyLight, blockLight) ? TOTAL_DARKNESS_MOVEMENT_MULTIPLIER : 1.0;
   }

   public static boolean isOrdinaryHostAttack(DamageSource source, LivingEntity host) {
      if (host == null || source.getEntity() != host || source.is(ShadowHassanDamageTypes.SHADOW_PENETRATION)) return false;
      if (source instanceof CombatDamageSource named && !"basic".equals(named.origin().skill())) return false;
      return source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
         || source.is(DamageTypes.ARROW) || source.is(DamageTypes.TRIDENT)
         || source.is(DamageTypes.MOB_PROJECTILE) || source.is(DamageTypes.THROWN);
   }

   public static boolean isShadowPenetratingDamage(DamageSource source) {
      if (source instanceof ShadowHassanDamageSource || source.is(ShadowHassanDamageTypes.SHADOW_PENETRATION)) return true;
      return source instanceof CombatDamageSource named && !"basic".equals(named.origin().skill());
   }

   public static boolean isSunlightShadow(boolean daytime, boolean skyVisible, boolean raining, int skyLight) {
      return daytime && skyVisible && !raining && skyLight > 0;
   }

   public static boolean isDirectSunlight(boolean daytime, boolean skyVisible, boolean raining, int skyLight) {
      return isSunlightShadow(daytime, skyVisible, raining, skyLight);
   }

   public static boolean isDirectSunlight(Level level, BlockPos pos) {
      return isDirectSunlight(level.isDay(), level.canSeeSky(pos), level.isRainingAt(pos),
         level.getBrightness(LightLayer.SKY, pos));
   }

   public static boolean isDirectMoonlight(boolean night, boolean skyVisible, boolean raining, int skyLight) {
      return night && skyVisible && !raining && skyLight > 0;
   }

   public static boolean isDirectSunOrMoonlight(Level level, BlockPos pos) {
      boolean skyVisible = level.canSeeSky(pos);
      boolean raining = level.isRainingAt(pos);
      int skyLight = level.getBrightness(LightLayer.SKY, pos);
      return isDirectSunlight(level.isDay(), skyVisible, raining, skyLight)
         || isDirectMoonlight(level.isNight(), skyVisible, raining, skyLight);
   }

   public static boolean castsSunlightShadow(Level level, LivingEntity entity) {
      if (level == null || entity == null || !entity.isAlive()) return false;
      BlockPos exposedPos = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
      return isSunlightShadow(level.isDay(), level.canSeeSky(exposedPos), level.isRainingAt(exposedPos),
         level.getBrightness(LightLayer.SKY, exposedPos));
   }

   public static boolean shouldTriggerNoblePhantasm(float health, float maxHealth) {
      return maxHealth > 0.0F && health > 0.0F && health <= maxHealth * NOBLE_PHANTASM_HEALTH_RATIO;
   }

   public static Vec3 advanceDeathShadow(Vec3 position, Vec3 destination) {
      Vec3 delta = destination.subtract(position);
      double distance = delta.length();
      if (distance <= DEATH_SHADOW_SPEED_PER_TICK || distance == 0.0) return destination;
      return position.add(delta.scale(DEATH_SHADOW_SPEED_PER_TICK / distance));
   }
}
