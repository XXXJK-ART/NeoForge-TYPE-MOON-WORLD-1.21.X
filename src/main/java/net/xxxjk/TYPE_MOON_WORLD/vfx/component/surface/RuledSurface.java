package net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;
import org.joml.Vector3f;

public class RuledSurface extends AbstractSurfaceComponent {
   private final Vector3f startA;
   private final Vector3f endA;
   private final Vector3f startB;
   private final Vector3f endB;

   public RuledSurface(Vector3f startA, Vector3f endA, Vector3f startB, Vector3f endB, int uSegments, int vSegments) {
      super(uSegments, vSegments, 0.0F);
      this.startA = new Vector3f(startA);
      this.endA = new Vector3f(endA);
      this.startB = new Vector3f(startB);
      this.endB = new Vector3f(endB);
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int u = 0; u < this.uSegments; u++) {
         float tu = u / (float)(this.uSegments - 1);
         Vector3f a = new Vector3f(this.startA).lerp(this.endA, tu);
         Vector3f b = new Vector3f(this.startB).lerp(this.endB, tu);
         for (int v = 0; v < this.vSegments; v++) {
            Vector3f p = new Vector3f(a).lerp(b, v / (float)(this.vSegments - 1));
            sample(out, p.x, p.y, p.z);
         }
      }
   }
}
