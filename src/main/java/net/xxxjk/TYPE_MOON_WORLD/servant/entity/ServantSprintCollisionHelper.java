package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;

public final class ServantSprintCollisionHelper {
   private static final String PLAYER_LAST_SPRINT_TAG = "ServantCardLastSprintCollisionRun";
   private static final String NPC_LAST_COLLISION_TAG = "ServantLastSprintCollisionBreak";

   private ServantSprintCollisionHelper() {
   }

   public static boolean tryPlayerSprintCollision(
      ServerPlayer player,
      ServerLevel level,
      CompoundTag data,
      String cooldownTag,
      boolean fiery,
      float damage,
      double knockback,
      double verticalKnockback,
      int maxBroken,
      float hardnessCap
   ) {
      return tryPlayerSprintCollision(player, level, data, cooldownTag, fiery, damage, knockback, verticalKnockback, maxBroken, hardnessCap, null);
   }

   public static boolean tryPlayerSprintCollision(
      ServerPlayer player,
      ServerLevel level,
      CompoundTag data,
      String cooldownTag,
      boolean fiery,
      float damage,
      double knockback,
      double verticalKnockback,
      int maxBroken,
      float hardnessCap,
      Vec3 direction
   ) {
      long now = level.getGameTime();
      if (player.isSprinting()) {
         data.putLong(PLAYER_LAST_SPRINT_TAG, now);
      }
      boolean recentlySprinting = now - data.getLong(PLAYER_LAST_SPRINT_TAG) <= 20L;
      if ((!player.isSprinting() && !recentlySprinting) || !canUsePlayerSprintCollision(player, recentlySprinting)) {
         return false;
      }
      if (now - data.getLong(cooldownTag) < 1L) {
         return false;
      }

      Vec3 dir = direction == null ? playerLookDirection(player) : direction.multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 1.0E-4) {
         return false;
      }
      dir = dir.normalize();

      int hit = hitForwardTargets(level, player, dir, fiery, damage, knockback, verticalKnockback, 1.85, 1.55);
      int broken = breakForwardCube(level, player, dir, maxBroken, hardnessCap);
      if (hit <= 0 && broken <= 0) {
         return false;
      }

