package net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve;

import java.util.List;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public class ParabolaCurve extends AbstractCurveComponent {
   private final float width;
   private final float height;

   public ParabolaCurve(float width, float height, int segments) {
      super(segments, 0.0F, 0.0F, 0);
      this.width = width;
      this.height = height;
   }

   @Override
   public void update(float deltaTime, float lifeProgress, List<VFXParticle> out) {
      for (int i = 0; i < this.segments; i++) {
         float t = i / (float)(this.segments - 1);
         float x = (t - 0.5F) * this.width;
         float y = this.height * (1.0F - 4.0F * (t - 0.5F) * (t - 0.5F));
         sample(out, x, y, Mth.lerp(t, -this.width * 0.25F, this.width * 0.25F));
      }
   }
}
