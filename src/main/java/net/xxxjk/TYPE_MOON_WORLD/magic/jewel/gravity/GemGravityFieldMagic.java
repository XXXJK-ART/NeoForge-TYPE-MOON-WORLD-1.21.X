package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.gravity;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.GravityFieldShellEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravityEffectHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class GemGravityFieldMagic {
   private static final String TAG_IS_GRAVITY_FIELD_GEM = "TypeMoonIsGravityFieldGem";
   private static final String TAG_FIELD_RADIUS = "TypeMoonGravityFieldRadius";
   private static final String TAG_FIELD_DURATION = "TypeMoonGravityFieldDuration";
   private static final String TAG_HEAVY_DURATION = "TypeMoonGravityHeavyDuration";
   private static final String TAG_PARTICLE_COUNT = "TypeMoonGravityParticleCount";
   private static final String TAG_APPLY_SLOW = "TypeMoonGravityApplySlow";
   private static final String TAG_SLOW_DURATION = "TypeMoonGravitySlowDuration";
   private static final String TAG_SLOW_AMPLIFIER = "TypeMoonGravitySlowAmplifier";
   private static final String TAG_APPLY_DAMAGE = "TypeMoonGravityApplyDamage";
   private static final String TAG_DAMAGE_PER_PULSE = "TypeMoonGravityDamagePerPulse";
   private static final int PULSE_INTERVAL_TICKS = 10;

   private GemGravityFieldMagic() {
   }

   public static boolean throwGravityFieldProjectile(ServerPlayer player, ItemStack sourceGem) {
      if (player != null && !sourceGem.isEmpty()) {
         float qualityMultiplier = 1.0F;
         if (sourceGem.getItem() instanceof FullManaCarvedGemItem fullGem) {
            qualityMultiplier = fullGem.getQuality().getEffectMultiplier();
         }

         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         double proficiency = Math.max(vars.proficiency_gravity_magic, vars.proficiency_jewel_magic_release);
         float radius = (float)(2.8 + 2.2 * qualityMultiplier);
         int fieldDuration = 160 + (int)Math.round(proficiency * 2.5);
         int heavyDuration = 120 + (int)Math.round(proficiency * 3.0);
         int particleCount = Math.max(72, 72 + Math.round(30.0F * qualityMultiplier));
         float damagePerPulse = (float)(0.25 + 0.35 * Math.max(0.0, Math.min(1.0, proficiency / 100.0)));
         return throwConfiguredGravityFieldProjectile(
            player, sourceGem, radius, fieldDuration, heavyDuration, particleCount, false, 0, 0, true, damagePerPulse, 5
         );
      } else {
         return false;
      }
   }

   public static boolean throwBlackShardGravityProjectile(ServerPlayer player, ItemStack sourceGem) {
      if (player != null && !sourceGem.isEmpty()) {
         float qualityMultiplier = 1.0F;
         double manaAmount = 100.0;
         if (sourceGem.getItem() instanceof FullManaCarvedGemItem fullGem) {
            qualityMultiplier = fullGem.getQuality().getEffectMultiplier();
            manaAmount = Math.max(1.0, fullGem.getManaAmount(sourceGem));
         }

         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         double proficiency = Math.max(vars.proficiency_gravity_magic, vars.proficiency_jewel_magic_release);
         ItemStack projectileStack = createBlackShardGravityProjectileStack(sourceGem, proficiency, manaAmount, qualityMultiplier);
         if (projectileStack.isEmpty()) {
            return false;
         } else {
            RubyProjectileEntity projectile = new RubyProjectileEntity(player.level(), player);
            projectile.setGemType(6);
            projectile.setItem(projectileStack);
            Vec3 direction = EntityUtils.getAutoAimDirection(player, 48.0, 55.0);
            projectile.shoot(direction.x, direction.y, direction.z, 1.5F, 0.6F);
            player.level().addFreshEntity(projectile);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.8F, 0.75F);
            return true;
         }
      } else {
         return false;
      }
   }

   public static ItemStack createBlackShardGravityProjectileStack(ItemStack sourceGem, double proficiency, double manaAmount, float qualityMultiplier) {
      if (sourceGem != null && !sourceGem.isEmpty()) {
         double manaNormalized = Math.max(0.0, Math.min(1.0, (manaAmount - 50.0) / 150.0));
         float radius = (float)(1.9 + 1.3 * qualityMultiplier + 0.9 * manaNormalized);
         int fieldDuration = 90 + (int)Math.round(proficiency * 1.4 + manaNormalized * 30.0);
         int heavyDuration = 100 + (int)Math.round(proficiency * 1.6 + manaNormalized * 40.0);
         int particleCount = Math.max(56, 56 + (int)Math.round(24.0F * qualityMultiplier + manaNormalized * 18.0));
         int slowDuration = Math.max(80, heavyDuration / 2);
         int slowAmplifier = manaNormalized >= 0.66 ? 3 : 2;
         return createConfiguredGravityFieldProjectileStack(
            sourceGem, radius, fieldDuration, heavyDuration, particleCount, true, slowDuration, slowAmplifier, false, 0.0F
         );
      } else {
         return ItemStack.EMPTY;
      }
   }

   private static boolean throwConfiguredGravityFieldProjectile(
      ServerPlayer player,
      ItemStack sourceGem,
      float radius,
      int fieldDuration,
      int heavyDuration,
      int particleCount,
      boolean applySlow,
      int slowDuration,
      int slowAmplifier,
      boolean applyDamage,
      float damagePerPulse,
      int projectileGemType
   ) {
      if (player != null && !sourceGem.isEmpty()) {
         ItemStack projectileStack = createConfiguredGravityFieldProjectileStack(
            sourceGem, radius, fieldDuration, heavyDuration, particleCount, applySlow, slowDuration, slowAmplifier, applyDamage, damagePerPulse
         );
         RubyProjectileEntity projectile = new RubyProjectileEntity(player.level(), player);
         projectile.setGemType(projectileGemType);
         projectile.setItem(projectileStack);
         Vec3 direction = EntityUtils.getAutoAimDirection(player, 48.0, 55.0);
         projectile.shoot(direction.x, direction.y, direction.z, 1.5F, 0.6F);
         player.level().addFreshEntity(projectile);
         player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.8F, 0.75F);
         return true;
      } else {
         return false;
      }
   }

   private static ItemStack createConfiguredGravityFieldProjectileStack(
      ItemStack sourceGem,
      float radius,
      int fieldDuration,
      int heavyDuration,
      int particleCount,
      boolean applySlow,
      int slowDuration,
      int slowAmplifier,
      boolean applyDamage,
      float damagePerPulse
   ) {
      ItemStack projectileStack = sourceGem.copy();
      projectileStack.setCount(1);
      CompoundTag tag = ((CustomData)projectileStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      tag.putBoolean(TAG_IS_GRAVITY_FIELD_GEM, true);
      tag.putFloat(TAG_FIELD_RADIUS, radius);
      tag.putInt(TAG_FIELD_DURATION, fieldDuration);
      tag.putInt(TAG_HEAVY_DURATION, heavyDuration);
      tag.putInt(TAG_PARTICLE_COUNT, particleCount);
      tag.putBoolean(TAG_APPLY_SLOW, applySlow);
      if (applySlow) {
         tag.putInt(TAG_SLOW_DURATION, Math.max(40, slowDuration));
         tag.putInt(TAG_SLOW_AMPLIFIER, Math.max(0, slowAmplifier));
      } else {
         tag.remove(TAG_SLOW_DURATION);
         tag.remove(TAG_SLOW_AMPLIFIER);
      }

      tag.putBoolean(TAG_APPLY_DAMAGE, applyDamage);
      if (applyDamage) {
         tag.putFloat(TAG_DAMAGE_PER_PULSE, Math.max(0.0F, damagePerPulse));
      } else {
         tag.remove(TAG_DAMAGE_PER_PULSE);
      }

      projectileStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return projectileStack;
   }

   public static boolean tryHandleProjectileImpact(RubyProjectileEntity projectile, ItemStack stack) {
      if (projectile != null && !stack.isEmpty()) {
         CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
         if (!tag.getBoolean(TAG_IS_GRAVITY_FIELD_GEM)) {
            return false;
         } else if (projectile.level() instanceof ServerLevel serverLevel) {
            float radius = tag.contains(TAG_FIELD_RADIUS) ? tag.getFloat(TAG_FIELD_RADIUS) : 4.6F;
            int fieldDuration = tag.contains(TAG_FIELD_DURATION) ? tag.getInt(TAG_FIELD_DURATION) : 220;
            int heavyDuration = tag.contains(TAG_HEAVY_DURATION) ? tag.getInt(TAG_HEAVY_DURATION) : 180;
            int particleCount = tag.contains(TAG_PARTICLE_COUNT) ? tag.getInt(TAG_PARTICLE_COUNT) : 90;
            boolean applySlow = tag.contains(TAG_APPLY_SLOW) && tag.getBoolean(TAG_APPLY_SLOW);
            int slowDuration = tag.contains(TAG_SLOW_DURATION) ? tag.getInt(TAG_SLOW_DURATION) : Math.max(100, heavyDuration / 2);
            int slowAmplifier = tag.contains(TAG_SLOW_AMPLIFIER) ? tag.getInt(TAG_SLOW_AMPLIFIER) : 4;
            boolean applyDamage = tag.contains(TAG_APPLY_DAMAGE) && tag.getBoolean(TAG_APPLY_DAMAGE);
            float damagePerPulse = tag.contains(TAG_DAMAGE_PER_PULSE) ? tag.getFloat(TAG_DAMAGE_PER_PULSE) : 0.0F;
            Vec3 gameplayCenter = projectile.position();
            if (applyDamage) {
               compressGroundOnce(serverLevel, gameplayCenter, radius);
            }

            double visualAnchorY = resolveGravityFieldVisualAnchorY(serverLevel, gameplayCenter, radius);

            spawnGravityField(
               serverLevel,
               gameplayCenter,
               visualAnchorY,
               projectile.getOwner() instanceof LivingEntity owner ? owner : null,
               radius,
               fieldDuration,
               heavyDuration,
               particleCount,
               applySlow,
               slowDuration,
               slowAmplifier,
               applyDamage,
               damagePerPulse
            );
            serverLevel.addFreshEntity(
               new GravityFieldShellEffectEntity(serverLevel, gameplayCenter.x, visualAnchorY + 0.02, gameplayCenter.z, radius * 0.74F, radius * 0.64F, 0.74F, heavyDuration)
            );
            serverLevel.sendParticles(
               ParticleTypes.SQUID_INK,
               gameplayCenter.x,
               visualAnchorY + 0.78,
               gameplayCenter.z,
               Math.max(12, particleCount / 3),
               radius * 0.28,
               0.22,
               radius * 0.28,
               0.002
            );
            serverLevel.sendParticles(
               ParticleTypes.PORTAL,
               gameplayCenter.x,
               visualAnchorY + 0.24,
               gameplayCenter.z,
               Math.max(8, particleCount / 6),
               radius * 0.18,
               0.12,
               radius * 0.18,
               0.008
            );
            serverLevel.sendParticles(
               ParticleTypes.REVERSE_PORTAL,
               gameplayCenter.x,
               visualAnchorY + 0.42,
               gameplayCenter.z,
               Math.max(6, particleCount / 8),
               radius * 0.16,
               0.12,
               radius * 0.16,
               0.0
            );
            serverLevel.playSound(
               null, gameplayCenter.x, gameplayCenter.y, gameplayCenter.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.85F, 0.55F
            );
            return true;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static void compressGroundOnce(ServerLevel level, Vec3 center, float radius) {
      int minX = (int)Math.floor(center.x - radius);
      int maxX = (int)Math.ceil(center.x + radius);
      int minZ = (int)Math.floor(center.z - radius);
      int maxZ = (int)Math.ceil(center.z + radius);
      double radiusSq = radius * radius;
      int minY = level.getMinBuildHeight();

      for (int x = minX; x <= maxX; x++) {
         double dx = x + 0.5 - center.x;

         for (int z = minZ; z <= maxZ; z++) {
            double dz = z + 0.5 - center.z;
            if (!(dx * dx + dz * dz > radiusSq)) {
               int topY = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
               if (topY > minY + 1) {
                  BlockPos topPos = new BlockPos(x, topY, z);
                  BlockState topState = level.getBlockState(topPos);
                  if (!topState.isAir()
                     && topState.blocksMotion()
                     && topState.getFluidState().isEmpty()
                     && !topState.hasBlockEntity()
                     && !(topState.getDestroySpeed(level, topPos) < 0.0F)) {
                     BlockPos belowPos = topPos.below();
                     BlockState belowState = level.getBlockState(belowPos);
                     if (!belowState.isAir() && belowState.blocksMotion() && belowState.getFluidState().isEmpty()) {
                        level.setBlock(topPos, Blocks.AIR.defaultBlockState(), 3);
                     }
                  }
               }
            }
         }
      }
   }

   private static void spawnGravityField(
      ServerLevel level,
      Vec3 gameplayCenter,
      double visualAnchorY,
      LivingEntity caster,
      float radius,
      int fieldDuration,
      int heavyDuration,
      int particleCount,
      boolean applySlow,
      int slowDuration,
      int slowAmplifier,
      boolean applyDamage,
      float damagePerPulse
   ) {
      int pulses = Math.max(1, fieldDuration / PULSE_INTERVAL_TICKS);

      for (int i = 0; i <= pulses; i++) {
         int pulseIndex = i;
         int delay = i * PULSE_INTERVAL_TICKS;
         TYPE_MOON_WORLD.queueServerWork(
            delay,
            () -> {
               if (level.getServer() != null) {
                  double progress = (double)pulseIndex / Math.max(1, pulses);
                  double wave = Math.sin(pulseIndex * 0.72) * 0.12;
                  double topY = visualAnchorY + 1.35 - progress * 0.95 + wave;
                  double midY = visualAnchorY + 0.72 - progress * 0.45 + wave * 0.45;
                  double lowY = visualAnchorY + 0.16 + wave * 0.18;
                  double spread = radius * (0.46 - progress * 0.18);
                  int blackParticles = Math.max(18, (int)(particleCount * 0.55));
                  int enderParticles = Math.max(14, (int)(particleCount * 0.3));
                  int pressureParticles = Math.max(10, (int)(particleCount * 0.22));
                  level.sendParticles(ParticleTypes.SQUID_INK, gameplayCenter.x, topY, gameplayCenter.z, blackParticles, spread, 0.45, spread, 0.004);
                  level.sendParticles(
                     ParticleTypes.SQUID_INK, gameplayCenter.x, midY, gameplayCenter.z, blackParticles / 2, spread * 0.75, 0.22, spread * 0.75, 0.003
                  );
                  level.sendParticles(
                     ParticleTypes.PORTAL, gameplayCenter.x, lowY, gameplayCenter.z, enderParticles, spread * 0.62, 0.16, spread * 0.62, 0.012
                  );
                  level.sendParticles(
                     ParticleTypes.REVERSE_PORTAL, gameplayCenter.x, midY, gameplayCenter.z, enderParticles / 2, spread * 0.48, 0.22, spread * 0.48, 0.0
                  );
                  level.sendParticles(
                     ParticleTypes.FALLING_OBSIDIAN_TEAR,
                     gameplayCenter.x,
                     topY + 0.2,
                     gameplayCenter.z,
                     pressureParticles,
                     spread * 0.4,
                     0.12,
                     spread * 0.4,
                     0.01
                  );
                  if (caster != null && (!caster.isAlive() || caster.isRemoved())) {
                     return;
                  }

                  AABB area = new AABB(
                     gameplayCenter.x - radius, gameplayCenter.y - 1.0, gameplayCenter.z - radius, gameplayCenter.x + radius, gameplayCenter.y + 2.0, gameplayCenter.z + radius
                  );
                  List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive);
                  long until = level.getGameTime() + heavyDuration;

                  for (LivingEntity target : targets) {
                     if ((caster == null || target != caster) && !(target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
                        MagicGravityEffectHandler.applyGravityState(target, 2, until, caster);
                        if (applySlow) {
                           MagicGravityEffectHandler.applyLinkedSlow(target, Math.max(40, slowDuration), Math.max(0, slowAmplifier), caster);
                        }

                        if (applyDamage && damagePerPulse > 0.0F) {
                           target.hurt(level.damageSources().magic(), damagePerPulse);
                        }
                     }
                  }
               }
            }
         );
      }
   }

   private static double resolveGravityFieldVisualAnchorY(ServerLevel level, Vec3 center, float radius) {
      double sampleOffset = radius * 0.35;
      double[] samples = new double[]{
         sampleGroundY(level, center.x, center.z, center.y),
         sampleGroundY(level, center.x + sampleOffset, center.z, center.y),
         sampleGroundY(level, center.x - sampleOffset, center.z, center.y),
         sampleGroundY(level, center.x, center.z + sampleOffset, center.y),
         sampleGroundY(level, center.x, center.z - sampleOffset, center.y)
      };
      Arrays.sort(samples);
      return samples[samples.length / 2];
   }

   private static double sampleGroundY(ServerLevel level, double x, double z, double fallbackY) {
      int sampledHeight = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
      return sampledHeight <= level.getMinBuildHeight() ? fallbackY : (double)sampledHeight;
   }
}
