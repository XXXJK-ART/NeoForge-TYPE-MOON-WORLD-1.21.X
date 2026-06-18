package net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve;

import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;
import org.joml.Vector3f;

public class FractalTreeCurve extends AbstractCurveComponent {
   private final float length;
   private final float angle;

   public FractalTreeCurve(float length, float angle, int depth) {
      super(2, 0.0F, 0.0F, Math.min(4, depth));
      this.length = length;
      this.angle = angle;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      branch(out, new Vector3f(), new Vector3f(0.0F, 1.0F, 0.0F), this.length, this.depth);
   }

   private void branch(List<VFXParticle> out, Vector3f start, Vector3f dir, float len, int d) {
      Vector3f end = new Vector3f(dir).mul(len).add(start);
      sample(out, start.x, start.y, start.z);
      sample(out, end.x, end.y, end.z);
      if (d <= 0) {
         return;
      }
      branch(out, end, new Vector3f(dir).rotateZ(this.angle).normalize(), len * 0.68F, d - 1);
      branch(out, end, new Vector3f(dir).rotateZ(-this.angle).normalize(), len * 0.68F, d - 1);
      branch(out, end, new Vector3f(dir).rotateX(this.angle).normalize(), len * 0.62F, d - 1);
   }
}
