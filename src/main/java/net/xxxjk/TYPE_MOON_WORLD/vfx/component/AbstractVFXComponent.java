package net.xxxjk.TYPE_MOON_WORLD.vfx.component;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.vfx.IVFXComponent;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public abstract class AbstractVFXComponent implements IVFXComponent {
   protected static final float TAU = (float)(Math.PI * 2.0);

   protected VFXParticle sample(List<VFXParticle> out, float x, float y, float z) {
      return sample(out, x, y, z, null, 1.0F);
   }

   protected VFXParticle sample(List<VFXParticle> out, float x, float y, float z, ResourceLocation texture, float billboardScale) {
      VFXParticle particle = VFXParticle.acquire();
      particle.position.set(x, y, z);
      particle.texture = texture;
      particle.billboardScale = billboardScale <= 0.0F ? 1.0F : billboardScale;
      out.add(particle);
      return particle;
   }
}
