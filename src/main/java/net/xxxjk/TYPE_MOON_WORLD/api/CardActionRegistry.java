package net.xxxjk.TYPE_MOON_WORLD.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.typemoonworld.api.CardActionContext;
import net.xxxjk.typemoonworld.api.CardActionExecutor;
import net.xxxjk.typemoonworld.api.ExecutionResult;

public final class CardActionRegistry {
   private static final Map<String, Entry> ACTIONS = new ConcurrentHashMap<>();
   private static final Map<String, SlotBinding> SLOT_BINDINGS = new ConcurrentHashMap<>();
   private static final Map<String, SlotBinding> DATA_BINDINGS = new ConcurrentHashMap<>();
   private static final AtomicBoolean FROZEN = new AtomicBoolean(false);
   private CardActionRegistry() { }
   public static void freeze() { FROZEN.set(true); }

   public static boolean register(ResourceLocation id, CardActionExecutor executor, String provider) {
      if (FROZEN.get() || id == null || executor == null || !id.toString().matches("[a-z0-9_]+:[a-z0-9_./-]+")) return false;
      return ACTIONS.putIfAbsent(id.toString(), new Entry(executor, provider)) == null;
   }

   public static boolean bindSlot(ResourceLocation servantId, int slot, String actionId, String provider) {
      return bindSlot(servantId, slot, actionId, "", provider);
   }

   public static boolean bindSlot(ResourceLocation servantId, int slot, String actionId, String translationKey, String provider) {
      if (FROZEN.get() || servantId == null || actionId == null || actionId.isBlank() || slot < -1 || slot > 9) return false;
      SLOT_BINDINGS.put(servantId + "#" + slot, new SlotBinding(actionId, translationKey == null ? "" : translationKey));
      return true;
   }

   public static boolean bindDataSlot(ResourceLocation servantId, int slot, String actionId, String translationKey) {
      if (servantId == null || actionId == null || actionId.isBlank() || slot < -1 || slot > 9) return false;
      DATA_BINDINGS.put(servantId + "#" + slot, new SlotBinding(actionId, translationKey == null ? "" : translationKey));
      return true;
   }

   public static void clearDataBindings() {
      DATA_BINDINGS.clear();
   }

   public static String translationKey(String servantId, int slot) {
      SlotBinding binding = binding(servantId, slot);
      return binding == null ? "" : binding.translationKey;
   }

   public static ExecutionResult executeSlot(net.minecraft.server.level.ServerPlayer player, String servantId, int slot, boolean crouching, long gameTick) {
      SlotBinding binding = binding(servantId, slot);
      return binding == null ? ExecutionResult.NOT_HANDLED : execute(binding.actionId, new CardActionContext(player, servantId, slot, crouching, gameTick));
   }

   public static String actionIdForSlot(String servantId, int slot) {
      SlotBinding binding = binding(servantId, slot);
      return binding == null ? "" : binding.actionId;
   }

   public static ExecutionResult execute(String id, CardActionContext context) {
      Entry entry = ACTIONS.get(id);
      if (entry == null) return ExecutionResult.NOT_HANDLED;
      try {
         ExecutionResult result = entry.executor.execute(context);
         return result == null ? ExecutionResult.FAILED : result;
      } catch (Exception ignored) {
         return ExecutionResult.FAILED;
      }
   }

   public static boolean contains(String id) { return ACTIONS.containsKey(id); }
   public static Map<String, String> snapshotBindings() {
      Map<String, String> result = new java.util.LinkedHashMap<>();
      SLOT_BINDINGS.forEach((key, value) -> result.put(key, value.actionId));
      DATA_BINDINGS.forEach((key, value) -> result.putIfAbsent(key, value.actionId));
      return java.util.Collections.unmodifiableMap(result);
   }
   private static SlotBinding binding(String servantId, int slot) {
      String key = servantId + "#" + slot;
      SlotBinding binding = SLOT_BINDINGS.get(key);
      return binding != null ? binding : DATA_BINDINGS.get(key);
   }
   private record Entry(CardActionExecutor executor, String provider) { }
   private record SlotBinding(String actionId, String translationKey) { }
}
