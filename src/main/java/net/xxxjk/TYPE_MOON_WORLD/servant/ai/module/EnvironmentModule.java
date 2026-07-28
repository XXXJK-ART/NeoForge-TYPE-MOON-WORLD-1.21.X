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
      if (context.aiConfig() == null || entity.tickCount % 40 != Math.floorMod(entity.getId(), 40)) return;
      var environment = context.aiConfig().environment();
      String biome = entity.level().getBiome(entity.blockPosition()).unwrapKey().map(key -> key.location().toString()).orElse("");
      double comfort = environment.likedBiomes().contains(biome) ? 1.0 : environment.dislikedBiomes().contains(biome) ? -1.0 : 0.0;
      String weather = environment.preferredWeather();
      boolean preferred = "any".equals(weather)
         || "rain".equals(weather) && entity.level().isRaining()
         || "clear".equals(weather) && !entity.level().isRaining()
         || "thunder".equals(weather) && entity.level().isThundering();
      entity.getPersistentData().putDouble("TypeMoonAiEnvironmentComfort", comfort + (preferred ? 0.25 : -0.25));
   }
}
