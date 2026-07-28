package net.xxxjk.TYPE_MOON_WORLD.servant.ai.module;

import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class CommandModule implements ServantAiModule {
   @Override
   public void tick(ServantEntity entity, ServantAiContext context) {
      if (context.aiConfig() == null) return;
      var command = context.aiConfig().command();
      double obedience = net.minecraft.util.Mth.clamp(
         command.baseObedienceRate() + entity.getFavor() * command.favorMultiplier(), 0.0, 1.0);
      entity.getPersistentData().putDouble("TypeMoonAiCommandObedience", obedience);
   }
}
