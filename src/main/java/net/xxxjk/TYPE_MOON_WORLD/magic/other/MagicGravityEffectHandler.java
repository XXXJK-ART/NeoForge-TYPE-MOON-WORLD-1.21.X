package net.xxxjk.TYPE_MOON_WORLD.magic.other;

import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent.Post;
import net.xxxjk.TYPE_MOON_WORLD.entity.GravityShellEffectEntity;
import org.joml.Vector3f;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class MagicGravityEffectHandler {
   private static final String TAG_MODE = "TypeMoonGravityMode";
   private static final String TAG_UNTIL = "TypeMoonGravityUntil";
   private static final String TAG_STACKS = "TypeMoonGravityStacks";
   private static final String TAG_STACK_WINDOW_UNTIL = "TypeMoonGravityStackWindowUntil";
   private static final String TAG_CASTER_UUID = "TypeMoonGravityCasterUUID";
   private static final String TAG_LINKED_SLOW_UNTIL = "TypeMoonGravityLinkedSlowUntil";
   private static final String TAG_LINKED_SLOW_AMPLIFIER = "TypeMoonGravityLinkedSlowAmplifier";
   private static final String TAG_LINKED_SLOW_CASTER_UUID = "TypeMoonGravityLinkedSlowCasterUUID";
   private static final String TAG_SHELL_UUID = "TypeMoonGravityShellUUID";
   private static final int STACK_WINDOW_TICKS = 2;
   private static final int MAX_STACKS = 5;
   private static final int AURA_INTERVAL_TICKS = 6;
   private static final DustParticleOptions LIGHT_DUST = new DustParticleOptions(new Vector3f(0.4F, 0.95F, 1.0F), 1.1F);
   private static final DustParticleOptions HEAVY_DUST = new DustParticleOptions(new Vector3f(0.42F, 0.06F, 0.08F), 1.15F);
   private static final double HEAVY_EXTRA_FALL_ACCEL = 0.09;
   private static final double ULTRA_HEAVY_EXTRA_FALL_ACCEL = 0.16;

   public static void applyGravityState(LivingEntity target, int mode, long untilGameTime) {
      applyGravityState(target, mode, untilGameTime, null);
   }

   public static void applyGravityState(LivingEntity target, int mode, long untilGameTime, LivingEntity caster) {
      CompoundTag tag = target.getPersistentData();
      long now = target.level().getGameTime();
      int currentMode = tag.contains(TAG_MODE) ? tag.getInt(TAG_MODE) : 0;
      long currentUntil = tag.contains(TAG_UNTIL) ? tag.getLong(TAG_UNTIL) : 0L;
      int stacks = 1;
      if (currentMode == mode && now < currentUntil) {
         stacks = Math.max(1, tag.getInt(TAG_STACKS));
         long stackWindowUntil = tag.contains(TAG_STACK_WINDOW_UNTIL) ? tag.getLong(TAG_STACK_WINDOW_UNTIL) : 0L;
         if (now <= stackWindowUntil) {
            stacks = Math.min(MAX_STACKS, stacks + 1);
         } else if (stacks > 1) {
            stacks--;
         }

         untilGameTime = Math.max(untilGameTime, currentUntil);
      }

      tag.putInt(TAG_MODE, mode);
      tag.putLong(TAG_UNTIL, untilGameTime);
      tag.putInt(TAG_STACKS, stacks);
      tag.putLong(TAG_STACK_WINDOW_UNTIL, now + STACK_WINDOW_TICKS);
      writeCasterUuid(tag, TAG_CASTER_UUID, caster, target);
   }

   public static void applyLinkedSlow(LivingEntity target, int durationTicks, int amplifier, LivingEntity caster) {
      if (target != null) {
         CompoundTag tag = target.getPersistentData();
         long now = target.level().getGameTime();
         tag.putLong(TAG_LINKED_SLOW_UNTIL, now + Math.max(20, durationTicks));
         tag.putInt(TAG_LINKED_SLOW_AMPLIFIER, Math.max(0, amplifier));
         writeCasterUuid(tag, TAG_LINKED_SLOW_CASTER_UUID, caster, target);
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, Math.max(0, amplifier), false, false, true));
      }
   }

   public static void clearGravityState(LivingEntity target) {
      CompoundTag tag = target.getPersistentData();
      tag.remove(TAG_MODE);
      tag.remove(TAG_UNTIL);
      tag.remove(TAG_STACKS);
      tag.remove(TAG_STACK_WINDOW_UNTIL);
      tag.remove(TAG_CASTER_UUID);
      clearLinkedSlowState(target, true);
      target.removeEffect(MobEffects.JUMP);
      target.removeEffect(MobEffects.SLOW_FALLING);
      target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
   }

   public static int getCurrentMode(LivingEntity target) {
      CompoundTag tag = target.getPersistentData();
      if (tag.contains(TAG_MODE) && tag.contains(TAG_UNTIL)) {
         if (hasInvalidCaster(target, tag, TAG_CASTER_UUID)) {
            clearGravityState(target);
            return 0;
         }

         long now = target.level().getGameTime();
         long until = tag.getLong(TAG_UNTIL);
         if (now >= until) {
            clearGravityState(target);
            return 0;
         } else {
            int mode = tag.getInt(TAG_MODE);
            if (mode >= -2 && mode <= 2) {
               return mode;
            } else {
               clearGravityState(target);
               return 0;
            }
         }
      } else {
         return 0;
      }
   }

   public static int getCurrentStacks(LivingEntity target) {
      int mode = getCurrentMode(target);
      return mode == 0 ? 1 : Math.max(1, Math.min(MAX_STACKS, target.getPersistentData().getInt(TAG_STACKS)));
   }

   public static void playGravityCastFx(LivingEntity caster, LivingEntity target, int mode) {
      if (caster != null && target != null && target.level() instanceof ServerLevel serverLevel && mode != MagicGravity.MODE_NORMAL) {
         SoundSource soundSource = resolveSoundSource(caster);
         boolean selfCast = caster == target;
         if (selfCast) {
            spawnGravityBurst(serverLevel, target.position(), 0.6, 0.9, mode, true);
         } else {
            spawnCasterBurst(serverLevel, caster);
            spawnGravityBurst(serverLevel, target.position(), 0.8, 1.15, mode, false);
         }

         serverLevel.playSound(
            null, target.getX(), target.getY(), target.getZ(), resolveGravitySound(mode), soundSource, selfCast ? 0.8F : 0.95F, resolveGravityPitch(mode)
         );
      }
   }

   public static void playGravityNormalizeFx(LivingEntity caster, LivingEntity target) {
      if (caster != null && target != null && target.level() instanceof ServerLevel serverLevel) {
         boolean selfCast = caster == target;
         if (selfCast) {
            spawnNormalizeBurst(serverLevel, target.position(), 0.55, 1.0);
         } else {
            spawnCasterBurst(serverLevel, caster);
            spawnNormalizeBurst(serverLevel, target.position(), 0.75, 1.1);
         }

         serverLevel.playSound(
            null,
            target.getX(),
            target.getY(),
            target.getZ(),
            SoundEvents.AMETHYST_BLOCK_CHIME,
            resolveSoundSource(caster),
            selfCast ? 0.45F : 0.55F,
            1.08F
         );
      }
   }

   public static void emitGravityAura(LivingEntity target, int mode, int stacks) {
      if (target != null && mode != MagicGravity.MODE_NORMAL && target.level() instanceof ServerLevel serverLevel) {
         Vec3 center = target.position();
         double radius = 0.32 + 0.05 * Math.max(0, stacks - 1);
         int primary = Math.max(2, 3 + Math.max(0, stacks - 1));
         int secondary = Math.max(1, 1 + Math.max(0, stacks - 1) / 2);
         if (mode < 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1.0, center.z, primary, radius * 0.82, 0.34, radius * 0.82, 0.02);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.72, center.z, primary + 1, radius, 0.3, radius, 0.14);
            serverLevel.sendParticles(LIGHT_DUST, center.x, center.y + 0.82, center.z, secondary + 1, radius * 0.52, 0.18, radius * 0.52, 0.0);
         } else {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 0.68, center.z, primary + 2, radius, 0.34, radius, 0.003);
            serverLevel.sendParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR, center.x, center.y + 0.94, center.z, secondary + 2, radius * 0.56, 0.22, radius * 0.56, 0.01);
            serverLevel.sendParticles(HEAVY_DUST, center.x, center.y + 0.62, center.z, secondary + 1, radius * 0.48, 0.14, radius * 0.48, 0.0);
         }
      }
   }

   @SubscribeEvent
   public static void onEntityTick(Post event) {
      if (event.getEntity() instanceof LivingEntity living) {
         if (!living.level().isClientSide()) {
            tickLinkedSlow(living);
            int mode = getCurrentMode(living);
            if (mode != 0) {
               int stacks = getCurrentStacks(living);
               if (living.tickCount % AURA_INTERVAL_TICKS == 0) {
                  emitGravityAura(living, mode, stacks);
               }

               if (mode != -1 && mode != -2) {
                  interruptFallFlying(living);
                  living.removeEffect(MobEffects.SLOW_FALLING);
                  int slowAmplifier = (mode == 2 ? 1 : 0) + (stacks - 1);
                  living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, slowAmplifier, false, false, false));
                  if (!living.onGround() && !living.isInWaterOrBubble()) {
                     Vec3 motion = living.getDeltaMovement();
                     double stackScale = 1.0 + 0.28 * (stacks - 1);
                     double extraFallAccel = (mode == 2 ? ULTRA_HEAVY_EXTRA_FALL_ACCEL : HEAVY_EXTRA_FALL_ACCEL) * stackScale;
                     double minY = (mode == 2 ? -3.6 : -3.0) - 0.35 * (stacks - 1);
                     double newY = Math.max(minY, motion.y - extraFallAccel);
                     living.setDeltaMovement(motion.x, newY, motion.z);
                     living.hurtMarked = true;
                  }
               } else {
                  int jumpAmplifier = (mode == -2 ? 4 : 2) + (stacks - 1);
                  living.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, jumpAmplifier, false, false, false));
                  living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));
                  if (living.isFallFlying()) {
                     boostFallFlying(living, mode, stacks);
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLivingJump(LivingJumpEvent event) {
      LivingEntity living = event.getEntity();
      if (!living.level().isClientSide()) {
         int mode = getCurrentMode(living);
         if (mode == 1 || mode == 2) {
            int stacks = getCurrentStacks(living);
            Vec3 motion = living.getDeltaMovement();
            double jumpScale = mode == 2 ? 0.18 : 0.35;
            jumpScale = Math.max(0.06, jumpScale / (1.0 + 0.25 * (stacks - 1)));
            living.setDeltaMovement(motion.x, motion.y * jumpScale, motion.z);
         }
      }
   }

   @SubscribeEvent
   public static void onLivingFall(LivingFallEvent event) {
      LivingEntity living = event.getEntity();
      if (!living.level().isClientSide()) {
         int mode = getCurrentMode(living);
         if (mode == -1 || mode == -2) {
            event.setDistance(0.0F);
            event.setCanceled(true);
         } else if (mode == 1 || mode == 2) {
            int stacks = getCurrentStacks(living);
            float scale = mode == 2 ? 2.3F : 1.6F;
            scale *= (float)(1.0 + 0.2 * (stacks - 1));
            event.setDamageMultiplier(event.getDamageMultiplier() * scale);
         }
      }
   }

   private static void spawnCasterBurst(ServerLevel serverLevel, LivingEntity caster) {
      Vec3 center = caster.position();
      serverLevel.sendParticles(ParticleTypes.PORTAL, center.x, center.y + 1.0, center.z, 8, 0.24, 0.2, 0.24, 0.01);
      serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.88, center.z, 6, 0.18, 0.16, 0.18, 0.0);
   }

   private static void spawnGravityBurst(ServerLevel serverLevel, Vec3 center, double xzSpread, double ySpread, int mode, boolean selfCast) {
      if (mode < 0) {
         int shimmer = mode == MagicGravity.MODE_ULTRA_LIGHT ? 16 : 12;
         int sparkle = mode == MagicGravity.MODE_ULTRA_LIGHT ? 12 : 8;
         serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1.0, center.z, shimmer, xzSpread * 0.75, ySpread * 0.48, xzSpread * 0.75, 0.025);
         serverLevel.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.84, center.z, sparkle, xzSpread * 0.92, ySpread * 0.42, xzSpread * 0.92, 0.16);
         serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.64, center.z, 8, xzSpread * 0.72, ySpread * 0.24, xzSpread * 0.72, 0.0);
         serverLevel.sendParticles(LIGHT_DUST, center.x, center.y + 0.78, center.z, mode == MagicGravity.MODE_ULTRA_LIGHT ? 14 : 10, xzSpread * 0.58, ySpread * 0.3, xzSpread * 0.58, 0.0);
      } else {
         int pressure = mode == MagicGravity.MODE_ULTRA_HEAVY ? 18 : 13;
         int fallout = mode == MagicGravity.MODE_ULTRA_HEAVY ? 12 : 8;
         serverLevel.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 0.76, center.z, pressure, xzSpread, ySpread * 0.34, xzSpread, 0.004);
         serverLevel.sendParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR, center.x, center.y + 1.02, center.z, fallout, xzSpread * 0.64, ySpread * 0.28, xzSpread * 0.64, 0.01);
         serverLevel.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.52, center.z, 7, xzSpread * 0.55, ySpread * 0.16, xzSpread * 0.55, 0.01);
         serverLevel.sendParticles(HEAVY_DUST, center.x, center.y + 0.66, center.z, mode == MagicGravity.MODE_ULTRA_HEAVY ? 12 : 8, xzSpread * 0.56, ySpread * 0.22, xzSpread * 0.56, 0.0);
      }

      if (selfCast) {
         if (mode < 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1.08, center.z, 5, xzSpread * 0.36, 0.18, xzSpread * 0.36, 0.02);
         } else {
            serverLevel.sendParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR, center.x, center.y + 1.02, center.z, 4, xzSpread * 0.34, 0.14, xzSpread * 0.34, 0.01);
         }
      }
   }

   private static void spawnNormalizeBurst(ServerLevel serverLevel, Vec3 center, double xzSpread, double ySpread) {
      serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.82, center.z, 8, xzSpread, ySpread * 0.32, xzSpread, 0.0);
      serverLevel.sendParticles(ParticleTypes.PORTAL, center.x, center.y + 0.66, center.z, 5, xzSpread * 0.72, ySpread * 0.22, xzSpread * 0.72, 0.008);
   }

   private static SoundEvent resolveGravitySound(int mode) {
      return mode < 0 ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.BEACON_ACTIVATE;
   }

   private static float resolveGravityPitch(int mode) {
      return switch (mode) {
         case MagicGravity.MODE_ULTRA_LIGHT -> 1.2F;
         case MagicGravity.MODE_LIGHT -> 1.05F;
         case MagicGravity.MODE_ULTRA_HEAVY -> 0.45F;
         default -> 0.55F;
      };
   }

   private static SoundSource resolveSoundSource(LivingEntity caster) {
      return caster instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
   }

   private static void interruptFallFlying(LivingEntity living) {
      if (living instanceof Player player && player.isFallFlying()) {
         player.stopFallFlying();
         player.hurtMarked = true;
      }
   }

   private static void boostFallFlying(LivingEntity living, int mode, int stacks) {
      Vec3 motion = living.getDeltaMovement();
      Vec3 look = living.getLookAngle();
      Vec3 horizontalLook = new Vec3(look.x, 0.0, look.z);
      double stackScale = 1.0 + 0.18 * (stacks - 1);
      double forwardBoost = (mode == MagicGravity.MODE_ULTRA_LIGHT ? 0.05 : 0.03) * stackScale;
      double verticalLift = (mode == MagicGravity.MODE_ULTRA_LIGHT ? 0.018 : 0.01) * stackScale;
      double maxHorizontal = (mode == MagicGravity.MODE_ULTRA_LIGHT ? 2.6 : 2.1) + 0.16 * (stacks - 1);
      double maxUpward = mode == MagicGravity.MODE_ULTRA_LIGHT ? 0.16 : 0.1;
      Vec3 boosted = motion;
      if (horizontalLook.lengthSqr() > 1.0E-6) {
         boosted = boosted.add(horizontalLook.normalize().scale(forwardBoost));
      }

      double horizontalSpeed = Math.sqrt(boosted.x * boosted.x + boosted.z * boosted.z);
      if (horizontalSpeed > maxHorizontal) {
         double scale = maxHorizontal / horizontalSpeed;
         boosted = new Vec3(boosted.x * scale, boosted.y, boosted.z * scale);
      }

      living.setDeltaMovement(boosted.x, Math.min(maxUpward, boosted.y + verticalLift), boosted.z);
      living.hurtMarked = true;
   }

   private static void tickLinkedSlow(LivingEntity living) {
      CompoundTag tag = living.getPersistentData();
      if (tag.contains(TAG_LINKED_SLOW_UNTIL)) {
         if (hasInvalidCaster(living, tag, TAG_LINKED_SLOW_CASTER_UUID)) {
            clearLinkedSlowState(living, true);
            return;
         }

         long now = living.level().getGameTime();
         long until = tag.getLong(TAG_LINKED_SLOW_UNTIL);
         if (now >= until) {
            clearLinkedSlowState(living, true);
         } else {
            int amplifier = Math.max(0, tag.getInt(TAG_LINKED_SLOW_AMPLIFIER));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, amplifier, false, false, true));
         }
      }
   }

   private static void clearLinkedSlowState(LivingEntity living, boolean removeEffect) {
      if (living != null) {
         CompoundTag tag = living.getPersistentData();
         tag.remove(TAG_LINKED_SLOW_UNTIL);
         tag.remove(TAG_LINKED_SLOW_AMPLIFIER);
         tag.remove(TAG_LINKED_SLOW_CASTER_UUID);
         if (removeEffect) {
            living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
         }
      }
   }

   private static boolean hasInvalidCaster(LivingEntity target, CompoundTag tag, String key) {
      if (target == null || tag == null || key == null || key.isEmpty() || !tag.hasUUID(key) || !(target.level() instanceof ServerLevel serverLevel)) {
         return false;
      } else {
         UUID casterId = tag.getUUID(key);
         Entity caster = serverLevel.getEntity(casterId);
         return !(caster instanceof LivingEntity living) || !living.isAlive() || living.isRemoved();
      }
   }

   private static void writeCasterUuid(CompoundTag tag, String key, LivingEntity caster, LivingEntity target) {
      if (tag != null && key != null && !key.isEmpty()) {
         if (caster != null && caster != target) {
            tag.putUUID(key, caster.getUUID());
         } else {
            tag.remove(key);
         }
      }
   }
}
