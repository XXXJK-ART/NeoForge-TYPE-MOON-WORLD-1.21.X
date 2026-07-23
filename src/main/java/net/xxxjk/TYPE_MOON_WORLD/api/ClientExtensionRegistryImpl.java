package net.xxxjk.TYPE_MOON_WORLD.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.typemoonworld.api.MagicOptionsExtension;

public final class ClientExtensionRegistryImpl {
   private static final Map<String, MagicOptionsExtension> EXTENSIONS = new ConcurrentHashMap<>();
   private static final AtomicBoolean FROZEN = new AtomicBoolean(false);
   private ClientExtensionRegistryImpl() { }
   public static void freeze() { FROZEN.set(true); }
   public static boolean register(ResourceLocation id, MagicOptionsExtension extension, String provider) {
      return !FROZEN.get() && EXTENSIONS.putIfAbsent(id.toString(), extension) == null;
   }
   public static MagicOptionsExtension get(ResourceLocation id) { return id == null ? null : EXTENSIONS.get(id.toString()); }
}
