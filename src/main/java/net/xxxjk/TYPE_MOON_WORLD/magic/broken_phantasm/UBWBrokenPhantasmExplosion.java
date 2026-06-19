package net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

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
            float totalDamage = Mth.clamp(400.0F + damagePower * 18.0F * clampedScale, 400.0F, 600.0F);
            DamageSource explosionSource = level.damageSources().explosion(source, owner);
            Set<Integer> damagedEntities = new HashSet<>();
            VFXServerEffects.spawn(serverLevel, "broken_phantasm_explosion", pos, 128.0);
            spawnShellEffects(serverLevel, pos, damageRadius);
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
                  processWave(serverLevel, pos, currentRadius, previousRadius, explosionSource, owner, damagedEntities, totalDamage);
               });
            }
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

   private static void processWave(
      ServerLevel level,
      Vec3 center,
      double currentRadius,
      double previousRadius,
      DamageSource source,
      Entity owner,
      Set<Integer> damagedEntities,
      float damage
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
      breakLowHardnessTerrain(level, center, currentRadius, previousRadius);
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

   private static void breakLowHardnessTerrain(ServerLevel level, Vec3 center, double currentRadius, double previousRadius) {
      int rInt = (int)Math.ceil(currentRadius);
      int broken = 0;
      int maxBroken = 20000;
      for (int x = -rInt; x <= rInt; x++) {
         for (int y = -rInt; y <= rInt; y++) {
            for (int z = -rInt; z <= rInt; z++) {
               double distSqr = x * x + y * y + z * z;
               if (distSqr > currentRadius * currentRadius || distSqr <= previousRadius * previousRadius) {
                  continue;
               }
               BlockPos blockPos = BlockPos.containing(center.x + x, center.y + y, center.z + z);
               BlockState state = level.getBlockState(blockPos);
               float hardness = state.getDestroySpeed(level, blockPos);
               if (state.isAir()
                  || state.is(Blocks.BEDROCK)
                  || hardness < 0.0F
                  || hardness > 32.0F
                  || state.getExplosionResistance(level, blockPos, null) >= 1200.0F) {
                  continue;
               }
               level.removeBlock(blockPos, false);
               broken++;
               if (broken >= maxBroken) {
                  return;
               }
               if ((broken & 1) == 0) {
                  level.sendParticles(ParticleTypes.CLOUD, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 4, 0.24, 0.24, 0.24, 0.025);
               }
            }
         }
      }
   }
}
