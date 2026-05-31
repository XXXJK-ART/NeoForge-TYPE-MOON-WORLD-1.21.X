package net.xxxjk.TYPE_MOON_WORLD.magic.api;

public interface IMagicAddonEntrypoint {
   void registerMagics(IMagicRegistry var1);

   default String providerId() {
      return this.getClass().getName();
   }
}
