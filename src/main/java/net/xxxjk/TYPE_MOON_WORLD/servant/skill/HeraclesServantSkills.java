package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CommonSkillService;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.GodHandService;

public final class HeraclesServantSkills {
   private HeraclesServantSkills() {}
   public static void registerBuiltin(ServantSkillRegistry registry) {
      registry.register("god_hand_passive",context->{ GodHandService.initialize(context.caster()); return ServantExecutionResult.SUCCESS; },"typemoonworld_core");
      for(String id:new String[]{"mad_enhancement_b","false_mind_eye_b","divinity_a"})
         registry.register(id,context->{ CommonSkillService.reconcile(context.caster()); return ServantExecutionResult.SUCCESS; },"typemoonworld_core");
   }
}
