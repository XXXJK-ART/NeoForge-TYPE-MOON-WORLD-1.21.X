package net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve;

import java.util.List;
import java.util.Random;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;
import org.joml.Vector3f;

public class FractalLightningCurve extends AbstractCurveComponent {
   private final Vector3f start;
   private final Vector3f end;
   private final int iterations;
   private final float roughness;
   private final float period;
   private final float density;
   private final float[] seeds;
   private final Random random = new Random(0L);

   public FractalLightningCurve(Vector3f start, Vector3f end, int iterations, float roughness, float period, float density, float seed1, float seed2, float seed3, float seed4, float seed5, int segments) {
      super(Math.max(2, segments), 0.0F, 0.0F, 0);
      this.start = new Vector3f(start);
      this.end = new Vector3f(end);
      this.iterations = Math.max(0, iterations);
      this.roughness = roughness;
      this.period = Math.max(0.001F, period);
      this.density = Math.max(0.001F, density);
      this.seeds = new float[] {seed1, seed2, seed3, seed4, seed5};
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      Vector3f dir = new Vector3f(this.end).sub(this.start);
      float length = dir.length();
      if (length < 1.0E-5F) {
         return;
      }
      Vector3f forward = dir.normalize();
      Vector3f right = Math.abs(forward.y) > 0.9F ? new Vector3f(1.0F, 0.0F, 0.0F).cross(forward).normalize() : forward.cross(new Vector3f(0.0F, 1.0F, 0.0F)).normalize();
      Vector3f up = new Vector3f(forward).cross(right).normalize();
      this.random.setSeed(0x9E3779B97F4A7C15L ^ Float.floatToIntBits(lifeProgress * this.period));
      emitBranch(out, new Vector3f(this.start), forward, right, up, length, this.iterations, 0);
   }

   private void emitBranch(List<VFXParticle> out, Vector3f start, Vector3f forward, Vector3f right, Vector3f up, float length, int depth, int branchIndex) {
      Vector3f end = new Vector3f(forward).mul(length).add(start);
      int samples = Math.max(2, (int)(this.density * length * (depth + 1)));
      Vector3f prev = new Vector3f(start);
      for (int i = 1; i <= samples; i++) {
         float t = i / (float)samples;
         Vector3f pos = new Vector3f(start).lerp(end, t);
         float envelope = 1.0F - Math.abs(t - 0.5F) * 2.0F;
         float wave = Mth.sin((t * this.period + branchIndex * 0.37F + this.seeds[(branchIndex + depth) % this.seeds.length]) * TAU);
         float jitter = this.roughness * envelope;
         pos.add(new Vector3f(right).mul(wave * jitter));
         pos.add(new Vector3f(up).mul(Mth.cos((t * this.period * 0.73F + depth * 0.19F) * TAU) * jitter * 0.65F));
         sample(out, pos.x, pos.y, pos.z);
         prev.set(pos);
      }
      if (depth <= 0) {
         return;
      }
      int branches = 2 + (branchIndex % 2);
      for (int i = 0; i < branches; i++) {
         float forkT = 0.25F + 0.15F * (i + 1);
         Vector3f forkStart = new Vector3f(start).lerp(end, forkT);
         float branchLength = length * (0.28F + 0.12F * i);
         Vector3f forkDir = new Vector3f(forward)
            .mul(0.65F)
            .add(new Vector3f(right).mul((i % 2 == 0 ? 1.0F : -1.0F) * (0.35F + 0.12F * depth)))
            .add(new Vector3f(up).mul(0.18F + 0.07F * i))
            .normalize();
         emitBranch(out, forkStart, forkDir, right, up, branchLength, depth - 1, branchIndex * 3 + i + 1);
      }
   }
}
