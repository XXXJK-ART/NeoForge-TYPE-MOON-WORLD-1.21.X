package net.xxxjk.TYPE_MOON_WORLD.magic.api;

public interface IMagicRegistry {
   boolean register(String var1, IMagicExecutor var2, String var3);

   default boolean register(String magicId, IMagicExecutor executor) {
      return this.register(magicId, executor, "unknown");
   }

   boolean unregister(String var1, String var2);
}
