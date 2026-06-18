package net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface;

import net.xxxjk.TYPE_MOON_WORLD.vfx.component.AbstractVFXComponent;

public abstract class AbstractSurfaceComponent extends AbstractVFXComponent {
   protected final int uSegments;
   protected final int vSegments;
   protected final float radius;

   protected AbstractSurfaceComponent(int uSegments, int vSegments, float radius) {
      this.uSegments = Math.max(3, uSegments);
      this.vSegments = Math.max(2, vSegments);
      this.radius = radius;
   }
}
