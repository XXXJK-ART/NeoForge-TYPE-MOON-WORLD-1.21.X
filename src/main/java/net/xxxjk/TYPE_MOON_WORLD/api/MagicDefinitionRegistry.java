package net.xxxjk.TYPE_MOON_WORLD.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;

public final class MagicDefinitionRegistry {
   private static final Map<String, MagicDefinitionData> PROGRAMMATIC = new ConcurrentHashMap<>();
   private static volatile Map<String, MagicDefinitionData> dataDefinitions = Map.of();
   private static final AtomicBoolean FROZEN = new AtomicBoolean(false);
   private MagicDefinitionRegistry() { }
   public static void freeze() { FROZEN.set(true); }

   /** Compatibility bridge: exposes every legacy built-in as a namespaced definition. */
   public static void bootstrapBuiltins() {
      if (FROZEN.get()) return;
      for (String path : net.xxxjk.TYPE_MOON_WORLD.magic.MagicClassification.getAllMagicIds()) {
         if (path.indexOf(':') >= 0) continue;
         ResourceLocation id = ResourceLocation.fromNamespaceAndPath(net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID, path);
         ResourceLocation school = ResourceLocation.fromNamespaceAndPath(net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID,
            net.xxxjk.TYPE_MOON_WORLD.magic.MagicClassification.getSchoolType(path).name().toLowerCase(java.util.Locale.ROOT));
         MagicDefinitionData defaults = MagicDefinitionData.defaults(id);
         PROGRAMMATIC.putIfAbsent(id.toString(), new MagicDefinitionData(id, defaults.nameKey(), defaults.category(), school,
            defaults.complexity(), defaults.chantSegments(), defaults.manaCost(), defaults.sustainedManaCost(),
            defaults.cooldownTicks(), defaults.learnable(), defaults.wheelSelectable(), defaults.crestAllowed(),
            defaults.npcAllowed(), defaults.knowledgeOnly(), defaults.copyable(), defaults.npcGlobalCooldown(),
            defaults.npcCooldown(), defaults.prerequisiteMagic(), defaults.prerequisiteProficiency(),
            defaults.requiredAttributes(), defaults.resolvedResistanceComplexity()));
      }
   }

   public static boolean register(MagicDefinitionData definition, String provider) {
      if (FROZEN.get() || definition == null) return false;
      return PROGRAMMATIC.putIfAbsent(definition.id().toString(), definition) == null;
   }

   public static void reload(Map<String, MagicDefinitionData> definitions) {
      dataDefinitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
   }

   public static MagicDefinitionData get(String id) {
      if (id == null) return null;
      MagicDefinitionData data = dataDefinitions.get(id);
      if (data != null) return data;
      data = PROGRAMMATIC.get(id);
      if (data != null || id.indexOf(':') >= 0) return data;
      String namespaced = net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID + ":" + id;
      data = dataDefinitions.get(namespaced);
      return data != null ? data : PROGRAMMATIC.get(namespaced);
   }

   public static boolean contains(String id) { return get(id) != null; }

   public static Set<String> ids() {
      java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>(PROGRAMMATIC.keySet());
      ids.addAll(dataDefinitions.keySet());
      return Collections.unmodifiableSet(ids);
   }

   public static Map<String, MagicDefinitionData> all() {
      Map<String, MagicDefinitionData> merged = new LinkedHashMap<>(PROGRAMMATIC);
      merged.putAll(dataDefinitions);
      return Collections.unmodifiableMap(merged);
   }

   public static boolean isCrestAllowed(String id) {
      MagicDefinitionData definition = get(id);
      return definition == null || definition.crestAllowed();
   }

   public static boolean isWheelSelectable(String id) {
      MagicDefinitionData definition = get(id);
      return definition == null || definition.wheelSelectable();
   }

   public static boolean isKnowledgeOnly(String id) {
      MagicDefinitionData definition = get(id);
      return definition != null && definition.knowledgeOnly();
   }

   public static boolean isCopyable(String id) {
      MagicDefinitionData definition = get(id);
      return definition == null || definition.copyable();
   }

   public static boolean hasPrerequisiteMagic(String id) {
      MagicDefinitionData definition = get(id);
      return definition != null && definition.prerequisiteMagic() != null;
   }

   public static String prerequisiteMagic(String id) {
      MagicDefinitionData definition = get(id);
      return definition == null || definition.prerequisiteMagic() == null ? null : definition.prerequisiteMagic().toString();
   }

   public static double prerequisiteProficiency(String id) {
      MagicDefinitionData definition = get(id);
      return definition == null ? 0.0 : definition.prerequisiteProficiency();
   }

   public static int complexity(String id) {
      MagicDefinitionData definition = get(id);
      return definition == null ? 50 : definition.complexity();
   }

   public static int chantSegments(String id) {
      MagicDefinitionData definition = get(id);
      return definition == null ? 1 : definition.chantSegments();
   }

   public static double sustainedManaCost(String id) {
      MagicDefinitionData definition = get(id);
      return definition == null ? 0.0 : definition.sustainedManaCost();
   }

   public static boolean meetsAttributeRequirements(
         net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PlayerVariables vars, String id) {
      MagicDefinitionData definition = get(id);
      return definition == null || MagicAttributeService.meets(vars, definition.requiredAttributes());
   }
}
