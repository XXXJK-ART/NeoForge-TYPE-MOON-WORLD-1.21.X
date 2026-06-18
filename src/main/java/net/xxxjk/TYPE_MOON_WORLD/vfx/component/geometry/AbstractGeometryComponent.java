package net.xxxjk.TYPE_MOON_WORLD.vfx.component.geometry;

import net.xxxjk.TYPE_MOON_WORLD.vfx.component.AbstractVFXComponent;

public abstract class AbstractGeometryComponent extends AbstractVFXComponent {
   protected final int sampleCount;

   protected AbstractGeometryComponent(int sampleCount) {
      this.sampleCount = Math.max(1, sampleCount);
   }
}
