package net.xxxjk.TYPE_MOON_WORLD.vfx.component.field;

import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXMath;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class ScalarNoiseField extends AbstractFieldComponent {
   private final float scale;
   private final float amplitude;
   private final int sampleCount;

   public ScalarNoiseField(float scale, float amplitude, int sampleCount) {
      this.scale = Math.max(0.001F, scale);
      this.amplitude = amplitude;
      this.sampleCount = Math.max(1, sampleCount);
   }

   @Override
   public float sample(float x, float y, float z) {
      return VFXMath.noise3D(x * this.scale, y * this.scale, z * this.scale) * this.amplitude;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int i = 0; i < this.sampleCount; i++) {
         float t = i / (float)Math.max(1, this.sampleCount - 1);
         float x = (t - 0.5F) * 2.0F;
         sample(out, x, sample(x, lifeProgress, t), t - 0.5F);
      }
   }
}
