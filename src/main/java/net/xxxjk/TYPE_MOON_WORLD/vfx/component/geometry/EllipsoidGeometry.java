package net.xxxjk.TYPE_MOON_WORLD.vfx.component.geometry;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class EllipsoidGeometry extends AbstractGeometryComponent {
   private final float radiusX;
   private final float radiusY;
   private final float radiusZ;

   public EllipsoidGeometry(float radiusX, float radiusY, float radiusZ, int sampleCount) {
      super(sampleCount);
      this.radiusX = radiusX;
      this.radiusY = radiusY;
      this.radiusZ = radiusZ;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      float golden = (float)(Math.PI * (3.0 - Math.sqrt(5.0)));
      for (int i = 0; i < this.sampleCount; i++) {
         float y = 1.0F - i / (float)(this.sampleCount - 1) * 2.0F;
         float r = Mth.sqrt(Math.max(0.0F, 1.0F - y * y));
         float a = golden * i;
         sample(out, this.radiusX * r * Mth.cos(a), this.radiusY * y, this.radiusZ * r * Mth.sin(a));
      }
   }
}
