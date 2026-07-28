package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;

public record AiActionDescriptor(
   ResourceLocation id,
   Set<Tag> tags,
   double minimumRange,
   double maximumRange,
   double manaCost,
   double staminaCost,
   Timing timing,
   ThreatSpec threat,
   TerrainImpactProfile.Tier terrainTier
) {
   private static final Codec<Tag> TAG_CODEC = Codec.STRING.xmap(value -> Tag.valueOf(value.toUpperCase()), value -> value.name().toLowerCase());
   private static final Codec<CombatThreat.Shape> SHAPE_CODEC = Codec.STRING.xmap(value -> CombatThreat.Shape.valueOf(value.toUpperCase()), value -> value.name().toLowerCase());
   private static final Codec<TerrainImpactProfile.Tier> TIER_CODEC = Codec.STRING.xmap(value -> TerrainImpactProfile.Tier.valueOf(value.toUpperCase()), value -> value.name().toLowerCase());
   public static final Codec<Timing> TIMING_CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.optionalFieldOf("windup", 0).forGetter(Timing::windupTicks),
      Codec.INT.optionalFieldOf("active", 1).forGetter(Timing::activeTicks),
      Codec.INT.optionalFieldOf("recovery", 0).forGetter(Timing::recoveryTicks)
   ).apply(instance, Timing::new));
   public static final Codec<ThreatSpec> THREAT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
      SHAPE_CODEC.optionalFieldOf("shape", CombatThreat.Shape.POINT).forGetter(ThreatSpec::shape),
      Codec.DOUBLE.optionalFieldOf("radius", 0.0).forGetter(ThreatSpec::radius),
      Codec.DOUBLE.optionalFieldOf("length", 0.0).forGetter(ThreatSpec::length),
      Codec.INT.optionalFieldOf("danger", 0).forGetter(ThreatSpec::danger),
      Codec.BOOL.optionalFieldOf("blockable", true).forGetter(ThreatSpec::blockable),
      Codec.BOOL.optionalFieldOf("dodgeable", true).forGetter(ThreatSpec::dodgeable),
      Codec.BOOL.optionalFieldOf("interruptible", true).forGetter(ThreatSpec::interruptible),
      Codec.DOUBLE.optionalFieldOf("friendly_fire_radius", 0.0).forGetter(ThreatSpec::collateralRadius)
   ).apply(instance, ThreatSpec::new));
   public static final Codec<AiActionDescriptor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      ResourceLocation.CODEC.fieldOf("id").forGetter(AiActionDescriptor::id),
      TAG_CODEC.listOf().optionalFieldOf("tags", List.of()).xmap(Set::copyOf, List::copyOf).forGetter(AiActionDescriptor::tags),
      Codec.DOUBLE.optionalFieldOf("minimum_range", 0.0).forGetter(AiActionDescriptor::minimumRange),
      Codec.DOUBLE.optionalFieldOf("maximum_range", 4.0).forGetter(AiActionDescriptor::maximumRange),
      Codec.DOUBLE.optionalFieldOf("mana_cost", 0.0).forGetter(AiActionDescriptor::manaCost),
      Codec.DOUBLE.optionalFieldOf("stamina_cost", 0.0).forGetter(AiActionDescriptor::staminaCost),
      TIMING_CODEC.optionalFieldOf("timing", new Timing(0, 1, 0)).forGetter(AiActionDescriptor::timing),
      THREAT_CODEC.optionalFieldOf("threat", ThreatSpec.NONE).forGetter(AiActionDescriptor::threat),
      TIER_CODEC.optionalFieldOf("terrain_impact", TerrainImpactProfile.Tier.NONE).forGetter(AiActionDescriptor::terrainTier)
   ).apply(instance, AiActionDescriptor::new));
   public AiActionDescriptor {
      if (id == null) throw new IllegalArgumentException("action id cannot be null");
      tags = tags == null || tags.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(tags));
      minimumRange = Math.max(0.0, minimumRange);
      maximumRange = Math.max(minimumRange, maximumRange);
      manaCost = Math.max(0.0, manaCost);
      staminaCost = Math.max(0.0, staminaCost);
      timing = timing == null ? new Timing(0, 1, 0) : timing;
      threat = threat == null ? ThreatSpec.NONE : threat;
      terrainTier = terrainTier == null ? TerrainImpactProfile.Tier.NONE : terrainTier;
   }

   public enum Tag { MELEE, PROJECTILE, AREA, NOBLE_PHANTASM, GUARD, EVADE, INTERRUPT, HEAL, CONTROL, SUMMON }
   public record Timing(int windupTicks, int activeTicks, int recoveryTicks) {
      public Timing { windupTicks=Math.max(0,windupTicks); activeTicks=Math.max(1,activeTicks); recoveryTicks=Math.max(0,recoveryTicks); }
   }
   public record ThreatSpec(CombatThreat.Shape shape, double radius, double length, int danger,
                            boolean blockable, boolean dodgeable, boolean interruptible, double collateralRadius) {
      public static final ThreatSpec NONE = new ThreatSpec(CombatThreat.Shape.POINT, 0.0, 0.0, 0, true, true, true, 0.0);
      public ThreatSpec { radius=Math.max(0.0,radius); length=Math.max(0.0,length); danger=Math.max(0,danger); collateralRadius=Math.max(0.0,collateralRadius); }
   }
}
