package net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class BicircleStarTorusSurface extends AbstractSurfaceComponent {
   private final float innerRadius;
   private final float tubeRadius;

   public BicircleStarTorusSurface(float radius, float innerRadius, float tubeRadius, int uSegments, int vSegments) {
      super(uSegments, vSegments, radius);
      this.innerRadius = innerRadius;
      this.tubeRadius = tubeRadius;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int u = 0; u < this.uSegments; u++) {
         float a = TAU * u / this.uSegments;
         float star = this.radius + this.innerRadius * Mth.cos(2.0F * a);
         for (int v = 0; v < this.vSegments; v++) {
            float b = TAU * v / this.vSegments;
            float r = star + this.tubeRadius * Mth.cos(b);
            sample(out, r * Mth.cos(a), this.tubeRadius * Mth.sin(b), r * Mth.sin(a));
         }
      }
   }
}
