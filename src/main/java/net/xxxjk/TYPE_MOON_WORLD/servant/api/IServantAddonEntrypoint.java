package net.xxxjk.TYPE_MOON_WORLD.servant.api;

public interface IServantAddonEntrypoint {
   String providerId();

   void registerSkills(IServantSkillRegistry registry);
}
