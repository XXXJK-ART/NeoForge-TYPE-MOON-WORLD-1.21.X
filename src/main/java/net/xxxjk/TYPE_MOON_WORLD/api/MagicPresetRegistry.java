package net.xxxjk.TYPE_MOON_WORLD.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.typemoonworld.api.MagicPresetHandler;

public final class MagicPresetRegistry {
   private static final Map<String, MagicPresetHandler> HANDLERS = new ConcurrentHashMap<>();
   private static final AtomicBoolean FROZEN = new AtomicBoolean(false);
   private MagicPresetRegistry() { }
   public static void freeze() { FROZEN.set(true); }
   public static boolean register(ResourceLocation id, MagicPresetHandler handler, String provider) {
      if (FROZEN.get() || id == null || handler == null) return false;
      return HANDLERS.putIfAbsent(id.toString(), handler) == null;
   }
   public static CompoundResult normalize(String id, net.minecraft.nbt.CompoundTag payload) {
      MagicPresetHandler handler = HANDLERS.get(id);
      net.minecraft.nbt.CompoundTag incoming = payload == null ? new net.minecraft.nbt.CompoundTag() : payload.copy();
      // Presets arrive from clients and are persisted in wheel/crest data. Keep a hard
      // upper bound before invoking addon code, including when no custom handler exists.
      if (incoming.getAllKeys().size() > 64 || incoming.toString().length() > 8192) incoming = new net.minecraft.nbt.CompoundTag();
      net.minecraft.nbt.CompoundTag normalized;
      try {
         normalized = handler == null ? incoming : handler.normalize(incoming);
      } catch (RuntimeException ex) {
         net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.warn("Magic preset handler rejected payload for {}", id, ex);
         normalized = new net.minecraft.nbt.CompoundTag();
      }
      if (normalized != null && (normalized.getAllKeys().size() > 64 || normalized.toString().length() > 8192)) normalized = new net.minecraft.nbt.CompoundTag();
      return new CompoundResult(normalized == null ? new net.minecraft.nbt.CompoundTag() : normalized, handler);
   }
   public record CompoundResult(net.minecraft.nbt.CompoundTag payload, MagicPresetHandler handler) { }
}
