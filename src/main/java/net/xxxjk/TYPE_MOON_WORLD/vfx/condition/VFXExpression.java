package net.xxxjk.TYPE_MOON_WORLD.vfx.condition;

import java.util.Map;
import java.util.Random;

@FunctionalInterface
public interface VFXExpression {
   float eval(Map<String, Float> variables, Random random);
}
