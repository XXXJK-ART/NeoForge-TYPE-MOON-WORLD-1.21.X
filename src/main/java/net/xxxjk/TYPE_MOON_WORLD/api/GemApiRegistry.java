package net.xxxjk.TYPE_MOON_WORLD.api;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemCompatibilityService;

public final class GemApiRegistry {
   private static final Map<ResourceLocation, net.xxxjk.typemoonworld.api.GemAffinity> AFFINITIES = new ConcurrentHashMap<>();
   private static final AtomicBoolean FROZEN = new AtomicBoolean(false);
   private GemApiRegistry() { }
   public static void freeze() { FROZEN.set(true); }
   public static boolean register(ResourceLocation id, net.xxxjk.typemoonworld.api.GemAffinity affinity) {
      return !FROZEN.get() && id != null && affinity != null && AFFINITIES.putIfAbsent(id, affinity) == null;
   }
   public static boolean contains(ResourceLocation id) {
      return id != null && (AFFINITIES.containsKey(id)
         || GemCompatibilityService.isWhitelistedMagic(id.toString())
         || GemCompatibilityService.isWhitelistedMagic(id.getPath()));
   }
   public static boolean hasCustom(ResourceLocation id) { return id != null && AFFINITIES.containsKey(id); }
   public static int calculate(ResourceLocation id, net.xxxjk.typemoonworld.api.GemType type,
                               net.xxxjk.typemoonworld.api.GemQuality quality, double proficiency) {
      if (id != null && AFFINITIES.containsKey(id)) {
         int base = switch (quality == null ? net.xxxjk.typemoonworld.api.GemQuality.NORMAL : quality) { case POOR -> 60; case NORMAL -> 80; case HIGH -> 95; };
         int chance = base + AFFINITIES.get(id).modifier(type) + (int)Math.round(Math.max(0.0, Math.min(100.0, proficiency)) * 0.15);
         return Math.max(5, Math.min(99, chance));
      }
      net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType internalType = type == null
         ? net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType.WHITE_GEMSTONE
         : net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType.valueOf(type.name());
      net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality internalQuality = quality == null
         ? net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality.NORMAL
         : net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality.valueOf(quality.name());
      String legacyId = id == null ? "" : id.getPath();
      return GemCompatibilityService.calculateEngraveSuccessChance(internalQuality, internalType, legacyId, proficiency);
   }
   public static Set<ResourceLocation> ids() {
      Set<ResourceLocation> ids = new LinkedHashSet<>(AFFINITIES.keySet());
      for (String magic : GemCompatibilityService.getWhitelistedMagics()) {
         ResourceLocation id = ResourceLocation.tryParse(magic);
         if (id != null) ids.add(id);
      }
      return Collections.unmodifiableSet(ids);
   }
}
