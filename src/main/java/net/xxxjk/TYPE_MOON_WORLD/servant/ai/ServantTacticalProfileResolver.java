package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.api.ExtensionApiRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.typemoonworld.api.AdvancedAiTacticProfile;
import net.xxxjk.typemoonworld.api.AiTacticProfile;

/** Resolves built-in and addon tactical profiles through one compatibility path. */
public final class ServantTacticalProfileResolver {
   private ServantTacticalProfileResolver() { }

   public static ServantAiDefinition.Tactical resolve(ServantEntity servant) {
      if (servant == null || servant.getDefinition() == null) return ServantAiDefinition.Tactical.DEFAULT;
      String configuredId = servant.getDefinition().aiConfigId();
      ServantAiDefinition builtIn = ServantAiDefinitionRegistry.get(configuredId);
      if (builtIn != null) return builtIn.tactical();

      ResourceLocation id = ResourceLocation.tryParse(configuredId);
      if (id == null) return ServantAiDefinition.Tactical.DEFAULT;
      AdvancedAiTacticProfile advanced = ExtensionApiRegistry.advancedAi(id);
      if (advanced == null) {
         AiTacticProfile base = ExtensionApiRegistry.ai(id);
         if (base == null) return ServantAiDefinition.Tactical.DEFAULT;
         advanced = AdvancedAiTacticProfile.compatible(base);
      }
      return fromAdvanced(advanced);
   }

   static ServantAiDefinition.Tactical fromAdvanced(AdvancedAiTacticProfile advanced) {
      return new ServantAiDefinition.Tactical(advanced.style().name().toLowerCase(), advanced.minimumRange(),
         advanced.preferredRange(), advanced.maximumRange(), advanced.repositionDistance(),
         advanced.pursuitAggression(), advanced.interceptBias(), advanced.verticalMobility(),
         advanced.recoveryTendency(), advanced.collateralCaution(), advanced.maximumTerrainImpact());
   }
}
