package net.xxxjk.TYPE_MOON_WORLD.vfx;

import java.util.Locale;
import net.minecraft.util.Mth;

public enum Easing {
   LINEAR,
   EASE_IN_SINE,
   EASE_OUT_SINE,
   EASE_IN_OUT_SINE,
   EASE_IN_QUAD,
   EASE_OUT_QUAD,
   EASE_IN_OUT_QUAD,
   EASE_IN_CUBIC,
   EASE_OUT_CUBIC,
   EASE_IN_OUT_CUBIC,
   EASE_IN_QUART,
   EASE_OUT_QUART,
   EASE_IN_OUT_QUART,
   EASE_IN_QUINT,
   EASE_OUT_QUINT,
   EASE_IN_OUT_QUINT,
   EASE_IN_EXPO,
   EASE_OUT_EXPO,
   EASE_IN_OUT_EXPO,
   EASE_IN_BACK,
   EASE_OUT_BACK,
   EASE_IN_OUT_BACK,
   EASE_IN_ELASTIC,
   EASE_OUT_ELASTIC,
   EASE_IN_OUT_ELASTIC;

   public float apply(float input) {
      float t = Mth.clamp(input, 0.0F, 1.0F);
      return switch (this) {
         case LINEAR -> t;
         case EASE_IN_SINE -> 1.0F - Mth.cos((float)(t * Math.PI * 0.5));
         case EASE_OUT_SINE -> Mth.sin((float)(t * Math.PI * 0.5));
         case EASE_IN_OUT_SINE -> -(Mth.cos((float)(Math.PI * t)) - 1.0F) * 0.5F;
         case EASE_IN_QUAD -> t * t;
         case EASE_OUT_QUAD -> 1.0F - (1.0F - t) * (1.0F - t);
         case EASE_IN_OUT_QUAD -> t < 0.5F ? 2.0F * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 2.0) * 0.5F;
         case EASE_IN_CUBIC -> t * t * t;
         case EASE_OUT_CUBIC -> 1.0F - (float)Math.pow(1.0F - t, 3.0);
         case EASE_IN_OUT_CUBIC -> t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0) * 0.5F;
         case EASE_IN_QUART -> t * t * t * t;
         case EASE_OUT_QUART -> 1.0F - (float)Math.pow(1.0F - t, 4.0);
         case EASE_IN_OUT_QUART -> t < 0.5F ? 8.0F * t * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 4.0) * 0.5F;
         case EASE_IN_QUINT -> t * t * t * t * t;
         case EASE_OUT_QUINT -> 1.0F - (float)Math.pow(1.0F - t, 5.0);
         case EASE_IN_OUT_QUINT -> t < 0.5F ? 16.0F * t * t * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 5.0) * 0.5F;
         case EASE_IN_EXPO -> t == 0.0F ? 0.0F : (float)Math.pow(2.0, 10.0F * t - 10.0F);
         case EASE_OUT_EXPO -> t == 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0F * t);
         case EASE_IN_OUT_EXPO -> easeInOutExpo(t);
         case EASE_IN_BACK -> 2.70158F * t * t * t - 1.70158F * t * t;
         case EASE_OUT_BACK -> 1.0F + 2.70158F * (float)Math.pow(t - 1.0F, 3.0) + 1.70158F * (float)Math.pow(t - 1.0F, 2.0);
         case EASE_IN_OUT_BACK -> easeInOutBack(t);
         case EASE_IN_ELASTIC -> easeInElastic(t);
         case EASE_OUT_ELASTIC -> easeOutElastic(t);
         case EASE_IN_OUT_ELASTIC -> easeInOutElastic(t);
      };
   }

   public static Easing byName(String name) {
      if (name == null || name.isBlank()) {
         return LINEAR;
      }
      String normalized = name.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
      for (Easing easing : values()) {
         if (easing.name().equals(normalized)) {
            return easing;
         }
      }
      return LINEAR;
   }

   private static float easeInOutExpo(float t) {
      if (t == 0.0F || t == 1.0F) {
         return t;
      }
      return t < 0.5F ? (float)Math.pow(2.0, 20.0F * t - 10.0F) * 0.5F : (2.0F - (float)Math.pow(2.0, -20.0F * t + 10.0F)) * 0.5F;
   }

   private static float easeInOutBack(float t) {
      float c = 1.70158F * 1.525F;
      return t < 0.5F
         ? ((float)Math.pow(2.0F * t, 2.0) * ((c + 1.0F) * 2.0F * t - c)) * 0.5F
         : ((float)Math.pow(2.0F * t - 2.0F, 2.0) * ((c + 1.0F) * (t * 2.0F - 2.0F) + c) + 2.0F) * 0.5F;
   }

   private static float easeInElastic(float t) {
      if (t == 0.0F || t == 1.0F) {
         return t;
      }
      return -((float)Math.pow(2.0, 10.0F * t - 10.0F) * Mth.sin((float)((t * 10.0F - 10.75F) * (2.0 * Math.PI / 3.0))));
   }

   private static float easeOutElastic(float t) {
      if (t == 0.0F || t == 1.0F) {
         return t;
      }
      return (float)Math.pow(2.0, -10.0F * t) * Mth.sin((float)((t * 10.0F - 0.75F) * (2.0 * Math.PI / 3.0))) + 1.0F;
   }

   private static float easeInOutElastic(float t) {
      if (t == 0.0F || t == 1.0F) {
         return t;
      }
      float c = (float)(2.0 * Math.PI / 4.5);
      return t < 0.5F
         ? -((float)Math.pow(2.0, 20.0F * t - 10.0F) * Mth.sin((20.0F * t - 11.125F) * c)) * 0.5F
         : (float)Math.pow(2.0, -20.0F * t + 10.0F) * Mth.sin((20.0F * t - 11.125F) * c) * 0.5F + 1.0F;
   }
}
