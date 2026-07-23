package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ServantAiDefinition(String id, Movement movement, Combat combat, Social social, Command command, Environment environment) {
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
      SOCIAL_CODEC.optionalFieldOf("social", new Social(200, 0.1)).forGetter(ServantAiDefinition::social),
      COMMAND_CODEC.optionalFieldOf("command", new Command(0.8, 0.005)).forGetter(ServantAiDefinition::command),
      ENVIRONMENT_CODEC.optionalFieldOf("environment", new Environment(List.of(), List.of(), "any")).forGetter(ServantAiDefinition::environment)
   ).apply(i, ServantAiDefinition::new));

   public ServantAiDefinition {
      id = id == null ? "" : id;
      movement = movement == null ? new Movement(6.0, 24.0, false) : movement;
      combat = combat == null ? new Combat(4.0, 0.25, 1.0, 0.2, 1.0) : combat;
      social = social == null ? new Social(200, 0.1) : social;
      command = command == null ? new Command(0.8, 0.005) : command;
      environment = environment == null ? new Environment(List.of(), List.of(), "any") : environment;
   }

   public record Movement(double followDistance, double wanderRadius, boolean likesHighPlaces) { }
   public record Combat(double preferredAttackDistance, double retreatHealthRatio, double meleePreference, double berserkHealthRatio, double berserkDamageMultiplier) { }
   public record Social(int talkInterval, double greetingProbability) { }
   public record Command(double baseObedienceRate, double favorMultiplier) { }
   public record Environment(List<String> likedBiomes, List<String> dislikedBiomes, String preferredWeather) { }
}
