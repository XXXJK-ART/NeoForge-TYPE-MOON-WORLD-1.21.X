package net.xxxjk.TYPE_MOON_WORLD.vfx.component.geometry;

import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;
import org.joml.Vector3f;

public class ConvexPolyhedronGeometry extends AbstractGeometryComponent {
   private final Vector3f[] vertices;

   public ConvexPolyhedronGeometry(Vector3f[] vertices, int sampleCount) {
      super(sampleCount);
      this.vertices = vertices.clone();
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      if (this.vertices.length == 0) {
         return;
      }
      for (int i = 0; i < this.sampleCount; i++) {
         Vector3f a = this.vertices[i % this.vertices.length];
         Vector3f b = this.vertices[(i * 7 + 1) % this.vertices.length];
         float t = (i % 31) / 30.0F;
         Vector3f p = new Vector3f(a).lerp(b, t);
         sample(out, p.x, p.y, p.z);
      }
   }
}
