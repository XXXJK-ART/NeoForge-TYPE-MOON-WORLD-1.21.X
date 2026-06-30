package net.xxxjk.TYPE_MOON_WORLD.servant.ai.module;

import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaWorkshopHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusWorkshopHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class EnvironmentModule implements ServantAiModule {
   @Override
   public void tick(ServantEntity entity, ServantAiContext context) {
      if (entity instanceof MedeaEntity medea) {
         MedeaWorkshopHelper.tickEnvironment(medea);
      }
      if (entity instanceof ParacelsusEntity paracelsus) {
         ParacelsusWorkshopHelper.tickEnvironment(paracelsus);
      }
   }
}
