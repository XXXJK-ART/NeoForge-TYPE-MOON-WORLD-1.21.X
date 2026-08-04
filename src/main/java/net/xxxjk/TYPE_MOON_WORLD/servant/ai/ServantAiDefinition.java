package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ServantAiDefinition(String id, Movement movement, Combat combat, Tactical tactical,
                                  Social social, Command command, Environment environment) {
   public static final Codec<Movement> MOVEMENT_CODEC = RecordCodecBuilder.create(i -> i.group(
      Codec.DOUBLE.optionalFieldOf("follow_distance", 6.0).forGetter(Movement::followDistance),
      Codec.DOUBLE.optionalFieldOf("wander_radius", 24.0).forGetter(Movement::wanderRadius),
      Codec.BOOL.optionalFieldOf("likes_high_places", false).forGetter(Movement::likesHighPlaces)
   ).apply(i, Movement::new));
   public static final Codec<Combat> COMBAT_CODEC = RecordCodecBuilder.create(i -> i.group(
      Codec.DOUBLE.optionalFieldOf("preferred_attack_distance", 4.0).forGetter(Combat::preferredAttackDistance),
      Codec.DOUBLE.optionalFieldOf("retreat_health_ratio", 0.25).forGetter(Combat::retreatHealthRatio),
      Codec.DOUBLE.optionalFieldOf("melee_preference", 1.0).forGetter(Combat::meleePreference),
      Codec.DOUBLE.optionalFieldOf("berserk_health_ratio", 0.2).forGetter(Combat::berserkHealthRatio),
      Codec.DOUBLE.optionalFieldOf("berserk_damage_multiplier", 1.0).forGetter(Combat::berserkDamageMultiplier)
   ).apply(i, Combat::new));
   public static final Codec<Tactical> TACTICAL_CODEC = RecordCodecBuilder.create(i -> i.group(
      Codec.STRING.optionalFieldOf("style", "balanced").forGetter(Tactical::style),
      Codec.DOUBLE.optionalFieldOf("minimum_range", 3.0).forGetter(Tactical::minimumRange),
      Codec.DOUBLE.optionalFieldOf("preferred_range", 8.0).forGetter(Tactical::preferredRange),
      Codec.DOUBLE.optionalFieldOf("maximum_range", 24.0).forGetter(Tactical::maximumRange),
      Codec.DOUBLE.optionalFieldOf("reposition_distance", 10.0).forGetter(Tactical::repositionDistance),
      Codec.DOUBLE.optionalFieldOf("pursuit_aggression", 0.5).forGetter(Tactical::pursuitAggression),
      Codec.DOUBLE.optionalFieldOf("intercept_bias", 1.5).forGetter(Tactical::interceptBias),
      Codec.DOUBLE.optionalFieldOf("vertical_mobility", 0.25).forGetter(Tactical::verticalMobility),
      Codec.DOUBLE.optionalFieldOf("recovery_tendency", 0.5).forGetter(Tactical::recoveryTendency),
      Codec.DOUBLE.optionalFieldOf("collateral_caution", 0.7).forGetter(Tactical::collateralCaution),
      Codec.STRING.optionalFieldOf("maximum_terrain_impact", "small").forGetter(Tactical::maximumTerrainImpact)
   ).apply(i, Tactical::new));
   public static final Codec<Social> SOCIAL_CODEC = RecordCodecBuilder.create(i -> i.group(
      Codec.INT.optionalFieldOf("talk_interval", 200).forGetter(Social::talkInterval),
      Codec.DOUBLE.optionalFieldOf("greeting_probability", 0.1).forGetter(Social::greetingProbability)
   ).apply(i, Social::new));
   public static final Codec<Command> COMMAND_CODEC = RecordCodecBuilder.create(i -> i.group(
      Codec.DOUBLE.optionalFieldOf("base_obedience_rate", 0.8).forGetter(Command::baseObedienceRate),
      Codec.DOUBLE.optionalFieldOf("favor_multiplier", 0.005).forGetter(Command::favorMultiplier)
   ).apply(i, Command::new));
   public static final Codec<Environment> ENVIRONMENT_CODEC = RecordCodecBuilder.create(i -> i.group(
      Codec.STRING.listOf().optionalFieldOf("liked_biomes", List.of()).forGetter(Environment::likedBiomes),
      Codec.STRING.listOf().optionalFieldOf("disliked_biomes", List.of()).forGetter(Environment::dislikedBiomes),
      Codec.STRING.optionalFieldOf("preferred_weather", "any").forGetter(Environment::preferredWeather)
   ).apply(i, Environment::new));
   public static final Codec<ServantAiDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
      Codec.STRING.optionalFieldOf("id", "").forGetter(ServantAiDefinition::id),
      MOVEMENT_CODEC.optionalFieldOf("movement", new Movement(6.0, 24.0, false)).forGetter(ServantAiDefinition::movement),
      COMBAT_CODEC.optionalFieldOf("combat", new Combat(4.0, 0.25, 1.0, 0.2, 1.0)).forGetter(ServantAiDefinition::combat),
      TACTICAL_CODEC.optionalFieldOf("tactical", Tactical.DEFAULT).forGetter(ServantAiDefinition::tactical),
      SOCIAL_CODEC.optionalFieldOf("social", new Social(200, 0.1)).forGetter(ServantAiDefinition::social),
      COMMAND_CODEC.optionalFieldOf("command", new Command(0.8, 0.005)).forGetter(ServantAiDefinition::command),
      ENVIRONMENT_CODEC.optionalFieldOf("environment", new Environment(List.of(), List.of(), "any")).forGetter(ServantAiDefinition::environment)
   ).apply(i, ServantAiDefinition::new));

   public ServantAiDefinition {
      id = id == null ? "" : id;
      movement = movement == null ? new Movement(6.0, 24.0, false) : movement;
      combat = combat == null ? new Combat(4.0, 0.25, 1.0, 0.2, 1.0) : combat;
      tactical = tactical == null ? Tactical.DEFAULT : tactical;
      social = social == null ? new Social(200, 0.1) : social;
      command = command == null ? new Command(0.8, 0.005) : command;
      environment = environment == null ? new Environment(List.of(), List.of(), "any") : environment;
   }

   public record Movement(double followDistance, double wanderRadius, boolean likesHighPlaces) { }
   public record Combat(double preferredAttackDistance, double retreatHealthRatio, double meleePreference, double berserkHealthRatio, double berserkDamageMultiplier) { }
   public record Tactical(String style, double minimumRange, double preferredRange, double maximumRange,
                          double repositionDistance, double pursuitAggression, double interceptBias,
                          double verticalMobility, double recoveryTendency, double collateralCaution,
                          String maximumTerrainImpact) {
      public static final Tactical DEFAULT = new Tactical("balanced", 3.0, 8.0, 24.0, 10.0,
         0.5, 1.5, 0.25, 0.5, 0.7, "small");

      public Tactical {
         style = style == null || style.isBlank() ? "balanced" : style;
         minimumRange = Math.max(0.0, Math.min(48.0, minimumRange));
         preferredRange = Math.max(minimumRange, Math.min(48.0, preferredRange));
         maximumRange = Math.max(preferredRange, Math.min(48.0, maximumRange));
         repositionDistance = Math.max(0.0, Math.min(24.0, repositionDistance));
         pursuitAggression = clamp01(pursuitAggression);
         interceptBias = Math.max(0.0, Math.min(8.0, interceptBias));
         verticalMobility = clamp01(verticalMobility);
         recoveryTendency = clamp01(recoveryTendency);
         collateralCaution = clamp01(collateralCaution);
         maximumTerrainImpact = maximumTerrainImpact == null || maximumTerrainImpact.isBlank() ? "small" : maximumTerrainImpact;
      }

      private static double clamp01(double value) {
         return Math.max(0.0, Math.min(1.0, value));
      }
   }
   public record Social(int talkInterval, double greetingProbability) { }
   public record Command(double baseObedienceRate, double favorMultiplier) { }
   public record Environment(List<String> likedBiomes, List<String> dislikedBiomes, String preferredWeather) { }
}
