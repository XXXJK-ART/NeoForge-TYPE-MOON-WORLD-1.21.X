package net.xxxjk.TYPE_MOON_WORLD.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.typemoonworld.api.AiTacticProfile;
import net.xxxjk.typemoonworld.api.AdvancedAiTacticProfile;
import net.xxxjk.typemoonworld.api.CommandSpellContext;
import net.xxxjk.typemoonworld.api.CommandSpellExecutor;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.MagicOption;
import net.xxxjk.typemoonworld.api.NoblePhantasmProjectileContext;
import net.xxxjk.typemoonworld.api.NoblePhantasmProjectileExecutor;
import net.xxxjk.typemoonworld.api.ProjectionItemContext;
import net.xxxjk.typemoonworld.api.ProjectionItemExecutor;
import net.xxxjk.typemoonworld.api.ProjectionStructureContext;
import net.xxxjk.typemoonworld.api.ProjectionStructureExecutor;

/** Runtime-only callbacks. Static game registries remain owned by NeoForge/addon mods. */
public final class ExtensionApiRegistry {
   private static final AtomicBoolean FROZEN = new AtomicBoolean();
   private static final Map<String, ProjectionItemExecutor> PROJECTION_ITEMS = new ConcurrentHashMap<>();
   private static final Map<String, ProjectionStructureExecutor> PROJECTION_STRUCTURES = new ConcurrentHashMap<>();
   private static final Map<String, CommandSpellExecutor> COMMAND_SPELLS = new ConcurrentHashMap<>();
   private static final Map<String, NoblePhantasmProjectileExecutor> PROJECTILES = new ConcurrentHashMap<>();
   private static final Map<String, AiTacticProfile> AI = new ConcurrentHashMap<>();
   private static final Map<String, AdvancedAiTacticProfile> ADVANCED_AI = new ConcurrentHashMap<>();
   private static final Map<String, List<MagicOption>> CONTROLS = new ConcurrentHashMap<>();

   private ExtensionApiRegistry() { }
   public static void freeze() { FROZEN.set(true); }
   private static boolean open(ResourceLocation id, Object value) { return !FROZEN.get() && id != null && value != null; }

   public static boolean registerProjectionItem(ResourceLocation id, ProjectionItemExecutor executor) {
      return open(id, executor) && PROJECTION_ITEMS.putIfAbsent(id.toString(), executor) == null;
   }
   public static boolean registerProjectionStructure(ResourceLocation id, ProjectionStructureExecutor executor) {
      return open(id, executor) && PROJECTION_STRUCTURES.putIfAbsent(id.toString(), executor) == null;
   }
   public static boolean projectionItem(ResourceLocation id, ProjectionItemContext context) {
      ProjectionItemExecutor executor = id == null ? null : PROJECTION_ITEMS.get(id.toString());
      try { return executor != null && executor.execute(context); }
      catch (RuntimeException exception) { net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Projection item executor failed: {}", id, exception); return false; }
   }
   public static boolean hasProjectionItem(ResourceLocation id) { return id != null && PROJECTION_ITEMS.containsKey(id.toString()); }
   public static boolean hasProjectionStructure(ResourceLocation id) { return id != null && PROJECTION_STRUCTURES.containsKey(id.toString()); }
   public static boolean projectionStructure(ResourceLocation id, ProjectionStructureContext context) {
      ProjectionStructureExecutor executor = id == null ? null : PROJECTION_STRUCTURES.get(id.toString());
      try { return executor != null && executor.execute(context); }
      catch (RuntimeException exception) { net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Projection structure executor failed: {}", id, exception); return false; }
   }
   public static boolean registerCommand(ResourceLocation id, CommandSpellExecutor executor) {
      return open(id, executor) && COMMAND_SPELLS.putIfAbsent(id.toString(), executor) == null;
   }
   public static ExecutionResult command(ResourceLocation id, CommandSpellContext context) {
      CommandSpellExecutor executor = id == null ? null : COMMAND_SPELLS.get(id.toString());
      if (executor == null) return ExecutionResult.NOT_HANDLED;
      try { ExecutionResult value = executor.execute(context); return value == null ? ExecutionResult.FAILED : value; }
      catch (RuntimeException exception) { net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Command spell executor failed: {}", id, exception); return ExecutionResult.FAILED; }
   }
   public static boolean registerProjectile(ResourceLocation id, NoblePhantasmProjectileExecutor executor) {
      return open(id, executor) && PROJECTILES.putIfAbsent(id.toString(), executor) == null;
   }
   public static boolean projectile(ResourceLocation id, NoblePhantasmProjectileContext context) {
      NoblePhantasmProjectileExecutor executor = id == null ? null : PROJECTILES.get(id.toString());
      try { return executor != null && executor.fire(context); }
      catch (RuntimeException exception) { net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("NP projectile executor failed: {}", id, exception); return false; }
   }
   public static boolean registerAi(ResourceLocation id, AiTacticProfile profile) {
      return open(id, profile) && AI.putIfAbsent(id.toString(), profile) == null;
   }
   public static AiTacticProfile ai(ResourceLocation id) { return id == null ? null : AI.get(id.toString()); }
   public static boolean registerAdvancedAi(ResourceLocation id, AdvancedAiTacticProfile profile) {
      if (!open(id, profile) || AI.containsKey(id.toString())) return false;
      AI.put(id.toString(), profile.base());
      ADVANCED_AI.put(id.toString(), profile);
      return true;
   }
   public static AdvancedAiTacticProfile advancedAi(ResourceLocation id) {
      return id == null ? null : ADVANCED_AI.get(id.toString());
   }
   public static List<ResourceLocation> aiIds() { return AI.keySet().stream().map(ResourceLocation::tryParse).filter(java.util.Objects::nonNull).sorted().toList(); }
   public static boolean registerControl(ResourceLocation id, MagicOption option) {
      if (!open(id, option)) return false;
      synchronized (CONTROLS) {
         List<MagicOption> old = CONTROLS.getOrDefault(id.toString(), List.of());
         if (old.stream().anyMatch(entry -> entry.key().equals(option.key()))) return false;
         List<MagicOption> next = new ArrayList<>(old); next.add(option);
         CONTROLS.put(id.toString(), List.copyOf(next)); return true;
      }
   }
   public static List<MagicOption> controls(ResourceLocation id) { return id == null ? List.of() : CONTROLS.getOrDefault(id.toString(), List.of()); }

   /**
    * Resolves both namespaced IDs and legacy wheel entries that only store a
    * magic path, including paths registered by addon namespaces.
    */
   public static ResourceLocation resolveControlId(String rawId) {
      if (rawId == null || rawId.isBlank()) return null;
      ResourceLocation direct = ResourceLocation.tryParse(rawId);
      if (direct != null && !controls(direct).isEmpty()) return direct;
      String path = direct == null ? rawId : direct.getPath();
      synchronized (CONTROLS) {
         for (String key : CONTROLS.keySet()) {
            ResourceLocation candidate = ResourceLocation.tryParse(key);
            if (candidate != null && candidate.getPath().equals(path) && !controls(candidate).isEmpty()) {
               return candidate;
            }
         }
      }
      return direct;
   }

   public static List<MagicOption> controlsFor(String rawId) {
      ResourceLocation resolved = resolveControlId(rawId);
      return controls(resolved);
   }
}
