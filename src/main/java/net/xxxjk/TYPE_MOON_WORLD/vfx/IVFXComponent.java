package net.xxxjk.TYPE_MOON_WORLD.vfx;

import java.util.List;

public interface IVFXComponent {
   void update(float deltaTime, float lifeProgress, List<VFXParticle> out);
}
