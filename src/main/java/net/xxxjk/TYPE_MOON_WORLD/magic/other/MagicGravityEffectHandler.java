package net.xxxjk.TYPE_MOON_WORLD.magic.other;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent.Post;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class MagicGravityEffectHandler {
   private static final String TAG_MODE = "TypeMoonGravityMode";
   private static final String TAG_UNTIL = "TypeMoonGravityUntil";
   private static final String TAG_STACKS = "TypeMoonGravityStacks";
   private static final String TAG_STACK_WINDOW_UNTIL = "TypeMoonGravityStackWindowUntil";
   private static final int STACK_WINDOW_TICKS = 2;
   private static final int MAX_STACKS = 5;
   private static final double HEAVY_EXTRA_FALL_ACCEL = 0.09;
   private static final double ULTRA_HEAVY_EXTRA_FALL_ACCEL = 0.16;

   public static void applyGravityState(LivingEntity target, int mode, long untilGameTime) {
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
   }

   public static void clearGravityState(LivingEntity target) {
      CompoundTag tag = target.getPersistentData();
      tag.remove(TAG_MODE);
      tag.remove(TAG_UNTIL);
      tag.remove(TAG_STACKS);
      tag.remove(TAG_STACK_WINDOW_UNTIL);
   }

   public static int getCurrentMode(LivingEntity target) {
      CompoundTag tag = target.getPersistentData();
      if (tag.contains(TAG_MODE) && tag.contains(TAG_UNTIL)) {
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

   @SubscribeEvent
   public static void onEntityTick(Post event) {
      if (event.getEntity() instanceof LivingEntity living) {
         if (!living.level().isClientSide()) {
            int mode = getCurrentMode(living);
            if (mode != 0) {
               int stacks = getCurrentStacks(living);
               if (mode != -1 && mode != -2) {
                  living.removeEffect(MobEffects.SLOW_FALLING);
                  int slowAmplifier = (mode == 2 ? 1 : 0) + (stacks - 1);
                  living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, slowAmplifier, false, false, false));
                  if (!living.onGround() && !living.isInWaterOrBubble() && !living.isFallFlying()) {
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
}
