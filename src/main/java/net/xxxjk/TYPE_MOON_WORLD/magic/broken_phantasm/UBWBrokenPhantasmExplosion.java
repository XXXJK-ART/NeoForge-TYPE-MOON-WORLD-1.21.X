package net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ExpandingRingEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;

public class UBWBrokenPhantasmExplosion {
   public static void explode(Level level, Entity source, Entity owner, ItemStack stack, Vec3 pos) {
      explode(level, source, owner, stack, pos, 1.0F);
   }

   public static void explode(Level level, Entity source, Entity owner, ItemStack stack, Vec3 pos, float scale) {
      if (!level.isClientSide) {
         double cost = MagicBrokenPhantasm.calculateCost(stack, false);
         float explosionPower = (float)(cost / 20.0);
         if (explosionPower < 2.0F) {
            explosionPower = 2.0F;
         }

         float damagePower = explosionPower;
         float radiusPower = Math.min(explosionPower, 6.0F);
         if (radiusPower < 3.0F) {
            radiusPower = 3.0F;
         }

         if (level instanceof ServerLevel serverLevel) {
            float clampedScale = Mth.clamp(scale, 0.35F, 2.0F);
            double damageRadius = Mth.clamp(radiusPower * 4.2 * clampedScale, 8.0, 25.0);
            float totalDamage = Mth.clamp(80.0F + damagePower * 14.0F * clampedScale, 40.0F, 600.0F);
            boolean emiyaDistanceFalloff = isEmiyaBrokenPhantasmOwner(owner);
            DamageSource explosionSource = level.damageSources().explosion(source, owner);
            Set<Integer> damagedEntities = new HashSet<>();
            VFXServerEffects.spawn(serverLevel, "broken_phantasm_explosion", pos, 128.0);
            spawnShellEffects(serverLevel, pos, damageRadius);
            queueTerrainFromCenter(serverLevel, pos, damageRadius, 32.0F, damageRadius >= 22.0 ? 80 : 45);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 5, 0.35, 0.35, 0.35, 0.0);
            serverLevel.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 7, 0.14, 0.14, 0.14, 0.0);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + 0.25, pos.z, 72, 1.45, 0.55, 1.45, 0.07);
            serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.2, pos.z, 96, 1.35, 0.55, 1.35, 0.1);
            serverLevel.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.5, pos.z, 56, 1.1, 0.75, 1.1, 0.16);
            serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.4F, 0.52F);
            serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 1.45F, 0.72F);
            int waveCount = Mth.clamp((int)Math.ceil(damageRadius * 1.65), 12, 26);
            double waveStep = damageRadius / waveCount;
            for (int wave = 1; wave <= waveCount; wave++) {
               final int waveIndex = wave;
               TYPE_MOON_WORLD.queueServerWork(waveIndex * 2, () -> {
                  double previousRadius = Math.max(0.0, (waveIndex - 1) * waveStep);
                  double currentRadius = waveIndex * waveStep;
                  processWave(serverLevel, pos, currentRadius, previousRadius, explosionSource, owner,
                     damagedEntities, totalDamage, damageRadius, emiyaDistanceFalloff);
               });
            }
         }
      }
   }

   public static void explodePlainNoTerrain(Level level, Entity source, Entity owner, Vec3 pos) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return;
      }

      double radius = 4.0;
      DamageSource explosionSource = level.damageSources().explosion(source, owner);
      serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 1.0F);
      serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 2, 0.35, 0.18, 0.35, 0.0);

      for (LivingEntity living : serverLevel.getEntitiesOfClass(
         LivingEntity.class,
         new AABB(pos, pos).inflate(radius),
         e -> e.isAlive() && e != owner && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         double distance = Math.sqrt(living.distanceToSqr(pos.x, pos.y, pos.z));
         if (distance > radius) {
            continue;
         }
         float damage = 5.0F + serverLevel.random.nextFloat() * 15.0F;
         living.invulnerableTime = 0;
         living.hurt(explosionSource, damage);
         living.invulnerableTime = 0;
         Vec3 push = living.position().subtract(pos).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() > 1.0E-4) {
            push = push.normalize();
            living.push(push.x * 0.35, 0.12, push.z * 0.35);
            living.hurtMarked = true;
         }
      }
   }

   private static void spawnShellEffects(ServerLevel level, Vec3 center, double radius) {
      float outerRadius = (float)radius;
      int ironWhite = 0xF1F4F6;
      int emberRed = 0xC73B2E;
      level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.08, center.z, 0.2F, outerRadius, 0.34F, 20, ironWhite, 0.85F, 0.0F));
      level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.16, center.z, 0.16F, outerRadius * 0.72F, 0.22F, 18, emberRed, 0.72F, 0.01F));
      level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.1, center.z, 0.18F, outerRadius, 0.18F, 18, emberRed, 0.48F, 0.0F, 90.0F, 0.0F));
      level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.1, center.z, 0.18F, outerRadius, 0.18F, 18, ironWhite, 0.42F, 0.0F, 90.0F, 90.0F));
   }

   private static void queueTerrainFromCenter(ServerLevel level, Vec3 center, double radius, float maxHardness, int targetTicks) {
      int waveCount = Mth.clamp((int)Math.ceil(radius), 6, 28);
      double waveStep = radius / waveCount;
      for (int wave = 1; wave <= waveCount; wave++) {
         final int waveIndex = wave;
         TYPE_MOON_WORLD.queueServerWork(waveIndex, () -> {
            double previousRadius = Math.max(0.0, (waveIndex - 1) * waveStep);
            double currentRadius = waveIndex * waveStep;
            DeferredTerrainDestruction.queueShell(level, center, currentRadius, previousRadius, Math.max(8, targetTicks / waveCount), (serverLevel, pos, distanceSqr, shellRadius, origin) -> {
               BlockState state = serverLevel.getBlockState(pos);
               float hardness = state.getDestroySpeed(serverLevel, pos);
               return !state.isAir()
                  && hardness >= 0.0F
                  && hardness <= maxHardness
                  && !state.is(Blocks.BEDROCK)
                  && state.getExplosionResistance(serverLevel, pos, null) < 1200.0F;
            }, null);
         });
      }
   }

   private static void processWave(
      ServerLevel level,
      Vec3 center,
      double currentRadius,
      double previousRadius,
      DamageSource source,
      Entity owner,
      Set<Integer> damagedEntities,
      float damage,
      double maximumRadius,
      boolean emiyaDistanceFalloff
   ) {
      int ringCount = Math.max(24, (int)(currentRadius * 9.0));
      for (int i = 0; i < ringCount; i++) {
         double angle = Math.PI * 2.0 * i / ringCount;
         double px = center.x + Math.cos(angle) * currentRadius;
         double pz = center.z + Math.sin(angle) * currentRadius;
         level.sendParticles(ParticleTypes.EXPLOSION, px, center.y + 0.25, pz, 1, 0.12, 0.12, 0.12, 0.0);
         if ((i & 1) == 0) {
            level.sendParticles(ParticleTypes.CRIT, px, center.y + 0.35, pz, 2, 0.15, 0.15, 0.15, 0.025);
         }
      }
      if (Math.ceil(currentRadius) % 3 == 0) {
         level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.35F, 0.68F);
      }
      AABB damageBox = new AABB(center, center).inflate(currentRadius);
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         damageBox,
         e -> e.isAlive() && e != owner && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         double dist = Math.sqrt(living.distanceToSqr(center.x, center.y, center.z));
         if (dist > currentRadius || dist <= previousRadius || !damagedEntities.add(living.getId())) {
            continue;
         }
         living.invulnerableTime = 0;
         float finalDamage = MagicResistanceHelper.applyNoblePhantasmMagicResistance(living, damage);
         finalDamage = HeraclesGodHandHelper.applyAntiHeraclesNoblePhantasmSpecialAttack(living, finalDamage);
         if (emiyaDistanceFalloff) {
            finalDamage = Math.min(finalDamage, emiyaDamageAtDistance(damage, dist, maximumRadius));
         }
         living.hurt(source, finalDamage);
         living.invulnerableTime = 0;
         Vec3 push = living.position().subtract(center);
         double horizontal = Math.sqrt(push.x * push.x + push.z * push.z);
         if (horizontal > 1.0E-4) {
            living.push(push.x / horizontal * 0.85, 0.32, push.z / horizontal * 0.85);
            living.hurtMarked = true;
         }
      }
   }

   static float emiyaDamageAtDistance(float centerDamage, double distance, double radius) {
      float safeCenter = Math.max(0.0F, centerDamage);
      float outerDamage = Math.min(80.0F, safeCenter);
      float progress = radius <= 0.0 ? 1.0F : Mth.clamp((float)(distance / radius), 0.0F, 1.0F);
      return Mth.lerp(progress, safeCenter, outerDamage);
   }

   private static boolean isEmiyaBrokenPhantasmOwner(Entity owner) {
      if (owner instanceof EmiyaArcherEntity) return true;
      if (!(owner instanceof ServerPlayer player)) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "emiya_archer".equals(vars.servant_card_id);
   }

}
