package net.xxxjk.TYPE_MOON_WORLD.servant.model;

import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.CombatDisposition;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.MoralAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.ObedienceAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.PrincipleAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.SpecialTargetPrinciple;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.SocialDisposition;

public record ServantDefinition(
   String id,
   String displayName,
   String displayNameZh,
   ServantClassType classType,
   ServantFaction faction,
   List<ServantTraitTag> traits,
   ServantParams parameters,
   ServantAnimations animations,
   ServantSpecialization specialization,
   String modelGeometryPath,
   String texturePath,
   String animationPath,
   List<String> skillIds,
   String noblePhantasmId,
   ObedienceAxis defaultObedience,
   PrincipleAxis defaultPrinciple,
   MoralAxis defaultMorality,
   SocialDisposition defaultSocial,
   CombatDisposition defaultCombat,
   List<SpecialTargetPrinciple> specialTargetPrinciples,
   double startingFavor,
   String aiConfigId,
   int primaryEggColor,
   int secondaryEggColor,
   ServantEra era,
   List<String> noblePhantasmIds
) {
   public ServantDefinition {
      era = era == null ? ServantEra.MODERN : era;
      noblePhantasmIds = noblePhantasmIds == null || noblePhantasmIds.isEmpty()
         ? (noblePhantasmId == null || noblePhantasmId.isBlank() ? List.of() : List.of(noblePhantasmId)) : List.copyOf(noblePhantasmIds);
   }
   public ServantDefinition(
   String id,
   String displayName,
   String displayNameZh,
   ServantClassType classType,
   ServantFaction faction,
   List<ServantTraitTag> traits,
   ServantParams parameters,
   ServantAnimations animations,
   ServantSpecialization specialization,
   String modelGeometryPath,
   String texturePath,
   String animationPath,
   List<String> skillIds,
   String noblePhantasmId,
   ObedienceAxis defaultObedience,
   PrincipleAxis defaultPrinciple,
   MoralAxis defaultMorality,
   SocialDisposition defaultSocial,
   CombatDisposition defaultCombat,
   List<SpecialTargetPrinciple> specialTargetPrinciples,
   double startingFavor,
   String aiConfigId,
   int primaryEggColor,
   int secondaryEggColor,
   ServantEra era) {
      this(id, displayName, displayNameZh, classType, faction, traits, parameters, animations, specialization, modelGeometryPath, texturePath, animationPath, skillIds, noblePhantasmId, defaultObedience, defaultPrinciple, defaultMorality, defaultSocial, defaultCombat, specialTargetPrinciples, startingFavor, aiConfigId, primaryEggColor, secondaryEggColor, era, null);
   }
   public ServantDefinition(String id, String displayName, String displayNameZh, ServantClassType classType,
      ServantFaction faction, List<ServantTraitTag> traits, ServantParams parameters, ServantAnimations animations,
      ServantSpecialization specialization, String modelGeometryPath, String texturePath, String animationPath,
      List<String> skillIds, String noblePhantasmId, ObedienceAxis defaultObedience, PrincipleAxis defaultPrinciple,
      MoralAxis defaultMorality, SocialDisposition defaultSocial, CombatDisposition defaultCombat,
      List<SpecialTargetPrinciple> specialTargetPrinciples, double startingFavor, String aiConfigId,
      int primaryEggColor, int secondaryEggColor) {
      this(id, displayName, displayNameZh, classType, faction, traits, parameters, animations, specialization,
         modelGeometryPath, texturePath, animationPath, skillIds, noblePhantasmId, defaultObedience, defaultPrinciple,
         defaultMorality, defaultSocial, defaultCombat, specialTargetPrinciples, startingFavor, aiConfigId,
         primaryEggColor, secondaryEggColor, ServantEra.MODERN);
   }
}
