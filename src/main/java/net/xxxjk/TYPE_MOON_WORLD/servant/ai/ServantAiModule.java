package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public interface ServantAiModule {
   void tick(ServantEntity entity, ServantAiContext context);
}
