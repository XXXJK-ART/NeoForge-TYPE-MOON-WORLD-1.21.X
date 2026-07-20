package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class NpcScaleHelper {
   private static final String TAG_RANDOM_SCALE_INITIALIZED = "TypeMoonNpcRandomScaleV1";
   private static final String TAG_FIXED_SCALE_INITIALIZED = "TypeMoonNpcFixedScaleV1";
   private static final double MIN_RANDOM_SCALE = 0.7;
   private static final double MAX_RANDOM_SCALE = 1.0;

   private NpcScaleHelper() {
   }

   public static void ensureRandomScale(LivingEntity entity) {
      if (entity == null || entity.getPersistentData().getBoolean(TAG_RANDOM_SCALE_INITIALIZED)) {
         return;
      }
      double scale = MIN_RANDOM_SCALE + entity.getRandom().nextDouble() * (MAX_RANDOM_SCALE - MIN_RANDOM_SCALE);
      setScale(entity, scale);
      entity.getPersistentData().putBoolean(TAG_RANDOM_SCALE_INITIALIZED, true);
   }

   public static void ensureFixedScale(LivingEntity entity, double scale) {
      if (entity == null || entity.getPersistentData().getBoolean(TAG_FIXED_SCALE_INITIALIZED)) {
         return;
      }
      setScale(entity, scale);
      entity.getPersistentData().putBoolean(TAG_RANDOM_SCALE_INITIALIZED, true);
      entity.getPersistentData().putBoolean(TAG_FIXED_SCALE_INITIALIZED, true);
   }

   private static void setScale(LivingEntity entity, double scale) {
      AttributeInstance attribute = entity.getAttribute(Attributes.SCALE);
      if (attribute == null) {
         return;
      }
      attribute.setBaseValue(Mth.clamp(scale, MIN_RANDOM_SCALE, MAX_RANDOM_SCALE));
      entity.refreshDimensions();
   }
}
