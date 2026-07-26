package net.xxxjk.TYPE_MOON_WORLD.vfx.component.geometry;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class HemisphereGeometry extends AbstractGeometryComponent {
   private final float radius;

   public HemisphereGeometry(float radius, int sampleCount) {
      super(sampleCount);
      this.radius = radius;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      float golden = (float)(Math.PI * (3.0 - Math.sqrt(5.0)));
      int denominator = Math.max(1, this.sampleCount - 1);
      for (int i = 0; i < this.sampleCount; i++) {
         float y = i / (float)denominator;
         float horizontalRadius = Mth.sqrt(Math.max(0.0F, 1.0F - y * y));
         float angle = golden * i;
         sample(out, this.radius * horizontalRadius * Mth.cos(angle), this.radius * y,
            this.radius * horizontalRadius * Mth.sin(angle));
      }
   }
}
