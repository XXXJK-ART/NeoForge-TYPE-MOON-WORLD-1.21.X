package net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;
import org.joml.Vector3f;

public class LineCurve extends AbstractCurveComponent {
   private final Vector3f start;
   private final Vector3f end;

   public LineCurve(Vector3f start, Vector3f end, int segments) {
      super(segments, 0.0F, 0.0F, 0);
      this.start = new Vector3f(start);
      this.end = new Vector3f(end);
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int i = 0; i < this.segments; i++) {
         float t = i / (float)(this.segments - 1);
         sample(out, Mth.lerp(t, this.start.x, this.end.x), Mth.lerp(t, this.start.y, this.end.y), Mth.lerp(t, this.start.z, this.end.z));
      }
   }
}
