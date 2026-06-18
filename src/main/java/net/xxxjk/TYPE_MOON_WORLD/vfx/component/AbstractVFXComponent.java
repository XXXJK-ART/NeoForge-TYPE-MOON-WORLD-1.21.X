package net.xxxjk.TYPE_MOON_WORLD.vfx.component;

import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.vfx.IVFXComponent;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXParticle;

public abstract class AbstractVFXComponent implements IVFXComponent {
   protected static final float TAU = (float)(Math.PI * 2.0);

   protected VFXParticle sample(List<VFXParticle> out, float x, float y, float z) {
      VFXParticle particle = VFXParticle.acquire();
      particle.position.set(x, y, z);
      out.add(particle);
      return particle;
   }
}
