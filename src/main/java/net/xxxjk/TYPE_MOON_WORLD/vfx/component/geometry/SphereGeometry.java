package net.xxxjk.TYPE_MOON_WORLD.vfx.component.geometry;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class SphereGeometry extends AbstractGeometryComponent {
   private final float radius;

   public SphereGeometry(float radius, int sampleCount) {
      super(sampleCount);
      this.radius = radius;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      float golden = (float)(Math.PI * (3.0 - Math.sqrt(5.0)));
      for (int i = 0; i < this.sampleCount; i++) {
         float y = 1.0F - i / (float)(this.sampleCount - 1) * 2.0F;
         float r = Mth.sqrt(Math.max(0.0F, 1.0F - y * y));
         float a = golden * i;
         sample(out, this.radius * r * Mth.cos(a), this.radius * y, this.radius * r * Mth.sin(a));
      }
   }
}
