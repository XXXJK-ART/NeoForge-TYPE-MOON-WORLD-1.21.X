package net.xxxjk.typemoonworld.api;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

public interface EffectsRegistry {
   boolean registerParticle(String id, ParticleOptions particle);
   boolean registerSound(String id, SoundEvent sound);
   boolean particle(ServerLevel level, String id, Vec3 position, int count, double spread, double speed);
   boolean sound(ServerLevel level, String id, Vec3 position, float volume, float pitch);
}
