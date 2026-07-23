package net.xxxjk.typemoonworld.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;

public record MagicCastContext(
   LivingEntity caster,
   LivingEntity target,
   Level level,
   String magicId,
   CompoundTag preset,
   boolean crestCast,
   double proficiency
) {
   public ServerPlayer serverPlayer() {
      return this.caster instanceof ServerPlayer player ? player : null;
   }
}
