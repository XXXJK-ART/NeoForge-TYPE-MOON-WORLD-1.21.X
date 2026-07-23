package net.xxxjk.typemoonworld.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public record ServantContext(
   LivingEntity caster,
   LivingEntity target,
   String servantId,
   Level level,
   double distance,
   boolean hasLineOfSight,
   long gameTick
) {
   public ServerPlayer serverPlayer() {
      return this.caster instanceof ServerPlayer player ? player : null;
   }
}
