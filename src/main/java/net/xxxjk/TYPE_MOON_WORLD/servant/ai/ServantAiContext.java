package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.ServantBehaviorProfile;

public record ServantAiContext(
   ServantEntity entity,
   @Nullable LivingEntity target,
   ServantDefinition definition,
   ServantBehaviorProfile behaviorProfile,
   long gameTick
) {
}
