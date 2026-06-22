package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

/**
 * Backwards-compatible facade.
 */
public final class BuiltinServantSkills {
   private BuiltinServantSkills() {
   }

   public static void registerBuiltin(ServantSkillRegistry registry) {
      CommonServantSkills.registerBuiltin(registry);
      HeraclesServantSkills.registerBuiltin(registry);
      SasakiKojiroServantSkills.registerBuiltin(registry);
      LiShuwenServantSkills.registerBuiltin(registry);
   }
}
