package net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve;

import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;
import net.xxxjk.TYPE_MOON_WORLD.vfx.condition.VFXExpression;
import net.xxxjk.TYPE_MOON_WORLD.vfx.condition.VFXExpressionParser;

public class ParametricCurve extends AbstractCurveComponent {
   public enum Mode {
      DEFAULT,
      LIGHTNING_ZIGZAG,
      LIGHTNING_NODES,
      EXPRESSION
   }

   private final float amplitude;
   private final float frequency;
   private final float zigzagFrequency;
   private final float zigzagAmplitude;
   private final int minNodes;
   private final int maxNodes;
   private final float jitter;
   private final Mode mode;
   private final VFXExpression exprX;
   private final VFXExpression exprY;
   private final VFXExpression exprZ;
   private final float tStart;
   private final float tEnd;
   private final Random expressionRandom = new Random(0L);

   public ParametricCurve(float amplitude, float frequency, int segments) {
      this(amplitude, frequency, 0.0F, 0.0F, 0, 0, 0.0F, Mode.DEFAULT, null, null, null, 0.0F, 1.0F, segments);
   }

   public ParametricCurve(float amplitude, float frequency, float zigzagFrequency, float zigzagAmplitude, Mode mode, int segments) {
      this(amplitude, frequency, zigzagFrequency, zigzagAmplitude, 0, 0, 0.0F, mode, null, null, null, 0.0F, 1.0F, segments);
   }

   public ParametricCurve(float amplitude, float frequency, float zigzagFrequency, float zigzagAmplitude, int minNodes, int maxNodes, float jitter, Mode mode, int segments) {
      this(amplitude, frequency, zigzagFrequency, zigzagAmplitude, minNodes, maxNodes, jitter, mode, null, null, null, 0.0F, 1.0F, segments);
   }

   public ParametricCurve(
      String exprX,
      String exprY,
      String exprZ,
      float tStart,
      float tEnd,
      int segments
   ) {
      this(1.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0.0F, Mode.EXPRESSION, exprX, exprY, exprZ, tStart, tEnd, segments);
   }

   public ParametricCurve(
      float amplitude,
      float frequency,
      float zigzagFrequency,
      float zigzagAmplitude,
      int minNodes,
      int maxNodes,
      float jitter,
      Mode mode,
      String exprX,
      String exprY,
      String exprZ,
      float tStart,
      float tEnd,
      int segments
   ) {
      super(segments, 0.0F, frequency, 0);
      this.amplitude = amplitude;
      this.frequency = frequency;
      this.zigzagFrequency = zigzagFrequency;
      this.zigzagAmplitude = zigzagAmplitude;
      this.minNodes = Math.max(0, minNodes);
      this.maxNodes = Math.max(this.minNodes, maxNodes);
      this.jitter = jitter;
      this.mode = mode == null ? Mode.DEFAULT : mode;
      this.exprX = exprX == null || exprX.isBlank() ? null : VFXExpressionParser.parse(exprX);
      this.exprY = exprY == null || exprY.isBlank() ? null : VFXExpressionParser.parse(exprY);
      this.exprZ = exprZ == null || exprZ.isBlank() ? null : VFXExpressionParser.parse(exprZ);
      this.tStart = tStart;
      this.tEnd = tEnd;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      this.expressionRandom.setSeed(Float.floatToIntBits(lifeProgress * 997.0F) * 31L + 17L);
      for (int i = 0; i < this.segments; i++) {
         float t = i / (float)(this.segments - 1);
         float a = TAU * this.frequency * t;
         float x = (t - 0.5F) * this.amplitude * 2.0F;
         float y = this.amplitude * Mth.sin(a);
         float z = this.amplitude * Mth.cos(a * 0.5F);
         if (this.mode == Mode.LIGHTNING_ZIGZAG) {
            z += Mth.sin(TAU * this.zigzagFrequency * t) * this.zigzagAmplitude;
            x = 0.0F;
            y = t * this.amplitude;
         } else if (this.mode == Mode.LIGHTNING_NODES) {
            int nodeCount = this.maxNodes <= this.minNodes ? this.minNodes : this.minNodes + Math.abs(Float.floatToIntBits(lifeProgress * 997.0F)) % (this.maxNodes - this.minNodes + 1);
            float nodeT = t * (nodeCount + 1);
            int node = Mth.floor(nodeT);
            float local = nodeT - node;
            float x0 = nodeOffset(node, lifeProgress, 0);
            float z0 = nodeOffset(node, lifeProgress, 1);
            float x1 = nodeOffset(node + 1, lifeProgress, 0);
            float z1 = nodeOffset(node + 1, lifeProgress, 1);
            x = node == 0 || node >= nodeCount + 1 ? 0.0F : Mth.lerp(local, x0, x1);
            y = t * this.amplitude;
            z = node == 0 || node >= nodeCount + 1 ? 0.0F : Mth.lerp(local, z0, z1);
         } else if (this.mode == Mode.EXPRESSION) {
            float u = Mth.lerp(t, this.tStart, this.tEnd);
            Map<String, Float> vars = Map.of(
               "t", u,
               "u", u,
               "progress", lifeProgress,
               "time", lifeProgress,
               "angle", TAU * t
            );
            x = this.exprX == null ? 0.0F : this.exprX.eval(vars, this.expressionRandom);
            y = this.exprY == null ? 0.0F : this.exprY.eval(vars, this.expressionRandom);
            z = this.exprZ == null ? 0.0F : this.exprZ.eval(vars, this.expressionRandom);
         }
         sample(out, x, y, z);
      }
   }

   private float nodeOffset(int node, float lifeProgress, int salt) {
      if (node <= 0) {
         return 0.0F;
      }
      int h = node * 734287 + salt * 912931 + Float.floatToIntBits(lifeProgress * 20.0F);
      h ^= h >>> 13;
      h *= 1274126177;
      float n = ((h >>> 8) & 65535) / 32767.5F - 1.0F;
      return n * this.jitter;
   }
}
