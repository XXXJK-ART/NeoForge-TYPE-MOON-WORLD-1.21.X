package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class NpcScaleHelper {
   public static final double DEFAULT_RANDOM_SCALE = 0.85;
   private static final String TAG_RANDOM_SCALE_INITIALIZED = "TypeMoonNpcRandomScaleV1";
   private static final String TAG_FIXED_SCALE_INITIALIZED = "TypeMoonNpcFixedScaleV1";
   private static final double MIN_RANDOM_SCALE = 0.7;
   private static final double MAX_RANDOM_SCALE = 1.0;
   private static final double MIN_SUPPORTED_SCALE = 0.0625;
   private static final double MAX_SUPPORTED_SCALE = 16.0;

   private NpcScaleHelper() {
   }

   public static void ensureRandomScale(LivingEntity entity) {
      ensureRandomScale(entity, MIN_RANDOM_SCALE, MAX_RANDOM_SCALE);
   }

   public static void ensureRandomScale(LivingEntity entity, double minimum, double maximum) {
      if (entity == null || entity.getPersistentData().getBoolean(TAG_RANDOM_SCALE_INITIALIZED)) {
         return;
      }
      double min = Mth.clamp(Math.min(minimum, maximum), MIN_SUPPORTED_SCALE, MAX_SUPPORTED_SCALE);
      double max = Mth.clamp(Math.max(minimum, maximum), min, MAX_SUPPORTED_SCALE);
      double scale = min + entity.getRandom().nextDouble() * (max - min);
      setScale(entity, scale, min, max);
      entity.getPersistentData().putBoolean(TAG_RANDOM_SCALE_INITIALIZED, true);
   }

   public static void setInheritedScale(LivingEntity entity, double scale) {
      if (entity == null) {
         return;
      }
      setScale(entity, scale, MIN_SUPPORTED_SCALE, MAX_SUPPORTED_SCALE);
      entity.getPersistentData().putBoolean(TAG_RANDOM_SCALE_INITIALIZED, true);
   }

   public static void ensureFixedScale(LivingEntity entity, double scale) {
      if (entity == null || entity.getPersistentData().getBoolean(TAG_FIXED_SCALE_INITIALIZED)) {
         return;
      }
      setScale(entity, scale, MIN_RANDOM_SCALE, MAX_RANDOM_SCALE);
      entity.getPersistentData().putBoolean(TAG_RANDOM_SCALE_INITIALIZED, true);
      entity.getPersistentData().putBoolean(TAG_FIXED_SCALE_INITIALIZED, true);
   }

   private static void setScale(LivingEntity entity, double scale, double minimum, double maximum) {
      AttributeInstance attribute = entity.getAttribute(Attributes.SCALE);
      if (attribute == null) {
         return;
      }
      attribute.setBaseValue(Mth.clamp(scale, minimum, maximum));
      entity.refreshDimensions();
   }
}
