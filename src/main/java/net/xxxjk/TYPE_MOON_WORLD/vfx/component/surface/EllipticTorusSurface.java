package net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class EllipticTorusSurface extends AbstractSurfaceComponent {
   private final float radiusZ;
   private final float tubeRadius;

   public EllipticTorusSurface(float radiusX, float radiusZ, float tubeRadius, int uSegments, int vSegments) {
      super(uSegments, vSegments, radiusX);
      this.radiusZ = radiusZ;
      this.tubeRadius = tubeRadius;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int u = 0; u < this.uSegments; u++) {
         float a = TAU * u / this.uSegments;
         for (int v = 0; v < this.vSegments; v++) {
            float b = TAU * v / this.vSegments;
            sample(out, (this.radius + this.tubeRadius * Mth.cos(b)) * Mth.cos(a), this.tubeRadius * Mth.sin(b), (this.radiusZ + this.tubeRadius * Mth.cos(b)) * Mth.sin(a));
         }
      }
   }
}
