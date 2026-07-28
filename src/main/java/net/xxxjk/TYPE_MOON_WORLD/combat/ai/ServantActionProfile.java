package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;

public record ServantActionProfile(String servant, List<AiActionDescriptor> actions, List<RivalRule> rivals) {
   private static final Codec<ServantCombatPhase> PHASE_CODEC = Codec.STRING.xmap(ServantCombatPhase::byName, phase -> phase.name().toLowerCase());
   public static final Codec<RivalRule> RIVAL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.fieldOf("opponent_servant").forGetter(RivalRule::opponentServant),
      PHASE_CODEC.optionalFieldOf("minimum_phase", ServantCombatPhase.NORMAL).forGetter(RivalRule::minimumPhase),
      Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("action_weights", Map.of()).forGetter(RivalRule::actionWeights)
   ).apply(instance, RivalRule::new));
   public static final Codec<ServantActionProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.fieldOf("servant").forGetter(ServantActionProfile::servant),
      AiActionDescriptor.CODEC.listOf().fieldOf("actions").forGetter(ServantActionProfile::actions),
      RIVAL_CODEC.listOf().optionalFieldOf("rivals", List.of()).forGetter(ServantActionProfile::rivals)
   ).apply(instance, ServantActionProfile::new));

   public ServantActionProfile {
      servant = servant == null ? "" : servant;
      actions = actions == null ? List.of() : List.copyOf(actions);
      rivals = rivals == null ? List.of() : List.copyOf(rivals);
   }

   public record RivalRule(String opponentServant, ServantCombatPhase minimumPhase, Map<String, Double> actionWeights) {
      public RivalRule {
         opponentServant = opponentServant == null ? "" : opponentServant;
         minimumPhase = minimumPhase == null ? ServantCombatPhase.NORMAL : minimumPhase;
         actionWeights = actionWeights == null ? Map.of() : Map.copyOf(actionWeights);
      }
   }
}
