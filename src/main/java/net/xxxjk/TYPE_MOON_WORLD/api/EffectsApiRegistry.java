package net.xxxjk.TYPE_MOON_WORLD.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class EffectsApiRegistry {
   private static final AtomicBoolean FROZEN = new AtomicBoolean();
   private static final Map<String, ParticleOptions> PARTICLES = new ConcurrentHashMap<>();
   private static final Map<String, SoundEvent> SOUNDS = new ConcurrentHashMap<>();
   private EffectsApiRegistry() { }
   public static void freeze() { FROZEN.set(true); }
   public static boolean particle(ResourceLocation id, ParticleOptions value) { return !FROZEN.get() && id != null && value != null && PARTICLES.putIfAbsent(id.toString(), value) == null; }
   public static boolean sound(ResourceLocation id, SoundEvent value) { return !FROZEN.get() && id != null && value != null && SOUNDS.putIfAbsent(id.toString(), value) == null; }
   public static ParticleOptions particle(ResourceLocation id) { return id == null ? null : PARTICLES.get(id.toString()); }
   public static SoundEvent sound(ResourceLocation id) { return id == null ? null : SOUNDS.get(id.toString()); }
}
