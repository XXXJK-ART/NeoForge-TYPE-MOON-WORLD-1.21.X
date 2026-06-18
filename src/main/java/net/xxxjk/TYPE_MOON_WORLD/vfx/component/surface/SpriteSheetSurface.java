package net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface;

import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class SpriteSheetSurface extends AbstractSurfaceComponent {
   private final int frames;
   private final float frameRate;
   private final float width;
   private final float height;

   public SpriteSheetSurface(int frames, float frameRate, float width, float height, int uSegments, int vSegments) {
      super(uSegments, vSegments, 0.0F);
      this.frames = Math.max(1, frames);
      this.frameRate = Math.max(0.0F, frameRate);
      this.width = width;
      this.height = height;
   }

   public int frameAt(float ageSeconds) {
      return (int)(ageSeconds * this.frameRate) % this.frames;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int u = 0; u < this.uSegments; u++) {
         float x = (u / (float)(this.uSegments - 1) - 0.5F) * this.width;
         for (int v = 0; v < this.vSegments; v++) {
            float y = (v / (float)(this.vSegments - 1) - 0.5F) * this.height;
            sample(out, x, y, 0.0F);
         }
      }
   }
}
