package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import net.minecraft.resources.ResourceLocation;

/** Immutable definition shared by the editor, validation and executor. */
public final class RuneDefinition {
   private final ResourceLocation id;
   private final String displayName;
   private final int color;
   private final ResourceLocation icon;
   private final double baseCost;
   private final Set<String> mediaTags;
   private final Set<String> conflictTags;
   private final Set<String> fusionTags;
   private final EnumMap<RunePosition, String> semantics;

   public RuneDefinition(ResourceLocation id, String displayName, int color, ResourceLocation icon,
      double baseCost, Set<String> mediaTags, Set<String> conflictTags, Set<String> fusionTags,
      Map<RunePosition, String> semantics) {
      if (id == null || displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Rune id/name is required");
      this.id = id;
      this.displayName = displayName;
      this.color = color;
      this.icon = icon == null ? id : icon;
      this.baseCost = Math.max(0.0D, baseCost);
      this.mediaTags = immutable(mediaTags);
      this.conflictTags = immutable(conflictTags);
      this.fusionTags = immutable(fusionTags);
      this.semantics = new EnumMap<>(RunePosition.class);
      if (semantics != null) this.semantics.putAll(semantics);
      for (RunePosition position : RunePosition.values()) this.semantics.putIfAbsent(position, "");
   }

   private static Set<String> immutable(Set<String> values) {
      return values == null ? Set.of() : Collections.unmodifiableSet(Set.copyOf(values));
   }

   public ResourceLocation id() { return id; }
   public String idPath() { return id.getPath(); }
   public String displayName() { return displayName; }
   public int color() { return color; }
   public ResourceLocation icon() { return icon; }
   public double baseCost() { return baseCost; }
   public Set<String> mediaTags() { return mediaTags; }
   public Set<String> conflictTags() { return conflictTags; }
   public Set<String> fusionTags() { return fusionTags; }
   public String semantic(RunePosition position) { return semantics.getOrDefault(position, ""); }
   public Map<RunePosition, String> semantics() { return Collections.unmodifiableMap(semantics); }
}
