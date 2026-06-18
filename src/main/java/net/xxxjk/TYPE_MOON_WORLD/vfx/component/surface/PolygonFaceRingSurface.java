package net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class PolygonFaceRingSurface extends AbstractSurfaceComponent {
   private final int sides;
   private final float innerRadius;

   public PolygonFaceRingSurface(float outerRadius, float innerRadius, int sides, int uSegments, int vSegments) {
      super(uSegments, vSegments, outerRadius);
      this.innerRadius = innerRadius;
      this.sides = Math.max(3, sides);
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int u = 0; u < this.uSegments; u++) {
         float angle = TAU * u / this.uSegments;
         float sector = TAU / this.sides;
         float polyScale = Mth.cos(sector * 0.5F) / Mth.cos(angle % sector - sector * 0.5F);
         for (int v = 0; v < this.vSegments; v++) {
            float r = Mth.lerp(v / (float)(this.vSegments - 1), this.innerRadius, this.radius * polyScale);
            sample(out, r * Mth.cos(angle), 0.0F, r * Mth.sin(angle));
         }
      }
   }
}
