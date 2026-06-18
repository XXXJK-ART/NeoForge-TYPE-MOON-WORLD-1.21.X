package net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class TorusSurface extends AbstractSurfaceComponent {
   private final float tubeRadius;

   public TorusSurface(float radius, float tubeRadius, int uSegments, int vSegments) {
      super(uSegments, vSegments, radius);
      this.tubeRadius = tubeRadius;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int u = 0; u < this.uSegments; u++) {
         float a = TAU * u / this.uSegments;
         for (int v = 0; v < this.vSegments; v++) {
            float b = TAU * v / this.vSegments;
            float ring = this.radius + this.tubeRadius * Mth.cos(b);
            sample(out, ring * Mth.cos(a), this.tubeRadius * Mth.sin(b), ring * Mth.sin(a));
         }
      }
   }
}
