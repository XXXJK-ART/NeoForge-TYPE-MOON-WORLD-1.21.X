package net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class ParametricSurface extends AbstractSurfaceComponent {
   private final float amplitude;
   private final float frequency;

   public ParametricSurface(float amplitude, float frequency, int uSegments, int vSegments) {
      super(uSegments, vSegments, 0.0F);
      this.amplitude = amplitude;
      this.frequency = frequency;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int u = 0; u < this.uSegments; u++) {
         float x = u / (float)(this.uSegments - 1) - 0.5F;
         for (int v = 0; v < this.vSegments; v++) {
            float z = v / (float)(this.vSegments - 1) - 0.5F;
            float y = this.amplitude * Mth.sin((x + lifeProgress) * TAU * this.frequency) * Mth.cos(z * TAU * this.frequency);
            sample(out, x * this.amplitude * 2.0F, y, z * this.amplitude * 2.0F);
         }
      }
   }
}