      data.putLong(cooldownTag, now);
      spawnCollisionFx(level, player, dir, fiery, broken > 0, SoundSource.PLAYERS);
      return true;
   }

   public static void tickNpcSprintCollision(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive() || entity.isSpiritualDissolving()) {
         return;
      }
      boolean gawain = entity instanceof GawainEntity;
      boolean heracles = entity instanceof HeraclesEntity;
      boolean lancelot = entity instanceof LancelotBerserkerEntity;
      if (!gawain && !heracles && !lancelot) {
         return;
      }
      // NPC terrain damage represents a body collision with an obstacle, not heavy footsteps.
      if (!entity.horizontalCollision) {
         return;
      }

      CompoundTag data = entity.getPersistentData();
      long now = level.getGameTime();
      if (now - data.getLong(NPC_LAST_COLLISION_TAG) < 5L) {
         return;
      }

      LivingEntity target = entity.getTarget();
      Vec3 dir = collisionDirection(entity, target);
      if (dir.lengthSqr() < 1.0E-4) {
         return;
      }

      boolean fiery = entity instanceof GawainEntity gawainEntity && (GawainCombatHelper.hasSunBlessing(gawainEntity) || isUnderSun(level, gawainEntity.blockPosition()));
      int hit = hitForwardTargets(level, entity, dir, fiery, heracles || lancelot ? 10.0F : 8.0F, heracles || lancelot ? 1.25 : 1.0,
         heracles || lancelot ? 0.26 : 0.2, 1.85, 1.55);
      int broken = breakForwardCube(level, entity, dir, heracles ? 32 : lancelot ? 30 : 27, heracles ? 45.0F : lancelot ? 44.0F : 42.0F);
      if (hit <= 0 && broken <= 0) {
         return;
      }

      data.putLong(NPC_LAST_COLLISION_TAG, now);
      spawnCollisionFx(level, entity, dir, fiery, broken > 0, SoundSource.HOSTILE);
   }

   private static boolean canUsePlayerSprintCollision(ServerPlayer player, boolean recentlySprinting) {
      if (!player.onGround() && !player.horizontalCollision) {
         return false;
      }
      return player.isSprinting() || recentlySprinting || player.horizontalCollision;
   }

   private static Vec3 playerLookDirection(ServerPlayer player) {
      Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() >= 1.0E-4) {
         return look.normalize();
      }
      return collisionDirection(player, null);
   }

   private static Vec3 collisionDirection(LivingEntity entity, LivingEntity target) {
      Vec3 motion = entity.getDeltaMovement().multiply(1.0, 0.0, 1.0);
      if (motion.lengthSqr() < 1.0E-4 && entity instanceof ServantEntity) {
         motion = new Vec3(entity.getX() - entity.xo, 0.0, entity.getZ() - entity.zo);
      }
      if (motion.lengthSqr() >= 1.0E-4) {
         return motion.normalize();
      }
      if (target != null && target.isAlive()) {
         Vec3 toTarget = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
         if (toTarget.lengthSqr() >= 1.0E-4) {
            return toTarget.normalize();
         }
      }
      Vec3 look = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      return look.lengthSqr() >= 1.0E-4 ? look.normalize() : Vec3.ZERO;
   }

   private static int hitForwardTargets(
      ServerLevel level,
      LivingEntity owner,
      Vec3 dir,
      boolean fiery,
      float damage,
      double knockback,
      double verticalKnockback,
      double distance,
      double inflate
   ) {
      AABB box = owner.getBoundingBox().move(dir.scale(distance * 0.55)).inflate(inflate, 0.35, inflate);
      Set<Integer> hit = new HashSet<>();
      int count = 0;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, target -> canHit(owner, target))) {
         if (!hit.add(target.getId())) {
            continue;
         }
         target.invulnerableTime = 0;
         if (owner instanceof ServerPlayer player) {
            target.hurt(player.damageSources().playerAttack(player), damage);
         } else {
            target.hurt(owner.damageSources().mobAttack(owner), damage);
         }
         target.invulnerableTime = 0;
         if (fiery) {
            target.igniteForSeconds(2.0F);
         }
         target.push(dir.x * knockback, verticalKnockback, dir.z * knockback);
         target.hurtMarked = true;
         target.hasImpulse = true;
         count++;
      }
      return count;
   }

   private static boolean canHit(LivingEntity owner, LivingEntity target) {
      if (target == owner || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      if (ServantMasterProtection.isProtectedMaster(owner, target)) {
         return false;
      }
      return !(owner instanceof ServantEntity servant) || !target.isAlliedTo(servant);
   }

   private static int breakForwardCube(
      ServerLevel level,
      LivingEntity owner,
      Vec3 dir,
      int limit,
      float hardnessCap
   ) {
      if (dir.lengthSqr() < 1.0E-4) {
         return 0;
      }
      Vec3 center = owner.position().add(dir.normalize().scale(1.65)).add(0.0, 0.9, 0.0);
      TerrainImpactProfile profile = new TerrainImpactProfile(TerrainImpactProfile.Tier.SMALL, 2.0,
         Math.min(TerrainImpactProfile.of(TerrainImpactProfile.Tier.SMALL).maximumHardness(), hardnessCap),
         Math.min(12, Math.max(1, limit)), 28);
      return TerrainImpactService.impact(level, owner, center, profile, TerrainImpactService.Shape.SURFACE_HEMISPHERE) ? 1 : 0;
   }

   private static void spawnCollisionFx(ServerLevel level, LivingEntity owner, Vec3 dir, boolean fiery, boolean blockHit, SoundSource source) {
      Vec3 fx = owner.position().add(dir.scale(1.15)).add(0.0, owner.getBbHeight() * 0.42, 0.0);
      level.sendParticles(ParticleTypes.CLOUD, fx.x, fx.y, fx.z, blockHit ? 12 : 6, 0.42, 0.25, 0.42, 0.06);
      level.sendParticles(fiery ? ParticleTypes.FLAME : ParticleTypes.CRIT, fx.x, fx.y + 0.1, fx.z, fiery ? 8 : 6, 0.28, 0.18, 0.28, 0.07);
      level.playSound(null, owner.blockPosition(), blockHit ? SoundEvents.ZOMBIE_ATTACK_IRON_DOOR : SoundEvents.PLAYER_ATTACK_KNOCKBACK, source, blockHit ? 0.68F : 0.55F, fiery ? 0.85F : 0.7F);
   }

   private static boolean isUnderSun(ServerLevel level, BlockPos pos) {
      long dayTime = level.getDayTime() % 24000L;
      return level.dimensionType().hasSkyLight()
         && dayTime >= 0L && dayTime < 12000L
         && !level.isRaining() && !level.isThundering()
         && level.canSeeSky(pos.above());
   }
}
