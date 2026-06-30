package net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class SchlafliStarCurve extends AbstractCurveComponent {
   private final int points;
   private final int step;

   public SchlafliStarCurve(float radius, int points, int step, int segments) {
      super(segments, radius, 0.0F, 0);
      this.points = Math.max(3, points);
      this.step = Math.max(1, step);
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int i = 0; i < this.segments; i++) {
         float u = i / (float)this.segments * this.points;
         int vertex = Mth.floor(u);
         float local = u - vertex;
         float a0 = TAU * (vertex % this.points) / this.points;
         float a1 = TAU * ((vertex + this.step) % this.points) / this.points;
         sample(out, Mth.lerp(local, this.radius * Mth.cos(a0), this.radius * Mth.cos(a1)), 0.0F, Mth.lerp(local, this.radius * Mth.sin(a0), this.radius * Mth.sin(a1)));
      }
   }
}
