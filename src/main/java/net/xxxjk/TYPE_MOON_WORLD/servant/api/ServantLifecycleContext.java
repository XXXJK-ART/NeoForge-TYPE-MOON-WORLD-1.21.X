package net.xxxjk.TYPE_MOON_WORLD.servant.api;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;

public record ServantLifecycleContext(
   ServantEntity entity,
   LivingEntity target,
   ServantAiContext aiContext,
   ServantDefinition definition,
   long gameTick
) {
}
