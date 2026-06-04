package net.xxxjk.TYPE_MOON_WORLD.servant.api;

public interface IServantAddonEntrypoint {
   default String providerId() {
      return this.getClass().getName();
   }

   default void registerSkills(IServantSkillRegistry registry) {
   }

   default void registerServants(IServantAddonRegistry registry) {
   }
}
