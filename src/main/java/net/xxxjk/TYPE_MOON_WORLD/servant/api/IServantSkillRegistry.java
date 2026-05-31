package net.xxxjk.TYPE_MOON_WORLD.servant.api;

public interface IServantSkillRegistry {
   boolean register(String skillId, IServantSkillExecutor executor, String providerId);

   boolean unregister(String skillId, String providerId);
}
