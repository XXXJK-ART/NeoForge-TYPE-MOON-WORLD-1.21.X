package net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class ConvexPolygonCurve extends AbstractCurveComponent {
   private final int sides;

   public ConvexPolygonCurve(float radius, int sides, int segments) {
      super(segments, radius, 0.0F, 0);
      this.sides = Math.max(3, sides);
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int i = 0; i < this.segments; i++) {
         float edgeT = i / (float)this.segments * this.sides;
         int side = Mth.floor(edgeT);
         float local = edgeT - side;
         float a0 = TAU * side / this.sides;
         float a1 = TAU * (side + 1) / this.sides;
         float x = Mth.lerp(local, this.radius * Mth.cos(a0), this.radius * Mth.cos(a1));
         float z = Mth.lerp(local, this.radius * Mth.sin(a0), this.radius * Mth.sin(a1));
         sample(out, x, 0.0F, z);
      }
   }
}
