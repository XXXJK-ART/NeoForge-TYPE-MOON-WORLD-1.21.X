package net.xxxjk.TYPE_MOON_WORLD.vfx.condition;

import java.util.Random;

public interface VFXCondition {
   VFXCondition ALWAYS = (t, random) -> true;

   boolean test(float t, Random random);

   default boolean test(float t, float timeSeconds, float durationSeconds, Random random) {
      return test(t, random);
   }
}
