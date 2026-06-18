package net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class HelixCurve extends AbstractCurveComponent {
   private final float height;

   public HelixCurve(float radius, float turns, float height, int segments) {
      super(segments, radius, turns, 0);
      this.height = height;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int i = 0; i < this.segments; i++) {
         float t = i / (float)(this.segments - 1);
         float a = TAU * this.turns * t;
         sample(out, this.radius * Mth.cos(a), this.height * t, this.radius * Mth.sin(a));
      }
   }
}
