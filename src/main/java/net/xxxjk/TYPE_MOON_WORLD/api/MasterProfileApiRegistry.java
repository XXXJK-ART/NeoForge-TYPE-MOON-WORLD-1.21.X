package net.xxxjk.TYPE_MOON_WORLD.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.typemoonworld.api.MasterProfileData;
import net.xxxjk.typemoonworld.api.MasterProfileInitializer;

public final class MasterProfileApiRegistry {
   private static final Map<ResourceLocation, Entry> ENTRIES = new ConcurrentHashMap<>();
   private static final AtomicBoolean FROZEN = new AtomicBoolean(false);
   private MasterProfileApiRegistry() { }
   public static void freeze() { FROZEN.set(true); }
   public static boolean register(MasterProfileData data, MasterProfileInitializer initializer) {
      return !FROZEN.get() && data != null && initializer != null && ENTRIES.putIfAbsent(data.id(), new Entry(data, initializer)) == null;
   }
   public static Entry get(ResourceLocation id) { return id == null ? null : ENTRIES.get(id); }
   public static Set<ResourceLocation> ids() { return Collections.unmodifiableSet(ENTRIES.keySet()); }
   public record Entry(MasterProfileData data, MasterProfileInitializer initializer) { }
}
