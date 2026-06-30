package net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;
import org.joml.Vector3f;

public class TCBSplineCurve extends AbstractCurveComponent {
   private final Vector3f[] points;
   private final float tension;
   private final float continuity;
   private final float bias;

   public TCBSplineCurve(Vector3f[] points, float tension, float continuity, float bias, int segments) {
      super(segments, 0.0F, 0.0F, 0);
      this.points = points.clone();
      this.tension = tension;
      this.continuity = continuity;
      this.bias = bias;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      if (this.points.length < 2) {
         return;
      }
      for (int i = 0; i < this.segments; i++) {
         float u = i / (float)(this.segments - 1) * (this.points.length - 1);
         int p = Math.min(this.points.length - 2, Mth.floor(u));
         float t = u - p;
         Vector3f p0 = this.points[Math.max(0, p - 1)];
         Vector3f p1 = this.points[p];
         Vector3f p2 = this.points[p + 1];
         Vector3f p3 = this.points[Math.min(this.points.length - 1, p + 2)];
         Vector3f m1 = tangent(p0, p1, p2, false);
         Vector3f m2 = tangent(p1, p2, p3, true);
         float t2 = t * t;
         float t3 = t2 * t;
         Vector3f result = new Vector3f(p1).mul(2.0F * t3 - 3.0F * t2 + 1.0F)
            .fma(t3 - 2.0F * t2 + t, m1)
            .fma(-2.0F * t3 + 3.0F * t2, p2)
            .fma(t3 - t2, m2);
         sample(out, result.x, result.y, result.z);
      }
   }

   private Vector3f tangent(Vector3f a, Vector3f b, Vector3f c, boolean incoming) {
      float s = (1.0F - this.tension) * 0.5F;
      float c0 = incoming ? (1.0F + this.continuity) * (1.0F + this.bias) : (1.0F - this.continuity) * (1.0F + this.bias);
      float c1 = incoming ? (1.0F - this.continuity) * (1.0F - this.bias) : (1.0F + this.continuity) * (1.0F - this.bias);
      return new Vector3f(b).sub(a).mul(s * c0).add(new Vector3f(c).sub(b).mul(s * c1));
   }
}
