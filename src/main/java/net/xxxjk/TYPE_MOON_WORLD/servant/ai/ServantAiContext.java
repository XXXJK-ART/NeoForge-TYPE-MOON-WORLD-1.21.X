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
   public ServantAiDefinition aiConfig() {
      ServantAiDefinition configured = ServantAiDefinitionRegistry.get(this.definition.aiConfigId());
      if (configured != null) return configured;
      net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(this.definition.aiConfigId());
      net.xxxjk.typemoonworld.api.AiTacticProfile profile = net.xxxjk.TYPE_MOON_WORLD.api.ExtensionApiRegistry.ai(id);
      if (profile == null) return null;
      return new ServantAiDefinition(id.toString(),
         new ServantAiDefinition.Movement(profile.followDistance(), profile.followDistance(), false),
         new ServantAiDefinition.Combat(profile.attackDistance(), profile.retreatHealthRatio(), 1.0, profile.retreatHealthRatio(), 1.0),
         new ServantAiDefinition.Social(200, 0.1), new ServantAiDefinition.Command(0.8, 0.005),
         new ServantAiDefinition.Environment(java.util.List.of(), java.util.List.of(), "any"));
   }
}
