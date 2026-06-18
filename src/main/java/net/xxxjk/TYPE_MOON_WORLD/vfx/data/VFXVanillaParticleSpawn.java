package net.xxxjk.TYPE_MOON_WORLD.vfx.data;

import net.minecraft.core.particles.ParticleOptions;
import org.joml.Vector3f;

public record VFXVanillaParticleSpawn(ParticleOptions options, Vector3f position, Vector3f velocity) {
   public VFXVanillaParticleSpawn {
      position = new Vector3f(position);
      velocity = new Vector3f(velocity);
   }
}
