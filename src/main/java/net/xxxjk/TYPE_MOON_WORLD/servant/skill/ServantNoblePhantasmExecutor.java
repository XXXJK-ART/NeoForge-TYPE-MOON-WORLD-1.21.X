package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantNoblePhantasmContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.registry.ServantAddonRegistry;

public final class ServantNoblePhantasmExecutor {
   private ServantNoblePhantasmExecutor() {
   }

   public static ServantExecutionResult activateNp(
      ServantEntity caster,
      LivingEntity target,
      ServantNoblePhantasmDefinition npDef,
      int overChargeLevel
   ) {
      if (npDef == null) {
         return ServantExecutionResult.FAILED;
      }

      double currentMp = caster.getCurrentMp();
      if (currentMp < npDef.mpCost()) {
         return ServantExecutionResult.FAILED;
      }

      ServantExecutionResult addonResult = ServantAddonRegistry.executeNoblePhantasm(
         new ServantNoblePhantasmContext(caster, target, caster.getDefinition(), npDef, overChargeLevel, currentMp)
      );
      if (addonResult.handled()) {
         if (addonResult.success() && addonResult.mpCost() > 0.0) {
            caster.setCurrentMp(Math.max(0.0, currentMp - addonResult.mpCost()));
         }
         return addonResult;
      }

      caster.setCurrentMp(currentMp - npDef.mpCost());

      return ServantExecutionResult.SUCCESS.withMpCost(npDef.mpCost());
   }
}
