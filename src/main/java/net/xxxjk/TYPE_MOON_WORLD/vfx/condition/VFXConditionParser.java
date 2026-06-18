package net.xxxjk.TYPE_MOON_WORLD.vfx.condition;

import java.util.Map;
import java.util.Random;

public final class VFXConditionParser {
   private VFXConditionParser() {
   }

   public static VFXCondition parse(String expression) {
      if (expression == null || expression.isBlank()) {
         return VFXCondition.ALWAYS;
      }
      VFXExpression parsed = VFXExpressionParser.parse(expression);
      return new VFXCondition() {
         @Override
         public boolean test(float t, Random random) {
            return test(t, t, 1.0F, random);
         }

         @Override
         public boolean test(float t, float timeSeconds, float durationSeconds, Random random) {
            return parsed.eval(
               Map.of(
                  "t",
                  t,
                  "progress",
                  t,
                  "time",
                  timeSeconds,
                  "seconds",
                  timeSeconds,
                  "duration",
                  durationSeconds,
                  "time_norm",
                  t,
                  "angle",
                  (float)(Math.PI * 2.0) * t
               ),
               random
            ) > 0.0F;
         }
      };
   }
}
