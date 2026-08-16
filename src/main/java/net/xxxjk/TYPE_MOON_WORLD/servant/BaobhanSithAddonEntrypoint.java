package net.xxxjk.TYPE_MOON_WORLD.servant;

import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.BaobhanSithServantSkills;

public final class BaobhanSithAddonEntrypoint implements IServantAddonEntrypoint {
   @Override
   public String providerId() {
      return "typemoonworld_core";
   }

   @Override
   public void registerServants(IServantAddonRegistry registry) {
      BaobhanSithServantSkills.registerCombatActions(registry);
   }
}
