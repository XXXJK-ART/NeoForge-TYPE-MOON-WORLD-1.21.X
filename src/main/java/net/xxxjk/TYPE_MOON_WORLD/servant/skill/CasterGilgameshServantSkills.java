package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;

public final class CasterGilgameshServantSkills {
   private CasterGilgameshServantSkills() {}

   public static void registerBuiltin(ServantSkillRegistry registry) {
      registry.register("caster_gilgamesh_wand_dominion", CasterGilgameshServantSkills::wandDominion, "typemoonworld_core");
      registry.register("territory_creation_a_caster_gilgamesh", context -> ServantExecutionResult.SUCCESS, "typemoonworld_core");
      registry.register("item_creation_fake_a", context -> ServantExecutionResult.SUCCESS, "typemoonworld_core");
   }

   private static ServantExecutionResult wandDominion(ServantExecutionContext context) {
      if (context.caster() == null) return ServantExecutionResult.FAILED;
      context.caster().getPersistentData().putBoolean("CasterWandDominionActive", true);
      context.caster().getPersistentData().putFloat("CasterWandDominionMultiplier", 1.20F);
      return ServantExecutionResult.SUCCESS;
   }
}
