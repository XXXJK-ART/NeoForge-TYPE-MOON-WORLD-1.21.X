package net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve;

import net.xxxjk.TYPE_MOON_WORLD.vfx.component.AbstractVFXComponent;

public abstract class AbstractCurveComponent extends AbstractVFXComponent {
   protected final int segments;
   protected final float radius;
   protected final float turns;
   protected final int depth;

   protected AbstractCurveComponent(int segments, float radius, float turns, int depth) {
      this.segments = Math.max(2, segments);
      this.radius = radius;
      this.turns = turns;
      this.depth = Math.max(0, depth);
   }
}
