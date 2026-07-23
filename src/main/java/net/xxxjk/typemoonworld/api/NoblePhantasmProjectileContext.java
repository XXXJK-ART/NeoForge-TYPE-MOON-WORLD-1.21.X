package net.xxxjk.typemoonworld.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public record NoblePhantasmProjectileContext(ServerLevel level, LivingEntity caster, LivingEntity target, Vec3 origin, Vec3 direction, int overcharge) {
   public NoblePhantasmProjectileContext {
      origin = origin == null ? Vec3.ZERO : origin;
      direction = direction == null || direction.lengthSqr() < 1.0E-8 ? Vec3.ZERO : direction.normalize();
      overcharge = Math.max(0, overcharge);
   }
}
