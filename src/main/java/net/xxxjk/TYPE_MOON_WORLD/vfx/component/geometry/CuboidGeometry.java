package net.xxxjk.TYPE_MOON_WORLD.vfx.component.geometry;

import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class CuboidGeometry extends AbstractGeometryComponent {
   public enum Mode {
      EDGES,
      FACES,
      VOXELS
   }

   private final float width;
   private final float height;
   private final float depth;
   private final Mode mode;

   public CuboidGeometry(float width, float height, float depth, Mode mode, int sampleCount) {
      super(sampleCount);
      this.width = width;
      this.height = height;
      this.depth = depth;
      this.mode = mode == null ? Mode.EDGES : mode;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      int side = Math.max(2, Math.round((float)Math.cbrt(this.sampleCount)));
      for (int x = 0; x < side; x++) {
         for (int y = 0; y < side; y++) {
            for (int z = 0; z < side; z++) {
               boolean edge = (x == 0 || x == side - 1 ? 1 : 0) + (y == 0 || y == side - 1 ? 1 : 0) + (z == 0 || z == side - 1 ? 1 : 0) >= 2;
               boolean face = x == 0 || y == 0 || z == 0 || x == side - 1 || y == side - 1 || z == side - 1;
               if (this.mode == Mode.VOXELS || this.mode == Mode.FACES && face || this.mode == Mode.EDGES && edge) {
                  sample(out, (x / (float)(side - 1) - 0.5F) * this.width, (y / (float)(side - 1) - 0.5F) * this.height, (z / (float)(side - 1) - 0.5F) * this.depth);
               }
            }
         }
      }
   }
}
