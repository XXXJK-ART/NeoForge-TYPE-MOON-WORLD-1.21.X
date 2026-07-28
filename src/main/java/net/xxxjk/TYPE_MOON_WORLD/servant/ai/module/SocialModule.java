package net.xxxjk.TYPE_MOON_WORLD.servant.ai.module;

import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.minecraft.world.entity.player.Player;

public final class SocialModule implements ServantAiModule {
   @Override
   public void tick(ServantEntity entity, ServantAiContext context) {
      if (context.target() != null || context.aiConfig() == null) return;
      var social = context.aiConfig().social();
      long next = entity.getPersistentData().getLong("TypeMoonAiNextSocialTick");
      if (context.gameTick() < next) return;
      entity.getPersistentData().putLong("TypeMoonAiNextSocialTick", context.gameTick() + Math.max(40, social.talkInterval()));
      if (entity.getRandom().nextDouble() > social.greetingProbability()) return;
      Player player = entity.level().getNearestPlayer(entity, 8.0);
      if (player != null && entity.isAlliedTo(player)) {
         entity.getLookControl().setLookAt(player, 20.0F, 20.0F);
         entity.playAmbientSound();
      }
   }
}
