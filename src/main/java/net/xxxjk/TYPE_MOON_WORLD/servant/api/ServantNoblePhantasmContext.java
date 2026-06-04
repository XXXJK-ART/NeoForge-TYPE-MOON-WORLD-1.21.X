package net.xxxjk.TYPE_MOON_WORLD.servant.api;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;

public record ServantNoblePhantasmContext(
   ServantEntity caster,
   LivingEntity target,
   ServantDefinition servantDefinition,
   ServantNoblePhantasmDefinition noblePhantasmDefinition,
   int overChargeLevel,
   double currentMp
) {
}
