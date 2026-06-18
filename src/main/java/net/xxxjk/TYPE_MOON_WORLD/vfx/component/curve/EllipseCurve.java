package net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class EllipseCurve extends AbstractCurveComponent {
   private final float radiusZ;

   public EllipseCurve(float radiusX, float radiusZ, int segments) {
      super(segments, radiusX, 0.0F, 0);
      this.radiusZ = radiusZ;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int i = 0; i < this.segments; i++) {
         float a = TAU * i / this.segments;
         sample(out, this.radius * Mth.cos(a), 0.0F, this.radiusZ * Mth.sin(a));
      }
   }
}
