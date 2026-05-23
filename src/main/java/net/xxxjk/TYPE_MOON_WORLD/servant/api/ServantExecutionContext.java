package net.xxxjk.TYPE_MOON_WORLD.servant.api;

import net.minecraft.world.entity.LivingEntity;

public record ServantExecutionContext(
   LivingEntity caster,
   LivingEntity target,
   double currentMp,
   int overChargeLevel
) {
}
