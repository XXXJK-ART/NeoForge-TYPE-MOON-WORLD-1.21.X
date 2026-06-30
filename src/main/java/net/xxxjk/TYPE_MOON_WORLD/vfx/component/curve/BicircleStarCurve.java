package net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class BicircleStarCurve extends AbstractCurveComponent {
   private final float innerRadius;

   public BicircleStarCurve(float radius, float innerRadius, int segments) {
      super(segments, radius, 0.0F, 0);
      this.innerRadius = innerRadius;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int i = 0; i < this.segments; i++) {
         float t = i / (float)this.segments;
         float a = TAU * t;
         float r = this.radius + this.innerRadius * Mth.cos(2.0F * a);
         sample(out, r * Mth.cos(a), this.innerRadius * Mth.sin(2.0F * a) * 0.25F, r * Mth.sin(a));
      }
   }
}
