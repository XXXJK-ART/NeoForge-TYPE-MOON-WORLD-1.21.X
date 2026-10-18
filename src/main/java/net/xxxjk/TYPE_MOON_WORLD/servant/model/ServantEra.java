package net.xxxjk.TYPE_MOON_WORLD.servant.model;

import java.util.Locale;

public enum ServantEra {
   MYTHOLOGY, ANCIENT, AGE_OF_SAIL, EARLY_MODERN, MODERN, FUTURE;

   public String key() { return name().toLowerCase(Locale.ROOT); }
   public String translationKey() { return "servant.typemoonworld.era." + key(); }
   public static ServantEra lookup(String id) {
      var definition = net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry.get(id);
      if (definition != null) return definition.era();
      try {
         var snapshot = net.xxxjk.typemoonworld.api.DefinitionSnapshotStore.current();
         var eras = com.google.gson.JsonParser.parseString(snapshot.sections().getOrDefault("servant_eras", "{}")).getAsJsonObject();
         var value = eras.get(id);
         if (value == null) value = eras.get("typemoonworld:" + id);
         if (value != null) return fromKey(value.getAsString());
      } catch (RuntimeException ignored) { }
      return MODERN;
   }
   public static ServantEra fromKey(String key) {
      try { return valueOf(key.toUpperCase(Locale.ROOT)); }
      catch (RuntimeException ignored) { return MODERN; }
   }
}
